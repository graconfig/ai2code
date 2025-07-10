sap.ui.define(
  ["sap/ui/core/mvc/Controller", "sap/m/MessageToast",
    "ai/orchestration/taskfree/util/Helper",
    "ai/orchestration/taskfree/service/ChatService",
    "ai/orchestration/taskfree/service/NewMessageHandler",
    "ai/orchestration/taskfree/util/UIHelper"
  ],
  /**
   * @param {typeof sap.ui.core.mvc.Controller} Controller
   */
  function (Controller, MessageToast, Helper, ChatService, NewMessageHandler, UIHelper) {
    "use strict";

    return Controller.extend(
      "ai.orchestration.taskfree.controller.BotInstanceDetail",
      {
        onInit: function () {
          const oRouter = this.getOwnerComponent().getRouter();
          oRouter.attachRouteMatched(this._onRouteMatched.bind(this));
        },

        _onRouteMatched: function(oEvent) {
          const sRouteName = oEvent.getParameter("name");
          const oArguments = oEvent.getParameter("arguments");
          
          if (sRouteName === "RouteBotInstanceDetail" && oArguments.botInstanceId) {
            // Remove quotes if present and validate ID
            const sBotInstanceId = oArguments.botInstanceId.replace(/'/g, '');
            
            if (sBotInstanceId && sBotInstanceId.trim() !== '') {
              // Clear previous binding context to force refresh
              this.getView().setBindingContext(null);
              
              // Force immediate loading with slight delay to prevent request collision
              setTimeout(() => {
                this._loadBotInstanceDetail(sBotInstanceId);
              }, 50);
            } else {
              MessageToast.show("Invalid Bot Instance ID: " + oArguments.botInstanceId);
            }
          }
        },

        _loadBotInstanceDetail: function(sBotInstanceId) {
          var oModel = this.getOwnerComponent().getModel();
          var that = this;
          
          // Validate GUID format
          var guidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
          if (!guidPattern.test(sBotInstanceId)) {
            MessageToast.show("Invalid GUID format for Bot Instance ID: " + sBotInstanceId);
            return;
          }
          
          // For cuid (GUID) primary keys in OData V4, don't use quotes
          var sPath = "/BotInstances(" + sBotInstanceId + ")";
          

          
          var oBinding = oModel.bindContext(sPath, null, {
            $expand: "type,messages"
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
                  var sTitle = (oData.type && oData.type.name) ? oData.type.name : "Bot Instance Detail";
                  that.byId("botInstanceDetailPage").setTitle(sTitle);
                  that._updateAIChatButtonVisibility(oData);
                  
                  // Store the binding context for AI conversation
                  that.oContext = oBoundContext;
                  that.bindingmodel = that.oContext;
                  that.servicemodel = that.getOwnerComponent().getModel();
                } else {
                  MessageToast.show("No data found for Bot Instance");
                }
              } else {
                MessageToast.show("Failed to load Bot Instance data");
              }
            } catch (error) {
              MessageToast.show("Error processing Bot Instance data: " + error.message);
            }
          });
          
          // Enhanced error handling
          oBinding.attachEvent("dataReceived", function(oEvent) {
            var oParameters = oEvent.getParameters();
            if (oParameters && oParameters.error) {
              MessageToast.show("Error loading Bot Instance: " + oParameters.error.message);
            }
          });
          
          // Request data with proper error handling
          oBinding.requestObject().catch(function(oError) {
            MessageToast.show("Failed to load Bot Instance data: " + (oError.message || oError.toString()));
          });
        },

        _updateAIChatButtonVisibility: function(oBotInstanceData) {
          var oAIChatButton = this.byId("aiChatButton");
          if (oAIChatButton && oBotInstanceData && oBotInstanceData.type && oBotInstanceData.type.name) {
            // Show AI Chat button only if BotInstance type name starts with "Chat"
            var bShowButton = oBotInstanceData.type.name.startsWith("Chat");
            oAIChatButton.setVisible(bShowButton);
          }
        },

        onAIChatPress: function() {
          // Same functionality as onBotInstancePress
          this.onBotInstancePress();
        },
        onBotInstancePress: function (oEvent) {
          if (!this.oContext) {
            MessageToast.show("No bot instance data available");
            return;
          }
          
          // Load and open the AI conversation dialog
          this.pDialog ??= this.loadFragment({
            name: "ai.orchestration.taskfree.view.fragment.AIConversation",
            addToDependents: false
          });

          const that = this;

          this.pDialog.then((oDialog) => {
            oDialog.setModel(that.getView().getModel());
            oDialog.setBindingContext(that.oContext);
            that._dialog = oDialog;
            oDialog.open();
            
            // Add list data loading event handler
            const messageList = oDialog.getContent()[0].getContent()[0].getItems()[0];
            messageList.getBinding("items").attachDataReceived(() => {
              this.scrollToListEnd();
            });
          });
          
          // Define dialog event handlers
          this.onAIConversationClose = function (oEvent) {
            this.pDialog.then((oDialog) => oDialog.close());
          };

          this.onPostMessage = function (event) {
            if (!event.getParameter("value")) {
              return;
            }
            const message = event.getParameter("value");
            const botInstance = this.bindingmodel;
            const messageList = this._dialog.getContent()[0].getContent()[0].getItems()[0];
            const binding = messageList.getBinding("items");

            const messageHandler = new NewMessageHandler({
              botInstance: botInstance,
              binding: binding,
              message: message,
              sender: "user",
              bindingmodel: this.bindingmodel,
              servicemodel: this.servicemodel
            });

            messageHandler.createMessageAndCompletion();
          };

          this.onBtnAdoptPress = function (event) {
            event.getSource().setBusy(true);
            var context = event.getSource().getBindingContext();
            
            if (!context) {
              MessageToast.show("No message context available");
              event.getSource().setBusy(false);
              return;
            }
            
            // Get the message ID and construct the correct path
            var oData = context.getObject();
            var sMessageId = oData.ID;
            var oModel = this.getView().getModel();
            
            // Try using the context's canonical path first
            try {
              var sPath = context.getCanonicalPath();
              var contextBinding = oModel.bindContext(sPath + "/MainService.adopt(...)");
              
              contextBinding.invoke().then(() => {
                MessageToast.show("Message adopted successfully");
                
                // Notify navigation controller that ContextNode data has changed
                sap.ui.getCore().getEventBus().publish("DataUpdate", "ContextNodeChanged");
                
                // Refresh the messages list to reflect changes
                var messageList = this._dialog.getContent()[0].getContent()[0].getItems()[0];
                if (messageList && messageList.getBinding("items")) {
                  messageList.getBinding("items").refresh();
                }
              }).catch((error) => {
                MessageToast.show("Error adopting message: " + error.message);
              }).finally(() => {
                event.getSource().setBusy(false);
              });
            } catch (error) {
              MessageToast.show("Error setting up adopt operation: " + error.message);
              event.getSource().setBusy(false);
            }
          };

          this.onPressSyncChangesToChatList = function (event) {
            const binding = this.getView().getModel().bindContext("ChatService.appendToChatRecord(...)",
              this.getView().getBindingContext()
            );
            binding.invoke().then(() => {
              // refresh botInstance 
              this.getView().getBindingContext().refresh();
            });
          };
          
          this.scrollToListEnd = function () {
            if (!this._dialog) {
              return;
            }

            const listEndMarker = this._dialog.getContent()[0].getContent()[0].getItems()[1];
            if (listEndMarker && listEndMarker.getDomRef()) {
              UIHelper.scrollToElement(listEndMarker.getDomRef());
            } else {
              // If element is not rendered yet, retry
              setTimeout(() => this.scrollToListEnd(), 100);
            }
          };
        },
        
        // Reserved method for execute functionality - to be implemented later
        onExecutePress: function() {
          var oContext = this.getView().getBindingContext();
          if (!oContext) {
            MessageToast.show("No bot instance context available");
            return;
          }
          
          // TODO: Implement execute functionality
          // When implemented, add this after successful execution:
          // sap.ui.getCore().getEventBus().publish("DataUpdate", "TaskChanged");
          
          MessageToast.show("Execute functionality not yet implemented");
        },

        onExecuteButtonPress: function(oEvent) {
          oEvent.getSource().setBusy(true);
          var context = oEvent.getSource().getBindingContext();
          
          if (!context) {
            MessageToast.show("No message context available");
            oEvent.getSource().setBusy(false);
            return;
          }

          var contextBinding = this.getView().getModel().bindContext("MainService.execute(...)", context);
          var that = this;

          contextBinding.invoke().then(function(result) {
            MessageToast.show("Message execute successfully");
            
            // Get BotInstance ID and notify navigation controller
            var oBotInstanceContext = that.getView().getBindingContext();
            var sBotInstanceId = oBotInstanceContext ? oBotInstanceContext.getProperty("ID") : null;
            
            // Notify navigation controller that BotInstance data has changed with new tasks
            sap.ui.getCore().getEventBus().publish("DataUpdate", "BotInstanceChanged", { botInstanceId: sBotInstanceId });
            
            if (oBotInstanceContext) {
              oBotInstanceContext.refresh();
            }
            
          }).catch(function(error) {
            MessageToast.show("Error executing message: " + (error.message || error.toString()));
            
          }).finally(function() {
            oEvent.getSource().setBusy(false);
          });

        },

        onGotoContextPress: function() {
          var oBindingContext = this.getView().getBindingContext();
          if (!oBindingContext) {
            MessageToast.show("No BotInstance context available");
            return;
          }

          var oBotInstance = oBindingContext.getObject();
          
          // 检查BotInstance是否有关联的ContextNode
          if (!oBotInstance.contextID) {  
            MessageToast.show("No ContextNode associated with this BotInstance. Please adopt a message first to create the context.");
            return;
          }
          
          var sContextNodeId = oBotInstance.contextID;
          
          // 获取ContextNode的详细信息以确定类型
          var oModel = this.getView().getModel();
          var sContextPath = "/ContextNodes(" + sContextNodeId + ")";
          
          var that = this;
          oModel.bindContext(sContextPath).requestObject().then(function(oContextNode) {
            if (!oContextNode) {
              MessageToast.show("No ContextNode found. Please adopt a message first to create the context.");
              return;
            }

            // 切换左侧导航到ContextNodes视图


            var sType = (oContextNode.type || "string").toLowerCase();
            var oRouter = that.getOwnerComponent().getRouter();
            
            // 根据类型跳转到对应的页面
            var sRouteName;
            switch (sType) {
              case "code":
              case "json":
                sRouteName = "RouteTextAreaNodePage";
                break;
              case "markdown":
                sRouteName = "RouteMarkDownNodePage";
                break;
              case "string":
              default:
                sRouteName = "RouteTextNodePage";
                break;
            }
            
            oRouter.navTo(sRouteName, {
              contextNodeId: sContextNodeId
            });
            
          }).catch(function(oError) {
            MessageToast.show("No ContextNode found. Please adopt a message first to create the context.");
          });
        }
      }
    );
  }
); 