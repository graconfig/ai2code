sap.ui.define(
  ["sap/ui/core/mvc/Controller", "sap/m/MessageToast", "sap/m/MessageBox",
    "ai/orchestration/taskfree/util/Helper",
    "ai/orchestration/taskfree/service/ChatService",
    "ai/orchestration/taskfree/service/NewMessageHandler",
    "ai/orchestration/taskfree/util/UIHelper"
  ],
  /**
   * @param {typeof sap.ui.core.mvc.Controller} Controller
   */
  function (Controller, MessageToast, MessageBox, Helper, ChatService, NewMessageHandler, UIHelper) {
    "use strict";

    return Controller.extend(
      "ai.orchestration.taskfree.controller.AIConversationPage",
      {
        onInit: function () {
          const oRouter = this.getOwnerComponent().getRouter();
          oRouter.attachRouteMatched(this._onRouteMatched.bind(this));
          
          this._currentBotInstanceId = null;
        },

        _onRouteMatched: function(oEvent) {
          const sRouteName = oEvent.getParameter("name");
          const oArguments = oEvent.getParameter("arguments");
          
          if (sRouteName === "RouteAIConversation" && oArguments.botInstanceId) {
            const sBotInstanceId = oArguments.botInstanceId.replace(/'/g, '');
            
            if (sBotInstanceId && sBotInstanceId.trim() !== '') {
              this._currentBotInstanceId = sBotInstanceId;
              
              // Clear previous binding context to force refresh
              this.getView().setBindingContext(null);
              this.getView().setBusy(true);
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
          
          var sPath = "/BotInstances(" + sBotInstanceId + ")";
          
          var oBinding = oModel.bindContext(sPath, null, {
            $expand: "messages,type"
          });
          
          oBinding.attachDataReceived(function() {
            that.getView().setBusy(false);
            var oBoundContext = oBinding.getBoundContext();
            if (oBoundContext) {
              var oData = oBoundContext.getObject();
              if (oData) {
                
                // Update page title
                var sTitle = (oData.type && oData.type.name) ? 
                  "AI Conversation - " + oData.type.name : 
                  "AI Conversation";
                that.byId("aiConversationPage").setTitle(sTitle);
                
                // Set binding context
                that.getView().setBindingContext(oBoundContext);
                that.oContext = oBoundContext;
                
                // Auto-scroll to bottom
                setTimeout(function() {
                  that._scheduleScrollToBottom();
                }, 500);
                
              } else {
                MessageToast.show("No data found for Bot Instance");
              }
            } else {
              MessageToast.show("Failed to load Bot Instance data");
            }
          });
          
          oBinding.requestObject().catch(function(oError) {
            that.getView().setBusy(false);
            MessageToast.show("Failed to load Bot Instance data: " + (oError.message || oError.toString()));
          });
        },

        onRefreshPress: function() {
          var oBindingContext = this.getView().getBindingContext();
          if (oBindingContext) {
            oBindingContext.refresh();
            var sMessage = this.getView().getModel("i18n").getResourceBundle().getText("messagesRefreshed");
            MessageToast.show(sMessage);
          } else if (this._currentBotInstanceId) {
            this._loadBotInstanceDetail(this._currentBotInstanceId);
          } else {
            MessageToast.show("No context available for refresh");
          }
        },

        onClearPress: function() {
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
              onClose: function(oAction) {
                if (oAction === MessageBox.Action.OK) {
                  that._clearAllMessages();
                }
              }
            }
          );
        },

        _clearAllMessages: function() {
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

          aContexts.forEach(function(oContext) {
            try {
              if (oContext && typeof oContext.getPath === 'function') {
                var sPath = oContext.getPath();
                
                var oDeletePromise = oContext.delete("$auto").then(function() {
                }).catch(function(oError) {
                  throw oError;
                });
                
                aPromises.push(oDeletePromise);
              }
            } catch (error) {
              // Silent error handling
            }
          });

          Promise.all(aPromises).then(function(aResults) {
            var oResourceBundle = that.getView().getModel("i18n").getResourceBundle();
            MessageToast.show(oResourceBundle.getText("messagesClearedSuccess") || "All messages cleared successfully");
            
            // Refresh the bot instance context instead of the list binding
            var oBotInstanceContext = that.getView().getBindingContext();
            if (oBotInstanceContext) {
              oBotInstanceContext.refresh();
            }
            
          }).catch(function(oError) {
            MessageToast.show("Error clearing messages: " + (oError.message || oError.toString()));
            
          }).finally(function() {
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
          
          contextBinding.invoke().then(function(result) {
            MessageToast.show("Message adopted successfully");
            
            // Notify navigation controller that ContextNode data has changed
            sap.ui.getCore().getEventBus().publish("DataUpdate", "ContextNodeChanged");
            
            var oBotInstanceContext = that.getView().getBindingContext();
            if (oBotInstanceContext) {
              oBotInstanceContext.refresh();
            }
            
          }).catch(function(error) {
            MessageToast.show("Error adopting message: " + (error.message || error.toString()));
            
          }).finally(function() {
            event.getSource().setBusy(false);
          });
        },

        _scheduleScrollToBottom: function() {
          setTimeout(() => {
            this._scrollToBottom();
          }, 100);
        },

        _scrollToBottom: function() {
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

        onExit: function() {
          if (this._scrollTimeout) {
            clearTimeout(this._scrollTimeout);
          }
        }
      }
    );
  }
); 