package org.apache.archiva.proxy.common;

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

import org.apache.archiva.common.plexusbridge.PlexusSisuBridge;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.wagon.Wagon;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * @author Olivier Lamy
 * @since 1.4-M1
 */
@Service( "wagonFactory" )
public class DefaultWagonFactory
    implements WagonFactory
{

    private ApplicationContext applicationContext;

    private DebugTransferListener debugTransferListener = new DebugTransferListener();

    @Inject
    public DefaultWagonFactory( ApplicationContext applicationContext )
    {
        this.applicationContext = applicationContext;
    }

    public Wagon getWagon( String protocol )
        throws WagonFactoryException
    {
        try
        {
            protocol = StringUtils.startsWith( protocol, "wagon#" ) ? protocol : "wagon#" + protocol;

            Wagon wagon = applicationContext.getBean( protocol, Wagon.class );
            wagon.addTransferListener( debugTransferListener );
            return wagon;
        }
        catch ( BeansException e )
        {
            throw new WagonFactoryException( e.getMessage(), e );
        }
    }
}
