package org.apache.archiva.configuration;
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

import net.sf.beanlib.provider.replicator.BeanReplicator;
import org.apache.archiva.admin.model.AuditInformation;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.beans.NetworkProxy;
import org.apache.archiva.admin.model.beans.ProxyConnector;
import org.apache.archiva.admin.model.beans.RemoteRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.admin.model.networkproxy.NetworkProxyAdmin;
import org.apache.archiva.admin.model.proxyconnector.ProxyConnectorAdmin;
import org.apache.archiva.admin.model.proxyconnector.ProxyConnectorOrderComparator;
import org.apache.archiva.admin.model.remote.RemoteRepositoryAdmin;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.index.context.IndexingContext;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Olivier Lamy
 */
@Service
public class MockRepoAdmin
    implements RemoteRepositoryAdmin, ManagedRepositoryAdmin, ProxyConnectorAdmin, NetworkProxyAdmin
{
    @Inject
    @Named( value = "archivaConfiguration#test" )
    private ArchivaConfiguration archivaConfiguration;

    public List<RemoteRepository> getRemoteRepositories()
        throws RepositoryAdminException
    {
        List<RemoteRepository> remoteRepositories =
            new ArrayList<RemoteRepository>( archivaConfiguration.getConfiguration().getRemoteRepositories().size() );
        for ( RemoteRepositoryConfiguration repositoryConfiguration : archivaConfiguration.getConfiguration().getRemoteRepositories() )
        {
            RemoteRepository remoteRepository =
                new RemoteRepository( repositoryConfiguration.getId(), repositoryConfiguration.getName(),
                                      repositoryConfiguration.getUrl(), repositoryConfiguration.getLayout(),
                                      repositoryConfiguration.getUsername(), repositoryConfiguration.getPassword(),
                                      repositoryConfiguration.getTimeout() );
            remoteRepository.setDownloadRemoteIndex( repositoryConfiguration.isDownloadRemoteIndex() );
            remoteRepository.setRemoteIndexUrl( repositoryConfiguration.getRemoteIndexUrl() );
            remoteRepository.setCronExpression( repositoryConfiguration.getRefreshCronExpression() );
            remoteRepository.setIndexDirectory( repositoryConfiguration.getIndexDir() );
            remoteRepository.setRemoteDownloadNetworkProxyId(
                repositoryConfiguration.getRemoteDownloadNetworkProxyId() );
            remoteRepository.setRemoteDownloadTimeout( repositoryConfiguration.getRemoteDownloadTimeout() );
            remoteRepository.setDownloadRemoteIndexOnStartup(
                repositoryConfiguration.isDownloadRemoteIndexOnStartup() );
            remoteRepositories.add( remoteRepository );
        }
        return remoteRepositories;
    }

    public RemoteRepository getRemoteRepository( String repositoryId )
        throws RepositoryAdminException
    {
        for ( RemoteRepository remoteRepository : getRemoteRepositories() )
        {
            if ( StringUtils.equals( repositoryId, remoteRepository.getId() ) )
            {
                return remoteRepository;
            }
        }
        return null;
    }

    public Boolean deleteRemoteRepository( String repositoryId, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean addRemoteRepository( RemoteRepository remoteRepository, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean updateRemoteRepository( RemoteRepository remoteRepository, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<String, RemoteRepository> getRemoteRepositoriesAsMap()
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public IndexingContext createIndexContext( RemoteRepository repository )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<ManagedRepository> getManagedRepositories()
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<String, ManagedRepository> getManagedRepositoriesAsMap()
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ManagedRepository getManagedRepository( String repositoryId )
        throws RepositoryAdminException
    {
        for ( ManagedRepositoryConfiguration repoConfig : archivaConfiguration.getConfiguration().getManagedRepositories() )
        {
            if ( StringUtils.equals( repositoryId, repoConfig.getId() ) )
            {
                return new ManagedRepository( repoConfig.getId(), repoConfig.getName(), repoConfig.getLocation(),
                                              repoConfig.getLayout(), repoConfig.isSnapshots(), repoConfig.isReleases(),
                                              repoConfig.isBlockRedeployments(), repoConfig.getRefreshCronExpression(),
                                              repoConfig.getIndexDir(), repoConfig.isScanned(),
                                              repoConfig.getDaysOlder(), repoConfig.getRetentionCount(),
                                              repoConfig.isDeleteReleasedSnapshots(), false );
            }
        }
        return null;
    }

    public Boolean deleteManagedRepository( String repositoryId, AuditInformation auditInformation,
                                            boolean deleteContent )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean addManagedRepository( ManagedRepository managedRepository, boolean needStageRepo,
                                         AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean updateManagedRepository( ManagedRepository managedRepository, boolean needStageRepo,
                                            AuditInformation auditInformation, boolean resetStats )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public IndexingContext createIndexContext( ManagedRepository repository )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<ProxyConnector> getProxyConnectors()
        throws RepositoryAdminException
    {
        List<ProxyConnectorConfiguration> proxyConnectorConfigurations =
            archivaConfiguration.getConfiguration().getProxyConnectors();
        List<ProxyConnector> proxyConnectors = new ArrayList<ProxyConnector>( proxyConnectorConfigurations.size() );
        for ( ProxyConnectorConfiguration configuration : proxyConnectorConfigurations )
        {
            proxyConnectors.add( getProxyConnector( configuration ) );
        }
        Collections.sort( proxyConnectors, ProxyConnectorOrderComparator.getInstance() );
        return proxyConnectors;
    }

    public ProxyConnector getProxyConnector( String sourceRepoId, String targetRepoId )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean addProxyConnector( ProxyConnector proxyConnector, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean deleteProxyConnector( ProxyConnector proxyConnector, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Boolean updateProxyConnector( ProxyConnector proxyConnector, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<String, List<ProxyConnector>> getProxyConnectorAsMap()
        throws RepositoryAdminException
    {
        Map<String, List<ProxyConnector>> proxyConnectorMap = new HashMap<String, List<ProxyConnector>>();

        Iterator<ProxyConnector> it = getProxyConnectors().iterator();
        while ( it.hasNext() )
        {
            ProxyConnector proxyConfig = it.next();
            String key = proxyConfig.getSourceRepoId();

            List<ProxyConnector> connectors = proxyConnectorMap.get( key );
            if ( connectors == null )
            {
                connectors = new ArrayList<ProxyConnector>( 1 );
                proxyConnectorMap.put( key, connectors );
            }

            connectors.add( proxyConfig );

            Collections.sort( connectors, ProxyConnectorOrderComparator.getInstance() );
        }

        return proxyConnectorMap;
    }

    public List<NetworkProxy> getNetworkProxies()
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NetworkProxy getNetworkProxy( String networkProxyId )
        throws RepositoryAdminException
    {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addNetworkProxy( NetworkProxy networkProxy, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateNetworkProxy( NetworkProxy networkProxy, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void deleteNetworkProxy( String networkProxyId, AuditInformation auditInformation )
        throws RepositoryAdminException
    {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    protected ProxyConnector getProxyConnector( ProxyConnectorConfiguration proxyConnectorConfiguration )
    {
        return proxyConnectorConfiguration == null
            ? null
            : new BeanReplicator().replicateBean( proxyConnectorConfiguration, ProxyConnector.class );
    }
}
