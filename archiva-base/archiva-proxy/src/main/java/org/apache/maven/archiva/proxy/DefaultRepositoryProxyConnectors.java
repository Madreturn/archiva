package org.apache.maven.archiva.proxy;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.configuration.ConfigurationNames;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.configuration.NetworkProxyConfiguration;
import org.apache.maven.archiva.configuration.ProxyConnectorConfiguration;
import org.apache.maven.archiva.configuration.RemoteRepositoryConfiguration;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.apache.maven.archiva.model.ArtifactReference;
import org.apache.maven.archiva.model.ProjectReference;
import org.apache.maven.archiva.model.VersionedReference;
import org.apache.maven.archiva.policies.DownloadPolicy;
import org.apache.maven.archiva.policies.urlcache.UrlFailureCache;
import org.apache.maven.archiva.repository.ArchivaConfigurationAdaptor;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayout;
import org.apache.maven.archiva.repository.layout.BidirectionalRepositoryLayoutFactory;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.repository.metadata.MetadataTools;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.maven.archiva.repository.scanner.RepositoryContentConsumers;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.WagonException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.registry.Registry;
import org.codehaus.plexus.registry.RegistryListener;
import org.codehaus.plexus.util.SelectorUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * DefaultRepositoryProxyConnectors
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role-hint="default"
 */
