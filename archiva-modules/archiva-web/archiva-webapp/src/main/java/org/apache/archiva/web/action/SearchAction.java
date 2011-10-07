package org.apache.archiva.web.action;

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

import com.opensymphony.xwork2.Preparable;
import org.apache.archiva.admin.model.RepositoryAdminException;
import org.apache.archiva.admin.model.beans.ManagedRepository;
import org.apache.archiva.admin.model.managed.ManagedRepositoryAdmin;
import org.apache.archiva.indexer.search.RepositorySearch;
import org.apache.archiva.indexer.search.RepositorySearchException;
import org.apache.archiva.indexer.search.SearchFields;
import org.apache.archiva.indexer.search.SearchResultHit;
import org.apache.archiva.indexer.search.SearchResultLimits;
import org.apache.archiva.indexer.search.SearchResults;
import org.apache.archiva.metadata.model.ArtifactMetadata;
import org.apache.archiva.metadata.repository.MetadataRepository;
import org.apache.archiva.metadata.repository.RepositorySession;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Search all indexed fields by the given criteria.
 */
@Controller( "searchAction" )
@Scope( "prototype" )
public class SearchAction
    extends AbstractRepositoryBasedAction
    implements Preparable
{

    @Inject
    protected ManagedRepositoryAdmin managedRepositoryAdmin;

    /**
     * Query string.
     */
    private String q;

    /**
     * The Search Results.
     */
    private SearchResults results;

    private static final String RESULTS = "results";

    private static final String ARTIFACT = "artifact";

    private List<ArtifactMetadata> databaseResults;

    private int currentPage = 0;

    private int totalPages;

    private boolean searchResultsOnly;

    private String completeQueryString;

    private static final String COMPLETE_QUERY_STRING_SEPARATOR = ";";

    private List<String> managedRepositoryList = new ArrayList<String>();

    private String groupId;

    private String artifactId;

    private String version;

    private String className;

    /**
     * contains osgi metadata Bundle-Version if available
     *
     * @since 1.4-M1
     */
    private String bundleVersion;

    /**
     * contains osgi metadata Bundle-SymbolicName if available
     *
     * @since 1.4-M1
     */
    private String bundleSymbolicName;

    /**
     * contains osgi metadata Export-Package if available
     *
     * @since 1.4-M1
     */
    private String bundleExportPackage;

    /**
     * contains osgi metadata import package if available
     *
     * @since 1.4-M1
     */
    private String bundleImportPackage;

    /**
     * contains osgi metadata name if available
     *
     * @since 1.4-M1
     */
    private String bundleName;

    /**
     * contains osgi metadata Export-Service if available
     *
     * @since 1.4-M1
     */
    private String bundleExportService;

    private int rowCount = 30;

    private String repositoryId;

    private boolean fromFilterSearch;

    private boolean filterSearch = false;

    private boolean fromResultsPage;

    @Inject
    private RepositorySearch nexusSearch;

    private Map<String, String> searchFields;

    private String infoMessage;

    public boolean isFromResultsPage()
    {
        return fromResultsPage;
    }

    public void setFromResultsPage(boolean fromResultsPage)
    {
        this.fromResultsPage = fromResultsPage;
    }

    public boolean isFromFilterSearch()
    {
        return fromFilterSearch;
    }

    public void setFromFilterSearch(boolean fromFilterSearch)
    {
        this.fromFilterSearch = fromFilterSearch;
    }

    public void prepare()
    {
        managedRepositoryList = getObservableRepos();

        if ( managedRepositoryList.size() > 0 )
        {
            managedRepositoryList.add("all");
        }

        searchFields = new LinkedHashMap<String, String>();
        searchFields.put("groupId", "Group ID");
        searchFields.put("artifactId", "Artifact ID");
        searchFields.put("version", "Version");
        searchFields.put("className", "Class/Package Name");
        searchFields.put("rowCount", "Row Count");
        searchFields.put("bundleVersion", "OSGI Bundle Version");
        searchFields.put("bundleSymbolicName", "OSGI Bundle-SymbolicName");
        searchFields.put("bundleExportPackage", "OSGI Export-Package");
        searchFields.put("bundleImportPackage", "OSGI import package");
        searchFields.put("bundleName", "OSGI name");
        searchFields.put("bundleExportService", "OSGI Export-Service");

        super.clearErrorsAndMessages();
        clearSearchFields();
    }

    private void clearSearchFields()
    {
        repositoryId = "";
        artifactId = "";
        groupId = "";
        version = "";
        className = "";
        rowCount = 30;
        currentPage = 0;
    }

    // advanced search MRM-90 -- filtered search
    public String filteredSearch()
        throws MalformedURLException
    {
        if ( StringUtils.isBlank(groupId) && StringUtils.isBlank(artifactId) && StringUtils.isBlank(className)
            && StringUtils.isBlank(version) && StringUtils.isBlank(bundleExportPackage) && StringUtils.isBlank(
            bundleExportService) && StringUtils.isBlank(bundleImportPackage) && StringUtils.isBlank(bundleName)
            && StringUtils.isBlank(bundleSymbolicName) && StringUtils.isBlank(bundleVersion) )
        {
            addActionError("Advanced Search - At least one search criteria must be provided.");
            return INPUT;
        }

        fromFilterSearch = true;

        if ( CollectionUtils.isEmpty(managedRepositoryList) )
        {
            return GlobalResults.ACCESS_TO_NO_REPOS;
        }

        SearchResultLimits limits = new SearchResultLimits(currentPage);
        limits.setPageSize(rowCount);
        List<String> selectedRepos = new ArrayList<String>();

        if ( repositoryId == null || StringUtils.isBlank(repositoryId) || "all".equals(
            StringUtils.stripToEmpty(repositoryId)) )
        {
            selectedRepos = getObservableRepos();
        }
        else
        {
            selectedRepos.add(repositoryId);
        }

        if ( CollectionUtils.isEmpty(selectedRepos) )
        {
            return GlobalResults.ACCESS_TO_NO_REPOS;
        }

        SearchFields searchFields = new SearchFields(groupId, artifactId, version, null, className, selectedRepos);

        if ( StringUtils.isNotBlank(this.bundleExportPackage) )
        {
            searchFields.setBundleExportPackage(this.bundleExportPackage);
        }

        if ( StringUtils.isNotBlank(this.bundleExportService) )
        {
            searchFields.setBundleExportService(this.bundleExportService);
        }

        if ( StringUtils.isNotBlank(this.bundleImportPackage) )
        {
            searchFields.setBundleImportPackage(this.bundleImportPackage);
        }

        if ( StringUtils.isNotBlank(this.bundleSymbolicName) )
        {
            searchFields.setBundleSymbolicName(this.bundleSymbolicName);
        }

        if ( StringUtils.isNotBlank(this.bundleName) )
        {
            searchFields.setBundleName(this.bundleName);
        }

        if ( StringUtils.isNotBlank(this.bundleVersion) )
        {
            searchFields.setBundleVersion(this.bundleVersion);
        }

        log.debug("filteredSearch with searchFields {}", searchFields);

        // TODO: add packaging in the list of fields for advanced search (UI)?
        try
        {
            results = getNexusSearch().search(getPrincipal(), searchFields, limits);
        }
        catch ( RepositorySearchException e )
        {
            addActionError(e.getMessage());
            return ERROR;
        }

        if ( results.isEmpty() )
        {
            addActionError("No results found");
            return INPUT;
        }

        totalPages = results.getTotalHits() / limits.getPageSize();

        if ( ( results.getTotalHits() % limits.getPageSize() ) != 0 )
        {
            totalPages = totalPages + 1;
        }

        for ( SearchResultHit hit : results.getHits() )
        {
            // fix version ?
            //hit.setVersion( VersionUtil.getBaseVersion( version ) );

        }

        return SUCCESS;
    }

    @SuppressWarnings( "unchecked" )
    public String quickSearch()
        throws MalformedURLException
    {
        /* TODO: give action message if indexing is in progress.
         * This should be based off a count of 'unprocessed' artifacts.
         * This (yet to be written) routine could tell the user that X (unprocessed) artifacts are not yet
         * present in the full text search.
         */

        assert q != null && q.length() != 0;

        fromFilterSearch = false;

        SearchResultLimits limits = new SearchResultLimits(currentPage);

        List<String> selectedRepos = getObservableRepos();
        if ( CollectionUtils.isEmpty(selectedRepos) )
        {
            return GlobalResults.ACCESS_TO_NO_REPOS;
        }

        log.debug("quickSearch with selectedRepos {} query {}", selectedRepos, q);

        try
        {
            if ( searchResultsOnly && !completeQueryString.equals("") )
            {
                results = getNexusSearch().search(getPrincipal(), selectedRepos, q, limits, parseCompleteQueryString());
            }
            else
            {
                completeQueryString = "";
                results = getNexusSearch().search(getPrincipal(), selectedRepos, q, limits, null);
            }
        }
        catch ( RepositorySearchException e )
        {
            addActionError(e.getMessage());
            return ERROR;
        }

        if ( results.isEmpty() )
        {
            addActionError("No results found");
            return INPUT;
        }

        totalPages = results.getTotalHitsMapSize() / limits.getPageSize();

        if ( ( results.getTotalHitsMapSize() % limits.getPageSize() ) != 0 )
        {
            totalPages = totalPages + 1;
        }

        if ( !isEqualToPreviousSearchTerm(q) )
        {
            buildCompleteQueryString(q);
        }

        return SUCCESS;
    }

    public String findArtifact()
        throws Exception
    {
        // TODO: give action message if indexing is in progress

        if ( StringUtils.isBlank(q) )
        {
            addActionError("Unable to search for a blank checksum");
            return INPUT;
        }

        databaseResults = new ArrayList<ArtifactMetadata>();
        RepositorySession repositorySession = repositorySessionFactory.createSession();
        try
        {
            MetadataRepository metadataRepository = repositorySession.getRepository();
            for ( String repoId : getObservableRepos() )
            {
                databaseResults.addAll(metadataRepository.getArtifactsByChecksum(repoId, q));
            }
        }
        finally
        {
            repositorySession.close();
        }

        if ( databaseResults.isEmpty() )
        {
            addActionError("No results found");
            return INPUT;
        }

        if ( databaseResults.size() == 1 )
        {
            // 1 hit? return it's information directly!
            return ARTIFACT;
        }

        return RESULTS;
    }

    public String doInput()
    {
        return INPUT;
    }

    private void buildCompleteQueryString(String searchTerm)
    {
        if ( searchTerm.indexOf(COMPLETE_QUERY_STRING_SEPARATOR) != -1 )
        {
            searchTerm = StringUtils.remove(searchTerm, COMPLETE_QUERY_STRING_SEPARATOR);
        }

        if ( completeQueryString == null || "".equals(completeQueryString) )
        {
            completeQueryString = searchTerm;
        }
        else
        {
            completeQueryString = completeQueryString + COMPLETE_QUERY_STRING_SEPARATOR + searchTerm;
        }
    }

    private List<String> parseCompleteQueryString()
    {
        List<String> parsedCompleteQueryString = new ArrayList<String>();
        String[] parsed = StringUtils.split(completeQueryString, COMPLETE_QUERY_STRING_SEPARATOR);
        CollectionUtils.addAll(parsedCompleteQueryString, parsed);

        return parsedCompleteQueryString;
    }

    private boolean isEqualToPreviousSearchTerm(String searchTerm)
    {
        if ( !"".equals(completeQueryString) )
        {
            String[] parsed = StringUtils.split(completeQueryString, COMPLETE_QUERY_STRING_SEPARATOR);
            if ( StringUtils.equalsIgnoreCase(searchTerm, parsed[parsed.length - 1]) )
            {
                return true;
            }
        }

        return false;
    }

    public String getQ()
    {
        return q;
    }

    public void setQ(String q)
    {
        this.q = q;
    }

    public SearchResults getResults()
    {
        return results;
    }

    public List<ArtifactMetadata> getDatabaseResults()
    {
        return databaseResults;
    }

    public void setCurrentPage(int page)
    {
        this.currentPage = page;
    }

    public int getCurrentPage()
    {
        return currentPage;
    }

    public int getTotalPages()
    {
        return totalPages;
    }

    public void setTotalPages(int totalPages)
    {
        this.totalPages = totalPages;
    }

    public boolean isSearchResultsOnly()
    {
        return searchResultsOnly;
    }

    public void setSearchResultsOnly(boolean searchResultsOnly)
    {
        this.searchResultsOnly = searchResultsOnly;
    }

    public String getCompleteQueryString()
    {
        return completeQueryString;
    }

    public void setCompleteQueryString(String completeQueryString)
    {
        this.completeQueryString = completeQueryString;
    }

    public Map<String, ManagedRepository> getManagedRepositories()
        throws RepositoryAdminException
    {
        return managedRepositoryAdmin.getManagedRepositoriesAsMap();
    }

    // wtf : does nothing ??
    public void setManagedRepositories(Map<String, ManagedRepository> managedRepositories)
    {
    }

    public String getGroupId()
    {
        return groupId;
    }

    public void setGroupId(String groupId)
    {
        this.groupId = groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public void setArtifactId(String artifactId)
    {
        this.artifactId = artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public int getRowCount()
    {
        return rowCount;
    }

    public void setRowCount(int rowCount)
    {
        this.rowCount = rowCount;
    }

    public boolean isFilterSearch()
    {
        return filterSearch;
    }

    public void setFilterSearch(boolean filterSearch)
    {
        this.filterSearch = filterSearch;
    }

    public String getRepositoryId()
    {
        return repositoryId;
    }

    public void setRepositoryId(String repositoryId)
    {
        this.repositoryId = repositoryId;
    }

    public List<String> getManagedRepositoryList()
    {
        return managedRepositoryList;
    }

    public void setManagedRepositoryList(List<String> managedRepositoryList)
    {
        this.managedRepositoryList = managedRepositoryList;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public RepositorySearch getNexusSearch()
    {
        if ( nexusSearch == null )
        {
            WebApplicationContext wac =
                WebApplicationContextUtils.getRequiredWebApplicationContext(ServletActionContext.getServletContext());
            nexusSearch = wac.getBean("nexusSearch", RepositorySearch.class);
        }
        return nexusSearch;
    }

    public void setNexusSearch(RepositorySearch nexusSearch)
    {
        this.nexusSearch = nexusSearch;
    }

    public Map<String, String> getSearchFields()
    {
        return searchFields;
    }

    public void setSearchFields(Map<String, String> searchFields)
    {
        this.searchFields = searchFields;
    }

    public String getInfoMessage()
    {
        return infoMessage;
    }

    public void setInfoMessage(String infoMessage)
    {
        this.infoMessage = infoMessage;
    }

    public ManagedRepositoryAdmin getManagedRepositoryAdmin()
    {
        return managedRepositoryAdmin;
    }

    public void setManagedRepositoryAdmin(ManagedRepositoryAdmin managedRepositoryAdmin)
    {
        this.managedRepositoryAdmin = managedRepositoryAdmin;
    }

    public String getBundleVersion()
    {
        return bundleVersion;
    }

    public void setBundleVersion(String bundleVersion)
    {
        this.bundleVersion = bundleVersion;
    }

    public String getBundleSymbolicName()
    {
        return bundleSymbolicName;
    }

    public void setBundleSymbolicName(String bundleSymbolicName)
    {
        this.bundleSymbolicName = bundleSymbolicName;
    }

    public String getBundleExportPackage()
    {
        return bundleExportPackage;
    }

    public void setBundleExportPackage(String bundleExportPackage)
    {
        this.bundleExportPackage = bundleExportPackage;
    }

    public String getBundleImportPackage()
    {
        return bundleImportPackage;
    }

    public void setBundleImportPackage(String bundleImportPackage)
    {
        this.bundleImportPackage = bundleImportPackage;
    }

    public String getBundleName()
    {
        return bundleName;
    }

    public void setBundleName(String bundleName)
    {
        this.bundleName = bundleName;
    }

    public String getBundleExportService()
    {
        return bundleExportService;
    }

    public void setBundleExportService(String bundleExportService)
    {
        this.bundleExportService = bundleExportService;
    }
}
