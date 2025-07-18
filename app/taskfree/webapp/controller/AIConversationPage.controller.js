sap.ui.define(
  ["sap/ui/core/mvc/Controller", "sap/m/MessageToast", "sap/m/MessageBox",
    "ai/orchestration/taskfree/service/NewMessageHandler"
  ],
  /**
   * @param {typeof sap.ui.core.mvc.Controller} Controller
   */
  function (Controller, MessageToast, MessageBox, NewMessageHandler,) {
    "use strict";

    return Controller.extend(
      "ai.orchestration.taskfree.controller.AIConversationPage",
      {
        onInit: function () {
          const oRouter = this.getOwnerComponent().getRouter();
          oRouter.getRoute("RouteAIConversation").attachPatternMatched(this._onRouteMatched, this);

          this._currentBotInstanceId = null;
        },

        _onRouteMatched: function (oEvent) {
          const oArguments = oEvent.getParameter("arguments");

          const sBotInstanceId = oArguments.botInstanceId;
          if (sBotInstanceId && sBotInstanceId.trim() !== '') {
            this._currentBotInstanceId = sBotInstanceId;

            this.getView().bindElement({
              path: "/BotInstances(" + sBotInstanceId + ")",
              parameters: {
                $expand: "messages,type,context"
              }
            });
          } else {
            MessageToast.show("Invalid Bot Instance ID: " + oArguments.botInstanceId);
          }
        },

        onRefreshPress: function () {
          var oElementBinding = this.getView().getElementBinding();
          if (oElementBinding) {
            oElementBinding.refresh();
            var sMessage = this.getView().getModel("i18n").getResourceBundle().getText("messagesRefreshed");
            MessageToast.show(sMessage);
          } else {
            MessageToast.show("No binding available for refresh");
          }
        },

        onClearPress: function () {
          var that = this;
          var oResourceBundle = this.getView().getModel("i18n").getResourceBundle();

          var oBindingContext = this.getView().getBindingContext();
          if (!oBindingContext) {
            MessageToast.show("No context available for clearing messages");
            return;
          }

          MessageBox.confirm(
            oResourceBundle.getText("clearConfirmMessage"),
            {
              title: oResourceBundle.getText("clearConfirmTitle"),
              onClose: function (oAction) {
                if (oAction === MessageBox.Action.OK) {
                  that._clearAllMessages();
                }
              }
            }
          );
        },

        _clearAllMessages: function () {
          var oBindingContext = this.getView().getBindingContext();
          if (!oBindingContext) {
            MessageToast.show("No context available for clearing messages");
            return;
          }

          var oMessageList = this.byId("aiPageMessageList");
          var oBinding = oMessageList ? oMessageList.getBinding("items") : null;

          if (!oBinding) {
            MessageToast.show("No message binding found");
            return;
          }

          var that = this;
          var aContexts = oBinding.getAllCurrentContexts();

          if (!aContexts || aContexts.length === 0) {
            MessageToast.show("No messages to clear");
            return;
          }

          if (oMessageList) {
            oMessageList.setBusy(true);
          }

          var aPromises = [];

          aContexts.forEach(function (oContext) {
            try {
              if (oContext && typeof oContext.getPath === 'function') {
                var sPath = oContext.getPath();

                var oDeletePromise = oContext.delete("$auto").then(function () {
                }).catch(function (oError) {
                  throw oError;
                });

                aPromises.push(oDeletePromise);
              }
            } catch (error) {
              // Silent error handling
            }
          });

          Promise.all(aPromises).then(function (aResults) {
            var oResourceBundle = that.getView().getModel("i18n").getResourceBundle();
            MessageToast.show(oResourceBundle.getText("messagesClearedSuccess") || "All messages cleared successfully");

            // Refresh the element binding
            var oElementBinding = that.getView().getElementBinding();
            if (oElementBinding) {
              oElementBinding.refresh();
            }

          }).catch(function (oError) {
            MessageToast.show("Error clearing messages: " + (oError.message || oError.toString()));

          }).finally(function () {
            if (oMessageList) {
              oMessageList.setBusy(false);
            }
          });
        },

        onPostMessage: function (event) {
          if (!event.getParameter("value")) {
            return;
          }

          const message = event.getParameter("value");
          const messageList = this.byId("aiPageMessageList");

          var oBindingContext = this.getView().getBindingContext();

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
              servicemodel: this.getOwnerComponent().getModel()
            });

            messageHandler.createMessageAndCompletion();

            this._scheduleScrollToBottom();

          } catch (error) {
            MessageToast.show("Error sending message: " + error.message);
          }
        },

        onBtnAdoptPress: function (event) {
          event.getSource().setBusy(true);
          var context = event.getSource().getBindingContext();

          if (!context) {
            MessageToast.show("No message context available");
            event.getSource().setBusy(false);
            return;
          }

          var contextBinding = this.getView().getModel().bindContext("MainService.adopt(...)", context);
          var that = this;

          contextBinding.invoke().then(function (result) {
            MessageToast.show("Message adopted successfully");

            // Notify navigation controller that ContextNode data has changed
            sap.ui.getCore().getEventBus().publish("DataUpdate", "ContextNodeChanged");

            var oElementBinding = that.getView().getElementBinding();
            if (oElementBinding) {
              oElementBinding.refresh();
            }

          }).catch(function (error) {
            MessageToast.show("Error adopting message: " + (error.message || error.toString()));

          }).finally(function () {
            event.getSource().setBusy(false);
          });
        },

        _scheduleScrollToBottom: function () {
          setTimeout(() => {
            this._scrollToBottom();
          }, 100);
        },

        _scrollToBottom: function () {
          try {
            var oMessageList = this.byId("aiPageMessageList");
            if (oMessageList && oMessageList.getItems().length > 0) {
              var oLastItem = oMessageList.getItems()[oMessageList.getItems().length - 1];
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

        onGotoContextPress: function () {
          var oBindingContext = this.getView().getBindingContext();
          if (!oBindingContext) {
            MessageToast.show("No BotInstance context available");
            return;
          }

          var that = this;

          // 重新获取最新的BotInstance数据
          oBindingContext.requestObject().then(function (oBotInstance) {
            // 检查BotInstance是否有关联的ContextNode
            if (!oBotInstance.context || !oBotInstance.context.ID) {
              MessageToast.show("No ContextNode associated with this BotInstance. Please adopt a message first to create the context.");
              return;
            }

            var sContextNodeId = oBotInstance.context.ID;

            // 获取ContextNode的详细信息以确定类型
            var oModel = that.getView().getModel();
            var sContextPath = "/ContextNodes(" + sContextNodeId + ")";

            oModel.bindContext(sContextPath).requestObject().then(function (oContextNode) {
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

            }).catch(function (oError) {
              MessageToast.show("No ContextNode found. Please adopt a message first to create the context.");
            });

          }).catch(function (oError) {
            MessageToast.show("Error loading BotInstance: " + (oError.message || oError.toString()));
          });
        },

        onExit: function () {
          if (this._scrollTimeout) {
            clearTimeout(this._scrollTimeout);
          }
        }
      }
    );
  }
); 