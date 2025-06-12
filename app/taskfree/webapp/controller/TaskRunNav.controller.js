sap.ui.define(
  ["sap/ui/core/mvc/Controller", "sap/m/MessageToast",
    "ai/orchestration/taskfree/util/Helper",
    "ai/orchestration/taskfree/service/ChatService",
    "ai/orchestration/taskfree/service/NewMessageHandler",
    "ai/orchestration/taskfree/util/UIHelper",
    "sap/ui/model/json/JSONModel",
    "sap/m/ResponsivePopover",
    "sap/m/MessagePopover",
    "sap/m/ActionSheet",
    "sap/m/Button",
    "sap/m/Link",
    "sap/m/NotificationListItem",
    "sap/m/MessageItem",
    "sap/ui/core/CustomData",
    "sap/ui/Device",
    "sap/ui/core/syncStyleClass",
    "sap/m/library"
  ],
  /**
   * @param {typeof sap.ui.core.mvc.Controller} Controller
   */
  function (Controller, MessageToast, Helper, ChatService, NewMessageHandler, UIHelper, JSONModel, ResponsivePopover, MessagePopover, ActionSheet, Button, Link, NotificationListItem, MessageItem, CustomData, Device, syncStyleClass, mobileLibrary) {
    "use strict";

    // shortcuts for sap.m library types
    var PlacementType = mobileLibrary.PlacementType;
    var VerticalPlacementType = mobileLibrary.VerticalPlacementType;
    var ButtonType = mobileLibrary.ButtonType;

    return Controller.extend(
      "ai.orchestration.taskfree.controller.TaskRunNav",
      {
        _bExpanded: true,
        
        onInit: function () {
          this.getView().addStyleClass(this.getOwnerComponent().getContentDensityClass());
          
          // Initialize navigation model
          this._initNavigationModel();
          
          // Initialize data cache
          this._initDataCache();
          
          // if the app starts on desktop devices with small or medium screen size, collapse the side navigation
          if (Device.resize.width <= 1024) {
            this.onSideNavButtonPress();
          }

          Device.media.attachHandler(this._handleWindowResize, this);
          this.getOwnerComponent().getRouter().attachRouteMatched(this.onRouteChange.bind(this));
        },
        
        _initDataCache: function() {
          // Initialize data cache for storing preloaded task data
          this._dataCache = {
            currentTask: null,
            botInstances: new Map(),
            contextNodes: new Map(),
            botMessages: new Map(),
            isLoaded: false
          };
        },

        onExit: function() {
          Device.media.detachHandler(this._handleWindowResize, this);
        },

        onRouteChange: function (oEvent) {
          var sRouteName = oEvent.getParameter('name');
          var oArguments = oEvent.getParameter('arguments');
          
          // Only update selectedKey for routes that match our navigation structure
          if (sRouteName === "RouteTaskRunNav") {
            this.getView().getModel('side').setProperty('/selectedKey', sRouteName);
          }

          if (Device.system.phone) {
            this.onSideNavButtonPress();
          }
          
          // Handle RouteTaskRunNav navigation from TaskRunList
          if (sRouteName === "RouteTaskRunNav" && oArguments && oArguments.taskRunId) {
            var sTaskId = oArguments.taskRunId;
            
            // Check if we need to load data for a new task
            if (!this._dataCache.currentTask || this._dataCache.currentTask.ID !== sTaskId) {
              console.log("Loading new task data with preload:", sTaskId);
              this._preloadTaskData(sTaskId);
            } else {
              console.log("Task data already cached, using cached data");
              this._buildNavigationFromCache();
            }
            
            // Store the task ID for reference, but don't bind the entire view to avoid context inheritance issues
            this._currentTaskId = sTaskId;
          }
          
          // Handle cases where we need to extract task ID from detail routes
          var sExtractedTaskId = null;
          if (sRouteName.indexOf("RouteTask") === 0 || sRouteName.indexOf("RouteBotInstance") === 0 || sRouteName.indexOf("RouteContextNode") === 0 || sRouteName.indexOf("RouteAI") === 0) {
            if (oArguments.taskRunId) {
              sExtractedTaskId = oArguments.taskRunId;
            } else if (oArguments.taskId) {
              sExtractedTaskId = oArguments.taskId;
            } else {
              // Try to extract from current hash
              var oRouter = this.getOwnerComponent().getRouter();
              var sHash = oRouter.getHashChanger().getHash();
              var aMatches = sHash.match(/Tasks\(([^)]+)\)/);
              if (aMatches && aMatches[1]) {
                sExtractedTaskId = aMatches[1].replace(/'/g, '');
              }
            }
            
            // If we found a task ID and don't have navigation data, load it
            if (sExtractedTaskId && (!this._dataCache.isLoaded || this._dataCache.currentTask?.ID !== sExtractedTaskId)) {
              console.log("Loading task data for detail route:", sExtractedTaskId);
              this._preloadTaskData(sExtractedTaskId);
            }
          }
          
          // For detail routes, maintain navigation state but update selection
          if (sRouteName === "RouteTaskDetail" && oArguments && oArguments.taskId) {
            this._restoreNavigationState();
            if (this._dataCache.isLoaded && this._dataCache.currentTask && this._dataCache.currentTask.ID === oArguments.taskId) {
              this.getView().getModel('side').setProperty('/selectedKey', 'task_' + oArguments.taskId);
            }
          } else if (sRouteName === "RouteBotInstanceDetail" && oArguments && oArguments.botInstanceId) {
            this._restoreNavigationState();
            if (this._dataCache.isLoaded) {
              this.getView().getModel('side').setProperty('/selectedKey', 'botinstance_' + oArguments.botInstanceId);
            }
          } else if (sRouteName === "RouteContextNodeDetail" && oArguments && oArguments.contextNodeId) {
            this._restoreNavigationState();
            if (this._dataCache.isLoaded) {
              this.getView().getModel('side').setProperty('/selectedKey', 'contextnode_' + oArguments.contextNodeId);
            }
          } else if (sRouteName === "RouteAIConversation" && oArguments && oArguments.botInstanceId) {
            this._restoreNavigationState();
            if (this._dataCache.isLoaded) {
              this.getView().getModel('side').setProperty('/selectedKey', 'botinstance_' + oArguments.botInstanceId);
            }
          }
        },

        _initNavigationModel: function() {
          var oNavigationModel = new JSONModel({
            selectedKey: "",
            currentView: "tasks", // "tasks" or "contextNodes"
            navigation: [],
            fixedNavigation: [
              {
                text: "Tasks",
                icon: "sap-icon://task",
                key: "tasks"
              },
              {
                text: "Context Nodes", 
                icon: "sap-icon://tree",
                key: "contextNodes"
              }
            ]
          });
          this.getView().setModel(oNavigationModel, "side");
        },

        _preloadTaskData: function(sTaskId) {
          var oModel = this.getOwnerComponent().getModel();
          var that = this;
          
          console.log("Preloading task data for:", sTaskId);
          
          // Create binding with comprehensive $expand to get all related data in one request
          var oBinding = oModel.bindContext("/Tasks(" + sTaskId + ")", null, {
            $expand: "botInstances($expand=type,messages),contextNodes"
          });
          
          oBinding.attachDataReceived(function() {
            var oContext = oBinding.getBoundContext();
            if (oContext) {
              var oTaskData = oContext.getObject();
              if (oTaskData) {
                console.log("Task data preloaded successfully:", oTaskData);
                that._cacheTaskData(oTaskData);
                that._buildNavigationFromCache();
              } else {
                console.error("No task data received");
              }
            } else {
              console.error("No bound context received");
            }
          });
          
          // Request the data
          oBinding.requestObject().catch(function(oError) {
            console.error("Failed to preload task data:", oError);
            MessageToast.show("Failed to load task data: " + (oError.message || oError.toString()));
          });
        },
        
        _cacheTaskData: function(oTaskData) {
          // Clear previous cache
          this._dataCache.botInstances.clear();
          this._dataCache.contextNodes.clear();
          this._dataCache.botMessages.clear();
          
          // Cache task data
          this._dataCache.currentTask = oTaskData;
          
          // Cache bot instances and their messages
          if (oTaskData.botInstances && Array.isArray(oTaskData.botInstances)) {
            oTaskData.botInstances.forEach(function(oBotInstance) {
              this._dataCache.botInstances.set(oBotInstance.ID, oBotInstance);
              
              // Cache messages for this bot instance
              if (oBotInstance.messages && Array.isArray(oBotInstance.messages)) {
                oBotInstance.messages.forEach(function(oMessage) {
                  this._dataCache.botMessages.set(oMessage.ID, oMessage);
                }.bind(this));
              }
            }.bind(this));
          }
          
          // Cache context nodes
          if (oTaskData.contextNodes && Array.isArray(oTaskData.contextNodes)) {
            oTaskData.contextNodes.forEach(function(oContextNode) {
              this._dataCache.contextNodes.set(oContextNode.ID, oContextNode);
            }.bind(this));
          }
          
          this._dataCache.isLoaded = true;
          console.log("Data cached successfully:", {
            task: oTaskData.name,
            botInstances: this._dataCache.botInstances.size,
            contextNodes: this._dataCache.contextNodes.size,
            messages: this._dataCache.botMessages.size
          });
        },
        
        _buildNavigationFromCache: function() {
          if (!this._dataCache.isLoaded || !this._dataCache.currentTask) {
            console.warn("No cached data available for navigation");
            return;
          }
          
          var sCurrentView = this.getView().getModel("side").getProperty("/currentView") || "tasks";
          
          if (sCurrentView === "tasks") {
            this._buildTasksNavigationFromCache();
          } else if (sCurrentView === "contextNodes") {
            this._buildContextNodesNavigationFromCache();
          }
        },
        
        _buildTasksNavigationFromCache: function() {
          var oTask = this._dataCache.currentTask;
          var aNavigationData = [];
          
          var oTaskItem = {
            text: oTask.name || "Unnamed Task",
            icon: "sap-icon://task",
            key: "task_" + oTask.ID,
            type: "Task",
            data: oTask,
            expanded: true,
            items: []
          };
          
          // Add BotInstances as child items from cache
          this._dataCache.botInstances.forEach(function(oBotInstance) {
            var sDisplayName = oBotInstance.type && oBotInstance.type.name ? 
              oBotInstance.type.name : 
              ("Bot Instance " + oBotInstance.sequence);
            oTaskItem.items.push({
              text: sDisplayName,
              icon: "sap-icon://robot",
              key: "botinstance_" + oBotInstance.ID,
              type: "BotInstance",
              data: oBotInstance
            });
          });
          
          aNavigationData.push(oTaskItem);
          this.getView().getModel("side").setProperty("/navigation", aNavigationData);
        },
        
        _buildContextNodesNavigationFromCache: function() {
          var oTask = this._dataCache.currentTask;
          var aNavigationData = [];
          
          if (this._dataCache.contextNodes.size > 0) {
            var oTaskItem = {
              text: oTask.name || "Unnamed Task",
              icon: "sap-icon://task",
              key: "task_" + oTask.ID,
              type: "Task",
              data: oTask,
              expanded: true,
              items: []
            };
            
            // Add ContextNodes as child items from cache
            this._dataCache.contextNodes.forEach(function(oContextNode) {
              oTaskItem.items.push({
                text: oContextNode.label || "Context Node",
                icon: "sap-icon://detail-view",
                key: "contextnode_" + oContextNode.ID,
                type: "ContextNode",
                data: oContextNode
              });
            });
            
            aNavigationData.push(oTaskItem);
          }
          
          this.getView().getModel("side").setProperty("/navigation", aNavigationData);
        },

        onFixedNavigationItemSelect: function(oEvent) {
          var sKey = oEvent.getParameter("item").getKey();
          var oSideModel = this.getView().getModel("side");
          
          oSideModel.setProperty("/currentView", sKey);
          oSideModel.setProperty("/selectedKey", sKey);
          
          // Use cached data to build navigation
          if (this._dataCache.isLoaded) {
            console.log("Using cached data for navigation view:", sKey);
            this._buildNavigationFromCache();
          } else {
            console.log("No cached data available, navigation will be built after data loads");
          }
        },

        onNavigationItemSelect: function(oEvent) {
          var oItem = oEvent.getParameter("item");
          var sKey = oItem.getKey();
          var oContext = oItem.getBindingContext("side");
          var oData = oContext.getObject();
          
          this.getView().getModel("side").setProperty("/selectedKey", sKey);
          
          // Ensure navigation data remains available after route change
          this._maintainNavigationState();
          
          // Navigate to appropriate view based on item type and data
          this._navigateToItem(oData);
        },
        
        _maintainNavigationState: function() {
          // Store current navigation state to prevent loss during route changes
          var oSideModel = this.getView().getModel("side");
          var aCurrentNavigation = oSideModel.getProperty("/navigation");
          
          if (aCurrentNavigation && aCurrentNavigation.length > 0) {
            // Store in a more permanent location
            this._lastNavigationState = {
              navigation: aCurrentNavigation,
              currentView: oSideModel.getProperty("/currentView")
            };
            
            // Set a flag to indicate we have valid navigation data
            oSideModel.setProperty("/hasNavigationData", true);
          }
        },
        
        _restoreNavigationState: function() {
          // Restore navigation state if it was lost
          var oSideModel = this.getView().getModel("side");
          var aCurrentNavigation = oSideModel.getProperty("/navigation");
          
          if ((!aCurrentNavigation || aCurrentNavigation.length === 0) && this._lastNavigationState) {
            console.log("Restoring navigation state");
            oSideModel.setProperty("/navigation", this._lastNavigationState.navigation);
            oSideModel.setProperty("/currentView", this._lastNavigationState.currentView);
            oSideModel.setProperty("/hasNavigationData", true);
          }
        },

        _navigateToItem: function(oItemData) {
          var oRouter = this.getOwnerComponent().getRouter();
          var that = this;
          
          // Add delay to prevent request collision and improve navigation reliability
          setTimeout(function() {
            // Navigate directly using entity IDs, no need for taskRunId dependency
            if (oItemData.type === "Task") {
              // Navigate to task detail with task ID
              var sTaskId = oItemData.data.ID;
              if (sTaskId) {
                console.log("Navigating to Task detail:", sTaskId);
                oRouter.navTo("RouteTaskDetail", {
                  taskId: sTaskId
                });
              } else {
                MessageToast.show("Task ID not available");
              }
            } else if (oItemData.type === "BotInstance") {
              // Navigate to bot instance detail or AI conversation based on type
              var sBotInstanceId = oItemData.data.ID;
              if (sBotInstanceId) {
                console.log("Navigating to BotInstance:", sBotInstanceId);
                
                // Check if this is a Chat BotInstance
                var sBotTypeName = oItemData.data.type && oItemData.data.type.name;
                if (sBotTypeName && sBotTypeName.startsWith("Chat")) {
                  // Navigate to AI Conversation page
                  oRouter.navTo("RouteAIConversation", {
                    taskRunId: that._getCurrentTaskRunId(),
                    botInstanceId: sBotInstanceId
                  });
                } else {
                  // Navigate to regular bot instance detail page
                  oRouter.navTo("RouteBotInstanceDetail", {
                    botInstanceId: sBotInstanceId
                  });
                }
              } else {
                MessageToast.show("Bot Instance ID not available");
              }
            } else if (oItemData.type === "ContextNode") {
              // Navigate to context node detail
              var sContextNodeId = oItemData.data.ID;
              if (sContextNodeId) {
                console.log("Navigating to ContextNode detail:", sContextNodeId);
                oRouter.navTo("RouteContextNodeDetail", {
                  contextNodeId: sContextNodeId
                });
              } else {
                MessageToast.show("Context Node ID not available");
              }
            }
          }, 100);
        },
        
        // Public methods for other controllers to access cached data
        getCachedBotInstance: function(sBotInstanceId) {
          return this._dataCache.botInstances.get(sBotInstanceId);
        },
        
        getCachedContextNode: function(sContextNodeId) {
          return this._dataCache.contextNodes.get(sContextNodeId);
        },
        
        getCachedTask: function() {
          return this._dataCache.currentTask;
        },
        
        getCachedBotMessages: function(sBotInstanceId) {
          var oBotInstance = this._dataCache.botInstances.get(sBotInstanceId);
          return oBotInstance ? oBotInstance.messages : [];
        },
        
        isCacheLoaded: function() {
          return this._dataCache.isLoaded;
        },

        _getCurrentTaskRunId: function() {
          // First try to get from stored current task ID
          if (this._currentTaskId) {
            return this._currentTaskId;
          }
          
          // Fallback: get from route hash
          var oRouter = this.getOwnerComponent().getRouter();
          var oHashChanger = oRouter.getHashChanger();
          var sHash = oHashChanger.getHash();
          
          // Extract taskRunId from hash pattern like "Tasks(guid)"
          var aMatches = sHash.match(/Tasks\(([^)]+)\)/);
          if (aMatches && aMatches[1]) {
            // Remove quotes if present (for backward compatibility)
            return aMatches[1].replace(/'/g, '');
          }
          
          // If we have cached data, use that
          if (this._dataCache.currentTask && this._dataCache.currentTask.ID) {
            return this._dataCache.currentTask.ID;
          }
          
          return null;
        },

        onSideNavButtonPress: function() {
          var oToolPage = this.byId("navToolPage");
          var bSideExpanded = oToolPage.getSideExpanded();
          this._setToggleButtonTooltip(bSideExpanded);
          oToolPage.setSideExpanded(!oToolPage.getSideExpanded());
        },

        _setToggleButtonTooltip: function(bSideExpanded) {
          var oToggleButton = this.byId('navSideNavigationToggleButton');
          if (bSideExpanded) {
            oToggleButton.setTooltip('Large Size Navigation Menu');
          } else {
            oToggleButton.setTooltip('Small Size Navigation Menu');
          }
        },

        _handleWindowResize: function() {
          // Handle window resize events
        },

        onMessagePopoverPress: function(oEvent) {
          // Handle message popover
        },

        onNotificationPress: function(oEvent) {
          // Handle notification press
        },

        onUserNamePress: function(oEvent) {
          // Handle user name press
        },

        onHomeButtonPress: function() {
          // Navigate back to the task list page
          var oRouter = this.getOwnerComponent().getRouter();
          oRouter.navTo("RouteTaskRunList");
        }
      }
    );
  }
); 