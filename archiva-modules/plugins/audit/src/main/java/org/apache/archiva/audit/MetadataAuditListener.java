package org.apache.archiva.audit;

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

/**
 * @plexus.component role="org.apache.maven.archiva.repository.audit.AuditListener" role-hint="metadata"
 */
public class MetadataAuditListener
    implements AuditListener
{
    /**
     * @plexus.requirement
     */
    private AuditManager auditManager;

    public void auditEvent( AuditEvent event )
    {
        // for now we only log upload events, some of the others are quite noisy
        if ( event.getAction().equals( AuditEvent.CREATE_FILE ) || event.getAction().equals( AuditEvent.UPLOAD_FILE ) )
        {
            auditManager.addAuditEvent( event );
        }
    }
}