public class DefaultRepositoryProxyConnectors
    extends AbstractLogEnabled
    implements RepositoryProxyConnectors, RegistryListener, Initializable
{
    /**
     * @plexus.requirement
     */
    private ArchivaConfiguration archivaConfiguration;

    /**
     * @plexus.requirement role="org.apache.maven.wagon.Wagon"
     */
    private Map/*<String,Wagon>*/wagons;

    /**
     * @plexus.requirement
     */
    private BidirectionalRepositoryLayoutFactory layoutFactory;

    /**
     * @plexus.requirement
     */
    private MetadataTools metadataTools;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.policies.PreDownloadPolicy"
     */
    private Map preDownloadPolicies;

    /**
     * @plexus.requirement role="org.apache.maven.archiva.policies.PostDownloadPolicy"
     */
    private Map postDownloadPolicies;

    /**
     * @plexus.requirement role-hint="default"
     */
    private UrlFailureCache urlFailureCache;

    private Map proxyConnectorMap = new HashMap();

    private Map networkProxyMap = new HashMap();

    /**
     * @plexus.requirement
     */
    private RepositoryContentConsumers consumers;

    /**
     * Fetch an artifact from a remote repository.
     *
     * @param repository the managed repository to utilize for the request.
     * @param artifact   the artifact reference to fetch.
     * @return the local file in the managed repository that was fetched, or null if the artifact was not (or
     *         could not be) fetched.
     * @throws ProxyException if there was a problem fetching the artifact.
     */
    public File fetchFromProxies( ArchivaRepository repository, ArtifactReference artifact )
        throws ProxyException
    {
        File localFile = toLocalFile( repository, artifact );

        Properties requestProperties = new Properties();
        requestProperties.setProperty( "version", artifact.getVersion() );

        List connectors = getProxyConnectors( repository );
        Iterator it = connectors.iterator();
        while ( it.hasNext() )
        {
            ProxyConnector connector = (ProxyConnector) it.next();
            ArchivaRepository targetRepository = connector.getTargetRepository();
            String targetPath = getLayout( targetRepository ).toPath( artifact );

            File downloadedFile = transferFile( connector, targetRepository, targetPath, localFile, requestProperties );

            if ( fileExists( downloadedFile ) )
            {
                getLogger().info( "Successfully transfered: " + downloadedFile.getAbsolutePath() );
                return downloadedFile;
            }
        }

        return null;
    }

    /**
     * Fetch, from the proxies, a metadata.xml file for the groupId:artifactId:version metadata contents.
     *
     * @return the (local) metadata file that was fetched/merged/updated, or null if no metadata file exists.
     */
    public File fetchFromProxies( ArchivaRepository repository, VersionedReference metadata )
        throws ProxyException
    {
        File localFile = toLocalFile( repository, metadata );

        Properties requestProperties = new Properties();
        boolean hasFetched = false;

        List connectors = getProxyConnectors( repository );
        Iterator it = connectors.iterator();
        while ( it.hasNext() )
        {
            ProxyConnector connector = (ProxyConnector) it.next();
            ArchivaRepository targetRepository = connector.getTargetRepository();
            String targetPath = metadataTools.toPath( metadata );

            File localRepoFile = toLocalRepoFile( repository, targetRepository, targetPath );
            File downloadedFile =
                transferFile( connector, targetRepository, targetPath, localRepoFile, requestProperties );

            if ( fileExists( downloadedFile ) )
            {
                getLogger().info( "Successfully transfered: " + downloadedFile.getAbsolutePath() );
                hasFetched = true;
            }
        }

        if ( hasFetched || fileExists( localFile ) )
        {
            try
            {
                metadataTools.updateMetadata( repository, metadata );
            }
            catch ( LayoutException e )
            {
                getLogger().warn( "Unable to update metadata " + localFile.getAbsolutePath() + ": " + e.getMessage() );
                // TODO: add into repository report?
            }
            catch ( RepositoryMetadataException e )
            {
                getLogger()
                    .warn( "Unable to update metadata " + localFile.getAbsolutePath() + ": " + e.getMessage(), e );
                // TODO: add into repository report?
            }
            catch ( IOException e )
            {
                getLogger()
                    .warn( "Unable to update metadata " + localFile.getAbsolutePath() + ": " + e.getMessage(), e );
                // TODO: add into repository report?
            }
        }

        if ( fileExists( localFile ) )
        {
            return localFile;
        }

        return null;
    }

    /**
     * Fetch from the proxies a metadata.xml file for the groupId:artifactId metadata contents.
     *
     * @return the (local) metadata file that was fetched/merged/updated, or null if no metadata file exists.
     */
    public File fetchFromProxies( ArchivaRepository repository, ProjectReference metadata )
        throws ProxyException
    {
        File localFile = toLocalFile( repository, metadata );

        Properties requestProperties = new Properties();
        boolean hasFetched = false;

        List connectors = getProxyConnectors( repository );
        Iterator it = connectors.iterator();
        while ( it.hasNext() )
        {
            ProxyConnector connector = (ProxyConnector) it.next();
            ArchivaRepository targetRepository = connector.getTargetRepository();
            String targetPath = metadataTools.toPath( metadata );

            File localRepoFile = toLocalRepoFile( repository, targetRepository, targetPath );
            File downloadedFile =
                transferFile( connector, targetRepository, targetPath, localRepoFile, requestProperties );

            if ( fileExists( downloadedFile ) )
            {
                getLogger().info( "Successfully transfered: " + downloadedFile.getAbsolutePath() );
                hasFetched = true;
            }
        }

        if ( hasFetched || fileExists( localFile ) )
        {
            try
            {
                metadataTools.updateMetadata( repository, metadata );
            }
            catch ( LayoutException e )
            {
                getLogger().warn( "Unable to update metadata " + localFile.getAbsolutePath() + ": " + e.getMessage() );
                // TODO: add into repository report?
            }
            catch ( RepositoryMetadataException e )
            {
                getLogger()
                    .warn( "Unable to update metadata " + localFile.getAbsolutePath() + ": " + e.getMessage(), e );
                // TODO: add into repository report?
            }
            catch ( IOException e )
            {
                getLogger()
                    .warn( "Unable to update metadata " + localFile.getAbsolutePath() + ": " + e.getMessage(), e );
                // TODO: add into repository report?
            }
        }

        if ( fileExists( localFile ) )
        {
            return localFile;
        }

        return null;
    }

    private File toLocalRepoFile( ArchivaRepository repository, ArchivaRepository targetRepository, String targetPath )
    {
        String repoPath = metadataTools.getRepositorySpecificName( targetRepository, targetPath );
        return new File( repository.getUrl().getPath(), repoPath );
    }

    /**
     * Test if the provided ArchivaRepository has any proxies configured for it.
     */
    public boolean hasProxies( ArchivaRepository repository )
    {
        synchronized ( this.proxyConnectorMap )
        {
            return this.proxyConnectorMap.containsKey( repository.getId() );
        }
    }

    private File toLocalFile( ArchivaRepository repository, ArtifactReference artifact )
        throws ProxyException
    {
        BidirectionalRepositoryLayout sourceLayout = getLayout( repository );
        String sourcePath = sourceLayout.toPath( artifact );
        return new File( repository.getUrl().getPath(), sourcePath );
    }

    private File toLocalFile( ArchivaRepository repository, ProjectReference metadata )
        throws ProxyException
    {
        String sourcePath = metadataTools.toPath( metadata );
        return new File( repository.getUrl().getPath(), sourcePath );
    }

    private File toLocalFile( ArchivaRepository repository, VersionedReference metadata )
        throws ProxyException
    {
        String sourcePath = metadataTools.toPath( metadata );
        return new File( repository.getUrl().getPath(), sourcePath );
    }

    /**
     * Get the layout for the repository.
     *
     * @param repository the repository to get the layout from.
     * @return the layout
     * @throws ProxyException if there was a problem obtaining the layout from the repository (usually due to a bad
     *                        configuration of the repository)
     */
    private BidirectionalRepositoryLayout getLayout( ArchivaRepository repository )
        throws ProxyException
    {
        try
        {
            return layoutFactory.getLayout( repository.getLayoutType() );
        }
        catch ( LayoutException e )
        {
            throw new ProxyException( "Unable to proxy due to bad repository layout definition [" + repository.getId() +
                "] had a layout defined as [" + repository.getLayoutType() + "] : " + e.getMessage(), e );
        }
    }

    /**
     * Simple method to test if the file exists on the local disk.
     *
     * @param file the file to test. (may be null)
     * @return true if file exists. false if the file param is null, doesn't exist, or is not of type File.
     */
    private boolean fileExists( File file )
    {
        if ( file == null )
        {
            return false;
        }

        if ( !file.exists() )
        {
            return false;
        }

        if ( !file.isFile() )
        {
            return false;
        }

        return true;
    }

    /**
     * Perform the transfer of the file.
     *
     * @param connector         the connector configuration to use.
     * @param remoteRepository  the remote repository get the resource from.
     * @param remotePath        the path in the remote repository to the resource to get.
     * @param localFile         the local file to place the downloaded resource into
     * @param requestProperties the request properties to utilize for policy handling.
     * @return the local file that was downloaded, or null if not downloaded.
     * @throws ProxyException if transfer was unsuccessful.
     */
    private File transferFile( ProxyConnector connector, ArchivaRepository remoteRepository, String remotePath,
                               File localFile, Properties requestProperties )
        throws ProxyException
    {
        String url = remoteRepository.getUrl().toString() + remotePath;
        requestProperties.setProperty( "url", url );

        // Is a whitelist defined?
        if ( !isEmpty( connector.getWhitelist() ) )
        {
            // Path must belong to whitelist.
            if ( !matchesPattern( remotePath, connector.getWhitelist() ) )
            {
                getLogger().debug( "Path [" + remotePath + "] is not part of defined whitelist (skipping transfer)." );
                return null;
            }
        }

        // Is target path part of blacklist?
        if ( matchesPattern( remotePath, connector.getBlacklist() ) )
        {
            getLogger().debug( "Path [" + remotePath + "] is part of blacklist (skipping transfer)." );
            return null;
        }

        // Handle pre-download policy
        if ( !applyPolicies( this.preDownloadPolicies, connector.getPolicies(), requestProperties, localFile ) )
        {
            getLogger().info( "Failed pre-download policies - " + localFile.getAbsolutePath() );

            if ( fileExists( localFile ) )
            {
                return localFile;
            }

            return null;
        }

        Wagon wagon = null;
        try
        {
            String protocol = remoteRepository.getUrl().getProtocol();
            wagon = (Wagon) wagons.get( protocol );
            if ( wagon == null )
            {
                throw new ProxyException( "Unsupported target repository protocol: " + protocol );
            }

            boolean connected = connectToRepository( connector, wagon, remoteRepository );
            if ( connected )
            {
                localFile = transferSimpleFile( wagon, remoteRepository, remotePath, localFile );

                transferChecksum( wagon, remoteRepository, remotePath, localFile, ".sha1" );
                transferChecksum( wagon, remoteRepository, remotePath, localFile, ".md5" );
            }
        }
        catch ( ResourceDoesNotExistException e )
        {
            // Do not cache url here.
            return null;
        }
        catch ( WagonException e )
        {
            urlFailureCache.cacheFailure( url );
            return null;
        }
        finally
        {
            if ( wagon != null )
            {
                try
                {
                    wagon.disconnect();
                }
                catch ( ConnectionException e )
                {
                    getLogger().warn( "Unable to disconnect wagon.", e );
                }
            }
        }

        // Handle post-download policies.
        if ( !applyPolicies( this.postDownloadPolicies, connector.getPolicies(), requestProperties, localFile ) )
        {
            getLogger().info( "Failed post-download policies - " + localFile.getAbsolutePath() );

            if ( fileExists( localFile ) )
            {
                return localFile;
            }

            return null;
        }

        // Just-in-time update of the index and database by executing the consumers for this artifact
        consumers.executeConsumers( connector.getSourceRepository(), localFile );

        // Everything passes.
        return localFile;
    }

    /**
     * Quietly transfer the checksum file from the remote repository to the local file.
     * <p/>
     * NOTE: This will not throw a WagonException if the checksum is unable to be downloaded.
     *
     * @param wagon            the wagon instance (should already be connected) to use.
     * @param remoteRepository the remote repository to transfer from.
     * @param remotePath       the remote path to the resource to get.
     * @param localFile        the local file that should contain the downloaded contents
     * @param type             the type of checksum to transfer (example: ".md5" or ".sha1")
     * @throws ProxyException if copying the downloaded file into place did not succeed.
     */
    private void transferChecksum( Wagon wagon, ArchivaRepository remoteRepository, String remotePath, File localFile,
                                   String type )
        throws ProxyException
    {
        String url = remoteRepository.getUrl().toString() + remotePath;

        // Transfer checksum does not use the policy. 
        if ( urlFailureCache.hasFailedBefore( url + type ) )
        {
            return;
        }

        try
        {
            File hashFile = new File( localFile.getAbsolutePath() + type );
            transferSimpleFile( wagon, remoteRepository, remotePath + type, hashFile );
            getLogger().debug( "Checksum" + type + " Downloaded: " + hashFile );
        }
        catch ( ResourceDoesNotExistException e )
        {
            getLogger().debug( "Checksum" + type + " Not Download: " + e.getMessage() );
        }
        catch ( WagonException e )
        {
            urlFailureCache.cacheFailure( url + type );
            getLogger().warn( "Transfer failed on checksum: " + url + " : " + e.getMessage(), e );
        }
    }

    /**
     * Perform the transfer of the remote file to the local file specified.
     *
     * @param wagon            the wagon instance to use.
     * @param remoteRepository the remote repository to use
     * @param remotePath       the remote path to attempt to get
     * @param localFile        the local file to save to
     * @return The local file that was transfered.
     * @throws ProxyException if there was a problem moving the downloaded file into place.
     * @throws WagonException if there was a problem tranfering the file.
     */
    private File transferSimpleFile( Wagon wagon, ArchivaRepository remoteRepository, String remotePath,
                                     File localFile )
        throws ProxyException, WagonException
    {
        assert ( remotePath != null );

        // Transfer the file.
        File temp = null;

        try
        {
            temp = new File( localFile.getAbsolutePath() + ".tmp" );

            boolean success = false;

            if ( localFile.exists() )
            {
                getLogger().debug( "Retrieving " + remotePath + " from " + remoteRepository.getName() );
                wagon.get( remotePath, temp );
                success = true;

                if ( temp.exists() )
                {
                    moveTempToTarget( temp, localFile );
                }

                // You wouldn't get here on failure, a WagonException would have been thrown.
                getLogger().debug( "Downloaded successfully." );
            }
            else
            {
                getLogger().debug( "Retrieving " + remotePath + " from " + remoteRepository.getName() + " if updated" );
                success = wagon.getIfNewer( remotePath, temp, localFile.lastModified() );
                if ( !success )
                {
                    getLogger().info(
                        "Not downloaded, as local file is newer than remote side: " + localFile.getAbsolutePath() );
                }
                else if ( temp.exists() )
                {
                    getLogger().debug( "Downloaded successfully." );
                    moveTempToTarget( temp, localFile );
                }
            }

            return localFile;
        }
        catch ( ResourceDoesNotExistException e )
        {
            getLogger().warn( "Resource does not exist: " + e.getMessage() );
            throw e;
        }
        catch ( WagonException e )
        {
            getLogger().warn( "Download failure:" + e.getMessage(), e );
            throw e;
        }
        finally
        {
            if ( temp != null )
            {
                temp.delete();
            }
        }
    }

    /**
     * Apply the policies.
     *
     * @param policies  the map of policies to execute. (Map of String policy keys, to {@link DownloadPolicy} objects)
     * @param settings  the map of settings for the policies to execute. (Map of String policy keys, to String policy setting)
     * @param request   the request properties (utilized by the {@link DownloadPolicy#applyPolicy(String,Properties,File)})
     * @param localFile the local file (utilized by the {@link DownloadPolicy#applyPolicy(String,Properties,File)})
     * @return true if all of the policies passed, false if a policy failed.
     */
    private boolean applyPolicies( Map policies, Map settings, Properties request, File localFile )
    {
        Iterator it = policies.entrySet().iterator();
        while ( it.hasNext() )
        {
            Map.Entry entry = (Entry) it.next();
            String key = (String) entry.getKey();
            DownloadPolicy policy = (DownloadPolicy) entry.getValue();
            String defaultSetting = policy.getDefaultOption();
            String setting = StringUtils.defaultString( (String) settings.get( key ), defaultSetting );

            getLogger().debug( "Applying [" + key + "] policy with [" + setting + "]" );
            if ( !policy.applyPolicy( setting, request, localFile ) )
            {
                getLogger().info( "Didn't pass the [" + key + "] policy." );
                return false;
            }
        }
        return true;
    }

    /**
     * Used to move the temporary file to its real destination.  This is patterned from the way WagonManager handles
     * its downloaded files.
     *
     * @param temp   The completed download file
     * @param target The final location of the downloaded file
     * @throws ProxyException when the temp file cannot replace the target file
     */
    private void moveTempToTarget( File temp, File target )
        throws ProxyException
    {
        if ( target.exists() && !target.delete() )
        {
            throw new ProxyException( "Unable to overwrite existing target file: " + target.getAbsolutePath() );
        }

        if ( !temp.renameTo( target ) )
        {
            getLogger().warn( "Unable to rename tmp file to its final name... resorting to copy command." );

            try
            {
                FileUtils.copyFile( temp, target );
            }
            catch ( IOException e )
            {
                throw new ProxyException( "Cannot copy tmp file to its final location", e );
            }
            finally
            {
                temp.delete();
            }
        }
    }

    /**
     * Using wagon, connect to the remote repository.
     *
     * @param connector        the connector configuration to utilize (for obtaining network proxy configuration from)
     * @param wagon            the wagon instance to establish the connection on.
     * @param remoteRepository the remote repository to connect to.
     * @return true if the connection was successful. false if not connected.
     */
    private boolean connectToRepository( ProxyConnector connector, Wagon wagon, ArchivaRepository remoteRepository )
    {
        boolean connected = false;

        ProxyInfo networkProxy = null;
        synchronized ( this.networkProxyMap )
        {
            networkProxy = (ProxyInfo) this.networkProxyMap.get( connector.getProxyId() );
        }

        try
        {
            Repository wagonRepository =
                new Repository( remoteRepository.getId(), remoteRepository.getUrl().toString() );
            if ( networkProxy != null )
            {
                wagon.connect( wagonRepository, networkProxy );
            }
            else
            {
                wagon.connect( wagonRepository );
            }
            connected = true;
        }
        catch ( ConnectionException e )
        {
            getLogger().info( "Could not connect to " + remoteRepository.getName() + ": " + e.getMessage() );
            connected = false;
        }
        catch ( AuthenticationException e )
        {
            getLogger().info( "Could not connect to " + remoteRepository.getName() + ": " + e.getMessage() );
            connected = false;
        }

        return connected;
    }

    /**
     * Tests whitelist and blacklist patterns against path.
     *
     * @param path     the path to test.
     * @param patterns the list of patterns to check.
     * @return true if the path matches at least 1 pattern in the provided patterns list.
     */
    private boolean matchesPattern( String path, List patterns )
    {
        if ( isEmpty( patterns ) )
        {
            return false;
        }

        Iterator it = patterns.iterator();
        while ( it.hasNext() )
        {
            String pattern = (String) it.next();
            if ( SelectorUtils.matchPath( pattern, path, false ) )
            {
                return true;
            }
        }

        return false;
    }

    /**
     * TODO: Ensure that list is correctly ordered based on configuration. See MRM-477
     */
    public List getProxyConnectors( ArchivaRepository repository )
    {
        synchronized ( this.proxyConnectorMap )
        {
            List ret = (List) this.proxyConnectorMap.get( repository.getId() );
            if ( ret == null )
            {
                return Collections.EMPTY_LIST;
            }
            return ret;
        }
    }

    public void afterConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        if ( ConfigurationNames.isNetworkProxy( propertyName ) ||
            ConfigurationNames.isManagedRepositories( propertyName ) ||
            ConfigurationNames.isRemoteRepositories( propertyName ) ||
            ConfigurationNames.isProxyConnector( propertyName ) )
        {
            initConnectorsAndNetworkProxies();
        }
    }

    public void beforeConfigurationChange( Registry registry, String propertyName, Object propertyValue )
    {
        /* do nothing */
    }

    private void initConnectorsAndNetworkProxies()
    {
        Iterator it;

        synchronized ( this.proxyConnectorMap )
        {
            this.proxyConnectorMap.clear();

            List proxyConfigs = archivaConfiguration.getConfiguration().getProxyConnectors();
            it = proxyConfigs.iterator();
            while ( it.hasNext() )
            {
                ProxyConnectorConfiguration proxyConfig = (ProxyConnectorConfiguration) it.next();
                String key = proxyConfig.getSourceRepoId();

                // Create connector object.
                ProxyConnector connector = new ProxyConnector();
                connector.setSourceRepository( getManagedRepository( proxyConfig.getSourceRepoId() ) );
                connector.setTargetRepository( getRemoteRepository( proxyConfig.getTargetRepoId() ) );
                connector.setProxyId( proxyConfig.getProxyId() );
                connector.setPolicies( proxyConfig.getPolicies() );

                // Copy any blacklist patterns.
                List blacklist = new ArrayList();
                if ( !isEmpty( proxyConfig.getBlackListPatterns() ) )
                {
                    blacklist.addAll( proxyConfig.getBlackListPatterns() );
                }
                connector.setBlacklist( blacklist );

                // Copy any whitelist patterns.
                List whitelist = new ArrayList();
                if ( !isEmpty( proxyConfig.getWhiteListPatterns() ) )
                {
                    whitelist.addAll( proxyConfig.getWhiteListPatterns() );
                }
                connector.setWhitelist( whitelist );

                // Get other connectors
                List connectors = (List) this.proxyConnectorMap.get( key );
                if ( connectors == null )
                {
                    // Create if we are the first.
                    connectors = new ArrayList();
                }

                // Add the connector.
                connectors.add( connector );

                // Set the key to the list of connectors.
                this.proxyConnectorMap.put( key, connectors );
            }
        }

        synchronized ( this.networkProxyMap )
        {
            this.networkProxyMap.clear();

            List networkProxies = archivaConfiguration.getConfiguration().getNetworkProxies();
            it = networkProxies.iterator();
            while ( it.hasNext() )
            {
                NetworkProxyConfiguration networkProxyConfig = (NetworkProxyConfiguration) it.next();
                String key = networkProxyConfig.getId();

                ProxyInfo proxy = new ProxyInfo();

                proxy.setType( networkProxyConfig.getProtocol() );
                proxy.setHost( networkProxyConfig.getHost() );
                proxy.setPort( networkProxyConfig.getPort() );
                proxy.setUserName( networkProxyConfig.getUsername() );
                proxy.setPassword( networkProxyConfig.getPassword() );

                this.networkProxyMap.put( key, proxy );
            }
        }
    }

    private boolean isEmpty( Collection collection )
    {
        if ( collection == null )
        {
            return true;
        }

        return collection.size() == 0;
    }

    private ArchivaRepository getRemoteRepository( String repoId )
    {
        RemoteRepositoryConfiguration repoConfig =
            archivaConfiguration.getConfiguration().findRemoteRepositoryById( repoId );

        ArchivaRepository repo = new ArchivaRepository( repoConfig.getId(), repoConfig.getName(), repoConfig.getUrl() );
        repo.getModel().setLayoutName( repoConfig.getLayout() );
        return repo;
    }

    private ArchivaRepository getManagedRepository( String repoId )
    {
        ManagedRepositoryConfiguration repoConfig =
            archivaConfiguration.getConfiguration().findManagedRepositoryById( repoId );

        return ArchivaConfigurationAdaptor.toArchivaRepository( repoConfig );
    }

    public void initialize()
        throws InitializationException
    {
        initConnectorsAndNetworkProxies();
        archivaConfiguration.addChangeListener( this );
    }
}
