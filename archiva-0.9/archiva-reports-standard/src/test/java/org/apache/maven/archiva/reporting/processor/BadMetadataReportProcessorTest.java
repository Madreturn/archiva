package org.apache.maven.archiva.reporting.processor;

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
import org.apache.maven.archiva.reporting.AbstractRepositoryReportsTestCase;
import org.apache.maven.archiva.reporting.database.MetadataResultsDatabase;
import org.apache.maven.archiva.reporting.model.MetadataResults;
import org.apache.maven.archiva.reporting.model.ResultReason;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.metadata.ArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.GroupRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.apache.maven.artifact.repository.metadata.RepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.SnapshotArtifactRepositoryMetadata;
import org.apache.maven.artifact.repository.metadata.Versioning;

import java.util.Iterator;

/**
 * BadMetadataReportProcessorTest 
 *
 * @version $Id$
 */
public class BadMetadataReportProcessorTest
    extends AbstractRepositoryReportsTestCase
{
    private ArtifactFactory artifactFactory;

    private MetadataReportProcessor badMetadataReportProcessor;

    private MetadataResultsDatabase database;

    protected void setUp()
        throws Exception
    {
        super.setUp();

        artifactFactory = (ArtifactFactory) lookup( ArtifactFactory.ROLE );

        database = (MetadataResultsDatabase) lookup( MetadataResultsDatabase.ROLE, "default" );

        badMetadataReportProcessor = (MetadataReportProcessor) lookup( MetadataReportProcessor.ROLE, "bad-metadata" );
    }

    protected void tearDown()
        throws Exception
    {
        release( artifactFactory );
        release( badMetadataReportProcessor );
        super.tearDown();
    }
    
    public void testMetadataMissingLastUpdated()
    {
        Artifact artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );

        Versioning versioning = new Versioning();
        versioning.addVersion( "1.0-alpha-1" );
        versioning.addVersion( "1.0-alpha-2" );

        RepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact, versioning );

        badMetadataReportProcessor.processMetadata( metadata, repository );

        Iterator failures = database.getIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        ResultReason result = (ResultReason) failures.next();
        assertMetadata( metadata, results );
        assertEquals( "check reason", "Missing lastUpdated element inside the metadata.", result.getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }

    public void testMetadataMissingVersioning()
    {
        Artifact artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );

        RepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact, null );

        badMetadataReportProcessor.processMetadata( metadata, repository );

        Iterator failures = database.getIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        assertMetadata( metadata, results );
        ResultReason result = (ResultReason) failures.next();
        assertEquals( "check reason", "Missing lastUpdated element inside the metadata.", result.getReason() );
        result = (ResultReason) failures.next();
        boolean alpha1First = false;
        if ( result.getReason().indexOf( "alpha-1" ) > 0 )
        {
            alpha1First = true;
        }
        if ( alpha1First )
        {
            assertEquals( "check reason",
                          "Artifact version 1.0-alpha-1 found in the repository but missing in the metadata.", result
                              .getReason() );
        }
        else
        {
            assertEquals( "check reason",
                          "Artifact version 1.0-alpha-2 found in the repository but missing in the metadata.", result
                              .getReason() );
        }
        result = (ResultReason) failures.next();
        if ( !alpha1First )
        {
            assertEquals( "check reason",
                          "Artifact version 1.0-alpha-1 found in the repository but missing in the metadata.", result
                              .getReason() );
        }
        else
        {
            assertEquals( "check reason",
                          "Artifact version 1.0-alpha-2 found in the repository but missing in the metadata.", result
                              .getReason() );
        }
        assertFalse( "check no more failures", failures.hasNext() );
    }

    public void testSnapshotMetadataMissingVersioning()
    {
        Artifact artifact = artifactFactory.createBuildArtifact( "groupId", "snapshot-artifact",
                                                                 "1.0-alpha-1-SNAPSHOT", "type" );

        RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact );

        badMetadataReportProcessor.processMetadata( metadata, repository );

        Iterator failures = database.getIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        assertMetadata( metadata, results );
        ResultReason result = (ResultReason) failures.next();
        assertEquals( "check reason", "Missing lastUpdated element inside the metadata.", result.getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }

    public void testMetadataValidVersions()
    {
        Artifact artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );

        Versioning versioning = new Versioning();
        versioning.addVersion( "1.0-alpha-1" );
        versioning.addVersion( "1.0-alpha-2" );
        versioning.setLastUpdated( "20050611.202020" );

        RepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact, versioning );

        badMetadataReportProcessor.processMetadata( metadata, repository );

        Iterator failures = database.getIterator();
        assertFalse( "check there are no failures", failures.hasNext() );
    }

    public void testMetadataMissingADirectory()
    {
        Artifact artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );

        Versioning versioning = new Versioning();
        versioning.addVersion( "1.0-alpha-1" );
        versioning.setLastUpdated( "20050611.202020" );

        RepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact, versioning );

        badMetadataReportProcessor.processMetadata( metadata, repository );

        Iterator failures = database.getIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        ResultReason result = (ResultReason) failures.next();
        assertMetadata( metadata, results );
        // TODO: should be more robust
        assertEquals( "check reason",
                      "Artifact version 1.0-alpha-2 found in the repository but missing in the metadata.", result
                          .getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }

    public void testMetadataInvalidArtifactVersion()
    {
        Artifact artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );

        Versioning versioning = new Versioning();
        versioning.addVersion( "1.0-alpha-1" );
        versioning.addVersion( "1.0-alpha-2" );
        versioning.addVersion( "1.0-alpha-3" );
        versioning.setLastUpdated( "20050611.202020" );

        RepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact, versioning );

        badMetadataReportProcessor.processMetadata( metadata, repository );

        Iterator failures = database.getIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        ResultReason result = (ResultReason) failures.next();
        assertMetadata( metadata, results );
        // TODO: should be more robust
        assertEquals( "check reason",
                      "Artifact version 1.0-alpha-3 is present in metadata but missing in the repository.", result
                          .getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }

    public void testMoreThanOneMetadataVersionErrors()
    {
        Artifact artifact = artifactFactory.createBuildArtifact( "groupId", "artifactId", "1.0-alpha-1", "type" );

        Versioning versioning = new Versioning();
        versioning.addVersion( "1.0-alpha-1" );
        versioning.addVersion( "1.0-alpha-3" );
        versioning.setLastUpdated( "20050611.202020" );

        RepositoryMetadata metadata = new ArtifactRepositoryMetadata( artifact, versioning );

        badMetadataReportProcessor.processMetadata( metadata, repository );

        Iterator failures = database.getIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        ResultReason result = (ResultReason) failures.next();
        assertMetadata( metadata, results );
        // TODO: should be more robust
        assertEquals( "check reason",
                      "Artifact version 1.0-alpha-3 is present in metadata but missing in the repository.", result
                          .getReason() );
        assertTrue( "check there is a 2nd failure", failures.hasNext() );
        result = (ResultReason) failures.next();
        // TODO: should be more robust
        assertEquals( "check reason",
                      "Artifact version 1.0-alpha-2 found in the repository but missing in the metadata.", result
                          .getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }

    public void testValidPluginMetadata()
    {
        RepositoryMetadata metadata = new GroupRepositoryMetadata( "groupId" );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "artifactId", "default" ) );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "snapshot-artifact", "default2" ) );

        badMetadataReportProcessor.processMetadata( metadata, repository );

        Iterator failures = database.getIterator();
        assertFalse( "check there are no failures", failures.hasNext() );
    }

    public void testMissingMetadataPlugin()
    {
        RepositoryMetadata metadata = new GroupRepositoryMetadata( "groupId" );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "artifactId", "default" ) );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "snapshot-artifact", "default2" ) );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "missing-plugin", "default3" ) );

        badMetadataReportProcessor.processMetadata( metadata, repository );

        Iterator failures = database.getIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        ResultReason result = (ResultReason) failures.next();
        // TODO: should be more robust
        assertEquals( "check reason", "Metadata plugin missing-plugin not found in the repository", result.getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }

    public void testIncompletePluginMetadata()
    {
        RepositoryMetadata metadata = new GroupRepositoryMetadata( "groupId" );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "artifactId", "default" ) );

        badMetadataReportProcessor.processMetadata( metadata, repository );

        Iterator failures = database.getIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        ResultReason result = (ResultReason) failures.next();
        // TODO: should be more robust
        assertEquals( "check reason", "Plugin snapshot-artifact is present in the repository but "
            + "missing in the metadata.", result.getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }

    public void testInvalidPluginArtifactId()
    {
        RepositoryMetadata metadata = new GroupRepositoryMetadata( "groupId" );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "artifactId", "default" ) );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "snapshot-artifact", "default2" ) );
        metadata.getMetadata().addPlugin( createMetadataPlugin( null, "default3" ) );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "", "default4" ) );

        badMetadataReportProcessor.processMetadata( metadata, repository );

        Iterator failures = database.getIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        ResultReason result = (ResultReason) failures.next();
        // TODO: should be more robust
        assertEquals( "check reason", "Missing or empty artifactId in group metadata for plugin default3", result
            .getReason() );
        assertTrue( "check there is a 2nd failure", failures.hasNext() );
        result = (ResultReason) failures.next();
        // TODO: should be more robust
        assertEquals( "check reason", "Missing or empty artifactId in group metadata for plugin default4", result
            .getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }

    public void testInvalidPluginPrefix()
    {
        RepositoryMetadata metadata = new GroupRepositoryMetadata( "groupId" );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "artifactId", null ) );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "snapshot-artifact", "" ) );

        badMetadataReportProcessor.processMetadata( metadata, repository );

        Iterator failures = database.getIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        ResultReason result = (ResultReason) failures.next();
        // TODO: should be more robust
        assertEquals( "check reason", "Missing or empty plugin prefix for artifactId artifactId.", result.getReason() );
        assertTrue( "check there is a 2nd failure", failures.hasNext() );
        result = (ResultReason) failures.next();
        // TODO: should be more robust
        assertEquals( "check reason", "Missing or empty plugin prefix for artifactId snapshot-artifact.", result
            .getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }

    public void testDuplicatePluginPrefixes()
    {
        RepositoryMetadata metadata = new GroupRepositoryMetadata( "groupId" );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "artifactId", "default" ) );
        metadata.getMetadata().addPlugin( createMetadataPlugin( "snapshot-artifact", "default" ) );

        badMetadataReportProcessor.processMetadata( metadata, repository );

        Iterator failures = database.getIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        ResultReason result = (ResultReason) failures.next();
        // TODO: should be more robust
        assertEquals( "check reason", "Duplicate plugin prefix found: default.", result.getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }

    public void testValidSnapshotMetadata()
    {
        Artifact artifact = artifactFactory.createBuildArtifact( "groupId", "snapshot-artifact",
                                                                 "1.0-alpha-1-SNAPSHOT", "type" );

        Snapshot snapshot = new Snapshot();
        snapshot.setBuildNumber( 1 );
        snapshot.setTimestamp( "20050611.202024" );

        RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact, snapshot );

        badMetadataReportProcessor.processMetadata( metadata, repository );

        Iterator failures = database.getIterator();
        assertFalse( "check there are no failures", failures.hasNext() );
    }

    public void testInvalidSnapshotMetadata()
    {
        Artifact artifact = artifactFactory.createBuildArtifact( "groupId", "snapshot-artifact",
                                                                 "1.0-alpha-1-SNAPSHOT", "type" );

        Snapshot snapshot = new Snapshot();
        snapshot.setBuildNumber( 2 );
        snapshot.setTimestamp( "20050611.202024" );

        RepositoryMetadata metadata = new SnapshotArtifactRepositoryMetadata( artifact, snapshot );

        badMetadataReportProcessor.processMetadata( metadata, repository );

        Iterator failures = database.getIterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        MetadataResults results = (MetadataResults) failures.next();
        failures = results.getFailures().iterator();
        assertTrue( "check there is a failure", failures.hasNext() );
        ResultReason result = (ResultReason) failures.next();
        assertMetadata( metadata, results );
        // TODO: should be more robust
        assertEquals( "check reason", "Snapshot artifact 1.0-alpha-1-20050611.202024-2 does not exist.", result
            .getReason() );
        assertFalse( "check no more failures", failures.hasNext() );
    }

    private static void assertMetadata( RepositoryMetadata metadata, MetadataResults results )
    {
        /* The funky StringUtils.defaultString() is used because of database constraints.
         * The MetadataResults object has a complex primary key consisting of groupId, artifactId, and version.
         * This also means that none of those fields may be null.  however, that doesn't eliminate the
         * ability to have an empty string in place of a null.
         */
        assertEquals( "check metadata", StringUtils.defaultString( metadata.getGroupId() ), results.getGroupId() );
        assertEquals( "check metadata", StringUtils.defaultString( metadata.getArtifactId() ), results.getArtifactId() );
        assertEquals( "check metadata", StringUtils.defaultString( metadata.getBaseVersion() ), results.getVersion() );
    }

    private Plugin createMetadataPlugin( String artifactId, String prefix )
    {
        Plugin plugin = new Plugin();
        plugin.setArtifactId( artifactId );
        plugin.setName( artifactId );
        plugin.setPrefix( prefix );
        return plugin;
    }
}
