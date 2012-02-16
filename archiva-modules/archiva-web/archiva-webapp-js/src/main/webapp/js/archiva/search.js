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
$(function() {

  BrowseTopViewModel=function(groupIds){
    this.groupIds=groupIds;
    var mainContent = $("#main-content");
    var browseResult=mainContent.find("#browse_result");
    displayGroupDetail=function(groupId){
      browseResult.hide( "slide", {}, 500,
        function(){
          browseResult.html(mediumSpinnerImg());
          browseResult.show();
          $.ajax("restServices/archivaServices/browseService/browseGroupId/"+encodeURIComponent(groupId), {
            type: "GET",
            dataType: 'json',
            success: function(data) {
              var browseGroupIdEntryies = $.isArray(data.browseGroupIdResult.browseGroupIdEntries) ?
                   $.map(data.browseGroupIdResult.browseGroupIdEntries,function(item){
                     return new BrowseGroupIdEntry(item.name, item.project);
                   }): [data.browseGroupIdResult.browseGroupIdEntries];
              browseResult.html($("#browse-groups-div-tmpl" ).tmpl());
              var browseGroupsViewModel = new BrowseGroupsViewModel(browseGroupIdEntryies);

              ko.applyBindings(browseGroupsViewModel,mainContent.find("#browse-groups-div" ).get(0));
            }
         });
        }
      );

    }
  }

  BrowseGroupsViewModel=function(browseGroupIdEntryies){
    this.browseGroupIdEntryies=browseGroupIdEntryies;
  }

  displayBrowse=function(){
    var mainContent = $("#main-content");
    mainContent.html($("#browse-tmpl" ).tmpl());
    mainContent.find("#browse_result").html(mediumSpinnerImg());
    $.ajax("restServices/archivaServices/browseService/rootGroups", {
        type: "GET",
        dataType: 'json',
        success: function(data) {
          var groupdIds =
              $.isArray(data.groupIdList.groupIds)? $.map(data.groupIdList.groupIds,function(item){
                return item;
              }): [data.groupIdList.groupIds];
          $.log("size:"+groupdIds.length);
          var browseTopViewModel = new BrowseTopViewModel(groupdIds);

          ko.applyBindings(browseTopViewModel,mainContent.find("#browse_result" ).get(0));
        }
    });
  }

  displaySearch=function(){
    $("#main-content" ).html("coming soon :-)");
  }

  BrowseGroupIdEntry=function(name,project){
    this.name=name;
    this.project=project;
  }
});