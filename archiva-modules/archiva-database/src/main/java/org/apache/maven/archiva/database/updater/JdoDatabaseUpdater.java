package org.apache.maven.archiva.database.updater;

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

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Predicate;
import org.apache.maven.archiva.database.ArchivaDAO;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.constraints.ArtifactsProcessedConstraint;
import org.apache.maven.archiva.model.ArchivaArtifact;
import org.apache.maven.archiva.model.functors.UnprocessedArtifactPredicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JdoDatabaseUpdater
 *
 * @version $Id$
 * 
 * @plexus.component role="org.apache.maven.archiva.database.updater.DatabaseUpdater" 
 *   role-hint="jdo" 
 */
public class JdoDatabaseUpdater
    implements DatabaseUpdater
{
    private Logger log = LoggerFactory.getLogger( JdoDatabaseUpdater.class );
    
    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaDAO dao;

    /**
     * @plexus.requirement
     */
    private DatabaseConsumers dbConsumers;

    private ProcessArchivaArtifactClosure processArtifactClosure = new ProcessArchivaArtifactClosure();

    public void updateAllUnprocessed()
        throws ArchivaDatabaseException
    {
        List<ArchivaArtifact> unprocessedArtifacts = dao.getArtifactDAO().queryArtifacts( new ArtifactsProcessedConstraint( false ) );

        beginConsumerLifecycle( dbConsumers.getSelectedUnprocessedConsumers() );

        try
        {
            // Process each consumer.
            Predicate predicate = UnprocessedArtifactPredicate.getInstance();

            Iterator<ArchivaArtifact> it = IteratorUtils.filteredIterator( unprocessedArtifacts.iterator(), predicate );
            while ( it.hasNext() )
            {
                ArchivaArtifact artifact = it.next();
                updateUnprocessed( artifact );
            }
        }
        finally
        {
            endConsumerLifecycle( dbConsumers.getSelectedUnprocessedConsumers() );
        }
    }

    private void endConsumerLifecycle( List<ArchivaArtifactConsumer> consumers )
    {
        for ( ArchivaArtifactConsumer consumer : consumers )
        {
            consumer.completeScan();
        }
    }

    private void beginConsumerLifecycle( List<ArchivaArtifactConsumer> consumers )
    {
        for ( ArchivaArtifactConsumer consumer : consumers )
        {
            consumer.beginScan();
        }
    }

    public void updateUnprocessed( ArchivaArtifact artifact )
        throws ArchivaDatabaseException
    {
        List<ArchivaArtifactConsumer> consumers = dbConsumers.getSelectedUnprocessedConsumers();

        if ( CollectionUtils.isEmpty( consumers ) )
        {
            log.warn( "There are no selected consumers for unprocessed artifacts." );
            return;
        }
        
        this.processArtifactClosure.setArtifact( artifact );
        CollectionUtils.forAllDo( consumers, this.processArtifactClosure );

        artifact.getModel().setWhenProcessed( new Date() );
        dao.getArtifactDAO().saveArtifact( artifact );
    }
}