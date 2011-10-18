package org.apache.archiva.rest.services;
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

import org.apache.archiva.admin.model.AuditInformation;
import org.apache.archiva.audit.AuditListener;
import org.codehaus.plexus.redback.users.User;
import org.codehaus.redback.rest.services.RedbackAuthenticationThreadLocal;
import org.codehaus.redback.rest.services.RedbackRequestInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * abstract class with common utilities methods
 *
 * @author Olivier Lamy
 * @since 1.4-M1
 */
public abstract class AbstractRestService
{

    protected Logger log = LoggerFactory.getLogger( getClass() );

    @Inject
    private List<AuditListener> auditListeners = new ArrayList<AuditListener>();

    protected AuditInformation getAuditInformation()
    {
        RedbackRequestInformation redbackRequestInformation = RedbackAuthenticationThreadLocal.get();
        User user = redbackRequestInformation == null ? null : redbackRequestInformation.getUser();
        String remoteAddr = redbackRequestInformation == null ? null : redbackRequestInformation.getRemoteAddr();
        return new AuditInformation( user, remoteAddr );
    }
}