package org.apache.archiva.admin.repository.managed;
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.archiva.admin.model.AuditInformation;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.admin.repository.AbstractRepositoryAdmin;
import org.apache.archiva.audit.AuditEvent;
import org.apache.archiva.common.plexusbridge.MavenIndexerUtils;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridgeException;
import org.apache.archiva.configuration.Configuration;
import org.apache.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.archiva.configuration.RepositoryGroupConfiguration;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.MetadataRepositoryException;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.archiva.metadata.repository.RepositorySessionFactory;
import org.apache.archiva.metadata.repository.stats.RepositoryStatisticsManager;
import org.apache.archiva.redback.components.taskqueue.TaskQueueException;
import org.apache.archiva.redback.role.RoleManager;
import org.apache.archiva.redback.role.RoleManagerException;
import org.apache.archiva.scheduler.repository.RepositoryArchivaTaskScheduler;
import org.apache.archiva.scheduler.repository.RepositoryTask;
import org.apache.archiva.security.common.ArchivaRoleConstants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.index.NexusIndexer;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.context.UnsupportedExistingLuceneIndexException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FIXME remove all generic Exception to have usefull ones
 * FIXME review the staging mechanism to have a per user session one
 *
 * @author Olivier Lamy
 */
@Service ("managedRepositoryAdmin#default")
public class DefaultManagedRepositoryAdmin
    extends AbstractRepositoryAdmin
    implements ManagedRepositoryAdmin
{

    private Logger log = LoggerFactory.getLogger( getClass() );

    public static final String STAGE_REPO_ID_END = "-stage";

    @Inject
    @Named (value = "archivaTaskScheduler#repository")
    private RepositoryArchivaTaskScheduler repositoryTaskScheduler;

    @Inject
    private RepositorySessionFactory repositorySessionFactory;

    @Inject
    private RepositoryStatisticsManager repositoryStatisticsManager;

    @Inject
    private PlexusSisuBridge plexusSisuBridge;

    @Inject
    private MavenIndexerUtils mavenIndexerUtils;

    @Inject
    protected RoleManager roleManager;

    // fields
    List<? extends IndexCreator> indexCreators;

    NexusIndexer indexer;

    @PostConstruct
    public void initialize()
        throws RepositoryAdminException
    {
        try
        {
            indexCreators = mavenIndexerUtils.getAllIndexCreators();
            indexer = plexusSisuBridge.lookup( NexusIndexer.class );
        }
        catch ( PlexusSisuBridgeException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
        // initialize index context on start
        for ( ManagedRepository managedRepository : getManagedRepositories() )
        {
            createIndexContext( managedRepository );
        }
    }

    @PreDestroy
    public void shutdown()
        throws RepositoryAdminException
    {
        try
        {
            // close index on shutdown
            for ( ManagedRepository managedRepository : getManagedRepositories() )
            {
                IndexingContext context = indexer.getIndexingContexts().get( managedRepository.getId() );
                if ( context != null )
                {
                    indexer.removeIndexingContext( context, false );
                }
            }
        }
        catch ( IOException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
    }

    public List<ManagedRepository> getManagedRepositories()
        throws RepositoryAdminException
    {
        List<ManagedRepositoryConfiguration> managedRepoConfigs =
            getArchivaConfiguration().getConfiguration().getManagedRepositories();

        if ( managedRepoConfigs == null )
        {
            return Collections.emptyList();
        }

        List<ManagedRepository> managedRepos = new ArrayList<ManagedRepository>( managedRepoConfigs.size() );

        for ( ManagedRepositoryConfiguration repoConfig : managedRepoConfigs )
        {
            // TODO add staging repo information back too
            ManagedRepository repo =
                new ManagedRepository( repoConfig.getId(), repoConfig.getName(), repoConfig.getLocation(),
                                       repoConfig.getLayout(), repoConfig.isSnapshots(), repoConfig.isReleases(),
                                       repoConfig.isBlockRedeployments(), repoConfig.getRefreshCronExpression(),
                                       repoConfig.getIndexDir(), repoConfig.isScanned(), repoConfig.getDaysOlder(),
                                       repoConfig.getRetentionCount(), repoConfig.isDeleteReleasedSnapshots(), false );
            repo.setDescription( repoConfig.getDescription() );
            repo.setSkipPackedIndexCreation( repoConfig.isSkipPackedIndexCreation() );
            managedRepos.add( repo );
        }

        return managedRepos;
    }

    public Map<String, ManagedRepository> getManagedRepositoriesAsMap()
        throws RepositoryAdminException
    {
        List<ManagedRepository> managedRepositories = getManagedRepositories();
        Map<String, ManagedRepository> repositoriesMap =
            new HashMap<String, ManagedRepository>( managedRepositories.size() );
        for ( ManagedRepository managedRepository : managedRepositories )
        {
            repositoriesMap.put( managedRepository.getId(), managedRepository );
        }
        return repositoriesMap;
    }

    public ManagedRepository getManagedRepository( String repositoryId )
        throws RepositoryAdminException
    {
        List<ManagedRepository> repos = getManagedRepositories();
        for ( ManagedRepository repo : repos )
        {
            if ( StringUtils.equals( repo.getId(), repositoryId ) )
            {
                return repo;
            }
        }
        return null;
    }

    public Boolean addManagedRepository( ManagedRepository managedRepository, boolean needStageRepo,
                                         AuditInformation auditInformation )
        throws RepositoryAdminException
    {

        getRepositoryCommonValidator().basicValidation( managedRepository, false );
        getRepositoryCommonValidator().validateManagedRepository( managedRepository );
        triggerAuditEvent( managedRepository.getId(), null, AuditEvent.ADD_MANAGED_REPO, auditInformation );
        Boolean res =
            addManagedRepository( managedRepository.getId(), managedRepository.getLayout(), managedRepository.getName(),
                                  managedRepository.getLocation(), managedRepository.isBlockRedeployments(),
                                  managedRepository.isReleases(), managedRepository.isSnapshots(), needStageRepo,
                                  managedRepository.getCronExpression(), managedRepository.getIndexDirectory(),
                                  managedRepository.getDaysOlder(), managedRepository.getRetentionCount(),
                                  managedRepository.isDeleteReleasedSnapshots(), managedRepository.getDescription(),
                                  managedRepository.isSkipPackedIndexCreation(), auditInformation,
                                  getArchivaConfiguration().getConfiguration() ) != null;

        createIndexContext( managedRepository );
        return res;

    }

    private ManagedRepositoryConfiguration addManagedRepository( String repoId, String layout, String name,
                                                                 String location, boolean blockRedeployments,
                                                                 boolean releasesIncluded, boolean snapshotsIncluded,
                                                                 boolean stageRepoNeeded, String cronExpression,
                                                                 String indexDir, int daysOlder, int retentionCount,
                                                                 boolean deteleReleasedSnapshots, String description,
                                                                 boolean skipPackedIndexCreation,
                                                                 AuditInformation auditInformation,
                                                                 Configuration config )
        throws RepositoryAdminException
    {

        ManagedRepositoryConfiguration repository = new ManagedRepositoryConfiguration();

        repository.setId( repoId );
        repository.setBlockRedeployments( blockRedeployments );
        repository.setReleases( releasesIncluded );
        repository.setSnapshots( snapshotsIncluded );
        repository.setName( name );
        repository.setLocation( getRepositoryCommonValidator().removeExpressions( location ) );
        repository.setLayout( layout );
        repository.setRefreshCronExpression( cronExpression );
        repository.setIndexDir( indexDir );
        repository.setDaysOlder( daysOlder );
        repository.setRetentionCount( retentionCount );
        repository.setDeleteReleasedSnapshots( deteleReleasedSnapshots );
        repository.setIndexDir( indexDir );
        repository.setDescription( description );
        repository.setSkipPackedIndexCreation( skipPackedIndexCreation );

        try
        {
            addRepository( repository, config );
            addRepositoryRoles( repository );

            if ( stageRepoNeeded )
            {
                ManagedRepositoryConfiguration stagingRepository = getStageRepoConfig( repository );
                addRepository( stagingRepository, config );
                addRepositoryRoles( stagingRepository );
                triggerAuditEvent( stagingRepository.getId(), null, AuditEvent.ADD_MANAGED_REPO, auditInformation );
            }
        }
        catch ( RoleManagerException e )
        {
            throw new RepositoryAdminException( "failed to add repository roles " + e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RepositoryAdminException( "failed to add repository " + e.getMessage(), e );
        }

        saveConfiguration( config );

        //MRM-1342 Repository statistics report doesn't appear to be working correctly
        //scan repository when adding of repository is successful
        try
        {
            scanRepository( repoId, true );
            // olamy no need of scanning staged repo
            /*
            if ( stageRepoNeeded )
            {
                ManagedRepositoryConfiguration stagingRepository = getStageRepoConfig( repository );
                scanRepository( stagingRepository.getId(), true );
            }*/
        }
        catch ( Exception e )
        {
            log.warn( new StringBuilder( "Unable to scan repository [" ).append( repoId ).append( "]: " ).append(
                e.getMessage() ).toString(), e );
        }

        return repository;
    }

    public Boolean deleteManagedRepository( String repositoryId, AuditInformation auditInformation,
                                            boolean deleteContent )
        throws RepositoryAdminException
    {
        Configuration config = getArchivaConfiguration().getConfiguration();

        ManagedRepositoryConfiguration repository = config.findManagedRepositoryById( repositoryId );

        if ( repository == null )
        {
            throw new RepositoryAdminException( "A repository with that id does not exist" );
        }

        triggerAuditEvent( repositoryId, null, AuditEvent.DELETE_MANAGED_REPO, auditInformation );

        deleteManagedRepository( repository, deleteContent, config, false );

        // stage repo exists ?
        ManagedRepositoryConfiguration stagingRepository =
            getArchivaConfiguration().getConfiguration().findManagedRepositoryById( repositoryId + STAGE_REPO_ID_END );
        if ( stagingRepository != null )
        {
            // do not trigger event when deleting the staged one
            deleteManagedRepository( stagingRepository, deleteContent, config, true );
        }

        try
        {
            saveConfiguration( config );
        }
        catch ( Exception e )
        {
            throw new RepositoryAdminException( "Error saving configuration for delete action" + e.getMessage() );
        }

        return Boolean.TRUE;
    }

    private Boolean deleteManagedRepository( ManagedRepositoryConfiguration repository, boolean deleteContent,
                                             Configuration config, boolean stagedOne )
        throws RepositoryAdminException
    {

        try
        {
            NexusIndexer nexusIndexer = plexusSisuBridge.lookup( NexusIndexer.class );

            IndexingContext context = nexusIndexer.getIndexingContexts().get( repository.getId() );
            if ( context != null )
            {
                // delete content only if directory exists
                nexusIndexer.removeIndexingContext( context,
                                                    deleteContent && context.getIndexDirectoryFile().exists() );
            }
        }
        catch ( PlexusSisuBridgeException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
        if ( !stagedOne )
        {
            RepositorySession repositorySession = getRepositorySessionFactory().createSession();
            try
            {
                MetadataRepository metadataRepository = repositorySession.getRepository();
                metadataRepository.removeRepository( repository.getId() );
                log.debug( "call repositoryStatisticsManager.deleteStatistics" );
                getRepositoryStatisticsManager().deleteStatistics( metadataRepository, repository.getId() );
                repositorySession.save();
            }
            catch ( MetadataRepositoryException e )
            {
                //throw new RepositoryAdminException( e.getMessage(), e );
                log.warn( "skip error during removing repository from MetadatRepository:" + e.getMessage(), e );
            }
            finally
            {
                repositorySession.close();
            }
        }
        config.removeManagedRepository( repository );

        if ( deleteContent )
        {
            // TODO could be async ? as directory can be huge
            File dir = new File( repository.getLocation() );
            if ( !FileUtils.deleteQuietly( dir ) )
            {
                throw new RepositoryAdminException( "Cannot delete repository " + dir );
            }
        }

        // olamy: copy list for reading as a unit test in webapp fail with ConcurrentModificationException
        List<ProxyConnectorConfiguration> proxyConnectors =
            new ArrayList<ProxyConnectorConfiguration>( config.getProxyConnectors() );
        for ( ProxyConnectorConfiguration proxyConnector : proxyConnectors )
        {
            if ( StringUtils.equals( proxyConnector.getSourceRepoId(), repository.getId() ) )
            {
                config.removeProxyConnector( proxyConnector );
            }
        }

        Map<String, List<String>> repoToGroupMap = config.getRepositoryToGroupMap();
        if ( repoToGroupMap != null )
        {
            if ( repoToGroupMap.containsKey( repository.getId() ) )
            {
                List<String> repoGroups = repoToGroupMap.get( repository.getId() );
                for ( String repoGroup : repoGroups )
                {
                    // copy to prevent UnsupportedOperationException
                    RepositoryGroupConfiguration repositoryGroupConfiguration =
                        config.findRepositoryGroupById( repoGroup );
                    List<String> repos = new ArrayList<String>( repositoryGroupConfiguration.getRepositories() );
                    config.removeRepositoryGroup( repositoryGroupConfiguration );
                    repos.remove( repository.getId() );
                    repositoryGroupConfiguration.setRepositories( repos );
                    config.addRepositoryGroup( repositoryGroupConfiguration );
                }
            }
        }

        try
        {
            removeRepositoryRoles( repository );
        }
        catch ( RoleManagerException e )
        {
            throw new RepositoryAdminException(
                "fail to remove repository roles for repository " + repository.getId() + " : " + e.getMessage(), e );
        }

        saveConfiguration( config );

        return Boolean.TRUE;
    }


    public Boolean updateManagedRepository( ManagedRepository managedRepository, boolean needStageRepo,
                                            AuditInformation auditInformation, boolean resetStats )
        throws RepositoryAdminException
    {

        log.debug( "updateManagedConfiguration repo {} needStage {} resetStats {} ",
                   Arrays.asList( managedRepository, needStageRepo, resetStats ).toArray() );

        // Ensure that the fields are valid.

        getRepositoryCommonValidator().basicValidation( managedRepository, true );

        getRepositoryCommonValidator().validateManagedRepository( managedRepository );

        Configuration configuration = getArchivaConfiguration().getConfiguration();

        ManagedRepositoryConfiguration toremove = configuration.findManagedRepositoryById( managedRepository.getId() );

        if ( toremove != null )
        {
            configuration.removeManagedRepository( toremove );
        }

        ManagedRepositoryConfiguration stagingRepository = getStageRepoConfig( toremove );

        // TODO remove content from old if path has changed !!!!!

        if ( stagingRepository != null )
        {
            configuration.removeManagedRepository( stagingRepository );
        }

        ManagedRepositoryConfiguration managedRepositoryConfiguration =
            addManagedRepository( managedRepository.getId(), managedRepository.getLayout(), managedRepository.getName(),
                                  managedRepository.getLocation(), managedRepository.isBlockRedeployments(),
                                  managedRepository.isReleases(), managedRepository.isSnapshots(), needStageRepo,
                                  managedRepository.getCronExpression(), managedRepository.getIndexDirectory(),
                                  managedRepository.getDaysOlder(), managedRepository.getRetentionCount(),
                                  managedRepository.isDeleteReleasedSnapshots(), managedRepository.getDescription(),
                                  managedRepository.isSkipPackedIndexCreation(), auditInformation,
                                  getArchivaConfiguration().getConfiguration() );

        // Save the repository configuration.
        RepositorySession repositorySession = getRepositorySessionFactory().createSession();

        try
        {
            triggerAuditEvent( managedRepositoryConfiguration.getId(), null, AuditEvent.MODIFY_MANAGED_REPO,
                               auditInformation );

            saveConfiguration( this.getArchivaConfiguration().getConfiguration() );
            if ( resetStats )
            {
                log.debug( "call repositoryStatisticsManager.deleteStatistics" );
                getRepositoryStatisticsManager().deleteStatistics( repositorySession.getRepository(),
                                                                   managedRepositoryConfiguration.getId() );
                repositorySession.save();
            }

        }
        catch ( MetadataRepositoryException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
        finally
        {
            repositorySession.close();
        }
        createIndexContext( managedRepository );
        return true;
    }

    //--------------------------
    // utils methods
    //--------------------------


    protected void addRepository( ManagedRepositoryConfiguration repository, Configuration configuration )
        throws RepositoryAdminException, IOException
    {
        // Normalize the path
        File file = new File( repository.getLocation() );
        if ( !file.isAbsolute() )
        {
            // add appserver.base/repositories
            file = new File( getRegistry().getString( "appserver.base" ) + File.separatorChar + "repositories",
                             repository.getLocation() );
        }
        repository.setLocation( file.getCanonicalPath() );
        if ( !file.exists() )
        {
            file.mkdirs();
        }
        if ( !file.exists() || !file.isDirectory() )
        {
            throw new RepositoryAdminException(
                "Unable to add repository - no write access, can not create the root directory: " + file );
        }

        configuration.addManagedRepository( repository );

    }

    public IndexingContext createIndexContext( ManagedRepository repository )
        throws RepositoryAdminException
    {
        try
        {

            IndexingContext context = indexer.getIndexingContexts().get( repository.getId() );

            if ( context != null )
            {
                log.debug( "skip adding repository indexingContent with id {} as already exists", repository.getId() );
                return context;
            }

            String indexDir = repository.getIndexDirectory();
            File managedRepository = new File( repository.getLocation() );

            File indexDirectory = null;
            if ( indexDir != null && !"".equals( indexDir ) )
            {
                indexDirectory = new File( repository.getIndexDirectory() );
                if ( !indexDirectory.isAbsolute() )
                {
                    indexDirectory = new File( managedRepository, repository.getIndexDirectory() );
                    repository.setIndexDirectory( indexDirectory.getAbsolutePath() );
                }
            }
            else
            {
                indexDirectory = new File( managedRepository, ".indexer" );
                if ( !managedRepository.isAbsolute() )
                {
                    indexDirectory = new File(
                        getRegistry().getString( "appserver.base" ) + File.separatorChar + "repositories"
                            + File.separatorChar +
                            repository.getLocation(), ".indexer" );
                    repository.setIndexDirectory( indexDirectory.getAbsolutePath() );
                }
            }

            if ( !indexDirectory.exists() )
            {
                indexDirectory.mkdirs();
            }

            context = indexer.getIndexingContexts().get( repository.getId() );

            if ( context == null )
            {
                context = indexer.addIndexingContext( repository.getId(), repository.getId(), managedRepository,
                                                      indexDirectory,
                                                      managedRepository.toURI().toURL().toExternalForm(),
                                                      indexDirectory.toURI().toURL().toString(), indexCreators );

                context.setSearchable( repository.isScanned() );
            }
            return context;
        }
        catch ( MalformedURLException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
        catch ( IOException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
        catch ( UnsupportedExistingLuceneIndexException e )
        {
            throw new RepositoryAdminException( e.getMessage(), e );
        }
    }

    private ManagedRepositoryConfiguration getStageRepoConfig( ManagedRepositoryConfiguration repository )
    {
        ManagedRepositoryConfiguration stagingRepository = new ManagedRepositoryConfiguration();
        stagingRepository.setId( repository.getId() + STAGE_REPO_ID_END );
        stagingRepository.setLayout( repository.getLayout() );
        stagingRepository.setName( repository.getName() + STAGE_REPO_ID_END );
        stagingRepository.setBlockRedeployments( repository.isBlockRedeployments() );
        stagingRepository.setDaysOlder( repository.getDaysOlder() );
        stagingRepository.setDeleteReleasedSnapshots( repository.isDeleteReleasedSnapshots() );
        stagingRepository.setIndexDir( repository.getIndexDir() );
        String path = repository.getLocation();
        int lastIndex = path.replace( '\\', '/' ).lastIndexOf( '/' );
        stagingRepository.setLocation( path.substring( 0, lastIndex ) + "/" + stagingRepository.getId() );
        stagingRepository.setRefreshCronExpression( repository.getRefreshCronExpression() );
        stagingRepository.setReleases( repository.isReleases() );
        stagingRepository.setRetentionCount( repository.getRetentionCount() );
        stagingRepository.setScanned( repository.isScanned() );
        stagingRepository.setSnapshots( repository.isSnapshots() );
        stagingRepository.setSkipPackedIndexCreation( repository.isSkipPackedIndexCreation() );
        // do not duplicate description
        //stagingRepository.getDescription("")
        return stagingRepository;
    }

    public Boolean scanRepository( String repositoryId, boolean fullScan )
    {
        if ( getRepositoryTaskScheduler().isProcessingRepositoryTask( repositoryId ) )
        {
            log.info( "scanning of repository with id {} already scheduled", repositoryId );
        }
        RepositoryTask task = new RepositoryTask();
        task.setRepositoryId( repositoryId );
        task.setScanAll( fullScan );
        try
        {
            getRepositoryTaskScheduler().queueTask( task );
        }
        catch ( TaskQueueException e )
        {
            log.error( "failed to schedule scanning of repo with id {}", repositoryId, e );
            return false;
        }
        return true;
    }

    protected void addRepositoryRoles( ManagedRepositoryConfiguration newRepository )
        throws RoleManagerException
    {
        String repoId = newRepository.getId();

        // TODO: double check these are configured on start up
        // TODO: belongs in the business logic

        if ( !getRoleManager().templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId ) )
        {
            getRoleManager().createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId );
        }

        if ( !getRoleManager().templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId ) )
        {
            getRoleManager().createTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId );
        }
    }

    protected void removeRepositoryRoles( ManagedRepositoryConfiguration existingRepository )
        throws RoleManagerException
    {
        String repoId = existingRepository.getId();

        if ( getRoleManager().templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId ) )
        {
            getRoleManager().removeTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_MANAGER, repoId );
        }

        if ( getRoleManager().templatedRoleExists( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId ) )
        {
            getRoleManager().removeTemplatedRole( ArchivaRoleConstants.TEMPLATE_REPOSITORY_OBSERVER, repoId );
        }

        log.debug( "removed user roles associated with repository {}", repoId );
    }

    //--------------------------
    // setters/getters
    //--------------------------


    public RoleManager getRoleManager()
    {
        return roleManager;
    }

    public void setRoleManager( RoleManager roleManager )
    {
        this.roleManager = roleManager;
    }

    public RepositoryStatisticsManager getRepositoryStatisticsManager()
    {
        return repositoryStatisticsManager;
    }

    public void setRepositoryStatisticsManager( RepositoryStatisticsManager repositoryStatisticsManager )
    {
        this.repositoryStatisticsManager = repositoryStatisticsManager;
    }

    public RepositorySessionFactory getRepositorySessionFactory()
    {
        return repositorySessionFactory;
    }

    public void setRepositorySessionFactory( RepositorySessionFactory repositorySessionFactory )
    {
        this.repositorySessionFactory = repositorySessionFactory;
    }


    public RepositoryArchivaTaskScheduler getRepositoryTaskScheduler()
    {
        return repositoryTaskScheduler;
    }

    public void setRepositoryTaskScheduler( RepositoryArchivaTaskScheduler repositoryTaskScheduler )
    {
        this.repositoryTaskScheduler = repositoryTaskScheduler;
    }

    public PlexusSisuBridge getPlexusSisuBridge()
    {
        return plexusSisuBridge;
    }

    public void setPlexusSisuBridge( PlexusSisuBridge plexusSisuBridge )
    {
        this.plexusSisuBridge = plexusSisuBridge;
    }

    public MavenIndexerUtils getMavenIndexerUtils()
    {
        return mavenIndexerUtils;
    }

    public void setMavenIndexerUtils( MavenIndexerUtils mavenIndexerUtils )
    {
        this.mavenIndexerUtils = mavenIndexerUtils;
    }
}
