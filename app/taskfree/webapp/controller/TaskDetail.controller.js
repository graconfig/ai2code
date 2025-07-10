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
         oRouter.getRoute("RouteTaskDetail").attachPatternMatched(this._onRouteMatched, this);
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
          var sPath = "/Tasks(" + sTaskId + ")";
          
          var oBinding = oModel.bindContext(sPath, null, {
            $expand: "type,botInstances($expand=type),contextNodes"
          });
          
          oBinding.attachDataReceived(function(oEvent) {
            try {
              var oBoundContext = oBinding.getBoundContext();
              if (oBoundContext) {
                var oData = oBoundContext.getObject();
                if (oData) {
    
                  
                  // Bind the view to the context
                  that.getView().setBindingContext(oBoundContext);
                  
                  // Update page title
                  var sTitle = oData.name || "Task Detail";
                  that.byId("taskDetailPage").setTitle(sTitle);
                } else {
                  MessageToast.show("No data found for Task");
                }
              } else {
                MessageToast.show("Failed to load Task data");
              }
            } catch (error) {
              MessageToast.show("Error processing Task data: " + error.message);
            }
          });
          
          // Enhanced error handling
          oBinding.attachEvent("dataReceived", function(oEvent) {
            var oParameters = oEvent.getParameters();
            if (oParameters && oParameters.error) {
              MessageToast.show("Error loading Task: " + oParameters.error.message);
            }
          });
          
          // Request data with proper error handling
          oBinding.requestObject().catch(function(oError) {
            MessageToast.show("Failed to load Task data: " + (oError.message || oError.toString()));
          });
        }
      }
    );
  }
); 