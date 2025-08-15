sap.ui.define(
  [
    "sap/ui/core/mvc/Controller",
    "sap/m/MessageToast",
    "sap/ui/model/json/JSONModel",
    "sap/ui/Device",
    "sap/ui/core/IconPool",
    "sap/ui/core/BusyIndicator",
  ],
  /**
   * @param {typeof sap.ui.core.mvc.Controller} Controller
   */
  function (
    Controller,
    MessageToast,
    JSONModel,
    Device,
    IconPool,
    BusyIndicator
  ) {
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

        onInit() {
          // 1. 视图样式
          this.getView().addStyleClass(
            this.getOwnerComponent().getContentDensityClass()
          );

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

          // 5. 处理页面刷新时的数据加载
          this._handlePageRefresh();
        },

        onExit() {
          Device.media.detachHandler(this._handleWindowResize, this);
          this._detachRouteHandler();
          this._unsubscribeEventBus();
        },

        /** ===================== 事件订阅与解绑 ===================== */
        _attachRouteHandler() {
          this._fnRouteHandler = this.onRouteChange.bind(this);
          this.getOwnerComponent()
            .getRouter()
            .attachRouteMatched(this._fnRouteHandler);
        },

        _detachRouteHandler() {
          if (this._fnRouteHandler) {
            this.getOwnerComponent()
              .getRouter()
              .detachRouteMatched(this._fnRouteHandler);
            this._fnRouteHandler = null;
          }
        },
        _subscribeEventBus() {
          const oBus = sap.ui.getCore().getEventBus();
          const subscriptions = [
            ["DataUpdate", "ContextNodeChanged", this._onDataUpdated],
            ["DataUpdate", "BotInstanceChanged", this._onBotInstanceUpdated],
            ["DataUpdate", "TaskChanged", this._onDataUpdated],
            ["TaskRun", "TaskSelectionChanged", this._onTaskSelectionChanged],
          ];

          subscriptions.forEach(([channel, event, handler]) => {
            oBus.subscribe(channel, event, handler, this);
          });
        },
        _unsubscribeEventBus() {
          const oBus = sap.ui.getCore().getEventBus();
          const subscriptions = [
            ["DataUpdate", "ContextNodeChanged", this._onDataUpdated],
            ["DataUpdate", "BotInstanceChanged", this._onBotInstanceUpdated],
            ["DataUpdate", "TaskChanged", this._onDataUpdated],
            ["TaskRun", "TaskSelectionChanged", this._onTaskSelectionChanged],
          ];

          subscriptions.forEach(([channel, event, handler]) => {
            oBus.unsubscribe(channel, event, handler, this);
          });
        },

        /** ===================== 模型与缓存初始化 ===================== */
        _initNavigationModel() {
          const oNavigationModel = new JSONModel({
            selectedTreeKey: "", // Tree控件的选中状态
            currentView: "tasks", // "tasks" or "contextNodes"
            navigation: [],
            fixedNavigation: [
              { text: "Tasks", icon: "sap-icon://task", key: "tasks" },
              {
                text: "Context Nodes",
                icon: "sap-icon://tree",
                key: "contextNodes",
              },
            ],
          });
          this.getView().setModel(oNavigationModel, "side");
        },

        _initDataCache() {
          this._dataCache = {
            currentTask: null,
            botInstances: new Map(),
            contextNodes: new Map(),
            botMessages: new Map(),
            isLoaded: false,
            invalidated: false,
          };
          // 事件订阅已集中管理
        },

        _initIcon() {
          // 注册自定义字体图标
          const iconConfigs = [
            {
              name: "SAP-icons-TNT",
              fontFamily: "SAP-icons-TNT",
              fontURI: sap.ui.require.toUrl("sap/tnt/themes/base/fonts/"),
            },
            {
              name: "BusinessSuiteInAppSymbols",
              fontFamily: "BusinessSuiteInAppSymbols",
              fontURI: sap.ui.require.toUrl("sap/ushell/themes/base/fonts/"),
            },
          ];

          iconConfigs.forEach(({ name, ...config }) => {
            IconPool.registerFont(config);
            IconPool.fontLoaded(name);
          });
        },

        /** ===================== 路由与导航 ===================== */
        onRouteChange(oEvent) {
          const sRouteName = oEvent.getParameter("name");
          const oArguments = oEvent.getParameter("arguments");

          // 详情页处理
          switch (sRouteName) {
            case "RouteTaskRunNav":
              this._handleTaskRunNavRoute(oArguments);
              break;
            case "RouteTaskDetail":
              this._handleTaskDetailRoute(oArguments);
              break;
            case "RouteBotInstanceDetail":
              this._handleBotInstanceDetailRoute(oArguments);
              break;
            case "RouteContextNodeDetail":
            case "RouteTextNodePage":
            case "RouteTextAreaNodePage":
            case "RouteMarkDownNodePage":
              this._handleContextNodeDetailRoute(oArguments);
              break;
            case "RouteAIConversation":
              this._handleAIConversationRoute(oArguments);
              break;

            default:
              break;
          }
        },

        _handleTaskRunNavRoute(args) {
          const taskId = args?.taskRunId;
          if (!taskId) return;

          this._initNavigationModel();

          if (
            !this._dataCache.currentTask ||
            this._dataCache.currentTask.id !== taskId
          ) {
            this._preloadTaskData(taskId, true);
          } else {
            this._buildNavigationFromCache();
            this._expandNavigationTree();
          }

          this.currentTaskId = taskId;
        },

        _handleTaskDetailRoute(args) {
          this._restoreNavigationState();
        },

        _handleBotInstanceDetailRoute(args) {
          this._restoreNavigationState();
        },

        _handleContextNodeDetailRoute(args) {
          this._restoreNavigationState();
        },

        _handleAIConversationRoute(args) {
          this._restoreNavigationState();
        },
        /** ===================== 数据加载与缓存 ===================== */
        async _preloadTaskData(sTaskId, isBusy) {
          const oModel = this.getOwnerComponent().getModel();

          if (isBusy) {
            BusyIndicator.show();
          }

          try {
            const sTaskPath = "/Tasks(" + sTaskId + ")";
            const sHierarchyPath = sTaskPath + "/MainService.getHierarchy()";
            const sContextHierarchyPath =
              sTaskPath + "/MainService.getContextHierarchy()";

            const [taskhierarchyResult, contextHierarchyResult] =
              await Promise.all([
                oModel.bindContext(sHierarchyPath).requestObject(),
                oModel.bindContext(sContextHierarchyPath).requestObject(),
              ]);

            const oTaskTree = taskhierarchyResult?.value
              ? JSON.parse(taskhierarchyResult.value)
              : [];
            this._cacheTaskData(oTaskTree);

            const oContextTree = contextHierarchyResult?.value
              ? JSON.parse(contextHierarchyResult.value)
              : [];
            this._cacheContextNodeTree(oContextTree);

            this._buildNavigationFromCache();
            this._expandNavigationTree();


          } catch (error) {
            MessageToast.show(
              "Failed to load data: " + (error.message || error.toString())
            );
          } finally {
            if (isBusy) {
              BusyIndicator.hide();
            }
            // 隐藏左侧导航的busy状态
            this._setNavigationBusy(false);
          }
        },

        _cacheTaskData(oTaskTree) {
          this._taskTreeData = Array.isArray(oTaskTree)
            ? oTaskTree
            : [oTaskTree];
          // 递归处理单个节点
          const processNode = (node) => {
            if (!node || typeof node !== "object") {
              return node;
            }

            // 如果有items数组，进行去重和排序处理
            if (node.items && Array.isArray(node.items)) {
              // 去重
              const uniqueItems = [];
              const seenIds = new Set();

              node.items.forEach((item) => {
                if (!seenIds.has(item.id)) {
                  seenIds.add(item.id);
                  uniqueItems.push(item);
                }
              });

              // 递归处理每个子节点
              const processedItems = uniqueItems.map(processNode);

              // 按sequence排序
              processedItems.sort((a, b) => {
                return (a.sequence || 0) - (b.sequence || 0);
              });

              // 返回处理后的节点
              return { ...node, items: processedItems };
            }

            // 没有items数组则直接返回节点（可能包含其他属性）
            return { ...node };
          };
          // 处理整个任务树
          this._taskTreeData = this._taskTreeData.map(processNode);

          this._adaptTreeNodeText(this._taskTreeData);
          this.getView()
            .getModel("side")
            .setProperty("/navigation", this._taskTreeData);

          Object.assign(this._dataCache, {
            isLoaded: true,
            invalidated: false,
          });

          this._buildTaskHierarchyMap();

          const [mainTask] = this._taskTreeData;
          if (mainTask) this._dataCache.currentTask = mainTask;

          // const [mainTask] = this._taskTreeData;
          // if (mainTask) {
          //   this._dataCache.currentTask = mainTask;
          //   const cacehSubTasks = (node) => {
          //     if (!node) return;
          //     if (node.id && node.id !== this._dataCache.currentTask.ID) {
          //       this._dataCache.subTasks.set(node.id, node);
          //     }
          //     node.items?.forEach(cacehSubTasks);
          //   };

          //   mainTask.items?.forEach(cacehSubTasks);
          // }
        },

        _cacheContextNodeTree(oContextTree) {
          this._contextNodeTreeData = Array.isArray(oContextTree)
            ? oContextTree
            : [oContextTree];
          this._adaptTreeNodeText(this._contextNodeTreeData);
        },

        _buildNavigationFromCache() {
          const oSideModel = this.getView().getModel("side");
          const sCurrentView = oSideModel.getProperty("/currentView") || "tasks";

          if (sCurrentView === "tasks") {
            oSideModel.setProperty("/navigation", this._taskTreeData || []);
          } else if (sCurrentView === "contextNodes") {
            oSideModel.setProperty("/navigation", this._contextNodeTreeData || []);
          }
        },

        _expandNavigationTree() {
          // 延迟执行以确保DOM已更新
          setTimeout(() => {
            const oTree = this.byId("idItemsNavigationTree");
            if (oTree) {
              // 默认展开所有层级
              oTree.expandToLevel(99);
            }
          }, 50);
        },



        /** ===================== 树结构适配与递归 ===================== */
        _adaptTreeNodeText(nodes) {
          if (!Array.isArray(nodes)) return;

          const typeIconMap = new Map([
            [
              "task",
              { icon: "sap-icon://task", keyPrefix: "task_", type: "Task" },
            ],
            [
              "bot",
              {
                icon: "sap-icon://activities",
                keyPrefix: "botinstance_",
                type: "BotInstance",
              },
            ],
            [
              "string",
              {
                icon: "sap-icon://syntax",
                keyPrefix: "code_",
                type: "ContextNode",
              },
            ],
            [
              "code",
              {
                icon: "sap-icon://syntax",
                keyPrefix: "code_",
                type: "ContextNode",
              },
            ],
            [
              "json",
              {
                icon: "sap-icon://syntax",
                keyPrefix: "code_",
                type: "ContextNode",
              },
            ],
            [
              "text",
              {
                icon: "sap-icon://syntax",
                keyPrefix: "code_",
                type: "ContextNode",
              },
            ],
            [
              "virtual",
              {
                icon: "sap-icon://syntax",
                keyPrefix: "code_",
                type: "ContextNode",
              },
            ],
            [
              "markdown",
              {
                icon: "sap-icon://text",
                keyPrefix: "markdown_",
                type: "ContextNode",
              },
            ],
          ]);

          nodes.forEach((node) => {
            // 使用空值合并操作符设置默认值
            node.text = node.name || node.label || "undefined";

            const nodeType = (node.type || "").toLowerCase();
            // let typeConfig = typeIconMap.get(nodeType) ||
            //   { icon: 'sap-icon://syntax', keyPrefix: 'code_', type: 'ContextNode' };

            let typeConfig = {
              ...(typeIconMap.get(nodeType) || {
                icon: "sap-icon://syntax",
                keyPrefix: "code_",
                type: "ContextNode",
              }),
            };

            // 特殊处理
            if (nodeType === "task" && node.isMain) {
              typeConfig.icon = "sap-icon://menu2";
            } else if (nodeType === "bot" && node.functionType === "A") {
              typeConfig.icon = "sap-icon://SAP-icons-TNT/robot";
            }

            // 使用对象展开语法
            Object.assign(node, {
              icon: typeConfig.icon,
              key: `${typeConfig.keyPrefix}${node.id}`,
              type: typeConfig.type,
            });

            // 递归处理子节点
            if (node.items?.length > 0) {
              this._adaptTreeNodeText(node.items);
            }
          });
        },

        /** ===================== 导航状态管理 ===================== */
        _maintainNavigationState() {
          const oSideModel = this.getView().getModel("side");
          const aCurrentNavigation = oSideModel.getProperty("/navigation");

          if (aCurrentNavigation) {
            this._lastNavigationState = {
              navigation: aCurrentNavigation,
              currentView: oSideModel.getProperty("/currentView"),
            };

            oSideModel.setProperty("/hasNavigationData", true);
          }
        },

        _restoreNavigationState() {
          const oSideModel = this.getView().getModel("side");
          const aCurrentNavigation = oSideModel.getProperty("/navigation");

          if (!aCurrentNavigation && this._lastNavigationState) {
            oSideModel.setProperty(
              "/navigation",
              this._lastNavigationState.navigation
            );
            oSideModel.setProperty(
              "/currentView",
              this._lastNavigationState.currentView
            );
            oSideModel.setProperty("/hasNavigationData", true);
          }
        },

        /** ===================== UI 事件处理 ===================== */
        onFixedNavigationItemSelect(oEvent) {
          const sKey = oEvent.getParameter("item").getKey();
          const oSideModel = this.getView().getModel("side");

          oSideModel?.setProperty("/currentView", sKey);

          if (
            this._dataCache.invalidated &&
            this._dataCache.isLoaded &&
            this._dataCache.currentTask
          ) {
            this._preloadTaskData(this._dataCache.currentTask.id, "");
          } else if (this._dataCache.isLoaded) {
            this._buildNavigationFromCache();
            this._expandNavigationTree();
          }
        },

        onNavigationItemSelect(oEvent) {
          const oItem = oEvent.getParameter("listItem");
          const oContext = oItem.getBindingContext("side");
          const oData = oContext.getObject();

          // 设置Tree的选中状态
          this.getView().getModel("side").setProperty("/selectedTreeKey", oData.key);

          this._maintainNavigationState();
          this._navigateToItem(oData);
        },

        onTreeSelectionChange(oEvent) {
          const oSelectedItem = oEvent.getParameter("listItem");
          if (oSelectedItem) {
            const oContext = oSelectedItem.getBindingContext("side");
            const oData = oContext.getObject();
            
            // 更新模型中的选中状态
            this.getView().getModel("side").setProperty("/selectedTreeKey", oData.key);
          }
        },

        onSideNavButtonPress() {
          const oToolPage = this.byId("navToolPage");
          const bSideExpanded = oToolPage.getSideExpanded();

          this._setToggleButtonTooltip(bSideExpanded);
          oToolPage.setSideExpanded(!oToolPage.getSideExpanded());
        },

        _setToggleButtonTooltip(isbSideExpanded) {
          const oToggleButton = this.byId("navSideNavigationToggleButton");
          const tooltip = isbSideExpanded
            ? "Large Size Navigation Menu"
            : "Small Size Navigation Menu";
          oToggleButton.setTooltip(tooltip);
        },

        onHomeButtonPress() {
          this.getOwnerComponent().getRouter().navTo("RouteTaskRunList");
        },

        /** ===================== 导航跳转 ===================== */
        _navigateToItem(oItemData) {
          const oRouter = this.getOwnerComponent().getRouter();

          setTimeout(() => {
            const { type, id, functionType, key } = oItemData;

            switch (type) {
              case "Task":
                if (id) {
                  oRouter.navTo("RouteTaskDetail", { taskId: id });
                } else {
                  MessageToast.show("Task ID not available");
                }
                break;
              case "BotInstance":
                if (id) {
                  if (functionType === "A") {
                    oRouter.navTo("RouteAIConversation", {
                      taskRunId: this._getCurrentTaskRunId(),
                      botInstanceId: id,
                    });
                  } else {
                    oRouter.navTo("RouteBotInstanceDetail", {
                      botInstanceId: id,
                    });
                  }
                } else {
                  MessageToast.show("Bot Instance ID not available");
                }
                break;
              case "ContextNode":
                if (id) {
                  const routeName = key.startsWith("markdown_")
                    ? "RouteMarkDownNodePage"
                    : "RouteTextAreaNodePage";
                  oRouter.navTo(routeName, { contextNodeId: id });
                } else {
                  MessageToast.show("Context Node ID not available");
                }
                break;
              default:
                MessageToast.show("Unknown item type");
                break;
            }
          }, 100);
        },

        /** ===================== 缓存与外部访问 ===================== */
        getCachedTask(sTaskId) {
          if (!sTaskId) {
            return this._dataCache.currentTask;
          }
          if (
            this._dataCache.currentTask &&
            this._dataCache.currentTask.id === sTaskId
          ) {
            return this._dataCache.currentTask;
          }
          return this._dataCache.subTasks
            ? this._dataCache.subTasks.get(sTaskId)
            : null;
        },

        isCacheLoaded() {
          return this._dataCache.isLoaded;
        },

        _getCurrentTaskRunId() {
          if (this.currentTaskId) {
            return this.currentTaskId;
          }

          const oRouter = this.getOwnerComponent().getRouter();
          const oHashChanger = oRouter.getHashChanger();
          const sHash = oHashChanger.getHash();
          const aMatches = sHash.match(/Tasks\(([^)]+)\)/);

          if (aMatches && aMatches[1]) {
            return aMatches[1].replace(/'/g, "");
          }
          if (this._dataCache.currentTask && this._dataCache.currentTask.id) {
            return this._dataCache.currentTask.id;
          }
          return null;
        },

        async _extractTaskRunIdFromUrl() {
          const oRouter = this.getOwnerComponent().getRouter();
          const oHashChanger = oRouter.getHashChanger();
          const sHash = oHashChanger.getHash();

          // 从Tasks路由中直接提取taskRunId
          const taskMatch = sHash.match(/Tasks\(([^)]+)\)/);
          if (taskMatch && taskMatch[1]) {
            return taskMatch[1].replace(/'/g, "");
          }

          // 从其他详情页面路由中提取ID并查找对应的taskRunId
          const patterns = [
            { regex: /TaskDetail\(([^)]+)\)/, type: 'task' },
            { regex: /BotInstanceDetail\(([^)]+)\)/, type: 'botInstance' },
            { regex: /ContextNodeDetail\(([^)]+)\)/, type: 'contextNode' },
            { regex: /ContextNodeDetail\(([^)]+)\)\/\w+/, type: 'contextNode' } // 文本节点页面
          ];

          for (const pattern of patterns) {
            const match = sHash.match(pattern.regex);
            if (match && match[1]) {
              const id = match[1].replace(/'/g, "");
              return await this._findTaskRunIdByDetailId(id, pattern.type);
            }
          }

          return null;
        },

        async _findTaskRunIdByDetailId(id, type) {
          const oModel = this.getOwnerComponent().getModel();

          try {
            switch (type) {
              case 'task':
                // 对于Task，需要找到主任务（isMain=true）
                return await this._findMainTaskId(id);

              case 'botInstance':
                // 通过BotInstances找到对应的Task
                const botPath = `/BotInstances(${id})`;
                const botResult = await oModel.bindContext(botPath, null, { $expand: "task" }).requestObject();
                const taskId = botResult?.task?.ID;
                if (taskId) {
                  return await this._findTaskRunIdByDetailId(taskId, 'task');
                }
                break;

              case 'contextNode':
                // 通过ContextNodes找到对应的Task
                const contextPath = `/ContextNodes(${id})`;
                const contextResult = await oModel.bindContext(contextPath, null, { $expand: "task" }).requestObject();
                const contextTaskId = contextResult?.task?.ID;
                if (contextTaskId) {
                  return await this._findTaskRunIdByDetailId(contextTaskId, 'task');
                }
                break;
            }
          } catch (error) {
            console.error("Error finding taskRunId:", error);
          }

          return null;
        },

        async _findMainTaskId(taskId) {
          const oModel = this.getOwnerComponent().getModel();

          try {
            // 首先检查当前任务是否为主任务
            const currentTaskPath = `/Tasks(${taskId})`;
            const currentTask = await oModel.bindContext(currentTaskPath, null, { $expand: "botInstance" }).requestObject();

            if (currentTask?.isMain) {
              return taskId;
            }

            // 如果不是主任务，说明这是一个子任务，需要向上查找主任务
            if (currentTask?.botInstance?.ID) {
              // 通过botInstance找到父任务
              const parentBotPath = `/BotInstances(${currentTask.botInstance.ID})`;
              const parentBot = await oModel.bindContext(parentBotPath, null, { $expand: "task" }).requestObject();

              if (parentBot?.task?.ID) {
                // 递归查找父任务的主任务
                return await this._findMainTaskId(parentBot.task.ID);
              }
            }

            // 如果没有botInstance关联，通过层次结构查找主任务
            const taskHierarchyPath = `/Tasks(${taskId})/MainService.getHierarchy()`;
            const taskResult = await oModel.bindContext(taskHierarchyPath).requestObject();
            const taskTree = taskResult?.value ? JSON.parse(taskResult.value) : [];

            // 在层次结构中查找主任务
            const mainTask = this._findMainTaskInTree(taskTree);

            if (mainTask) {
              return mainTask.id || mainTask.ID;
            }

            // 如果在层次结构中没找到主任务，尝试查找根节点
            const rootTask = Array.isArray(taskTree) ? taskTree[0] : taskTree;
            return rootTask?.id || rootTask?.ID || taskId;

          } catch (error) {
            console.error("Error finding main task:", error);
            return taskId; // 如果出错，返回原始taskId
          }
        },

        _findMainTaskInTree(taskTree) {
          if (!taskTree) return null;

          const tasks = Array.isArray(taskTree) ? taskTree : [taskTree];

          // 递归查找主任务
          const findMain = (nodes) => {
            for (const node of nodes) {
              if (node.isMain) {
                return node;
              }
              if (node.items && node.items.length > 0) {
                const found = findMain(node.items);
                if (found) return found;
              }
            }
            return null;
          };

          return findMain(tasks);
        },

        /** ===================== 事件回调 ===================== */
        _onTaskSelectionChanged(sChannel, sEvent, oData) {
          const sNewTaskId = oData.taskId;

          if (
            !this._dataCache.currentTask ||
            this._dataCache.currentTask.id !== sNewTaskId
          ) {
            Object.assign(this._dataCache, {
              isLoaded: false,
              currentTask: null,
            });
          }
        },

        _onDataUpdated() {
          this._dataCache.invalidated = true;
        },

        _onBotInstanceUpdated(sChannelId, sEventId, oData) {
          if (this._dataCache.isLoaded && this._dataCache.currentTask) {
            this._preloadTaskData(this._dataCache.currentTask.id, true);
          }
        },

        /** ===================== 页面刷新处理 ===================== */
        _handlePageRefresh() {
          // 延迟执行，确保路由已经初始化
          setTimeout(async () => {
            if (!this._dataCache.isLoaded) {
              // 显示左侧导航的busy状态
              this._setNavigationBusy(true);

              // 根据URL判断应该显示哪个视图
              const currentView = this._detectViewFromUrl();
              this.getView().getModel("side").setProperty("/currentView", currentView);

              const taskRunId = await this._extractTaskRunIdFromUrl();
              if (taskRunId) {
                this._preloadTaskData(taskRunId, false);
              } else {
                // 如果无法提取taskRunId，隐藏busy状态
                this._setNavigationBusy(false);
              }
            }
          }, 100);
        },

        _detectViewFromUrl() {
          const oRouter = this.getOwnerComponent().getRouter();
          const oHashChanger = oRouter.getHashChanger();
          const sHash = oHashChanger.getHash();

          // 如果URL包含ContextNodeDetail，说明应该显示Context Nodes视图
          if (sHash.includes("ContextNodeDetail")) {
            return "contextNodes";
          }

          // 默认显示Tasks视图
          return "tasks";
        },

        _setNavigationBusy(bBusy) {
          const oTree = this.byId("idItemsNavigationTree");
          if (oTree) {
            oTree.setBusy(bBusy);
          }
        },

        /** ===================== 任务树映射与根节点查找 ===================== */
        _findRootTaskId(sTaskId) {
          return this._taskHierarchyMap?.get(sTaskId) || null;
        },



        // _loadRootTaskForSubTask(sTaskId) {
        //   const oModel = this.getOwnerComponent().getModel();
        //   const sHierarchyPath = "/Tasks(" + sTaskId + ")/MainService.getHierarchy()";

        //   oModel.bindContext(sHierarchyPath)
        //     .requestObject()
        //     .then((result) => {
        //       const oTaskTree = result && result.value ? JSON.parse(result.value) : [];
        //       const rootNode = Array.isArray(oTaskTree) ? oTaskTree[0] : oTaskTree;
        //       if (!rootNode) {
        //         that._preloadTaskData(sTaskId, "");
        //         return;
        //       }
        //       const rootId = rootNode.id || rootNode.ID;
        //       if (rootId === sTaskId) {
        //         that._preloadTaskData(sTaskId, "");
        //       } else {
        //         that._preloadTaskData(rootId, "");
        //         setTimeout(() => {
        //           that._updateNavigationSelection(sTaskId);
        //         }, 500);
        //       }
        //     })
        //     .catch(() => {
        //       that._preloadTaskData(sTaskId, "");
        //     });
        // },

        _buildTaskHierarchyMap() {
          this._taskHierarchyMap = new Map();

          const traverse = (node, rootId) => {
            if (!node) return;

            this._taskHierarchyMap.set(node.id || node.ID, rootId);
            if (Array.isArray(node.items)) {
              node.items.forEach((child) => traverse(child, rootId));
            }
          };
          (this._taskTreeData || []).forEach((root) =>
            traverse(root, root.id || root.ID)
          );
        },
      }
    );
  }
);
