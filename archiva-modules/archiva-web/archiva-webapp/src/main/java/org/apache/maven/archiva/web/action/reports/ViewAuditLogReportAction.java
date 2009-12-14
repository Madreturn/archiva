package org.apache.maven.archiva.web.action.reports;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.maven.archiva.database.ArchivaAuditLogsDao;
import org.apache.maven.archiva.database.ArchivaDatabaseException;
import org.apache.maven.archiva.database.Constraint;
import org.apache.maven.archiva.database.ObjectNotFoundException;
import org.apache.maven.archiva.database.constraints.MostRecentArchivaAuditLogsConstraint;
import org.apache.maven.archiva.model.ArchivaAuditLogs;
import org.apache.maven.archiva.security.AccessDeniedException;
import org.apache.maven.archiva.security.ArchivaSecurityException;
import org.apache.maven.archiva.security.PrincipalNotFoundException;
import org.apache.maven.archiva.security.UserRepositories;
import org.apache.maven.archiva.web.action.PlexusActionSupport;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.codehaus.redback.integration.interceptor.SecureAction;
import org.codehaus.redback.integration.interceptor.SecureActionBundle;
import org.codehaus.redback.integration.interceptor.SecureActionException;

import com.opensymphony.xwork2.Preparable;

/**
 * @plexus.component role="com.opensymphony.xwork2.Action" role-hint="viewAuditLogReport" instantiation-strategy="per-lookup"
 */
public class ViewAuditLogReportAction
    extends PlexusActionSupport
    implements SecureAction, ServletRequestAware, Preparable
{    
    protected HttpServletRequest request;
    
    /**
     * @plexus.requirement
     */
    private UserRepositories userRepositories;
    
    /**
     * @plexus.requirement role-hint="jdo"
     */
    private ArchivaAuditLogsDao auditLogsDao;
    
    private String repository;

    private List<String> repositories;
    
    private String groupId;
    
    private String artifactId;
        
    private int rowCount = 30;
    
    private List<ArchivaAuditLogs> auditLogs = new ArrayList<ArchivaAuditLogs>();    
    
    public SecureActionBundle getSecureActionBundle()
        throws SecureActionException
    {        
        return null;
    }

    public void setServletRequest( HttpServletRequest request )
    {   
        this.request = request;
    }

    public void prepare()
        throws Exception
    {     
        repositories = getObservableRepositories();
        
        Constraint constraint = new MostRecentArchivaAuditLogsConstraint();
        
        try
        {
            this.auditLogs = auditLogsDao.queryAuditLogs( constraint );            
        }
        catch( ObjectNotFoundException e )
        {
            log.warn( "No audit logs found." );
        }
        catch ( ArchivaDatabaseException e )
        {
            log.warn( "Error occurred while querying audit logs." );
        }
    }
    
    public String execute()
        throws Exception
    {   
        return SUCCESS;
    }
    
    private List<String> getObservableRepositories()
    {
        try
        {
            return userRepositories.getObservableRepositoryIds( getPrincipal() );
        }
        catch ( PrincipalNotFoundException e )
        {
            log.warn( e.getMessage(), e );
        }
        catch ( AccessDeniedException e )
        {
            log.warn( e.getMessage(), e );
        }
        catch ( ArchivaSecurityException e )
        {
            log.warn( e.getMessage(), e );
        }
        return Collections.emptyList();
    }
    
    public String getRepository()
    {
        return repository;
    }

    public void setRepository( String repository )
    {
        this.repository = repository;
    }

    public List<String> getRepositories()
    {
        return repositories;
    }

    public void setRepositories( List<String> repositories )
    {
        this.repositories = repositories;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId( String groupId )
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId( String artifactId )
    {
        this.artifactId = artifactId;
    }
    
    public List<ArchivaAuditLogs> getAuditLogs()
    {
        return auditLogs;
    }

    public void setAuditLogs( List<ArchivaAuditLogs> auditLogs )
    {
        this.auditLogs = auditLogs;
    }

    public int getRowCount()
    {
        return rowCount;
    }

    public void setRowCount( int rowCount )
    {
        this.rowCount = rowCount;
    }
}