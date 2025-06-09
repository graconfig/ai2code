sap.ui.define(
  ["sap/ui/core/mvc/Controller", "sap/m/MessageToast",
    "ai/orchestration/taskrun/util/Helper",
    "ai/orchestration/taskrun/service/ChatService",
    "ai/orchestration/taskrun/service/NewMessageHandler",
    "ai/orchestration/taskrun/util/UIHelper"
  ],
  /**
   * @param {typeof sap.ui.core.mvc.Controller} Controller
   */
  function (Controller, MessageToast,Helper, ChatService, NewMessageHandler, UIHelper) {
    "use strict";

    return Controller.extend(
      "ai.orchestration.taskrun.controller.TaskRunDetail",
      {
        onInit: function () {
          const oRouter = this.getOwnerComponent().getRouter();
          oRouter.attachRouteMatched(
            function (oEvent) {
              const oArgs = oEvent.getParameter("arguments");
              if (oArgs && oArgs.taskRunId) {
                this.getView().bindElement({
                  path: "/Tasks(" + oArgs.taskRunId + ")",
                });
              }
            }.bind(this)
          );
        },

        onBotInstancePress: function (oEvent) {
          // 这里可以获取被点击行的数据
          const oContext = oEvent.getSource().getBindingContext();
          if (oContext) {
            const oData = oContext.getObject(); // 你可以在这里处理点击事件，比如弹窗、跳转等
            //MessageToast.show("点击了BotInstance");
              
            //console.log("BotInstances.onChatCompletion triggered");
            // 在这里执行你的逻辑
            // 使用正确的 Fragment 路径加载对话框
            this.pDialog ??= this.loadFragment({
              name: "ai.orchestration.taskrun.view.fragment.AIConversation"
            });

            const that = this;

            this.pDialog.then((oDialog) => {
                that._dialog = oDialog;

                oDialog.open();
                // 添加列表数据加载完成的事件处理
                const messageList = oDialog.getContent()[0].getContent()[0].getItems()[0];
                messageList.getBinding("items").attachDataReceived(() => {
                    this.scrollToListEnd();
                });
            });
            this.onAIConversationClose = function (oEvent) {
                this.pDialog.then((oDialog) => oDialog.close());
            };

            this.onPostMessage = function (event) {
                if (!event.getParameter("value")) {
                    return;
                }
                const message = event.getParameter("value");
                const report = this.getView().getBindingContext();
                const messageList = this._dialog.getContent()[0].getContent()[0].getItems()[0];
                const binding = messageList.getBinding("items");

                const userModel = this.getView().getModel("user");

                const messageHandler = new NewMessageHandler({
                    report: report,
                    binding: binding,
                    message: message,
                    sender: "user"
                });

                messageHandler.createMessageAndCompletion();
            };

            this.onBtnAdoptPress = function (event) {
                event.getSource().setBusy(true);
                var context = event.getSource().getBindingContext();
                var contextBinding = this.getView().getModel().bindContext("ChatService.adopt(...)", context, { $$inheritExpandSelect: true });
                contextBinding.invoke().finally(() => {
                    event.getSource().setBusy(false);
                    // refresh Reports Context
                    this.getView().getBindingContext().refresh();
                });
            };

            this.onPressSyncChangesToChatList = function (event) {
                const binding = this.getView().getModel().bindContext("ChatService.appendToChatRecord(...)",
                    this.getView().getBindingContext()
                    // { $$inheritExpandSelect: true }
                );
                binding.invoke().then(() => {
                    // refresh Reports Context
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
                    // 如果元素还没有渲染完成，延迟执行
                    setTimeout(() => this.scrollToListEnd(), 100);
                }
            };


          }
        },
      }
    );
  }
);
