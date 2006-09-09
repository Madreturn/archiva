package org.apache.maven.archiva.web.action;

/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.opensymphony.xwork.ActionSupport;
import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.configuration.ConfigurationStore;
import org.apache.maven.archiva.configuration.ConfiguredRepositoryFactory;
import org.apache.maven.archiva.configuration.RepositoryConfiguration;
import org.apache.maven.archiva.discoverer.filter.AcceptAllArtifactFilter;
import org.apache.maven.archiva.discoverer.filter.SnapshotArtifactFilter;
import org.apache.maven.archiva.reporting.ReportExecutor;
import org.apache.maven.archiva.reporting.ReportingDatabase;
import org.apache.maven.archiva.reporting.ReportingStore;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Repository reporting.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="reportsAction"
 */
public class ReportsAction
    extends ActionSupport
{
    /**
     * @plexus.requirement
     */
    private ReportingStore reportingStore;

    /**
     * @plexus.requirement
     */
    private ConfigurationStore configurationStore;

    /**
     * @plexus.requirement
     */
    private ConfiguredRepositoryFactory factory;

    private List databases;

    private String repositoryId;

    /**
     * @plexus.requirement
     */
    private ReportExecutor executor;

    public String execute()
        throws Exception
    {
        databases = new ArrayList();

        Configuration configuration = configurationStore.getConfigurationFromStore();

        for ( Iterator i = configuration.getRepositories().iterator(); i.hasNext(); )
        {
            RepositoryConfiguration repositoryConfiguration = (RepositoryConfiguration) i.next();

            ArtifactRepository repository = factory.createRepository( repositoryConfiguration );

            ReportingDatabase database = reportingStore.getReportsFromStore( repository );

            databases.add( database );
        }
        return SUCCESS;
    }

    public String runReport()
        throws Exception
    {
        // TODO: this should be one that runs in the background - see the showcase

        Configuration configuration = configurationStore.getConfigurationFromStore();

        RepositoryConfiguration repositoryConfiguration = configuration.getRepositoryById( repositoryId );
        ArtifactRepository repository = factory.createRepository( repositoryConfiguration );

        ReportingDatabase database = executor.getReportDatabase( repository );
        if ( database.isInProgress() )
        {
            return SUCCESS;
        }

        database.setInProgress( true );

        List blacklistedPatterns = new ArrayList();
        if ( repositoryConfiguration.getBlackListPatterns() != null )
        {
            blacklistedPatterns.addAll( repositoryConfiguration.getBlackListPatterns() );
        }
        if ( configuration.getGlobalBlackListPatterns() != null )
        {
            blacklistedPatterns.addAll( configuration.getGlobalBlackListPatterns() );
        }

        ArtifactFilter filter;
        if ( repositoryConfiguration.isIncludeSnapshots() )
        {
            filter = new AcceptAllArtifactFilter();
        }
        else
        {
            filter = new SnapshotArtifactFilter();
        }

        try
        {
            executor.runReports( repository, blacklistedPatterns, filter );
        }
        finally
        {
            database.setInProgress( false );
        }

        return SUCCESS;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId( String repositoryId )
    {
        this.repositoryId = repositoryId;
    }

    public List getDatabases()
    {
        return databases;
    }
}
