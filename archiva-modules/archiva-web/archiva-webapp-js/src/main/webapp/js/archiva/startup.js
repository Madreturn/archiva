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

define("startup",["jquery","sammy","utils"],
function() {

  // define a container object with various datas
  window.archivaModel = {};

  $.log("devMode:"+window.archivaDevMode);

  // no cache for ajax queries as we get datas from servers so preventing caching !!
  jQuery.ajaxSetup( {
    cache: false,//!window.archivaDevMode
    dataType: 'json',
    statusCode: {
      403: function() {
        removeSmallSpinnerImg();
        removeMediumSpinnerImg("#main-content");
        clearUserMessages();
        displayErrorMessage($.i18n.prop('authz.karma.needed'));
      },
      500: function(data){
        removeSmallSpinnerImg();
        removeMediumSpinnerImg("#main-content");
        clearUserMessages();
        displayRestError($.parseJSON(data.responseText));
        $("#main-content" ).html("");
        //displayErrorMessage($.i18n.prop('error.500'));
      }
    }
  });

});