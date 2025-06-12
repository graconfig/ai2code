sap.ui.define(
  ["sap/ui/core/mvc/Controller", "sap/m/MessageToast"],
  /**
   * @param {typeof sap.ui.core.mvc.Controller} Controller
   */
  function (Controller, MessageToast) {
    "use strict";

    return Controller.extend(
      "ai.orchestration.taskfree.controller.TaskDetail",
      {
        onInit: function () {
          const oRouter = this.getOwnerComponent().getRouter();
          oRouter.attachRouteMatched(this._onRouteMatched.bind(this));
        },

        _onRouteMatched: function(oEvent) {
          const sRouteName = oEvent.getParameter("name");
          const oArguments = oEvent.getParameter("arguments");
          
          if (sRouteName === "RouteTaskDetail" && oArguments.taskId) {
            // Remove quotes if present and validate ID
            const sTaskId = oArguments.taskId.replace(/'/g, '');
            
            if (sTaskId && sTaskId.trim() !== '') {
              // Clear previous binding context to force refresh
              this.getView().setBindingContext(null);
              
              // Force immediate loading with slight delay to prevent request collision
              setTimeout(() => {
                this._loadTaskDetail(sTaskId);
              }, 50);
            } else {
              MessageToast.show("Invalid Task ID: " + oArguments.taskId);
            }
          }
        },

        _loadTaskDetail: function(sTaskId) {
          var oModel = this.getOwnerComponent().getModel();
          var that = this;
          
          // Validate GUID format
          var guidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
          if (!guidPattern.test(sTaskId)) {
            MessageToast.show("Invalid GUID format for Task ID: " + sTaskId);
            return;
          }
          
          // For cuid (GUID) primary keys in OData V4, don't use quotes
          var sPath = "/Tasks(" + sTaskId + ")";
          
          console.log("Loading Task detail with path:", sPath);
          
          var oBinding = oModel.bindContext(sPath, null, {
            $expand: "type,botInstances($expand=type),contextNodes"
          });
          
          oBinding.attachDataReceived(function(oEvent) {
            try {
              var oBoundContext = oBinding.getBoundContext();
              if (oBoundContext) {
                var oData = oBoundContext.getObject();
                if (oData) {
                  console.log("Task data loaded successfully:", oData);
                  
                  // Bind the view to the context
                  that.getView().setBindingContext(oBoundContext);
                  
                  // Update page title
                  var sTitle = oData.name || "Task Detail";
                  that.byId("taskDetailPage").setTitle(sTitle);
                } else {
                  console.error("No data found in bound context");
                  MessageToast.show("No data found for Task");
                }
              } else {
                console.error("Failed to get bound context");
                MessageToast.show("Failed to load Task data");
              }
            } catch (error) {
              console.error("Error processing Task data:", error);
              MessageToast.show("Error processing Task data: " + error.message);
            }
          });
          
          oBinding.attachDataRequested(function() {
            console.log("Task data requested");
          });
          
          // Enhanced error handling
          oBinding.attachEvent("dataReceived", function(oEvent) {
            var oParameters = oEvent.getParameters();
            if (oParameters && oParameters.error) {
              console.error("OData error loading Task:", oParameters.error);
              MessageToast.show("Error loading Task: " + oParameters.error.message);
            }
          });
          
          // Request data with proper error handling
          oBinding.requestObject().catch(function(oError) {
            console.error("Failed to request Task data:", oError);
            MessageToast.show("Failed to load Task data: " + (oError.message || oError.toString()));
          });
        }
      }
    );
  }
); 