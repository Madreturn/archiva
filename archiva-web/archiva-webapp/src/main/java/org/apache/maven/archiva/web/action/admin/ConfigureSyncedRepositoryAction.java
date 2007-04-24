package org.apache.maven.archiva.web.action.admin;

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

//import org.apache.maven.archiva.configuration.AbstractRepositoryConfiguration;
//import org.apache.maven.archiva.configuration.SyncedRepositoryConfiguration;

import java.io.IOException;

/**
 * Configures the application repositories.
 *
 * @plexus.component role="com.opensymphony.xwork.Action" role-hint="configureSyncedRepositoryAction"
 */
public class ConfigureSyncedRepositoryAction
    extends AbstractConfigureRepositoryAction
{
//    protected void removeRepository( AbstractRepositoryConfiguration existingRepository )
//    {
//        configuration.removeSyncedRepository( (SyncedRepositoryConfiguration) existingRepository );
//    }
//
//    protected AbstractRepositoryConfiguration getRepository( String id )
//    {
//        return configuration.getSyncedRepositoryById( id );
//    }

    protected void addRepository()
        throws IOException
    {
//        SyncedRepositoryConfiguration repository = (SyncedRepositoryConfiguration) getRepository();
//
//        configuration.addSyncedRepository( repository );
    }

//    protected AbstractRepositoryConfiguration createRepository()
//    {
//        return new SyncedRepositoryConfiguration();
//    }
}
