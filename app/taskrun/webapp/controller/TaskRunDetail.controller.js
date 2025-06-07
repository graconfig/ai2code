sap.ui.define(
  ["sap/ui/core/mvc/Controller", "sap/m/MessageToast"],
  /**
   * @param {typeof sap.ui.core.mvc.Controller} Controller
   */
  function (Controller, MessageToast) {
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
            MessageToast.show("点击了BotInstance");
          }
        },
      }
    );
  }
);
