sap.ui.define(
  ["sap/ui/core/mvc/Controller", "sap/m/MessageToast",
    "sap/ui/layout/BlockLayout",
    "sap/ui/layout/BlockLayoutRow",
    "sap/ui/layout/BlockLayoutCell",
    "sap/m/Title",
    "sap/m/Text",
    "sap/m/VBox",
    "sap/ui/core/Icon"
  ],
  /**
   * @param {typeof sap.ui.core.mvc.Controller} Controller
   */
  function (Controller, MessageToast, BlockLayout, BlockLayoutRow, BlockLayoutCell, Title, Text, VBox, Icon) {
    "use strict";

    return Controller.extend(
      "ai.orchestration.taskfree.controller.TaskRunDetail",
      {
        onInit() {
          this.getOwnerComponent()
            .getRouter()
            .getRoute("RouteTaskRunNav")
            .attachPatternMatched(this._onRouteMatched, this);

          // Show default home content
          this._renderDefaultHome();
        },

        _onRouteMatched(oEvent) {
          const sRouteName = oEvent.getParameter("name");
          const oArguments = oEvent.getParameter("arguments");

          if (sRouteName === "RouteTaskRunNav" && oArguments.taskRunId) {
            const sTaskRunId = oArguments.taskRunId

            if (sTaskRunId) {
              this._loadTaskRunDetail(sTaskRunId);
            }
          } else if (sRouteName === "RouteTaskRunNav") {
            this._renderDefaultHome();
          }
        },

        _loadTaskRunDetail(sTaskRunId) {
          // 获取TaskRunNav控制器的缓存数据
          const oTaskRunNavController = this._getTaskRunNavController();

          if (oTaskRunNavController && oTaskRunNavController.isCacheLoaded()) {
            // 使用预加载的缓存数据
            const oTaskData = oTaskRunNavController.getCachedTask(sTaskRunId);

            if (oTaskData) {
              // 直接渲染内容，无需OData请求
              this._renderTaskRunContent(oTaskData);
            } else {
              //MessageToast.show("No data found for Task Run");
            }
          } else {
            // 缓存数据尚未加载，等待加载完成
            this._waitForCacheLoad(sTaskRunId);
          }
        },

        _getTaskRunNavController() {
          // 获取TaskRunNav控制器实例
          const oTaskRunNavView = sap.ui.getCore().byId("container-ai.orchestration.taskfree---TaskRunNav");
          return oTaskRunNavView ? oTaskRunNavView.getController() : null;
        },

        _waitForCacheLoad(sTaskRunId) {
          let iRetryCount = 0;
          const iMaxRetries = 300; // 最多等待30秒 (300 * 100ms)

          const fnCheckCache = () => {
            const oTaskRunNavController = this._getTaskRunNavController();

            if (oTaskRunNavController && oTaskRunNavController.isCacheLoaded()) {
              // 缓存已加载，获取数据
              const oTaskData = oTaskRunNavController.getCachedTask(sTaskRunId);

              if (oTaskData) {
                this._renderTaskRunContent(oTaskData);
              } else {
                //MessageToast.show("No data found for Task Run");
              }
            } else if (iRetryCount < iMaxRetries) {
              // 继续等待
              iRetryCount++;
              setTimeout(fnCheckCache, 100);
            } else {
              // 超时，显示错误
              MessageToast.show("Failed to load Task Run data: Timeout waiting for data cache");
            }
          };

          fnCheckCache();
        },

        _renderTaskRunContent(oTaskData) {
          const oPage = this.byId("taskRunDetailPage");
          oPage.removeAllContent();

          let oBlockLayout = new BlockLayout({
            background: "Default"
          });

          // Header row with task run info
          let oHeaderRow = new BlockLayoutRow();
          let oHeaderCell = new BlockLayoutCell({
            class: "sapUiNoContentPadding"
          });

          const oHeaderContent = new VBox({
            items: [
              new Title({
                text: oTaskData.name || "Unnamed Task Run",
                level: "H1"
              }),
              new Text({
                text: oTaskData.description || "No description available"
              })
            ]
          });

          oHeaderCell.addContent(oHeaderContent);
          oHeaderRow.addContent(oHeaderCell);
          oBlockLayout.addContent(oHeaderRow);

          // Task run overview
          let oOverviewRow = new BlockLayoutRow();
          let oOverviewCell = new BlockLayoutCell({
            backgroundColorSet: "ColorSet11",
            backgroundColorShade: "ShadeD",
            width: 2
          });

          const oOverviewContent = new VBox({
            items: [
              new Icon({
                src: "sap-icon://task",
                size: "2.5rem",
                color: "Default"
              }),
              new Title({
                text: "Task Run Overview",
                level: "H3"
              }),
              new Text({
                text: "View and manage your task execution details using the navigation panel."
              })
            ]
          });

          oOverviewCell.addContent(oOverviewContent);
          oOverviewRow.addContent(oOverviewCell);

          // Statistics cell
          let oStatsCell = new BlockLayoutCell({
            backgroundColorSet: "ColorSet5",
            backgroundColorShade: "ShadeB",
            width: 2
          });

          const iBotInstanceCount = (oTaskData.botInstances && oTaskData.botInstances.length) || 0;
          const iContextNodeCount = (oTaskData.contextNodes && oTaskData.contextNodes.length) || 0;

          const oStatsContent = new VBox({
            items: [
              new Icon({
                src: "sap-icon://pie-chart",
                size: "2.5rem",
                color: "Default"
              }),
              new Title({
                text: "Statistics",
                level: "H3"
              }),
              new Text({
                text: iBotInstanceCount + " Bot Instances"
              }),
              new Text({
                text: iContextNodeCount + " Context Nodes"
              })
            ]
          });

          oStatsCell.addContent(oStatsContent);
          oOverviewRow.addContent(oStatsCell);

          oBlockLayout.addContent(oOverviewRow);

          oPage.addContent(oBlockLayout);
        },

        _renderDefaultHome() {
          const oPage = this.byId("taskRunDetailPage");
          oPage.removeAllContent();

          let oBlockLayout = new BlockLayout({
            background: "Default"
          });

          // Welcome row
          let oWelcomeRow = new BlockLayoutRow();
          let oWelcomeCell = new BlockLayoutCell();

          const oWelcomeContent = new VBox({
            items: [
              new Title({
                text: "Task Run Management",
                level: "H1"
              }),
              new Text({
                text: "Select a task, bot instance, or context node from the navigation to view details."
              })
            ]
          });

          oWelcomeCell.addContent(oWelcomeContent);
          oWelcomeRow.addContent(oWelcomeCell);
          oBlockLayout.addContent(oWelcomeRow);

          oPage.addContent(oBlockLayout);
        }
      }
    );
  }
);