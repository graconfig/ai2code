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
        onInit() {
          this.getOwnerComponent()
            .getRouter()
            .getRoute("RouteTaskDetail")
            .attachPatternMatched(this._onRouteMatched, this);
        },

        _onRouteMatched(oEvent) {
          const sRouteName = oEvent.getParameter("name");
          const oArguments = oEvent.getParameter("arguments");

          if (sRouteName === "RouteTaskDetail" && oArguments.taskId) {
            // Remove quotes if present and validate ID
            const sTaskId = oArguments.taskId.replace(/'/g, '');

            if (sTaskId) {
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

        _loadTaskDetail(sTaskId) {
          const oModel = this.getOwnerComponent().getModel();
          const sPath = "/Tasks(" + sTaskId + ")";

          const oBinding = oModel.bindContext(sPath, null, {
            $expand: "type,botInstances($expand=type),contextNodes"
          });

          oBinding.attachDataReceived((oEvent) => {
            try {
              const oBoundContext = oBinding.getBoundContext();
              if (oBoundContext) {
                const oData = oBoundContext.getObject();
                if (oData) {
                  // Bind the view to the context
                  this.getView().setBindingContext(oBoundContext);

                  // Update page title
                  const sTitle = oData.name || "Task Detail";
                  this.byId("taskDetailPage").setTitle(sTitle);
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
          oBinding.attachEvent("dataReceived", (oEvent) => {
            const oParameters = oEvent.getParameters();
            if (oParameters && oParameters.error) {
              MessageToast.show("Error loading Task: " + oParameters.error.message);
            }
          });

          // Request data with proper error handling
          oBinding.requestObject()
            .catch((oError) => {
              MessageToast.show("Failed to load Task data: " + (oError.message || oError.toString()));
            });
        }
      }
    );
  }
); 