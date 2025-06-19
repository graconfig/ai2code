sap.ui.define(
  ["sap/ui/core/mvc/Controller", "sap/m/MessageToast"],
  /**
   * @param {typeof sap.ui.core.mvc.Controller} Controller
   */
  function (Controller, MessageToast) {
    "use strict";

    return Controller.extend(
      "ai.orchestration.taskfree.controller.ContextNodeDetail",
      {
        onInit: function () {
          const oRouter = this.getOwnerComponent().getRouter();
          oRouter.attachRouteMatched(this._onRouteMatched.bind(this));
        },

        _onRouteMatched: function(oEvent) {
          const sRouteName = oEvent.getParameter("name");
          const oArguments = oEvent.getParameter("arguments");
          
          if (sRouteName === "RouteContextNodeDetail" && oArguments.contextNodeId) {
            // Remove quotes if present and validate ID
            const sContextNodeId = oArguments.contextNodeId.replace(/'/g, '');
            
            if (sContextNodeId && sContextNodeId.trim() !== '') {
              // Clear previous binding context to force refresh
              this.getView().setBindingContext(null);
              
              // Force immediate loading with slight delay to prevent request collision
              setTimeout(() => {
                this._loadContextNodeDetail(sContextNodeId);
              }, 50);
            } else {
              MessageToast.show("Invalid Context Node ID: " + oArguments.contextNodeId);
            }
          }
        },

        _loadContextNodeDetail: function(sContextNodeId) {
          var oModel = this.getOwnerComponent().getModel();
          var that = this;
          
          // Validate GUID format
          var guidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
          if (!guidPattern.test(sContextNodeId)) {
            MessageToast.show("Invalid GUID format for Context Node ID: " + sContextNodeId);
            return;
          }
          
          // For cuid (GUID) primary keys in OData V4, don't use quotes
          var sPath = "/ContextNodes(" + sContextNodeId + ")";
          

          
          var oBinding = oModel.bindContext(sPath, null, {
            $expand: "task"
          });
          
          oBinding.attachDataReceived(function(oEvent) {
            try {
              var oBoundContext = oBinding.getBoundContext();
              if (oBoundContext) {
                var oData = oBoundContext.getObject();
                if (oData) {

                  
                  // Bind the view to the context
                  that.getView().setBindingContext(oBoundContext);
                  
                  // Update page title if needed
                  var sTitle = oData.label || "Context Node Detail";
                  that.byId("contextNodeDetailPage").setTitle(sTitle);
                } else {
                  MessageToast.show("No data found for Context Node");
                }
              } else {
                MessageToast.show("Failed to load Context Node data");
              }
            } catch (error) {
              MessageToast.show("Error processing Context Node data: " + error.message);
            }
          });
          
          // Enhanced error handling
          oBinding.attachEvent("dataReceived", function(oEvent) {
            var oParameters = oEvent.getParameters();
            if (oParameters && oParameters.error) {
              MessageToast.show("Error loading Context Node: " + oParameters.error.message);
            }
          });
          
          // Request data with proper error handling
          oBinding.requestObject().catch(function(oError) {
            MessageToast.show("Failed to load Context Node data: " + (oError.message || oError.toString()));
          });
        }
      }
    );
  }
); 