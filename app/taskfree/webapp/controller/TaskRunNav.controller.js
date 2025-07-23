sap.ui.define(
  ["sap/ui/core/mvc/Controller", "sap/m/MessageToast",
    "sap/ui/model/json/JSONModel",
    "sap/ui/Device",
    "sap/ui/core/IconPool",
    'sap/ui/core/BusyIndicator'
  ],
  /**
   * @param {typeof sap.ui.core.mvc.Controller} Controller
   */
  function (Controller, MessageToast, JSONModel, Device, IconPool, BusyIndicator) {
    "use strict";

    /**
     * TaskRunNav Controller
     * 优化结构、事件管理、缓存与导航逻辑，保持 JSONModel 绑定和现有功能不变
     */
    return Controller.extend(
      "ai.orchestration.taskfree.controller.TaskRunNav",
      {
        _bExpanded: true,
        _isBusy: false,

        onInit: function () {
          // 1. 视图样式
          this.getView().addStyleClass(this.getOwnerComponent().getContentDensityClass());
          // 2. 初始化模型和缓存
          this._initNavigationModel();
          this._initDataCache();
          this._initIcon();
          // 3. 侧边栏自适应
          if (Device.resize.width <= 1024) {
            this.onSideNavButtonPress();
          }
          // 4. 事件监听
          Device.media.attachHandler(this._handleWindowResize, this);
          this._attachRouteHandler();
          this._subscribeEventBus();
        },

        onExit: function () {
          Device.media.detachHandler(this._handleWindowResize, this);
          this._detachRouteHandler();
          this._unsubscribeEventBus();
        },

        /** ===================== 事件订阅与解绑 ===================== */
        _attachRouteHandler: function () {
          this._fnRouteHandler = this.onRouteChange.bind(this);
          this.getOwnerComponent().getRouter().attachRouteMatched(this._fnRouteHandler);
        },
        _detachRouteHandler: function () {
          if (this._fnRouteHandler) {
            this.getOwnerComponent().getRouter().detachRouteMatched(this._fnRouteHandler);
            this._fnRouteHandler = null;
          }
        },
        _subscribeEventBus: function () {
          var oBus = sap.ui.getCore().getEventBus();
          oBus.subscribe("DataUpdate", "ContextNodeChanged", this._onDataUpdated, this);
          oBus.subscribe("DataUpdate", "BotInstanceChanged", this._onBotInstanceUpdated, this);
          oBus.subscribe("DataUpdate", "TaskChanged", this._onDataUpdated, this);
          oBus.subscribe("TaskRun", "TaskSelectionChanged", this._onTaskSelectionChanged, this);
        },
        _unsubscribeEventBus: function () {
          var oBus = sap.ui.getCore().getEventBus();
          oBus.unsubscribe("DataUpdate", "ContextNodeChanged", this._onDataUpdated, this);
          oBus.unsubscribe("DataUpdate", "BotInstanceChanged", this._onBotInstanceUpdated, this);
          oBus.unsubscribe("DataUpdate", "TaskChanged", this._onDataUpdated, this);
          oBus.unsubscribe("TaskRun", "TaskSelectionChanged", this._onTaskSelectionChanged, this);
        },

        /** ===================== 模型与缓存初始化 ===================== */
        _initNavigationModel: function () {
          var oNavigationModel = new JSONModel({
            selectedKey: "",
            currentView: "tasks", // "tasks" or "contextNodes"
            navigation: [],
            fixedNavigation: [
              { text: "Tasks", icon: "sap-icon://task", key: "tasks" },
              { text: "Context Nodes", icon: "sap-icon://tree", key: "contextNodes" }
            ]
          });
          this.getView().setModel(oNavigationModel, "side");
        },
        _initDataCache: function () {
          this._dataCache = {
            currentTask: null,
            botInstances: new Map(),
            contextNodes: new Map(),
            botMessages: new Map(),
            isLoaded: false,
            invalidated: false
          };
          // 事件订阅已集中管理
        },
        _initIcon: function () {
          // 注册自定义字体图标
          var tnt = { fontFamily: "SAP-icons-TNT", fontURI: sap.ui.require.toUrl("sap/tnt/themes/base/fonts/") };
          var bsuite = { fontFamily: "BusinessSuiteInAppSymbols", fontURI: sap.ui.require.toUrl("sap/ushell/themes/base/fonts/") };
          IconPool.registerFont(tnt); IconPool.fontLoaded("SAP-icons-TNT");
          IconPool.registerFont(bsuite); IconPool.fontLoaded("BusinessSuiteInAppSymbols");
        },

        /** ===================== 路由与导航 ===================== */
        onRouteChange: function (oEvent) {
          var sRouteName = oEvent.getParameter('name');
          var oArguments = oEvent.getParameter('arguments');
          // 1. 侧边栏选中项
          if (sRouteName === "RouteTaskRunNav") {
            this.getView().getModel('side').setProperty('/selectedKey', sRouteName);
          }
          if (Device.system.phone) {
            this.onSideNavButtonPress();
          }
          // 2. 主导航入口
          if (sRouteName === "RouteTaskRunNav" && oArguments && oArguments.taskRunId) {
            var sTaskId = oArguments.taskRunId;
            this._initNavigationModel();
            this.byId("idItemsNavigationTree").expandToLevel(1);
            if (!this._dataCache.currentTask || this._dataCache.currentTask.ID !== sTaskId) {
              this._preloadTaskData(sTaskId, "X");
            } else {
              this._buildNavigationFromCache();
            }
            this._currentTaskId = sTaskId;
          }
          // 3. 详情页/子任务导航
          // var sExtractedTaskId = null;
          // if (sRouteName.indexOf("RouteTask") === 0 || sRouteName.indexOf("RouteBotInstance") === 0 || sRouteName.indexOf("RouteContextNode") === 0 || sRouteName.indexOf("RouteAI") === 0) {
          //   if (oArguments.taskRunId) {
          //     sExtractedTaskId = oArguments.taskRunId;
          //   } else if (oArguments.taskId) {
          //     sExtractedTaskId = oArguments.taskId;
          //   } else {
          //     var oRouter = this.getOwnerComponent().getRouter();
          //     var sHash = oRouter.getHashChanger().getHash();
          //     var aMatches = sHash.match(/Tasks\(([^)]+)\)/);
          //     if (aMatches && aMatches[1]) {
          //       sExtractedTaskId = aMatches[1].replace(/'/g, '');
          //     }
          //   }
          //   if (sExtractedTaskId) {
          //     if (this._dataCache.isLoaded) {
          //       var sRootTaskId = this._findRootTaskId(sExtractedTaskId);
          //       if (sRootTaskId === this._dataCache.currentTask?.ID) {
          //         this._restoreNavigationState();
          //         this._updateNavigationSelection(sExtractedTaskId);
          //         return;
          //       } else if (sRootTaskId && sRootTaskId !== sExtractedTaskId) {
          //         this._preloadTaskData(sRootTaskId, "");
          //         return;
          //       }
          //     } else {
          //       this._loadRootTaskForSubTask(sExtractedTaskId);
          //       return;
          //     }
          //   }
          //   if (sExtractedTaskId && (!this._dataCache.isLoaded || this._dataCache.currentTask?.ID !== sExtractedTaskId)) {
          //     this._preloadTaskData(sExtractedTaskId, "");
          //   }
          // }
          // 4. 详情页选中项恢复
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

        /** ===================== 数据加载与缓存 ===================== */
        _preloadTaskData: function (sTaskId, isBusy) {
          var oModel = this.getOwnerComponent().getModel();
          var that = this;
          if (isBusy === "X") {
            BusyIndicator.show();
          }
          var sTaskPath = "/Tasks(" + sTaskId + ")";
          var sHierarchyPath = sTaskPath + "/MainService.getHierarchy()";
          var sContextHierarchyPath = sTaskPath + "/MainService.getContextHierarchy()";
          Promise.all([
            oModel.bindContext(sHierarchyPath).requestObject(),
            oModel.bindContext(sContextHierarchyPath).requestObject()
          ]).then(function ([taskhierarchyResult, contextHierarchyResult]) {
            var oTaskTree = taskhierarchyResult && taskhierarchyResult.value ? JSON.parse(taskhierarchyResult.value) : [];
            that._cacheTaskData(oTaskTree);
            var oContextTree = contextHierarchyResult && contextHierarchyResult.value ? JSON.parse(contextHierarchyResult.value) : [];
            that._cacheContextNodeTree(oContextTree);
            that._buildNavigationFromCache();
          }).catch(function (error) {
            MessageToast.show("Failed to load data: " + (error.message || error.toString()));
          }).finally(function () {
            if (isBusy === "X") {
              BusyIndicator.hide();
            }
          });
        },
        _cacheTaskData: function (oTaskTree) {
          this._taskTreeData = Array.isArray(oTaskTree) ? oTaskTree : [oTaskTree];
          // 遍历_taskTreeData中的每个元素的items数组根据id去重
          this._taskTreeData = this._taskTreeData.map(taskItem => {
            if (taskItem.items && Array.isArray(taskItem.items)) {
              const uniqueItems = [];
              const seenIds = new Set();

              taskItem.items.forEach(subItem => {
                if (!seenIds.has(subItem.id)) {
                  seenIds.add(subItem.id);
                  uniqueItems.push(subItem);
                }
              });

              return { ...taskItem, items: uniqueItems };
            }
            // 如果没有items数组则直接返回原对象
            return taskItem;
          });
          this._adaptTreeNodeText(this._taskTreeData);
          this.getView().getModel("side").setProperty("/navigation", this._taskTreeData);
          this._dataCache.isLoaded = true;
          this._dataCache.invalidated = false;
          this._buildTaskHierarchyMap();
          // 主任务缓存
          if (this._taskTreeData && this._taskTreeData[0]) {
            this._dataCache.currentTask = this._taskTreeData[0];
            // 构建子任务缓存
            this._dataCache.subTasks = new Map();
            var traverse = (node) => {
              if (!node) return;
              if (node.id && node.id !== this._dataCache.currentTask.ID) {
                this._dataCache.subTasks.set(node.id, node);
              }
              if (Array.isArray(node.items)) {
                node.items.forEach(traverse);
              }
            };
            this._taskTreeData[0].items && this._taskTreeData[0].items.forEach(traverse);
          }
        },
        _cacheContextNodeTree: function (oContextTree) {
          this._contextNodeTreeData = Array.isArray(oContextTree) ? oContextTree : [oContextTree];
          this._adaptTreeNodeText(this._contextNodeTreeData);
        },
        _buildNavigationFromCache: function () {
          var sCurrentView = this.getView().getModel("side").getProperty("/currentView") || "tasks";
          if (sCurrentView === "tasks") {
            this.getView().getModel("side").setProperty("/navigation", this._taskTreeData || []);
          } else if (sCurrentView === "contextNodes") {
            this.getView().getModel("side").setProperty("/navigation", this._contextNodeTreeData || []);
          }
        },

        /** ===================== 树结构适配与递归 ===================== */
        _adaptTreeNodeText: function (arr) {
          if (!Array.isArray(arr)) return;
          for (var i = 0; i < arr.length; i++) {
            var node = arr[i];
            node.text = node.name || node.label || "undefined";
            var type = (node.type || "").toLowerCase();
            if (type === "task") {
              node.icon = node.isMain ? "sap-icon://menu2" : "sap-icon://task";
              node.key = "task_" + node.id;
              node.type = "Task";
            } else if (type === "bot") {
              node.icon = node.functionType === "A" ? "sap-icon://SAP-icons-TNT/robot" : "sap-icon://activities";
              node.key = "botinstance_" + node.id;
              node.type = "BotInstance";
            } else if (["string", "code", "json", "text", "virtual"].indexOf(type) !== -1) {
              node.icon = "sap-icon://syntax";
              node.key = "code_" + node.id;
              node.type = "ContextNode";
            } else if (type === "markdown") {
              node.icon = "sap-icon://text";
              node.key = "markdown_" + node.id;
              node.type = "ContextNode";
            } else {
              node.icon = "sap-icon://syntax";
              node.key = "code_" + node.id;
              node.type = "ContextNode";
            }
            if (Array.isArray(node.items) && node.items.length > 0) {
              this._adaptTreeNodeText(node.items);
            }
          }
        },

        /** ===================== 导航状态管理 ===================== */
        _maintainNavigationState: function () {
          var oSideModel = this.getView().getModel("side");
          var aCurrentNavigation = oSideModel.getProperty("/navigation");
          if (aCurrentNavigation && aCurrentNavigation.length > 0) {
            this._lastNavigationState = {
              navigation: aCurrentNavigation,
              currentView: oSideModel.getProperty("/currentView")
            };
            oSideModel.setProperty("/hasNavigationData", true);
          }
        },
        _restoreNavigationState: function () {
          var oSideModel = this.getView().getModel("side");
          var aCurrentNavigation = oSideModel.getProperty("/navigation");
          if ((!aCurrentNavigation || aCurrentNavigation.length === 0) && this._lastNavigationState) {
            oSideModel.setProperty("/navigation", this._lastNavigationState.navigation);
            oSideModel.setProperty("/currentView", this._lastNavigationState.currentView);
            oSideModel.setProperty("/hasNavigationData", true);
          }
        },

        /** ===================== UI 事件处理 ===================== */
        onFixedNavigationItemSelect: function (oEvent) {
          var sKey = oEvent.getParameter("item").getKey();
          var oSideModel = this.getView().getModel("side");
          oSideModel.setProperty("/currentView", sKey);
          oSideModel.setProperty("/selectedKey", sKey);
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
          this._maintainNavigationState();
          this._navigateToItem(oData);
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
          // 可扩展：窗口大小变化处理
        },
        onHomeButtonPress: function () {
          var oRouter = this.getOwnerComponent().getRouter();
          oRouter.navTo("RouteTaskRunList");
        },

        /** ===================== 导航跳转 ===================== */
        _navigateToItem: function (oItemData) {
          var oRouter = this.getOwnerComponent().getRouter();
          var that = this;
          setTimeout(function () {
            if (oItemData.type === "Task") {
              var sTaskId = oItemData.id;
              if (sTaskId) {
                oRouter.navTo("RouteTaskDetail", { taskId: sTaskId });
              } else {
                MessageToast.show("Task ID not available");
              }
            } else if (oItemData.type === "BotInstance") {
              var sBotInstanceId = oItemData.id;
              if (sBotInstanceId) {
                var functionType_code = oItemData.functionType;
                if (functionType_code && functionType_code === "A") {
                  oRouter.navTo("RouteAIConversation", {
                    taskRunId: that._getCurrentTaskRunId(),
                    botInstanceId: sBotInstanceId
                  });
                } else {
                  oRouter.navTo("RouteBotInstanceDetail", { botInstanceId: sBotInstanceId });
                }
              } else {
                MessageToast.show("Bot Instance ID not available");
              }
            } else if (oItemData.type === "ContextNode") {
              var sContextNodeId = oItemData.id;
              if (sContextNodeId) {
                if (oItemData.key === "code_" + sContextNodeId) {
                  oRouter.navTo("RouteTextAreaNodePage", { contextNodeId: sContextNodeId });
                } else if (oItemData.key === "markdown_" + sContextNodeId) {
                  oRouter.navTo("RouteMarkDownNodePage", { contextNodeId: sContextNodeId });
                } else {
                  oRouter.navTo("RouteTextAreaNodePage", { contextNodeId: sContextNodeId });
                }
              } else {
                MessageToast.show("Context Node ID not available");
              }
            }
          }, 100);
        },

        /** ===================== 缓存与外部访问 ===================== */
        getCachedTask: function (sTaskId) {
          if (!sTaskId) {
            return this._dataCache.currentTask;
          }
          if (this._dataCache.currentTask && this._dataCache.currentTask.ID === sTaskId) {
            return this._dataCache.currentTask;
          }
          return this._dataCache.subTasks ? this._dataCache.subTasks.get(sTaskId) : null;
        },
        isCacheLoaded: function () {
          return this._dataCache.isLoaded;
        },
        _getCurrentTaskRunId: function () {
          if (this._currentTaskId) {
            return this._currentTaskId;
          }
          var oRouter = this.getOwnerComponent().getRouter();
          var oHashChanger = oRouter.getHashChanger();
          var sHash = oHashChanger.getHash();
          var aMatches = sHash.match(/Tasks\(([^)]+)\)/);
          if (aMatches && aMatches[1]) {
            return aMatches[1].replace(/'/g, '');
          }
          if (this._dataCache.currentTask && this._dataCache.currentTask.ID) {
            return this._dataCache.currentTask.ID;
          }
          return null;
        },

        /** ===================== 事件回调 ===================== */
        _onTaskSelectionChanged: function (sChannel, sEvent, oData) {
          var sNewTaskId = oData.taskId;
          if (!this._dataCache.currentTask || this._dataCache.currentTask.ID !== sNewTaskId) {
            this._dataCache.isLoaded = false;
            this._dataCache.currentTask = null;
          }
        },
        _onDataUpdated: function () {
          this._dataCache.invalidated = true;
        },
        _onBotInstanceUpdated: function (sChannelId, sEventId, oData) {
          if (this._dataCache.isLoaded && this._dataCache.currentTask) {
            this._preloadTaskData(this._dataCache.currentTask.id, "");
          }
        },

        /** ===================== 任务树映射与根节点查找 ===================== */
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
          var sHierarchyPath = "/Tasks(" + sTaskId + ")/MainService.getHierarchy()";
          oModel.bindContext(sHierarchyPath).requestObject().then(function (result) {
            var oTaskTree = result && result.value ? JSON.parse(result.value) : [];
            var rootNode = Array.isArray(oTaskTree) ? oTaskTree[0] : oTaskTree;
            if (!rootNode) {
              that._preloadTaskData(sTaskId, "");
              return;
            }
            var rootId = rootNode.id || rootNode.ID;
            if (rootId === sTaskId) {
              that._preloadTaskData(sTaskId, "");
            } else {
              that._preloadTaskData(rootId, "");
              setTimeout(function () {
                that._updateNavigationSelection(sTaskId);
              }, 500);
            }
          }).catch(function () {
            that._preloadTaskData(sTaskId, "");
          });
        },
        _buildTaskHierarchyMap: function () {
          this._taskHierarchyMap = new Map();
          var traverse = (node, rootId) => {
            if (!node) return;
            this._taskHierarchyMap.set(node.id || node.ID, rootId);
            if (Array.isArray(node.items)) {
              node.items.forEach(child => traverse(child, rootId));
            }
          };
          (this._taskTreeData || []).forEach(root => traverse(root, root.id || root.ID));
        }
      }
    );
  }
); 