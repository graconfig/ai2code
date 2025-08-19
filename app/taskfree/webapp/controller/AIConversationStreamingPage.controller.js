sap.ui.define(
  ["sap/ui/core/mvc/Controller", "sap/m/MessageToast", "sap/m/MessageBox",
    "ai/orchestration/taskfree/service/NewMessageStreamingHandler"
  ],
  /**
   * @param {typeof sap.ui.core.mvc.Controller} Controller
   */
  function (Controller,
	MessageToast,
	MessageBox,
	NewMessageHandler) {
    "use strict";

    return Controller.extend(
      "ai.orchestration.taskfree.controller.AIConversationStreamingPage",
      {
        onInit() {
          this.getOwnerComponent()
            .getRouter()
            .getRoute("RouteAIConversation")
            .attachPatternMatched(this._onRouteMatched, this);

          this._currentBotInstanceId = null;
        },

        _onRouteMatched(oEvent) {
          const oArguments = oEvent.getParameter("arguments");
          const sBotInstanceId = oArguments.botInstanceId;

          if (sBotInstanceId) {
            this._currentBotInstanceId = sBotInstanceId;

            this.getView().bindElement({
              path: "/BotInstances(" + sBotInstanceId + ")",
              parameters: {
                $expand: "messages,type,context"
              }
            });
             
             // Load messages when route matched
            this.loadMessages();

          } else {
            MessageToast.show("Invalid Bot Instance ID: " + oArguments.botInstanceId);
          }
        },
        loadMessages: function () {
            // Get messages from backend
            const BotInstance = this.getView().getBindingContext();
            const binding =  this.getView().getModel().bindList(`messages`, BotInstance, new sap.ui.model.Sorter("createdAt", true), null, {
                $select: ["ID", "role", "message", "createdAt"]
            });


            binding.requestContexts().then((contexts) => {
                // const messages = contexts.map(context => context.getObject());
                const messages = contexts.map(context => {
                    return {
                        ...context.getObject(),
                        createdAt: new Date(context.getObject().createdAt)
                    }
                });
                const localModel = this.getView().getModel("local");
                localModel.setProperty("/messages", messages);
            });
        },

        onRefreshPress() {
          const oElementBinding = this.getView().getElementBinding();
          if (oElementBinding) {
            oElementBinding.refresh();
            const sMessage = this.getView().getModel("i18n").getResourceBundle().getText("messagesRefreshed");
            MessageToast.show(sMessage);
          } else {
            MessageToast.show("No binding available for refresh");
          }
        },

        onClearPress() {
          const oResourceBundle = this.getView().getModel("i18n").getResourceBundle();

          const oBindingContext = this.getView().getBindingContext();
          if (!oBindingContext) {
            MessageToast.show("No context available for clearing messages");
            return;
          }

          MessageBox.confirm(
            oResourceBundle.getText("clearConfirmMessage"),
            {
              title: oResourceBundle.getText("clearConfirmTitle"),
              onClose: (oAction) => {
                if (oAction === MessageBox.Action.OK) {
                  this._clearAllMessages();
                }
              }
            }
          );
        },

        _clearAllMessages() {
          const oBindingContext = this.getView().getBindingContext();
          if (!oBindingContext) {
            MessageToast.show("No context available for clearing messages");
            return;
          }

          const oMessageList = this.getView().byId("aiPageMessageList");
          const oBinding = oMessageList ? oMessageList.getBinding("items") : null;

          if (!oBinding) {
            MessageToast.show("No message binding found");
            return;
          }

          const aContexts = oBinding.getAllCurrentContexts();

          if (!aContexts || aContexts.length === 0) {
            MessageToast.show("No messages to clear");
            return;
          }

          if (oMessageList) {
            oMessageList.setBusy(true);
          }

          let aPromises = [];

          aContexts.forEach((oContext) => {
            try {
              if (oContext && typeof oContext.getPath === 'function') {
                const sPath = oContext.getPath();

                const oDeletePromise = oContext.delete("$auto")
                  .then(() => {
                  }).catch((oError) => {
                    throw oError;
                  });

                aPromises.push(oDeletePromise);
              }
            } catch (error) {
              // Silent error handling
            }
          });

          Promise.all(aPromises)
            .then((aResults) => {
              const oResourceBundle = this.getView().getModel("i18n").getResourceBundle();
              MessageToast.show(oResourceBundle.getText("messagesClearedSuccess") || "All messages cleared successfully");

              // Refresh the element binding
              const oElementBinding = this.getView().getElementBinding();
              if (oElementBinding) {
                oElementBinding.refresh();
              }

            }).catch((oError) => {
              MessageToast.show("Error clearing messages: " + (oError.message || oError.toString()));

            }).finally(() => {
              if (oMessageList) {
                oMessageList.setBusy(false);
              }
            });
        },

        onPostMessage(event) {
          if (!event.getParameter("value")) {
            return;
          }

          const message = event.getParameter("value");
          const messageList = this.getView().byId("aiPageMessageList");

          const oBindingContext = this.getView().getBindingContext();

          if (!oBindingContext) {
            MessageToast.show("Bot instance not available. Please refresh the page.");
            return;
          }

          try {
            const binding = messageList ? messageList.getBinding("items") : null;
            

            if (!binding) {
              MessageToast.show("Message binding not available. Please refresh the page.");
              return;
            }

            const messageHandler = new NewMessageHandler({
              botInstance: oBindingContext,
              binding: binding,
              message: message,
              sender: "user",
              bindingmodel: oBindingContext,
              servicemodel: this.getOwnerComponent().getModel(),
              onCreatedEmptyAssistantMessage: function (replyContext) {
                    
                    //初始化空值
                    replyContext.setProperty("role", "assistant");
                    replyContext.setProperty("message", "");
                    replyContext.setProperty("createdAt", "");

                    // 设置Busy状态                        
                    const messageListItem = messageList.getItems().find(item =>
                        item.getBindingContext("local") === replyContext
                    );
                    if (messageListItem) {
                        messageListItem.setLoading(true);
                    }
                    this._scheduleScrollToBottom();
                }.bind(this),
                streamingCallback: function (chunk, replyContext) {
                    if (!chunk) return;
                    
                    //判断chunk内容包含 {"role":"assistant"}
                    //为当前返回的消息内容，此时获取ID还有createdAt进行赋值
                    var assistant_start = "{\"role\":\"assistant\"";  
                    if( chunk.substring(0, 19) === assistant_start ){

                      var assistantMessage = JSON.parse(chunk);
                      replyContext.setProperty("ID", assistantMessage.ID); 
                      replyContext.setProperty("createdAt", new Date());

                      const messageListItem = messageList.getItems().find(item =>
                            item.getBindingContext("local") === replyContext
                        );
                        if (messageListItem) {
                            messageListItem.setLoading(false);
                            messageListItem.invalidate();
                        }

                        // const listEndMarker = this._dialogWithStream.getContent()[0].getContent()[0].getItems()[1];
                        // UIHelper.scrollToElement(listEndMarker.getDomRef());
                        this._scheduleScrollToBottom();

                    }else{
                        // replyContext.setProperty("content", `${replyContext.getProperty("content")}${chunk}`);
                        replyContext.setProperty("message", chunk); 

                        const messageListItem = messageList.getItems().find(item =>
                            item.getBindingContext("local") === replyContext
                        );
                        if (messageListItem) {
                            messageListItem.setLoading(false);
                            messageListItem.invalidate();
                        }

                        // const listEndMarker = this._dialogWithStream.getContent()[0].getContent()[0].getItems()[1];
                        // UIHelper.scrollToElement(listEndMarker.getDomRef());
                        this._scheduleScrollToBottom();
                    }
                }.bind(this),
                onComplete: function () {
                    // 对话完成后恢复状态
                    this._isProcessing = false;
                    //feedInput.setEnabled(true);
                    //feedInput.setValue("");
                    // refresh title of Object Page
                    oBindingContext.refresh();
                    // refresh current context of the chat list
                    this._triggerChatListRefresh(oBindingContext);
                }.bind(this)
            });

            //messageHandler.createMessageAndCompletion();
            messageHandler.createMessageAndCompletion(true,
                '/rest/v1/chat/streaming'
            );

            this._scheduleScrollToBottom();

          } catch (error) {
            MessageToast.show("Error sending message: " + error.message);
          }
        },
        _triggerChatListRefresh: function (oBindingContext) {
            var that = this
            oBindingContext.requestProperty("ID").then((MessageId) => {
                var oBus = that.getOwnerComponent().getEventBus();
                oBus.publish("MessagesChannel", "TitleUpdated", {
                    ID: MessageId
                });
            });
        },

        onBtnAdoptPress(event) {
          event.getSource().setBusy(true);
          const context = event.getSource().getBindingContext();

          if (!context) {
            MessageToast.show("No message context available");
            event.getSource().setBusy(false);
            return;
          }

          const contextBinding = this.getView().getModel().bindContext("MainService.adopt(...)", context);

          contextBinding.invoke()
            .then((result) => {
              MessageToast.show("Message adopted successfully");

              // Notify navigation controller this ContextNode data has changed
              sap.ui.getCore().getEventBus().publish("DataUpdate", "ContextNodeChanged");

              const oElementBinding = this.getView().getElementBinding();
              if (oElementBinding) {
                oElementBinding.refresh();
              }

            }).catch((error) => {
              MessageToast.show("Error adopting message: " + (error.message || error.toString()));

            }).finally(() => {
              event.getSource().setBusy(false);
            });
        },

        _scheduleScrollToBottom() {
          setTimeout(() => {
            this._scrollToBottom();
          }, 100);
        },

        _scrollToBottom() {
          try {
            const oMessageList = this.getView().byId("aiPageMessageList");
            if (oMessageList && oMessageList.getItems().length > 0) {
              const oLastItem = oMessageList.getItems()[oMessageList.getItems().length - 1];
              if (oLastItem) {
                oLastItem.getDomRef()?.scrollIntoView({
                  behavior: "smooth",
                  block: "end"
                });
              }
            }
          } catch (error) {
            // Silent error handling
          }
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
              if (!oBotInstance.context || !oBotInstance.context.ID) {
                MessageToast.show("No ContextNode associated with this BotInstance. Please adopt a message first to create the context.");
                return;
              }

              const sContextNodeId = oBotInstance.context.ID;

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

                  const sType = (oContextNode.type || "string").toLowerCase();
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
                    default:
                      sRouteName = "RouteTextNodePage";
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
        },

        onExit() {
          if (this._scrollTimeout) {
            clearTimeout(this._scrollTimeout);
          }
        }
      }
    );
  }
); 