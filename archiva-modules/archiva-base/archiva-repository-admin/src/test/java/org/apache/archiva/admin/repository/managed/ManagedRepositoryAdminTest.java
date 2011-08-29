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

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import javax.inject.Inject;
import java.util.List;

/**
 * @author Olivier Lamy
 */
public class ManagedRepositoryAdminTest
    extends AbstractRepositoryAdminTest
{

    @Inject
    private ManagedRepositoryAdmin managedRepositoryAdmin;

    @Test
    public void getAllManagedRepos()
        throws Exception
    {
        List<ManagedRepository> repos = managedRepositoryAdmin.getManagedRepositories();
        assertNotNull( repos );
        assertTrue( repos.size() > 0 );
        log.info( "repos " + repos );

        // check default internal
        ManagedRepository internal = findManagedRepoById( repos, "internal" );
        assertNotNull( internal );
        assertTrue( internal.isReleases() );
        assertFalse( internal.isSnapshots() );
    }

    @Test
    public void getById()
        throws Exception
    {
        ManagedRepository repo = managedRepositoryAdmin.getManagedRepository( "internal" );
        assertNotNull( repo );
    }

    @Test
    public void addManagedRepo()
        throws Exception
    {
        List<ManagedRepository> repos = managedRepositoryAdmin.getManagedRepositories();
        assertNotNull( repos );
        int initialSize = repos.size();
        assertTrue( initialSize > 0 );

        ManagedRepository repo = new ManagedRepository();
        repo.setId( "test-new-one" );
        repo.setName( "test repo" );
        repo.setUrl( APPSERVER_BASE_PATH + repo.getId() );
        managedRepositoryAdmin.addManagedRepository( repo, false );
        repos = managedRepositoryAdmin.getManagedRepositories();
        assertNotNull( repos );
        assertEquals( initialSize + 1, repos.size() );

        assertNotNull( managedRepositoryAdmin.getManagedRepository( "test-new-one" ) );
    }


    private ManagedRepository findManagedRepoById( List<ManagedRepository> repos, String id )
    {
        for ( ManagedRepository repo : repos )
        {
            if ( StringUtils.equals( id, repo.getId() ) )
            {
                return repo;
            }
        }
        return null;
    }

}