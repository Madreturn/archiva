package org.apache.maven.archiva.repository.scanner;

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

import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.InvalidRepositoryContentConsumer;
import org.apache.maven.archiva.model.ArchivaRepository;

import java.util.List;

/**
 * InvalidScanConsumer 
 *
 * @author <a href="mailto:joakim@erdfelt.com">Joakim Erdfelt</a>
 * @version $Id$
 */
public class InvalidScanConsumer
    extends AbstractMonitoredConsumer
    implements InvalidRepositoryContentConsumer
{
    private int processCount = 0;

    public void beginScan( ArchivaRepository repository )
        throws ConsumerException
    {
        /* do nothing */
    }

    public void completeScan()
    {
        /* do nothing */
    }

    public List getExcludes()
    {
        return null;
    }

    public List getIncludes()
    {
        return null;
    }

    public void processFile( String path )
        throws ConsumerException
    {
        processCount++;
    }

    public String getDescription()
    {
        return "Bad Content Scan Consumer (for testing)";
    }

    public String getId()
    {
        return "test-invalid-consumer";
    }

    public boolean isPermanent()
    {
        return false;
    }

    public int getProcessCount()
    {
        return processCount;
    }

    public void setProcessCount( int processCount )
    {
        this.processCount = processCount;
    }
}
