package org.apache.archiva.rest.services;
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

import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.archiva.rest.api.services.ArchivaRestServiceException;
import org.apache.archiva.rest.api.services.ManagedRepositoriesService;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Service( "managedRepositoriesService#rest" )
public class DefaultManagedRepositoriesService
    extends AbstractRestService
    implements ManagedRepositoriesService
{

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Inject
    private PlexusSisuBridge plexusSisuBridge;


    public List<ManagedRepository> getManagedRepositories()
        throws ArchivaRestServiceException
    {
        try
        {
            List<org.apache.archiva.admin.model.beans.ManagedRepository> repos =
                managedRepositoryAdmin.getManagedRepositories();
            return repos == null ? Collections.<ManagedRepository>emptyList() : repos;
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public ManagedRepository getManagedRepository( String repositoryId )
        throws ArchivaRestServiceException
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


    public Boolean deleteManagedRepository( String repoId, boolean deleteContent )
        throws ArchivaRestServiceException
    {

        try
        {
            return managedRepositoryAdmin.deleteManagedRepository( repoId, getAuditInformation(), deleteContent );
        }
        catch ( RepositoryAdminException e )
        {
            log.error( e.getMessage(), e );
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

    public Boolean addManagedRepository( ManagedRepository managedRepository )
        throws ArchivaRestServiceException
    {

        try
        {
            return managedRepositoryAdmin.addManagedRepository( managedRepository, getAuditInformation() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }


    public Boolean updateManagedRepository( ManagedRepository managedRepository )
        throws ArchivaRestServiceException
    {

        try
        {
            return managedRepositoryAdmin.updateManagedRepository( managedRepository, getAuditInformation(),
                                                                   managedRepository.isResetStats() );
        }
        catch ( RepositoryAdminException e )
        {
            throw new ArchivaRestServiceException( e.getMessage() );
        }
    }

}
