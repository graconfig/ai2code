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
    "sap/m/library",
    "sap/ui/core/IconPool",
    'sap/ui/core/BusyIndicator'
  ],
  /**
   * @param {typeof sap.ui.core.mvc.Controller} Controller
   */
  function (Controller, MessageToast, Helper, ChatService, NewMessageHandler, UIHelper, JSONModel, ResponsivePopover, MessagePopover, ActionSheet, Button, Link, NotificationListItem, MessageItem, CustomData, Device, syncStyleClass, mobileLibrary, IconPool, BusyIndicator) {
    "use strict";

    // shortcuts for sap.m library types
    var PlacementType = mobileLibrary.PlacementType;
    var VerticalPlacementType = mobileLibrary.VerticalPlacementType;
    var ButtonType = mobileLibrary.ButtonType;

    return Controller.extend(
      "ai.orchestration.taskfree.controller.TaskRunNav",
      {
        _bExpanded: true,

        _isBusy: false,

        onInit: function () {
          this.getView().addStyleClass(this.getOwnerComponent().getContentDensityClass());

          // Initialize navigation model
          this._initNavigationModel();

          // Initialize data cache
          this._initDataCache();

          this._initIcon();

          // if the app starts on desktop devices with small or medium screen size, collapse the side navigation
          if (Device.resize.width <= 1024) {
            this.onSideNavButtonPress();
          }

          Device.media.attachHandler(this._handleWindowResize, this);
          this.getOwnerComponent().getRouter().attachRouteMatched(this.onRouteChange.bind(this));
        },

        _initIcon: function () {
          var b = [];
          var c = {};
          //Fiori Theme font family and URI
          var t = {
            fontFamily: "SAP-icons-TNT",
            fontURI: sap.ui.require.toUrl("sap/tnt/themes/base/fonts/")
          };
          //Registering to the icon pool
          IconPool.registerFont(t);
          b.push(IconPool.fontLoaded("SAP-icons-TNT"));
          c["SAP-icons-TNT"] = t;
          //SAP Business Suite Theme font family and URI
          var B = {
            fontFamily: "BusinessSuiteInAppSymbols",
            fontURI: sap.ui.require.toUrl("sap/ushell/themes/base/fonts/")
          };
          //Registering to the icon pool
          IconPool.registerFont(B);
          b.push(IconPool.fontLoaded("BusinessSuiteInAppSymbols"));
          c["BusinessSuiteInAppSymbols"] = B;
        },

        _initDataCache: function () {
          // Initialize data cache for storing preloaded task data
          this._dataCache = {
            currentTask: null,
            botInstances: new Map(),
            contextNodes: new Map(),
            botMessages: new Map(),
            isLoaded: false,
            invalidated: false
          };

          // Listen for data update events
          sap.ui.getCore().getEventBus().subscribe("DataUpdate", "ContextNodeChanged", this._onDataUpdated, this);
          sap.ui.getCore().getEventBus().subscribe("DataUpdate", "BotInstanceChanged", this._onBotInstanceUpdated, this);
          sap.ui.getCore().getEventBus().subscribe("DataUpdate", "TaskChanged", this._onDataUpdated, this);
        },

        onExit: function () {
          Device.media.detachHandler(this._handleWindowResize, this);
          // Unsubscribe from events
          sap.ui.getCore().getEventBus().unsubscribe("DataUpdate", "ContextNodeChanged", this._onDataUpdated, this);
          sap.ui.getCore().getEventBus().unsubscribe("DataUpdate", "BotInstanceChanged", this._onBotInstanceUpdated, this);
          sap.ui.getCore().getEventBus().unsubscribe("DataUpdate", "TaskChanged", this._onDataUpdated, this);
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

            // Initialize navigation model
            this._initNavigationModel();

            //set the nav level to 1
            this.byId("idItemsNavigationTree").expandToLevel(1);

            // Check if we need to load data for a new task
            if (!this._dataCache.currentTask || this._dataCache.currentTask.ID !== sTaskId) {
              this._preloadTaskData(sTaskId, "X");
            } else {
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

            // Smart routing: check if it's a subTask of current hierarchy
            if (sExtractedTaskId) {
              if (this._dataCache.isLoaded) {
                var sRootTaskId = this._findRootTaskId(sExtractedTaskId);
                if (sRootTaskId === this._dataCache.currentTask?.ID) {
                  // It's a subTask of current hierarchy, just update selection
                  this._restoreNavigationState();
                  this._updateNavigationSelection(sExtractedTaskId);
                  return;
                } else if (sRootTaskId && sRootTaskId !== sExtractedTaskId) {
                  // It's a subTask but not of current hierarchy, load the root task
                  this._preloadTaskData(sRootTaskId, "");
                  return;
                }
              } else {
                // No data loaded yet, check if it might be a subTask by looking for root task
                this._loadRootTaskForSubTask(sExtractedTaskId);
                return;
              }
            }

            // If we found a task ID and don't have navigation data, load it
            if (sExtractedTaskId && (!this._dataCache.isLoaded || this._dataCache.currentTask?.ID !== sExtractedTaskId)) {
              this._preloadTaskData(sExtractedTaskId, "");
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

        _initNavigationModel: function () {
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

        _preloadTaskData: function (sTaskId, isBusy) {
          var oModel = this.getOwnerComponent().getModel();
          var that = this;

          //加载busy
          if (isBusy === "X") {
            BusyIndicator.show();
          }
          // Create binding with comprehensive $expand to get all related data in one request
          var oBinding = oModel.bindContext("/Tasks(" + sTaskId + ")", null, {
            $expand: "botInstances($expand=type,messages,tasks($expand=type,botInstances($expand=type,messages,tasks($expand=type,botInstances($expand=type))))),contextNodes"
          });

          oBinding.attachDataReceived(function () {
            var oContext = oBinding.getBoundContext();
            if (oContext) {
              var oTaskData = oContext.getObject();
              if (oTaskData) {
                that._cacheTaskData(oTaskData);
                that._buildNavigationFromCache();
              }
            }
            if (BusyIndicator) {
              BusyIndicator.hide();
            }
          });

          // Request the data
          oBinding.requestObject().catch(function (oError) {
            MessageToast.show("Failed to load task data: " + (oError.message || oError.toString()));
            if (BusyIndicator) {
              BusyIndicator.hide();
            }
          });
        },

        _cacheTaskData: function (oTaskData) {
          // Clear previous cache
          this._dataCache.botInstances.clear();
          this._dataCache.contextNodes.clear();
          this._dataCache.botMessages.clear();

          // Initialize sub-tasks cache if not exists
          if (!this._dataCache.subTasks) {
            this._dataCache.subTasks = new Map();
          } else {
            this._dataCache.subTasks.clear();
          }

          // Cache task data
          this._dataCache.currentTask = oTaskData;

          // Cache bot instances and their messages
          if (oTaskData.botInstances && Array.isArray(oTaskData.botInstances)) {
            this._cacheBotInstancesRecursively(oTaskData.botInstances);
          }

          // Cache context nodes
          if (oTaskData.contextNodes && Array.isArray(oTaskData.contextNodes)) {
            oTaskData.contextNodes.forEach(function (oContextNode) {
              this._dataCache.contextNodes.set(oContextNode.ID, oContextNode);
            }.bind(this));
          }

          // Build task hierarchy mapping
          this._buildTaskHierarchyMap();

          this._dataCache.isLoaded = true;
          this._dataCache.invalidated = false;
        },

        _cacheBotInstancesRecursively: function (aBotInstances) {
          aBotInstances.forEach(function (oBotInstance) {
            this._dataCache.botInstances.set(oBotInstance.ID, oBotInstance);

            // Cache messages for this bot instance
            if (oBotInstance.messages && Array.isArray(oBotInstance.messages)) {
              oBotInstance.messages.forEach(function (oMessage) {
                this._dataCache.botMessages.set(oMessage.ID, oMessage);
              }.bind(this));
            }

            // Cache sub-tasks and their bot instances recursively
            if (oBotInstance.tasks && Array.isArray(oBotInstance.tasks)) {
              oBotInstance.tasks.forEach(function (oSubTask) {
                this._dataCache.subTasks.set(oSubTask.ID, oSubTask);

                // Recursively cache bot instances of sub-tasks
                if (oSubTask.botInstances && Array.isArray(oSubTask.botInstances)) {
                  this._cacheBotInstancesRecursively(oSubTask.botInstances);
                }
              }.bind(this));
            }
          }.bind(this));
        },

        _buildNavigationFromCache: function () {
          if (!this._dataCache.isLoaded || !this._dataCache.currentTask) {
            return;
          }

          var sCurrentView = this.getView().getModel("side").getProperty("/currentView") || "tasks";

          if (sCurrentView === "tasks") {
            this._buildTasksNavigationFromCache();
          } else if (sCurrentView === "contextNodes") {
            this._buildContextNodesNavigationFromCache();
          }
        },

        _buildTasksNavigationFromCache: function () {
          var oTask = this._dataCache.currentTask;
          var aNavigationData = [];

          var oTaskItem = this._buildTaskItemRecursively(oTask);
          aNavigationData.push(oTaskItem);
          this.getView().getModel("side").setProperty("/navigation", aNavigationData);
        },

        _buildTaskItemRecursively: function (oTask) {

          var icon = "";
          if (oTask.isMain === true) {
            icon = "sap-icon://menu2";
          } else {
            icon = "sap-icon://task";
          }

          var oTaskItem = {
            text: oTask.name || "Unnamed Task",
            key: "task_" + oTask.ID,
            type: "Task",
            data: oTask,
            icon: icon,
            items: []
          };

          // Add BotInstances as child items
          if (oTask.botInstances && Array.isArray(oTask.botInstances)) {
            // Sort botInstances by sequence before processing
            var aSortedBotInstances = oTask.botInstances.slice().sort(function (a, b) {
              return (a.sequence || 0) - (b.sequence || 0);
            });

            aSortedBotInstances.forEach(function (oBotInstance) {
              var oBotInstanceItem = this._buildBotInstanceItem(oBotInstance);
              oTaskItem.items.push(oBotInstanceItem);
            }.bind(this));
          }

          return oTaskItem;
        },

        _buildBotInstanceItem: function (oBotInstance) {
          var icon = "";
          var functionType_code = oBotInstance.type && oBotInstance.type.functionType_code;
          if (functionType_code && functionType_code === "A") {
            icon = "sap-icon://SAP-icons-TNT/robot";
          } else {
            icon = "sap-icon://activities";
          }

          var sDisplayName = oBotInstance.type && oBotInstance.type.name ?
            oBotInstance.type.name :
            ("Bot Instance " + oBotInstance.sequence);

          var oBotInstanceItem = {
            text: sDisplayName,
            key: "botinstance_" + oBotInstance.ID,
            type: "BotInstance",
            data: oBotInstance,
            icon: icon,
            items: []
          };

          // Add sub-tasks recursively
          if (oBotInstance.tasks && Array.isArray(oBotInstance.tasks)) {
            // Sort sub-tasks by sequence before processing
            var aSortedSubTasks = oBotInstance.tasks.slice().sort(function (a, b) {
              return (a.sequence || 0) - (b.sequence || 0);
            });

            aSortedSubTasks.forEach(function (oSubTask) {
              var oSubTaskItem = this._buildTaskItemRecursively(oSubTask);
              oBotInstanceItem.items.push(oSubTaskItem);
            }.bind(this));
          }

          return oBotInstanceItem;
        },

        _buildContextNodesNavigationFromCache: function () {
          var oTask = this._dataCache.currentTask;
          var aNavigationData = [];

          var Taskicon = "";
          if (oTask.isMain === true) {
            Taskicon = "sap-icon://menu2";
          } else {
            Taskicon = "sap-icon://task";
          }

          var contextNodeIcon = "sap-icon://document-text";

          if (this._dataCache.contextNodes.size > 0) {
            var oTaskItem = {
              text: oTask.name || "Unnamed Task",
              key: "task_" + oTask.ID,
              type: "Task",
              data: oTask,
              icon: Taskicon,
              items: []
            };

            // Add ContextNodes as child items from cache
            this._dataCache.contextNodes.forEach(function (oContextNode) {
              oTaskItem.items.push({
                text: oContextNode.label || "Context Node",
                key: "contextnode_" + oContextNode.ID,
                type: "ContextNode",
                data: oContextNode,
                icon: contextNodeIcon,
                items: []
              });
            });

            aNavigationData.push(oTaskItem);
          }

          this.getView().getModel("side").setProperty("/navigation", aNavigationData);
        },

        onFixedNavigationItemSelect: function (oEvent) {
          var sKey = oEvent.getParameter("item").getKey();
          var oSideModel = this.getView().getModel("side");

          oSideModel.setProperty("/currentView", sKey);
          oSideModel.setProperty("/selectedKey", sKey);

          // Check if cache is invalidated and refresh if needed
          if (this._dataCache.invalidated && this._dataCache.isLoaded && this._dataCache.currentTask) {
            this._preloadTaskData(this._dataCache.currentTask.ID, "");
          } else if (this._dataCache.isLoaded) {
            this._buildNavigationFromCache();
          }
        },

        onNavigationItemSelect: function (oEvent) {
          var oItem = oEvent.getParameter("listItem");
          var oContext = oItem.getBindingContext("side");
          var oData = oContext.getObject();
          var sKey = oData.key;

          this.getView().getModel("side").setProperty("/selectedKey", sKey);

          // Ensure navigation data remains available after route change
          this._maintainNavigationState();

          // Navigate to appropriate view based on item type and data
          this._navigateToItem(oData);
        },

        _maintainNavigationState: function () {
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

        _restoreNavigationState: function () {
          // Restore navigation state if it was lost
          var oSideModel = this.getView().getModel("side");
          var aCurrentNavigation = oSideModel.getProperty("/navigation");

          if ((!aCurrentNavigation || aCurrentNavigation.length === 0) && this._lastNavigationState) {
            oSideModel.setProperty("/navigation", this._lastNavigationState.navigation);
            oSideModel.setProperty("/currentView", this._lastNavigationState.currentView);
            oSideModel.setProperty("/hasNavigationData", true);
          }
        },

        _navigateToItem: function (oItemData) {
          var oRouter = this.getOwnerComponent().getRouter();
          var that = this;

          // Add delay to prevent request collision and improve navigation reliability
          setTimeout(function () {
            // Navigate directly using entity IDs, no need for taskRunId dependency
            if (oItemData.type === "Task") {
              // Navigate to task detail with task ID
              var sTaskId = oItemData.data.ID;
              if (sTaskId) {
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

                // Check if this is a Chat BotInstance
                var sBotTypeName = oItemData.data.type && oItemData.data.type.name;
                var functionType_code = oItemData.data.type && oItemData.data.type.functionType_code;
                if (functionType_code && functionType_code === "A") {
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
              var sContextNodeId = oItemData.data.ID;
              var sNodeType = (oItemData.data.type || "").toLowerCase();
              if (sContextNodeId) {
                if (sNodeType === "string") {
                  //oRouter.navTo("RouteTextNodePage", { contextNodeId: sContextNodeId });
                  oRouter.navTo("RouteTextAreaNodePage", { contextNodeId: sContextNodeId });
                } else if (sNodeType === "markdown") {
                  oRouter.navTo("RouteMarkDownNodePage", { contextNodeId: sContextNodeId });
                } else if (sNodeType === "code" || sNodeType === "json") {
                  oRouter.navTo("RouteTextAreaNodePage", { contextNodeId: sContextNodeId });
                } else {
                  // 默认跳转
                  //oRouter.navTo("RouteContextNodeDetail", { contextNodeId: sContextNodeId });
                  oRouter.navTo("RouteTextAreaNodePage", { contextNodeId: sContextNodeId });
                }
              } else {
                MessageToast.show("Context Node ID not available");
              }
            }
          }, 100);
        },

        // Public methods for other controllers to access cached data
        getCachedBotInstance: function (sBotInstanceId) {
          return this._dataCache.botInstances.get(sBotInstanceId);
        },

        getCachedContextNode: function (sContextNodeId) {
          return this._dataCache.contextNodes.get(sContextNodeId);
        },

        getCachedTask: function (sTaskId) {
          if (!sTaskId) {
            return this._dataCache.currentTask;
          }
          return this._dataCache.subTasks ? this._dataCache.subTasks.get(sTaskId) : null;
        },

        getCachedBotMessages: function (sBotInstanceId) {
          var oBotInstance = this._dataCache.botInstances.get(sBotInstanceId);
          return oBotInstance ? oBotInstance.messages : [];
        },

        isCacheLoaded: function () {
          return this._dataCache.isLoaded;
        },

        _getCurrentTaskRunId: function () {
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

        onSideNavButtonPress: function () {
          var oToolPage = this.byId("navToolPage");
          var bSideExpanded = oToolPage.getSideExpanded();
          this._setToggleButtonTooltip(bSideExpanded);
          oToolPage.setSideExpanded(!oToolPage.getSideExpanded());
        },

        _setToggleButtonTooltip: function (bSideExpanded) {
          var oToggleButton = this.byId('navSideNavigationToggleButton');
          if (bSideExpanded) {
            oToggleButton.setTooltip('Large Size Navigation Menu');
          } else {
            oToggleButton.setTooltip('Small Size Navigation Menu');
          }
        },

        _handleWindowResize: function () {
          // Handle window resize events
        },

        onMessagePopoverPress: function (oEvent) {
          // Handle message popover
        },

        onNotificationPress: function (oEvent) {
          // Handle notification press
        },

        onUserNamePress: function (oEvent) {
          // Handle user name press
        },

        onHomeButtonPress: function () {
          // Navigate back to the task list page
          var oRouter = this.getOwnerComponent().getRouter();
          oRouter.navTo("RouteTaskRunList");
        },

        _onDataUpdated: function () {
          // Mark cache as invalidated when data is updated
          this._dataCache.invalidated = true;
        },

        _onBotInstanceUpdated: function (sChannelId, sEventId, oData) {
          // Handle BotInstance updates (like execute creating new tasks)
          if (this._dataCache.isLoaded && this._dataCache.currentTask) {
            // Reload the current task data to get updated BotInstance with new tasks
            this._preloadTaskData(this._dataCache.currentTask.ID, "");
          }
        },

        _buildTaskHierarchyMap: function () {
          if (!this._taskHierarchyMap) {
            this._taskHierarchyMap = new Map();
          } else {
            this._taskHierarchyMap.clear();
          }

          if (this._dataCache.currentTask) {
            this._mapTaskRecursively(this._dataCache.currentTask, this._dataCache.currentTask.ID);
          }
        },

        _mapTaskRecursively: function (oTask, sRootTaskId) {
          this._taskHierarchyMap.set(oTask.ID, sRootTaskId);

          if (oTask.botInstances && Array.isArray(oTask.botInstances)) {
            oTask.botInstances.forEach(function (oBotInstance) {
              if (oBotInstance.tasks && Array.isArray(oBotInstance.tasks)) {
                oBotInstance.tasks.forEach(function (oSubTask) {
                  this._mapTaskRecursively(oSubTask, sRootTaskId);
                }.bind(this));
              }
            }.bind(this));
          }
        },

        _findRootTaskId: function (sTaskId) {
          if (!this._taskHierarchyMap) {
            return null;
          }

          var sRootTaskId = this._taskHierarchyMap.get(sTaskId);
          return sRootTaskId || null;
        },

        _updateNavigationSelection: function (sTaskId) {
          var sNodeKey = "task_" + sTaskId;
          this.getView().getModel("side").setProperty("/selectedKey", sNodeKey);
        },

        _loadRootTaskForSubTask: function (sTaskId) {
          var oModel = this.getOwnerComponent().getModel();
          var that = this;

          // Try to load the task and check if it has a botInstance (indicating it's a subTask)
          var oBinding = oModel.bindContext("/Tasks(" + sTaskId + ")", null, {
            $expand: "botInstance/task($expand=botInstances($expand=type,messages,tasks($expand=type,botInstances($expand=type,messages,tasks($expand=type,botInstances($expand=type))))),contextNodes)"
          });

          oBinding.attachDataReceived(function () {
            var oContext = oBinding.getBoundContext();
            if (oContext) {
              var oTaskData = oContext.getObject();
              if (oTaskData && oTaskData.botInstance && oTaskData.botInstance.task) {
                // This is a subTask, load the root task
                var oRootTask = oTaskData.botInstance.task;
                that._cacheTaskData(oRootTask);
                that._buildNavigationFromCache();
                that._updateNavigationSelection(sTaskId);
              } else {
                // This is likely a root task, load it directly
                that._preloadTaskData(sTaskId, "");
              }
            }
          });

          oBinding.requestObject().catch(function (oError) {
            // Fallback: try loading as root task
            that._preloadTaskData(sTaskId, "");
          });
        },

      }
    );
  }
); 