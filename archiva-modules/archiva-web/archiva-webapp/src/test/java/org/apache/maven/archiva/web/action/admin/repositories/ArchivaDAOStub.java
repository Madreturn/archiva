package org.apache.maven.archiva.web.action.admin.repositories;

import junit.framework.Assert;
import org.apache.maven.archiva.configuration.ArchivaConfiguration;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArtifactDAO;
import org.apache.maven.archiva.database.ProjectModelDAO;
import org.apache.maven.archiva.database.RepositoryContentStatisticsDAO;
import org.apache.maven.archiva.database.RepositoryProblemDAO;
import org.apache.maven.archiva.database.SimpleConstraint;
import org.apache.maven.archiva.database.constraints.UniqueArtifactIdConstraint;
import org.apache.maven.archiva.database.constraints.UniqueFieldConstraint;
import org.apache.maven.archiva.database.constraints.UniqueGroupIdConstraint;
import org.apache.maven.archiva.database.constraints.UniqueVersionConstraint;
import org.apache.maven.archiva.model.RepositoryContentStatistics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

/**
 * Stub class for Archiva DAO to avoid having to set up a database for tests.
 *
 * @todo a mock would be better, but that won't play nicely with Plexus injection.
 */
public class ArchivaDAOStub
    implements ArchivaDAO
{
    private ArchivaConfiguration configuration;

    private ArtifactDAO artifactDao;

    private ProjectModelDAO projectDao;

    private List<String> versions;

    private List<String> groups;

    private List<String> artifacts;

    private List<String> repositoryIds;

    private RepositoryContentStatisticsDAO repositoryContentStatisticsDAO;

    private RepositoryProblemDAO repositoryProblemDAO;

    public List<?> query( SimpleConstraint constraint )
    {
        if ( constraint instanceof UniqueVersionConstraint )
        {
            return versions;
        }
        else if ( constraint instanceof UniqueGroupIdConstraint )
        {
            return groups;
        }
        else if ( constraint instanceof UniqueArtifactIdConstraint )
        {
            return artifacts;
        }
        else if ( constraint instanceof UniqueFieldConstraint )
        {
            return repositoryIds;
        }
        else
        {
            Assert.assertEquals( RepositoryContentStatistics.class, constraint.getResultClass() );

            List<RepositoryContentStatistics> stats = new ArrayList<RepositoryContentStatistics>();
            for ( String repo : configuration.getConfiguration().getManagedRepositoriesAsMap().keySet() )
            {
                RepositoryContentStatistics statistics = new RepositoryContentStatistics();
                statistics.setRepositoryId( repo );
                stats.add( statistics );
            }
            return stats;
        }
    }

    public Object save( Serializable obj )
    {
        throw new UnsupportedOperationException( "query not implemented for stub" );
    }

    public ArtifactDAO getArtifactDAO()
    {
        return artifactDao;
    }

    public ProjectModelDAO getProjectModelDAO()
    {
        return projectDao;
    }

    public RepositoryProblemDAO getRepositoryProblemDAO()
    {
        return repositoryProblemDAO;
    }

    public RepositoryContentStatisticsDAO getRepositoryContentStatisticsDAO()
    {
        return repositoryContentStatisticsDAO;
    }

    public long count( SimpleConstraint constraint )
    {
        return query( constraint ).size();
    }

    public void setArtifactDao( ArtifactDAO artifactDao )
    {
        this.artifactDao = artifactDao;
    }

    public void setProjectDao( ProjectModelDAO projectDao )
    {
        this.projectDao = projectDao;
    }

    public void setVersions( List<String> versions )
    {
        this.versions = versions;
    }

    public void setGroups( List<String> groups )
    {
        this.groups = groups;
    }

    public void setArtifacts( List<String> artifacts )
    {
        this.artifacts = artifacts;
    }

    public void setRepositoryIds( List<String> repositoryIds )
    {
        this.repositoryIds = repositoryIds;
    }

    public void setRepositoryProblemDAO( RepositoryProblemDAO repositoryProblemDAO )
    {
        this.repositoryProblemDAO = repositoryProblemDAO;
    }
}
