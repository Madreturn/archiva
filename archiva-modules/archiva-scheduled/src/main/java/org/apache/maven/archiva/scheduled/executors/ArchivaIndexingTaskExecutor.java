package org.apache.maven.archiva.scheduled.executors;

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

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.maven.archiva.configuration.ManagedRepositoryConfiguration;
import org.apache.maven.archiva.scheduled.tasks.ArtifactIndexingTask;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.codehaus.plexus.taskqueue.Task;
import org.codehaus.plexus.taskqueue.execution.TaskExecutionException;
import org.codehaus.plexus.taskqueue.execution.TaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.nexus.index.ArtifactContext;
import org.sonatype.nexus.index.ArtifactContextProducer;
import org.sonatype.nexus.index.ArtifactInfo;
import org.sonatype.nexus.index.DefaultArtifactContextProducer;
import org.sonatype.nexus.index.IndexerEngine;
import org.sonatype.nexus.index.context.IndexingContext;
import org.sonatype.nexus.index.packer.IndexPacker;
import org.sonatype.nexus.index.packer.IndexPackingRequest;

/**
 * ArchivaIndexingTaskExecutor Executes all indexing tasks. Adding, updating and removing artifacts from the index are
 * all performed by this executor. Add and update artifact in index tasks are added in the indexing task queue by the
 * NexusIndexerConsumer while remove artifact from index tasks are added by the LuceneCleanupRemoveIndexedConsumer.
 * 
 * @todo Nexus specifics shouldn't be in the archiva-scheduled module
 * @plexus.component role="org.codehaus.plexus.taskqueue.execution.TaskExecutor" role-hint="indexing"
 *                   instantiation-strategy="singleton"
 */
public class ArchivaIndexingTaskExecutor
    implements TaskExecutor, Initializable
{
    private Logger log = LoggerFactory.getLogger( ArchivaIndexingTaskExecutor.class );

    /**
     * @plexus.requirement
     */
    private IndexerEngine indexerEngine;

    /**
     * @plexus.requirement
     */
    private IndexPacker indexPacker;

    private ArtifactContextProducer artifactContextProducer;

    public void executeTask( Task task )
        throws TaskExecutionException
    {
        synchronized ( indexerEngine )
        {
            ArtifactIndexingTask indexingTask = (ArtifactIndexingTask) task;

            ManagedRepositoryConfiguration repository = indexingTask.getRepository();
            IndexingContext context = indexingTask.getContext();

            if ( ArtifactIndexingTask.Action.FINISH.equals( indexingTask.getAction() ) )
            {
                try
                {
                    context.optimize();

                    File managedRepository = new File( repository.getLocation() );
                    final File indexLocation = new File( managedRepository, ".index" );
                    IndexPackingRequest request = new IndexPackingRequest( context, indexLocation );
                    indexPacker.packIndex( request );

                    log.debug( "Index file packaged at '" + indexLocation.getPath() + "'." );
                }
                catch ( IOException e )
                {
                    log.error( "Error occurred while executing indexing task '" + indexingTask + "'" );
                    throw new TaskExecutionException( "Error occurred while executing indexing task '" + indexingTask
                        + "'" );
                }
                finally
                {
                    if ( context != null )
                    {
                        try
                        {
                            context.close( false );
                        }
                        catch ( IOException e )
                        {
                            log.error( "Error occurred while closing context: " + e.getMessage() );
                            throw new TaskExecutionException( "Error occurred while closing context: " + e.getMessage() );
                        }
                    }
                }
            }
            else
            {
                if ( context.getIndexDirectory() == null )
                {
                    throw new TaskExecutionException( "Trying to index an artifact but the context is already closed" );
                }
                
                try
                {
                    File artifactFile = indexingTask.getResourceFile();
                    ArtifactContext ac = artifactContextProducer.getArtifactContext( context, artifactFile );

                    if ( ac != null )
                    {
                        if ( indexingTask.getAction().equals( ArtifactIndexingTask.Action.ADD ) )
                        {
                            boolean add = true;
                            IndexReader r = context.getIndexReader();
                            for ( int i = 0; i < r.numDocs(); i++ )
                            {
                                if ( !r.isDeleted( i ) )
                                {
                                    Document d = r.document( i );
                                    String uinfo = d.get( ArtifactInfo.UINFO );
                                    if ( ac.getArtifactInfo().getUinfo().equals( uinfo ) )
                                    {
                                        add = false;
                                        break;
                                    }
                                }
                            }

                            if ( add )
                            {
                                log.debug( "Adding artifact '" + ac.getArtifactInfo() + "' to index.." );
                                indexerEngine.index( context, ac );
                                context.getIndexWriter().commit();
                            }
                            else
                            {
                                log.debug( "Updating artifact '" + ac.getArtifactInfo() + "' in index.." );
                                indexerEngine.update( context, ac );
                                context.getIndexWriter().commit();
                            }
                        }
                        else
                        {
                            log.debug( "Removing artifact '" + ac.getArtifactInfo() + "' from index.." );
                            indexerEngine.remove( context, ac );
                            context.getIndexWriter().commit();
                        }
                    }
                }
                catch ( IOException e )
                {
                    log.error( "Error occurred while executing indexing task '" + indexingTask + "'" );
                    throw new TaskExecutionException( "Error occurred while executing indexing task '" + indexingTask
                        + "'" );
                }
            }
        }
    }

    public void initialize()
        throws InitializationException
    {
        log.info( "Initialized " + this.getClass().getName() );

        artifactContextProducer = new DefaultArtifactContextProducer();
    }

    public void setIndexerEngine( IndexerEngine indexerEngine )
    {
        this.indexerEngine = indexerEngine;
    }

    public void setIndexPacker( IndexPacker indexPacker )
    {
        this.indexPacker = indexPacker;
    }
}