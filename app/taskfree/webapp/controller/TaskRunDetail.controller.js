sap.ui.define(
  ["sap/ui/core/mvc/Controller", "sap/m/MessageToast",
    "sap/ui/model/json/JSONModel",
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
  function (Controller, MessageToast, JSONModel, BlockLayout, BlockLayoutRow, BlockLayoutCell, Title, Text, VBox, Icon) {
    "use strict";

    return Controller.extend(
      "ai.orchestration.taskfree.controller.TaskRunDetail",
      {
        onInit: function () {
          const oRouter = this.getOwnerComponent().getRouter();
          oRouter.attachRouteMatched(this._onRouteMatched.bind(this));
          
          // Show default home content
          this._renderDefaultHome();
        },

        _onRouteMatched: function(oEvent) {
          const sRouteName = oEvent.getParameter("name");
          const oArguments = oEvent.getParameter("arguments");
          
          if (sRouteName === "RouteTaskRunNav" && oArguments.taskRunId) {
            // Load task data for the main task run detail page
            const sTaskRunId = oArguments.taskRunId.replace(/'/g, '');
            if (sTaskRunId && sTaskRunId.trim() !== '') {
              this._loadTaskRunDetail(sTaskRunId);
            }
          } else if (sRouteName === "RouteTaskRunNav") {
            // Show default home content when no specific task is selected
            this._renderDefaultHome();
          }
        },

        _loadTaskRunDetail: function(sTaskRunId) {
          var oModel = this.getOwnerComponent().getModel();
          var that = this;
          
          // Validate GUID format
          var guidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
          if (!guidPattern.test(sTaskRunId)) {
            MessageToast.show("Invalid GUID format for Task Run ID: " + sTaskRunId);
            return;
          }
          
          // For cuid (GUID) primary keys in OData V4, don't use quotes
          var sPath = "/Tasks(" + sTaskRunId + ")";
          
          console.log("Loading TaskRun detail with path:", sPath);
          
          var oBinding = oModel.bindContext(sPath, null, {
            $expand: "type,botInstances($expand=type),contextNodes"
          });
          
          oBinding.attachDataReceived(function(oEvent) {
            try {
              var oBoundContext = oBinding.getBoundContext();
              if (oBoundContext) {
                var oData = oBoundContext.getObject();
                if (oData) {
                  console.log("TaskRun data loaded successfully:", oData);
                  
                  // Bind the view to the context
                  that.getView().setBindingContext(oBoundContext);
                  
                  // Render task run overview content
                  that._renderTaskRunContent(oData);
                } else {
                  console.error("No data found in bound context");
                  MessageToast.show("No data found for Task Run");
                }
              } else {
                console.error("Failed to get bound context");
                MessageToast.show("Failed to load Task Run data");
              }
            } catch (error) {
              console.error("Error processing TaskRun data:", error);
              MessageToast.show("Error processing Task Run data: " + error.message);
            }
          });
          
          oBinding.attachDataRequested(function() {
            console.log("TaskRun data requested");
          });
          
          // Enhanced error handling
          oBinding.attachEvent("dataReceived", function(oEvent) {
            var oParameters = oEvent.getParameters();
            if (oParameters && oParameters.error) {
              console.error("OData error loading TaskRun:", oParameters.error);
              MessageToast.show("Error loading Task Run: " + oParameters.error.message);
            }
          });
          
          // Request data with proper error handling
          oBinding.requestObject().catch(function(oError) {
            console.error("Failed to request TaskRun data:", oError);
            MessageToast.show("Failed to load Task Run data: " + (oError.message || oError.toString()));
          });
        },

        _renderTaskRunContent: function(oTaskData) {
          var oPage = this.byId("taskRunDetailPage");
          oPage.removeAllContent();
          
          var oBlockLayout = new BlockLayout({
            background: "Default"
          });
          
          // Header row with task run info
          var oHeaderRow = new BlockLayoutRow();
          var oHeaderCell = new BlockLayoutCell({
            class: "sapUiNoContentPadding"
          });
          
          var oHeaderContent = new VBox({
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
          var oOverviewRow = new BlockLayoutRow();
          var oOverviewCell = new BlockLayoutCell({
            backgroundColorSet: "ColorSet11",
            backgroundColorShade: "ShadeD",
            width: 2
          });
          
          var oOverviewContent = new VBox({
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
          var oStatsCell = new BlockLayoutCell({
            backgroundColorSet: "ColorSet5",
            backgroundColorShade: "ShadeB",
            width: 2
          });
          
          var iBotInstanceCount = (oTaskData.botInstances && oTaskData.botInstances.length) || 0;
          var iContextNodeCount = (oTaskData.contextNodes && oTaskData.contextNodes.length) || 0;
          
          var oStatsContent = new VBox({
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

        _renderDefaultHome: function() {
          var oPage = this.byId("taskRunDetailPage");
          oPage.removeAllContent();
          
          var oBlockLayout = new BlockLayout({
            background: "Default"
          });
          
          // Welcome row
          var oWelcomeRow = new BlockLayoutRow();
          var oWelcomeCell = new BlockLayoutCell();
          
          var oWelcomeContent = new VBox({
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