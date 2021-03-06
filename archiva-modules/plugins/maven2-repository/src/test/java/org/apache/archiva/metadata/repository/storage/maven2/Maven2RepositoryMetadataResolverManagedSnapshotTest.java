package org.apache.archiva.metadata.repository.storage.maven2;

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

import javax.inject.Inject;
import javax.inject.Named;
import org.apache.archiva.configuration.ArchivaConfiguration;
import org.apache.archiva.metadata.model.ProjectVersionMetadata;
import org.apache.archiva.metadata.repository.filter.AllFilter;
import org.apache.archiva.metadata.repository.filter.Filter;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageMetadataInvalidException;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageMetadataNotFoundException;
import org.apache.archiva.metadata.repository.storage.RepositoryStorageRuntimeException;
import org.apache.archiva.test.utils.ArchivaSpringJUnit4ClassRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;


@RunWith( ArchivaSpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = { "classpath*:/META-INF/spring-context.xml", "classpath:/spring-context.xml" } )
public class Maven2RepositoryMetadataResolverManagedSnapshotTest
    extends Maven2RepositoryMetadataResolverTest
{
    private static final Filter<String> ALL = new AllFilter<String>();

    @Inject
    @Named( value = "repositoryStorage#maven2" )
    private Maven2RepositoryStorage storage;

    private static final String TEST_REPO_ID = "test";

    private static final String TEST_REMOTE_REPO_ID = "central";

    private static final String ASF_SCM_CONN_BASE = "scm:svn:http://svn.apache.org/repos/asf/";

    private static final String ASF_SCM_DEV_CONN_BASE = "scm:svn:https://svn.apache.org/repos/asf/";

    private static final String ASF_SCM_VIEWVC_BASE = "http://svn.apache.org/viewvc/";

    private static final String TEST_SCM_CONN_BASE = "scm:svn:http://svn.example.com/repos/";

    private static final String TEST_SCM_DEV_CONN_BASE = "scm:svn:https://svn.example.com/repos/";

    private static final String TEST_SCM_URL_BASE = "http://svn.example.com/repos/";

    private static final String EMPTY_MD5 = "d41d8cd98f00b204e9800998ecf8427e";

    private static final String EMPTY_SHA1 = "da39a3ee5e6b4b0d3255bfef95601890afd80709";

    @Inject
    private ArchivaConfiguration configuration;


    @Before
    @Override
    public void setUp()
        throws Exception
    {
        super.setUp();

        testRepo.setReleases( false );
        testRepo.setSnapshots( true );
        
        configuration.save( c );

        assertTrue ( c.getManagedRepositories().get( 0 ).isSnapshots() );
        assertFalse ( c.getManagedRepositories().get( 0 ).isReleases() );
    }

    @Test( expected = RepositoryStorageRuntimeException.class)
    @Override
    public void testModelWithJdkProfileActivation() 
        throws Exception
    {
        // skygo IMHO must fail because TEST_REPO_ID ( is snap ,no release) and we seek for a snapshot
      
        ProjectVersionMetadata metadata =
            storage.readProjectVersionMetadata( TEST_REPO_ID, "org.apache.maven", "maven-archiver", "2.4.1" );
    }   
    
    @Test( expected = RepositoryStorageRuntimeException.class )
    @Override
    public void testGetProjectVersionMetadataForMislocatedPom()
        throws Exception    
    {
        storage.readProjectVersionMetadata( TEST_REPO_ID, "com.example.test", "mislocated-pom", "1.0" );
        
    }
    
    @Test
    @Override
    public void testGetProjectVersionMetadata()
        throws Exception
    {
       // super test is on release
    }    
    
    @Test( expected = RepositoryStorageRuntimeException.class )
    @Override
    public void testGetProjectVersionMetadataForInvalidPom()
        throws Exception
    {
        storage.readProjectVersionMetadata( TEST_REPO_ID, "com.example.test", "invalid-pom", "1.0" );       
    }
    
    @Test( expected = RepositoryStorageRuntimeException.class )
    @Override
    public void testGetProjectVersionMetadataForMissingPom()
        throws Exception
    {
        storage.readProjectVersionMetadata( TEST_REPO_ID, "com.example.test", "missing-pom", "1.0" );
    }
}
