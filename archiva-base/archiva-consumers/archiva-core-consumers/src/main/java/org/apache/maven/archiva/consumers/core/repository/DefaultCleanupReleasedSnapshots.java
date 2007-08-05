package org.apache.maven.archiva.consumers.core.repository;

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

import org.apache.maven.archiva.configuration.Configuration;
import org.apache.maven.archiva.model.ArchivaRepositoryMetadata;
import org.apache.maven.archiva.repository.layout.FilenameParts;
import org.apache.maven.archiva.repository.layout.LayoutException;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataReader;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataWriter;
import org.apache.maven.archiva.repository.metadata.RepositoryMetadataException;
import org.apache.maven.archiva.indexer.RepositoryIndexException;
import org.apache.maven.archiva.common.utils.VersionUtil;
import org.apache.maven.archiva.common.utils.VersionComparator;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Date;

/**
 * M2 implementation for cleaning up the released snapshots.
 *
 * @author <a href="mailto:oching@apache.org">Maria Odea Ching</a>
 */
public class DefaultCleanupReleasedSnapshots
    extends AbstractRepositoryPurge
{
    public static final String SNAPSHOT = "-SNAPSHOT";

    private RepositoryMetadataReader metadataReader;

    public DefaultCleanupReleasedSnapshots()
    {
        metadataReader = new RepositoryMetadataReader();
    }

    public void process( String path, Configuration configuration )
        throws RepositoryPurgeException
    {
        try
        {
            File artifactFile = new File( getRepository().getUrl().getPath(), path );

            if ( !artifactFile.exists() )
            {
                return;
            }

            FilenameParts parts = getFilenameParts( path );

            if ( VersionUtil.isSnapshot( parts.version ) )
            {
                // version
                File versionDir = artifactFile.getParentFile();

                // artifactID - scan for other versions
                File artifactIdDir = versionDir.getParentFile();

                boolean updated = false;

                List versions = getVersionsInDir( artifactIdDir );
                Collections.sort( versions, VersionComparator.getInstance() );
                for ( int j = 0; j < versions.size(); j++ )
                {
                    String version = (String) versions.get( j );

                    if ( VersionComparator.getInstance().compare( version, versionDir.getName() ) > 0 )
                    {
                        purge( versionDir.listFiles() );

                        FileUtils.deleteDirectory( versionDir );

                        updated = true;
                        
                        break;
                    }
                }

                if ( updated )
                {
                    updateMetadata( artifactIdDir );
                }
            }
        }
        catch ( LayoutException le )
        {
            throw new RepositoryPurgeException( le.getMessage() );
        }
        catch ( IOException ie )
        {
            throw new RepositoryPurgeException( ie.getMessage() );
        }
        catch ( RepositoryIndexException re )
        {
            throw new RepositoryPurgeException( re.getMessage() );
        }
    }

    private void updateMetadata( File artifactIdDir )
        throws RepositoryPurgeException
    {

        File[] metadataFiles = getFiles( artifactIdDir, "maven-metadata" );
        List availableVersions = getVersionsInDir( artifactIdDir );

        Collections.sort( availableVersions );

        String latestReleased = getLatestReleased( availableVersions );
        for ( int i = 0; i < metadataFiles.length; i++ )
        {
            if ( !( metadataFiles[i].getName().toUpperCase() ).endsWith( "SHA1" ) &&
                !( metadataFiles[i].getName().toUpperCase() ).endsWith( "MD5" ) )
            {
                try
                {
                    Date lastUpdated = new Date();
                    ArchivaRepositoryMetadata metadata = metadataReader.read( metadataFiles[i] );
                    metadata.setAvailableVersions( availableVersions );
                    metadata.setLatestVersion( (String) availableVersions.get( availableVersions.size() - 1 ) );
                    metadata.setReleasedVersion( latestReleased );
                    metadata.setLastUpdatedTimestamp( lastUpdated );
                    metadata.setLastUpdated( Long.toString( lastUpdated.getTime() ) );

                    RepositoryMetadataWriter.write( metadata, metadataFiles[i] );
                }
                catch ( RepositoryMetadataException rme )
                {
                    System.out.println( "Error updating metadata " + metadataFiles[i].getAbsoluteFile() );
                }
            }
        }
    }

    private String getLatestReleased( List availableVersions )
    {
        List reversedOrder = new ArrayList( availableVersions );
        Collections.reverse( reversedOrder );
        String latestReleased = "";

        for ( Iterator iter = reversedOrder.iterator(); iter.hasNext(); )
        {
            String version = (String) iter.next();
            if ( !VersionUtil.getBaseVersion( version ).endsWith( SNAPSHOT ) )
            {
                latestReleased = version;
                return latestReleased;
            }
        }

        return latestReleased;
    }

    private List getVersionsInDir( File artifactIdDir )
    {
        String[] versionsAndMore = artifactIdDir.list();
        List versions = new ArrayList();
        for ( int j = 0; j < versionsAndMore.length; j++ )
        {
            if ( VersionUtil.isVersion( versionsAndMore[j] ) )
            {
                versions.add( versionsAndMore[j] );
            }
        }

        return versions;
    }

}
