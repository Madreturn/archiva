package org.apache.archiva.webapp.ui.services.api;
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

import org.apache.archiva.web.runtime.ArchivaRuntimeInfo;
import org.apache.archiva.webapp.ui.services.model.ApplicationRuntimeInfo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author Olivier Lamy
 */
@Service( "runtimeInfoService#rest" )
public class DefaultRuntimeInfoService
    implements RuntimeInfoService
{

    private Logger i18nLogger = LoggerFactory.getLogger( "archivaMissingi18n.logger" );

    private ArchivaRuntimeInfo archivaRuntimeInfo;

    @Context
    protected HttpServletRequest httpServletRequest;

    @Inject
    public DefaultRuntimeInfoService( ArchivaRuntimeInfo archivaRuntimeInfo )
    {
        this.archivaRuntimeInfo = archivaRuntimeInfo;
    }

    public ApplicationRuntimeInfo getApplicationRuntimeInfo( String locale )
    {
        ApplicationRuntimeInfo applicationRuntimeInfo = new ApplicationRuntimeInfo();
        applicationRuntimeInfo.setBuildNumber( this.archivaRuntimeInfo.getBuildNumber() );
        applicationRuntimeInfo.setTimestamp( this.archivaRuntimeInfo.getTimestamp() );
        applicationRuntimeInfo.setVersion( this.archivaRuntimeInfo.getVersion() );
        applicationRuntimeInfo.setBaseUrl( getBaseUrl( httpServletRequest ) );

        SimpleDateFormat sfd = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssz",
                                                     new Locale( StringUtils.isEmpty( locale ) ? "en" : locale ) );
        applicationRuntimeInfo.setTimestampStr( sfd.format( new Date( archivaRuntimeInfo.getTimestamp() ) ) );

        return applicationRuntimeInfo;
    }

    protected String getBaseUrl( HttpServletRequest req )
    {
        return req.getScheme() + "://" + req.getServerName() + ( req.getServerPort() == 80
            ? ""
            : ":" + req.getServerPort() ) + req.getContextPath();
    }

    public Boolean logMissingI18n( String key )
    {
        i18nLogger.info( "missing i18n key : '{}'", key );
        return Boolean.TRUE;
    }
}
