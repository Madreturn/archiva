package org.apache.archiva.rest.api.services;

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


import org.apache.archiva.maven2.model.Artifact;
import org.apache.archiva.rest.api.model.GroupIdList;
import org.apache.archiva.rest.api.model.SearchRequest;
import org.apache.archiva.rest.api.model.StringList;
import org.apache.archiva.redback.authorization.RedbackAuthorization;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path( "/searchService/" )
public interface SearchService
{
    /*
    * quick/general text search which returns a list of artifacts
    * query for an artifact based on a checksum
    * query for all available versions of an artifact, sorted in version significance order
    * query for an artifact's direct dependencies
    * TODO query for an artifact's dependency tree (as with mvn dependency:tree - no duplicates should be included)
    * TODO query for all artifacts that depend on a given artifact
    */

    @Path( "quickSearch" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    /**
     * <b>search will be apply on all repositories the current user has karma</b>
     */
    List<Artifact> quickSearch( @QueryParam( "queryString" ) String queryString )
        throws ArchivaRestServiceException;

    @Path( "quickSearchWithRepositories" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    /**
     * <b>if not repositories in SearchRequest: search will be apply on all repositories the current user has karma</b>
     */
    List<Artifact> quickSearchWithRepositories( SearchRequest searchRequest )
        throws ArchivaRestServiceException;

    @Path( "searchArtifacts" )
    @POST
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    /**
     * If searchRequest contains repositories, the search will be done only on those repositories.
     * <b>if no repositories, the search will be apply on all repositories the current user has karma</b>
     */
    List<Artifact> searchArtifacts( SearchRequest searchRequest )
        throws ArchivaRestServiceException;

    @Path( "getArtifactVersions" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    /**
     * <b>search will be apply on all repositories the current user has karma</b>
     */
    List<Artifact> getArtifactVersions( @QueryParam( "groupId" ) String groupId,
                                        @QueryParam( "artifactId" ) String artifactId,
                                        @QueryParam( "packaging" ) String packaging )
        throws ArchivaRestServiceException;


    @Path( "getAllGroupIds" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noPermission = true, noRestriction = false )
    /**
     * <b>this method applies on Maven Indexer lucene index, so datas not yet indexed won't be available</b>
     */
    GroupIdList getAllGroupIds( @QueryParam( "selectedRepos" ) List<String> selectedRepos )
        throws ArchivaRestServiceException;

    @Path( "observableRepoIds" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML } )
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    /**
     * @since 1.4-M3
     */
    StringList getObservablesRepoIds()
        throws ArchivaRestServiceException;

    /*
    @Path( "getDependencies" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    List<Dependency> getDependencies( @QueryParam( "groupId" ) String groupId,
                                      @QueryParam( "artifactId" ) String artifactId,
                                      @QueryParam( "version" ) String version )
        throws ArchivaRestServiceException;


    @Path( "getArtifactByChecksum" )
    @GET
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN } )
    @RedbackAuthorization( noPermission = true, noRestriction = true )
    List<Artifact> getArtifactByChecksum( @QueryParam( "checksum" ) String checksum )
        throws ArchivaRestServiceException;
    */

}
