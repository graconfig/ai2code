sap.ui.define(
  ["sap/ui/core/mvc/Controller", "sap/m/MessageToast",
    "ai/orchestration/taskfree/service/NewMessageHandler",
    "ai/orchestration/taskfree/util/UIHelper"
  ],
  /**
   * @param {typeof sap.ui.core.mvc.Controller} Controller
   */
  function (Controller, MessageToast, NewMessageHandler, UIHelper) {
    "use strict";

    return Controller.extend(
      "ai.orchestration.taskfree.controller.BotInstanceDetail",
      {
        onInit() {
          this.getOwnerComponent()
            .getRouter()
            .getRoute("RouteBotInstanceDetail")
            .attachPatternMatched(this._onRouteMatched, this);
        },

        _onRouteMatched(oEvent) {
          const oArguments = oEvent.getParameter("arguments");

          const sBotInstanceId = oArguments.botInstanceId;
          if (sBotInstanceId) {
            this.getView().setBindingContext(null);
            setTimeout(() => {
              this._loadBotInstanceDetail(sBotInstanceId);
            }, 50);
          } else {
            MessageToast.show("Invalid Bot Instance ID: " + oArguments.botInstanceId);
          }
        },

        _loadBotInstanceDetail(sBotInstanceId) {
          var oModel = this.getOwnerComponent().getModel();

          var sPath = "/BotInstances(" + sBotInstanceId + ")";
          var oBinding = oModel.bindContext(sPath, null, {
            $expand: "type,messages"
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
                  const sTitle = (oData.type && oData.type.name) ? oData.type.name : "Bot Instance Detail";
                  this.getView().byId("botInstanceDetailPage").setTitle(sTitle);
                  this._updateAIChatButtonVisibility(oData);

                  // Store the binding context for AI conversation
                  this.oContext = oBoundContext;
                  this.bindingmodel = this.oContext;
                  this.servicemodel = this.getOwnerComponent().getModel();
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
          oBinding.attachEvent("dataReceived", (oEvent) => {
            const oParameters = oEvent.getParameters();
            if (oParameters && oParameters.error) {
              MessageToast.show("Error loading Bot Instance: " + oParameters.error.message);
            }
          });

          // Request data with proper error handling
          oBinding.requestObject()
            .catch((oError) => {
              MessageToast.show("Failed to load Bot Instance data: " + (oError.message || oError.toString()));
            });
        },

        _updateAIChatButtonVisibility(oBotInstanceData) {
          const oAIChatButton = this.getView().byId("aiChatButton");
          if (oAIChatButton && oBotInstanceData && oBotInstanceData.type && oBotInstanceData.type.name) {
            // Show AI Chat button only if BotInstance type name starts with "Chat"
            const bShowButton = oBotInstanceData.type.name.startsWith("Chat");
            oAIChatButton.setVisible(bShowButton);
          }
        },

        onAIChatPress() {
          this.onBotInstancePress();
        },

        onBotInstancePress(oEvent) {
          if (!this.oContext) {
            MessageToast.show("No bot instance data available");
            return;
          }

          this.pDialog ??= this.loadFragment({
            name: "ai.orchestration.taskfree.view.fragment.AIConversation",
            addToDependents: false
          });

          this.pDialog.then((oDialog) => {
            oDialog.setModel(this.getView().getModel());
            oDialog.setBindingContext(this.oContext);
            this._dialog = oDialog;
            oDialog.open();

            // Add list data loading event handler
            const messageList = oDialog.getContent()[0].getContent()[0].getItems()[0];
            messageList.getBinding("items").attachDataReceived(() => {
              this.scrollToListEnd();
            });
          });

          // Define dialog event handlers
          this.onAIConversationClose = (oEvent) => {
            this.pDialog.then((oDialog) => oDialog.close());
          };

          this.onPostMessage = (event) => {
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

          this.onBtnAdoptPress = (event) => {
            event.getSource().setBusy(true);
            const context = event.getSource().getBindingContext();

            if (!context) {
              MessageToast.show("No message context available");
              event.getSource().setBusy(false);
              return;
            }

            // Get the message ID and construct the correct path
            const oData = context.getObject();
            const sMessageId = oData.ID;
            const oModel = this.getView().getModel();

            // Try using the context's canonical path first
            try {
              const sPath = context.getCanonicalPath();
              const contextBinding = oModel.bindContext(sPath + "/MainService.adopt(...)");

              contextBinding.invoke()
                .then(() => {
                  MessageToast.show("Message adopted successfully");

                  // Notify navigation controller this ContextNode data has changed
                  sap.ui.getCore().getEventBus().publish("DataUpdate", "ContextNodeChanged");

                  // Refresh the messages list to reflect changes
                  const messageList = this._dialog.getContent()[0].getContent()[0].getItems()[0];
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

          this.onPressSyncChangesToChatList = (event) => {
            const binding = this.getView().getModel().bindContext("ChatService.appendToChatRecord(...)",
              this.getView().getBindingContext()
            );
            binding.invoke().then(() => {
              // refresh botInstance 
              this.getView().getBindingContext().refresh();
            });
          };

          this.scrollToListEnd = () => {
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

        onExecuteButtonPress(oEvent) {
          oEvent.getSource().setBusy(true);
          const context = oEvent.getSource().getBindingContext();

          if (!context) {
            MessageToast.show("No message context available");
            oEvent.getSource().setBusy(false);
            return;
          }

          const contextBinding = this.getView().getModel().bindContext("MainService.execute(...)", context);

          contextBinding.invoke().then((result) => {
            MessageToast.show("Message execute successfully");

            // Get BotInstance ID and notify navigation controller
            const oBotInstanceContext = this.getView().getBindingContext();
            const sBotInstanceId = oBotInstanceContext ? oBotInstanceContext.getProperty("ID") : null;

            // Notify navigation controller this BotInstance data has changed with new tasks
            sap.ui.getCore().getEventBus().publish("DataUpdate", "BotInstanceChanged", { botInstanceId: sBotInstanceId });

            if (oBotInstanceContext) {
              oBotInstanceContext.refresh();
            }

          }).catch((error) => {
            MessageToast.show("Error executing message: " + (error.message || error.toString()));

          }).finally(() => {
            oEvent.getSource().setBusy(false);
          });

        },

        onGotoContextPress() {
          const oBindingContext = this.getView().getBindingContext();
          if (!oBindingContext) {
            MessageToast.show("No BotInstance context available");
            return;
          }

          // 重新获取最新的BotInstance数据
          oBindingContext.requestObject()
            .then((oBotInstance) => {
              // 检查BotInstance是否有关联的ContextNode
              if (!oBotInstance.contextID) {
                MessageToast.show("No ContextNode associated with this BotInstance.");
                return;
              }

              const sContextNodeId = oBotInstance.contextID;

              // 获取ContextNode的详细信息以确定类型
              const oModel = this.getView().getModel();
              const sContextPath = "/ContextNodes(" + sContextNodeId + ")";

              oModel.bindContext(sContextPath)
                .requestObject()
                .then((oContextNode) => {
                  if (!oContextNode) {
                    MessageToast.show("No ContextNode found. Please adopt a message first to create the context.");
                    return;
                  }

                  // 切换左侧导航到ContextNodes视图

                  const sType = (oContextNode.type)?.toLowerCase();
                  const oRouter = this.getOwnerComponent().getRouter();

                  // 根据类型跳转到对应的页面
                  let sRouteName;
                  switch (sType) {
                    case "code":
                    case "json":
                      sRouteName = "RouteTextAreaNodePage";
                      break;
                    case "markdown":
                      sRouteName = "RouteMarkDownNodePage";
                      break;
                    case "string":
                      sRouteName = "RouteTextAreaNodePage";
                      break;
                    default:
                      sRouteName = "RouteTextAreaNodePage";
                      break;
                  }

                  oRouter.navTo(sRouteName, {
                    contextNodeId: sContextNodeId
                  });

                }).catch((oError) => {
                  MessageToast.show("No ContextNode found. Please adopt a message first to create the context.");
                });

            }).catch((oError) => {
              MessageToast.show("Error loading BotInstance: " + (oError.message || oError.toString()));
            });
        }
      }
    );
  }
); 