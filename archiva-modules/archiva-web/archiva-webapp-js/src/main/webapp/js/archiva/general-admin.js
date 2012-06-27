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
define("archiva.general-admin",["jquery","i18n","order!utils","order!jquery.tmpl","order!knockout","order!knockout.simpleGrid",
  "jquery.validate","bootstrap"]
    , function() {

  //-------------------------
  // legacy path part
  //-------------------------

  LegacyArtifactPath=function(path,groupId,artifactId,version,classifier,type,update){
    //private String path;
    this.path=ko.observable(path);

    /**
     * The artifact reference, as " [groupId] :
     * [artifactId] : [version] : [classifier] : [type] ".
     */
    //private String artifact;
    //this.artifact=ko.observable(artifact);
    this.update=update;
    //private String groupId;
    this.groupId=ko.observable(groupId);

    //private String artifactId;
    this.artifactId=ko.observable(artifactId);

    //private String version;
    this.version=ko.observable(version);

    //private String classifier;
    this.classifier=ko.observable(classifier);

    //private String type;
    this.type=ko.observable(type);

    this.modified=ko.observable();

    this.artifact = ko.computed(function() {
      var artifactValue="";
      if (this.groupId()){
        artifactValue+=this.groupId();
      }
      if (this.artifactId()){
        artifactValue+=":"+this.artifactId();
      }
      if (this.version()){
        artifactValue+=":"+this.version();
      }
      if (this.classifier()){
        artifactValue+=":"+this.classifier();
      }
      if (this.type()){
        artifactValue+=":"+this.type();
      }
      return artifactValue;
    }, this);
  }

  mapLegacyArtifactPaths=function(data){
    if (data){
      return $.isArray(data)? $.map(data,function(item){
        return mapLegacyArtifactPath(item);
      }):[mapLegacyArtifactPath(data)];
    }
    return [];
  }

  mapLegacyArtifactPath=function(data){
    return data?new LegacyArtifactPath(data.path,data.groupId,data.artifactId,data.version,data.classifier,data.type):null;
  }

  activateLegacyArtifactPathFormValidation=function(){
    var theForm=$("#main-content #legacy-artifact-paths-edit-form");
    var validator = theForm.validate({
      showErrors: function(validator, errorMap, errorList) {
       customShowError("#main-content #legacy-artifact-paths-edit-form",validator,errorMap,errorMap);
      }
    });
  }

  LegacyArtifactPathViewModel=function(legacyArtifactPath,update,legacyArtifactPathsViewModel){
    var self=this;
    this.update=update;
    this.legacyArtifactPath=legacyArtifactPath;
    this.legacyArtifactPathsViewModel=legacyArtifactPathsViewModel;

    this.display=function(){
      var mainContent=$("#main-content");
      ko.applyBindings(self,mainContent.find("#legacy-artifact-paths-edit" ).get(0));
      mainContent.find("#legacy-artifact-paths-view-tabs-li-edit a").html($.i18n.prop("edit"));
      activateLegacyArtifactPathFormValidation();
      activateLegacyArtifactPathsEditTab();
    }

    displayGrid=function(){
      activateLegacyArtifactPathsGridTab();
    }

    calculatePath=function(){
      var path="";
      if (self.legacyArtifactPath.groupId()){
        path+=self.legacyArtifactPath.groupId()+"/jars/";
      }
      if (self.legacyArtifactPath.artifactId()){
        path+=self.legacyArtifactPath.artifactId();
      }
      if (self.legacyArtifactPath.version()){
        path+="-"+self.legacyArtifactPath.version();
      }
      if (self.legacyArtifactPath.classifier()){
        path+="-"+self.legacyArtifactPath.classifier();
      }
      if (self.legacyArtifactPath.type()){
        path+="."+self.legacyArtifactPath.type();
      }
      self.legacyArtifactPath.path(path);
    }

    this.save=function(){
      var theForm=$("#main-content #legacy-artifact-paths-edit-form");
      if (!theForm.valid()){
        return;
      }
      // do that on server side
      /*if (theForm.find("#artifact" ).val()
          !=theForm.find("#path" ).val()){
        var errorList=[{
          message: $.i18n.prop("path must match artifact"),
    		  element: theForm.find("#path" ).get(0)
        }];
        customShowError("#main-content #legacy-artifact-paths-edit-form", null, null, errorList);
        return;
      }*/
      // TODO call id exists if add ?
      clearUserMessages();
      $.log("save ok");
      if (self.update){
        $.log("update");
      }else {
        $.ajax("restServices/archivaServices/archivaAdministrationService/addLegacyArtifactPath",
          {
            type: "POST",
            contentType: 'application/json',
            data: ko.toJSON(self.legacyArtifactPath),
            dataType: 'json',
            success: function(data) {
              self.legacyArtifactPath.modified(false);
              self.legacyArtifactPathsViewModel.legacyArtifactPaths.push(self.legacyArtifactPath);
              displaySuccessMessage($.i18n.prop('legacy-artifact-path.added',self.legacyArtifactPath.path()));
              activateLegacyArtifactPathsGridTab();
            },
            error: function(data) {
              var res = $.parseJSON(data.responseText);
              displayRestError(res);
            }
          }
        );
      }
    }
  }

  LegacyArtifactPathsViewModel=function(){
    var self=this;
    this.legacyArtifactPaths=ko.observableArray([]);

    this.gridViewModel = new ko.simpleGrid.viewModel({
      data: self.legacyArtifactPaths,
      columns: [
        {
          headerText: $.i18n.prop('legacy-artifact-paths.path'),
          rowText: "path"
        },
        {
          headerText: $.i18n.prop('legacy-artifact-paths.artifact'),
          rowText: "artifact"
        }
      ],
      pageSize: 5,
      gridUpdateCallBack: function(networkProxy){
        $("#main-content #legacy-artifact-paths-table [title]").tooltip();
      }
    });


    editLegacyArtifactPath=function(legacyArtifactPath){
      var legacyArtifactPathViewModel=new LegacyArtifactPathViewModel(legacyArtifactPath,true);
      legacyArtifactPathViewModel.display();
    }

    removeLegacyArtifactPath=function(legacyArtifactPath){

      openDialogConfirm(
          function(){

            $.ajax("restServices/archivaServices/archivaAdministrationService/deleteLegacyArtifactPath?path="+encodeURIComponent(legacyArtifactPath.path()),
              {
                type: "GET",
                dataType: 'json',
                success: function(data) {
                  self.legacyArtifactPaths.remove(legacyArtifactPath);
                  displaySuccessMessage($.i18n.prop('legacy-artifact-path.removed',legacyArtifactPath.path()));
                  activateLegacyArtifactPathsGridTab();
                },
                error: function(data) {
                  var res = $.parseJSON(data.responseText);
                  displayRestError(res);
                },
                complete: function(){
                  closeDialogConfirm();
                }
              }
            );
          }, $.i18n.prop('ok'), $.i18n.prop('cancel'), $.i18n.prop('legacy-artifact-path.delete.confirm',legacyArtifactPath.path()),
                      $("#legacy-artifact-path-delete-warning-tmpl" ).tmpl(legacyArtifactPath));

    }

    updateLegacyArtifactPath=function(legacyArtifactPath){

    }

  }

  displayLegacyArtifactPathSupport=function(){
    clearUserMessages();
    var mainContent=$("#main-content");
    mainContent.html(mediumSpinnerImg());

    $.ajax("restServices/archivaServices/archivaAdministrationService/getLegacyArtifactPaths", {
        type: "GET",
        dataType: 'json',
        success: function(data){
          mainContent.html($("#legacy-artifact-path-main").tmpl());
          var legacyArtifactPathsViewModel=new LegacyArtifactPathsViewModel();
          var legacyPaths=mapLegacyArtifactPaths(data);
          $.log("legacyPaths:"+legacyPaths.length);
          legacyArtifactPathsViewModel.legacyArtifactPaths(legacyPaths);
          ko.applyBindings(legacyArtifactPathsViewModel,mainContent.find("#legacy-artifact-paths-view" ).get(0));

          mainContent.find("#legacy-artifact-paths-view-tabs").on('show', function (e) {
            if ($(e.target).attr("href")=="#legacy-artifact-paths-edit") {
              var viewModel = new LegacyArtifactPathViewModel(new LegacyArtifactPath(),false,legacyArtifactPathsViewModel);
              viewModel.display();
              activateLegacyArtifactPathFormValidation();
              clearUserMessages();
            }
            if ($(e.target).attr("href")=="#legacy-artifact-paths-view") {
              mainContent.find("#legacy-artifact-paths-view-tabs-li-edit a").html($.i18n.prop("add"));
              clearUserMessages();
            }

          });


          activateLegacyArtifactPathsGridTab();
        }
    });


  }


  activateLegacyArtifactPathsGridTab=function(){
    var mainContent = $("#main-content");
    mainContent.find("#legacy-artifact-paths-view-tabs-li-edit").removeClass("active");
    mainContent.find("#legacy-artifact-paths-edit").removeClass("active");

    mainContent.find("#legacy-artifact-paths-view-tabs-li-grid").addClass("active");
    mainContent.find("#legacy-artifact-paths-view").addClass("active");
    mainContent.find("#legacy-artifact-paths-view-tabs-li-edit a").html($.i18n.prop("add"));

  }

  activateLegacyArtifactPathsEditTab=function(){
    var mainContent = $("#main-content");
    mainContent.find("#legacy-artifact-paths-view-tabs-li-grid").removeClass("active");
    mainContent.find("#legacy-artifact-paths-view").removeClass("active");

    mainContent.find("#legacy-artifact-paths-view-tabs-li-edit").addClass("active");
    mainContent.find("#legacy-artifact-paths-edit").addClass("active");
  }


  //---------------------------
  // repository scanning part
  //---------------------------

  FileType=function(id,patterns){
    //private String id;
    this.id=ko.observable(id);

    //private List<String> patterns;
    this.patterns=ko.observableArray(patterns);

  }

  mapFileType=function(data){
    return new FileType(data.id,data.patterns);
  }

  mapFileTypes=function(data){
    if (data!=null){
      return $.isArray(data)? $.map(data,function(item){
        return mapFileType(item)
      }):[mapFileType(data)];
    }
    return [];
  }

  AdminRepositoryConsumer=function(enabled,id,description){
    //private boolean enabled = false;
    this.enabled=ko.observable(enabled);

    //private String id;
    this.id=ko.observable(id)

    //private String description;
    this.description=ko.observable(description);
  }

  mapAdminRepositoryConsumer=function(data){
    return new AdminRepositoryConsumer(data.enabled,data.id,data.description);
  }

  mapAdminRepositoryConsumers=function(data){
    if (data!=null){
      return $.isArray(data)? $.map(data,function(item){
        return mapAdminRepositoryConsumer(item)
      }):[mapAdminRepositoryConsumer(data)];
    }
    return [];
  }



  RepositoryScanningViewModel=function(){
    var self=this;
    this.fileTypes=ko.observableArray([]);
    this.knownAdminRepositoryConsumers=ko.observableArray([]);
    this.invalidAdminRepositoryConsumers=ko.observableArray([]);

    this.findFileType=function(id){
      var fileType=null;
      for (var i=0;i<self.fileTypes().length;i++){
        if (id==self.fileTypes()[i].id()){
          fileType=self.fileTypes()[i];
        }
      }
      return fileType;
    }

    removeFileTypePattern=function(id,pattern){
      clearUserMessages();
      var url="restServices/archivaServices/archivaAdministrationService/removeFileTypePattern?"
      url+="fileTypeId="+encodeURIComponent(id);
      url+="&pattern="+encodeURIComponent(pattern);
      $.ajax(url, {
          type: "GET",
          dataType: 'json',
          success: function(data){
            self.findFileType(id ).patterns.remove(pattern);
            displaySuccessMessage( $.i18n.prop("repository-scanning.file-types.removed.pattern",id,pattern));

          }
      });
    }

    addFileTypePattern=function(id){
      var pattern=$("#main-content #pattern-"+id ).val();
      $.log("addFileTypePattern:"+id+":"+pattern);
      clearUserMessages();
      var url="restServices/archivaServices/archivaAdministrationService/addFileTypePattern?"
      url+="fileTypeId="+encodeURIComponent(id);
      url+="&pattern="+encodeURIComponent(pattern);
      $.ajax(url, {
          type: "GET",
          dataType: 'json',
          success: function(data){
            self.findFileType(id ).patterns.push(pattern);
            displaySuccessMessage( $.i18n.prop("repository-scanning.file-types.added.pattern",id,pattern));

          }
      });
    }

    disableKnowContentConsumer=function(adminRepositoryConsumer){
      clearUserMessages();
      var url="restServices/archivaServices/archivaAdministrationService/disabledKnownContentConsumer/"
      url+=encodeURIComponent(adminRepositoryConsumer.id());
      $.ajax(url, {
          type: "GET",
          dataType: 'json',
          success: function(data){
            adminRepositoryConsumer.enabled(false);
            displaySuccessMessage( $.i18n.prop("repository-scanning.consumers.know.disabled",adminRepositoryConsumer.id()));
          }
      });
    }

    enableKnowContentConsumer=function(adminRepositoryConsumer){
      clearUserMessages();
      var url="restServices/archivaServices/archivaAdministrationService/enabledKnownContentConsumer/"
      url+=encodeURIComponent(adminRepositoryConsumer.id());
      $.ajax(url, {
          type: "GET",
          dataType: 'json',
          success: function(data){
            adminRepositoryConsumer.enabled(true);
            displaySuccessMessage( $.i18n.prop("repository-scanning.consumers.know.enabled",adminRepositoryConsumer.id()));
          }
      });
    }

    disableInvalidContentConsumer=function(adminRepositoryConsumer){
      clearUserMessages();
      var url="restServices/archivaServices/archivaAdministrationService/disabledInvalidContentConsumer/"
      url+=encodeURIComponent(adminRepositoryConsumer.id());
      $.ajax(url, {
          type: "GET",
          dataType: 'json',
          success: function(data){
            adminRepositoryConsumer.enabled(false);
            displaySuccessMessage( $.i18n.prop("repository-scanning.consumers.invalid.disabled",adminRepositoryConsumer.id()));
          }
      });
    }

    enableInvalidContentConsumer=function(adminRepositoryConsumer){
      clearUserMessages();
      var url="restServices/archivaServices/archivaAdministrationService/enabledInvalidContentConsumer/"
      url+=encodeURIComponent(adminRepositoryConsumer.id());
      $.ajax(url, {
          type: "GET",
          dataType: 'json',
          success: function(data){
            adminRepositoryConsumer.enabled(true);
            displaySuccessMessage( $.i18n.prop("repository-scanning.consumers.invalid.enabled",adminRepositoryConsumer.id()));
          }
      });
    }

  }

  displayRepositoryScanning=function(){
    screenChange();
    var mainContent=$("#main-content");

    mainContent.html($("#repository-scanning-main").tmpl());
    mainContent.find("#file-types-content").html(mediumSpinnerImg());
    mainContent.find("#consumers-known-content").html(mediumSpinnerImg());
    mainContent.find("#consumers-invalid-content").html(mediumSpinnerImg());

    var repositoryScanningViewModel=new RepositoryScanningViewModel();

    $.ajax("restServices/archivaServices/archivaAdministrationService/getFileTypes", {
        type: "GET",
        dataType: 'json',
        success: function(data){
          var fileTypes=mapFileTypes(data);
          repositoryScanningViewModel.fileTypes(fileTypes);
          ko.applyBindings(repositoryScanningViewModel,mainContent.find("#file-types-content").get(0));
        }
    });

    $.ajax("restServices/archivaServices/archivaAdministrationService/getKnownContentAdminRepositoryConsumers", {
        type: "GET",
        dataType: 'json',
        success: function(data){
          var knownAdminRepositoryConsumers=mapAdminRepositoryConsumers(data);
          repositoryScanningViewModel.knownAdminRepositoryConsumers(knownAdminRepositoryConsumers);
          ko.applyBindings(repositoryScanningViewModel,mainContent.find("#consumers-known-content").get(0));
        }
    });

    $.ajax("restServices/archivaServices/archivaAdministrationService/getInvalidContentAdminRepositoryConsumers", {
        type: "GET",
        dataType: 'json',
        success: function(data){
          var invalidAdminRepositoryConsumers=mapAdminRepositoryConsumers(data);
          repositoryScanningViewModel.invalidAdminRepositoryConsumers(invalidAdminRepositoryConsumers);
          ko.applyBindings(repositoryScanningViewModel,mainContent.find("#consumers-invalid-content").get(0));
        }
    });

  }

  //---------------------------
  // network configuration part
  //---------------------------

  NetworkConfiguration=function(maxTotal,maxTotalPerHost,usePooling){
    //private int maxTotal = 30;
    this.maxTotal=ko.observable(maxTotal);

    //private int maxTotalPerHost = 30;
    this.maxTotalPerHost=ko.observable(maxTotalPerHost);

    //private boolean usePooling = true;
    this.usePooling=ko.observable(usePooling);
  }

  NetworkConfigurationViewModel=function(networkConfiguration){
    var self=this;
    this.networkConfiguration=ko.observable(networkConfiguration);

    save=function(){
      if (!$("#main-content #network-configuration-edit-form").valid()){
        return;
      }
      clearUserMessages();
      $.ajax("restServices/archivaServices/archivaAdministrationService/setNetworkConfiguration", {
        type: "POST",
        contentType: 'application/json',
        data: ko.toJSON(self.networkConfiguration),
        dataType: 'json',
        success: function(data){
          displaySuccessMessage( $.i18n.prop("network-configuration.updated"));
        }
      });
    }
  }

  displayNetworkConfiguration=function(){
    screenChange();
    var mainContent=$("#main-content");
    mainContent.html(mediumSpinnerImg());

    $.ajax("restServices/archivaServices/archivaAdministrationService/getNetworkConfiguration", {
        type: "GET",
        dataType: 'json',
        success: function(data){
          mainContent.html($("#network-configuration-screen").tmpl());
          var networkConfiguration=new NetworkConfiguration(data.maxTotal,data.maxTotalPerHost,data.usePooling);
          var networkConfigurationViewModel=new NetworkConfigurationViewModel(networkConfiguration);
          ko.applyBindings(networkConfigurationViewModel,mainContent.get(0));
          var validator = mainContent.find("#network-configuration-edit-form").validate({
            showErrors: function(validator, errorMap, errorList) {
             customShowError(mainContent.find("#network-configuration-edit-form" ).get(0),validator,errorMap,errorMap);
            }
          });
        }
    });

  }

  //---------------------------
  // UiConfiguration part
  //---------------------------

  displayUiConfiguration=function(){
    screenChange();
    var mainContent=$("#main-content");
    mainContent.html(mediumSpinnerImg());
    $.ajax("restServices/archivaServices/archivaAdministrationService/getUiConfiguration", {
        type: "GET",
        dataType: 'json',
        success: function(data){
          mainContent.html($("#ui-configuration-screen").tmpl());
          var uiConfiguration=new UiConfiguration(data.showFindArtifacts,data.appletFindEnabled,data.disableEasterEggs,data.applicationUrl);
          var uiConfigurationViewModel=new UiConfigurationViewModel(uiConfiguration);
          ko.applyBindings(uiConfigurationViewModel,mainContent.get(0));
        }
    });
  }

  UiConfiguration=function(showFindArtifacts,appletFindEnabled,disableEasterEggs,applicationUrl){
    this.showFindArtifacts = ko.observable(showFindArtifacts);

    this.appletFindEnabled = ko.observable(appletFindEnabled);

    this.disableEasterEggs = ko.observable(disableEasterEggs);

    this.applicationUrl = ko.observable(applicationUrl);
  }

  UiConfigurationViewModel=function(uiConfiguration){
    this.uiConfiguration=ko.observable(uiConfiguration);
    var self=this;
    save=function(){
      $.ajax("restServices/archivaServices/archivaAdministrationService/setUiConfiguration", {
        type: "POST",
        contentType: 'application/json',
        data: ko.toJSON(self.uiConfiguration),
        dataType: 'json',
        success: function(data){
          displaySuccessMessage( $.i18n.prop("ui-configuration.updated"));
        }
      });
    }
  }


  //---------------------------
  // System status part
  //---------------------------

  QueueEntry=function(key,entriesNumber){
    this.key=key;
    this.entriesNumber=entriesNumber;
  }


  mapQueueEntries=function(data){
    if (data!=null){
      return $.map(data,function(item){
        return new QueueEntry(item.key,item.entriesNumber);
      })
    }
    return [];
  }

  CacheEntry=function(key,size,cacheHits,cacheMiss,cacheHitRate,inMemorySize){
    this.key=key;
    this.size=size;
    this.cacheHits=cacheHits;
    this.cacheMiss=cacheMiss;
    this.cacheHitRate=cacheHitRate;
    this.inMemorySize=inMemorySize;
  }

  mapCacheEntries=function(data){
    if(data!=null){
      return $.map(data,function(item){
        return new CacheEntry(item.key,item.size,item.cacheHits,item.cacheMiss,item.cacheHitRate,item.inMemorySize);
      })
    }
    return [];
  }



  displayCacheEntries=function(){
    var divContent = $("#main-content #status_caches");
    divContent.html(smallSpinnerImg());
    $.ajax("restServices/archivaServices/systemStatusService/cacheEntries", {
        type: "GET",
        success: function(data){
          var cacheEntries=mapCacheEntries(data);
          divContent.html($("#status_caches_tmpl" ).tmpl({cacheEntries: cacheEntries}));
        }
    });
  }

  flushCache=function(key){
    clearUserMessages();
    $("#main-content #status_caches").html(smallSpinnerImg());
    $.ajax("restServices/archivaServices/systemStatusService/clearCache/"+encodeURIComponent(key), {
        type: "GET",
        success: function(data){
          displaySuccessMessage( $.i18n.prop("system-status.caches.flushed",key));
          displayCacheEntries();
        }
    });
  }

  flushAllCaches=function(){
    clearUserMessages();
    $("#main-content #status_caches").html(smallSpinnerImg());
    $.ajax("restServices/archivaServices/systemStatusService/clearAllCaches", {
        type: "GET",
        success: function(data){
          displaySuccessMessage( $.i18n.prop("system-status.caches.all.flushed"));
          displayCacheEntries();
        }
    });
  }

  mapRepositoryScannerStatisticsList=function(data){
    if(data!=null){
      return $.isArray(data)? $.map(data,function(item){
        return mapRepositoryScannerStatistics(item);
      }):[data];
    }
    return [];
  }


  mapRepositoryScannerStatistics=function(data){
    return new RepositoryScannerStatistics(mapManagedRepository(data.managedRepository),data.totalFileCount,
                                            data.newFileCount,data.consumerScanningStatistics);
  }

  RepositoryScannerStatistics=function(managedRepository,totalFileCount,newFileCount,consumerScanningStatisticsList){
    //private ManagedRepository managedRepository;
    this.managedRepository=managedRepository

    this.consumerScanningStatisticsList= consumerScanningStatisticsList;

    //private long totalFileCount = 0;
    this.totalFileCount=totalFileCount;

    //private long newFileCount = 0;
    this.newFileCount=newFileCount;
  }

  displayScanningStats=function(){
    var divContent = $("#main-content #status_scanning");
    divContent.html(smallSpinnerImg());
    $.ajax("restServices/archivaServices/systemStatusService/repositoryScannerStatistics", {
        type: "GET",
        success: function(data){
          var stats= mapRepositoryScannerStatisticsList(data);
          $.log("size:"+data.length);
          divContent.html($("#status_scanning_tmpl").tmpl({repositoryScannerStatisticsList:stats}));
        }
    });
  }

  displayMemoryUsage=function(){
    var divContent = $("#main-content #status_memory_info");
    divContent.html(smallSpinnerImg());
    $.ajax("restServices/archivaServices/systemStatusService/memoryStatus", {
        type: "GET",
        dataType: "text",
        success: function(data){
          var memUsage = data;
          $.log("memUsage:"+memUsage);
          divContent.html(memUsage);
        }
    });
  }

  displayQueueEntries=function(){
    var divContent = $("#main-content #status_queues");
    divContent.html(smallSpinnerImg());
    $.ajax("restServices/archivaServices/systemStatusService/queueEntries", {
        type: "GET",
        success: function(data){
          var queueEntries=mapQueueEntries(data);
          divContent.html($("#status_queues_tmpl" ).tmpl({queueEntries: queueEntries}));
        }
    });
  }

  displayServerTime=function(){
    var divContent = $("#main-content #status_current_time");
    divContent.html(smallSpinnerImg());
    $.ajax("restServices/archivaServices/systemStatusService/currentServerTime/"+encodeURIComponent(usedLang()), {
        type: "GET",
        dataType: "text",
        success: function(data){
          var curTime=data;
          $.log("currentServerTime:"+curTime);
          divContent.html(curTime);
        }
    });
  }

  displaySystemStatus=function(){
    screenChange();
    var mainContent=$("#main-content");
    mainContent.html($("#system-status-main").tmpl());

    var versionInfo=$.i18n.prop('system-status.header.version.buildNumber')+": "+window.archivaRuntimeInfo.buildNumber
        +" - "+$.i18n.prop('system-status.header.version.timestampStr')+": "+window.archivaRuntimeInfo.timestampStr;
    mainContent.find("#status_version_info").html(versionInfo);

    displayMemoryUsage();

    displayServerTime();

    displayQueueEntries();

    displayScanningStats();

    displayCacheEntries();
  }

  refreshSystemStatus=function(){
    displayCacheEntries();
    displayScanningStats();
    displayMemoryUsage();
    displayQueueEntries();
    displayServerTime();
  }

  //---------------------------
  // network configuration part
  //---------------------------
  OrganisationInformation=function(name,url,logoLocation){
    this.name=ko.observable(name);
    this.url=ko.observable(url);
    this.logoLocation=ko.observable(logoLocation);
  }
  mapOrganisationInformation=function(data){
    return new OrganisationInformation(data.name, data.url, data.logoLocation);
  }
  mapOrganisationInformations=function(data){
    if (data!=null){
      return $.isArray(data)? $.map(data, function(item){
        return mapOrganisationInformation(item);
      }):[mapOrganisationInformation(data)];
    }
  }
  activateOrganisationInformationFormValidation=function(){
    var validate = $("#main-content #appearance-configuration-form-id").validate({
      rules: {
        name: {
          required: true
        },
        url: {
          required:true,
          url:true
        },
        logoLocation: {
          required:false,
          url:true
        }
      },
      showErrors: function(validator, errorMap, errorList) {
        customShowError("#main-content #appearance-configuration-form-id", validator, errorMap, errorMap);
      }
    })
  }
  OrganisationInformationViewModel=function(organisationInformation){
    activateOrganisationInformationFormValidation();
    this.organisationInformation=ko.observable(organisationInformation);

    this.save=function(){
      if (!$("#main-content #appearance-configuration-form-id").valid()) {
          return;
      }
      clearUserMessages();
      $.ajax("restServices/archivaServices/archivaAdministrationService/setOrganisationInformation", {
        type: "POST",
        contentType: "application/json",
        data: ko.toJSON(this.organisationInformation),
        dataType: "json",
        success: function(data){
          displaySuccessMessage($.i18n.prop('appearance-configuration.updated'));
          updateAppearanceToolBar();
        },
        error: function(data){
          displayErrorMessage($.i18n.prop('appearance-configuration.updating-error'));
        }
      });
    }
  }
  displayAppearanceConfiguration=function(){
    screenChange();
    var mainContent=$("#main-content");
    mainContent.html(mediumSpinnerImg());

    $.ajax("restServices/archivaServices/archivaAdministrationService/getOrganisationInformation", {
      type: "GET",
      dataType: 'json',
      success: function(data) {
        mainContent.html($("#changeAppearance").tmpl());
        var organisationInformation=new OrganisationInformation(data.name,data.url,data.logoLocation);
        var organisationInformationViewModel=new OrganisationInformationViewModel(organisationInformation);
        ko.applyBindings(organisationInformationViewModel, mainContent.get(0));
        var validator = $("#main-content #appearance-configuration-form-id").validate({
          showErrors: function(validator,errorMap,errorList) {
            customShowError(mainContent.find("#appearance-configuration-form-id").get(0),validator,errorMap,errorMap);
          }
        });
      }
    });
  }

});