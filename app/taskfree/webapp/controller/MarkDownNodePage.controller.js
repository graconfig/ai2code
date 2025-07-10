sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/m/MessageToast",
    "sap/ui/model/json/JSONModel"
], function (Controller, MessageToast, JSONModel) {
    "use strict";

    return Controller.extend("ai.orchestration.taskfree.controller.MarkDownNodePage", {
        onInit: function () {
            // 创建 viewModel 用于页面数据绑定
            var oViewModel = new JSONModel({
                value: "",
                title: "Context Node"
            });
            this.getView().setModel(oViewModel, "viewModel");

            // 监听路由
            var oRouter = this.getOwnerComponent().getRouter();
            oRouter.getRoute("RouteMarkDownNodePage").attachPatternMatched(this._onRouteMatched, this);
        },

        _onRouteMatched: function (oEvent) {
            var oArgs = oEvent.getParameter("arguments");
            var contextNodeId = oArgs && oArgs.contextNodeId;
            var oViewModel = this.getView().getModel("viewModel");
            var oController = this;

            // 立即 busy，立即移除 HTML 控件
            oController.getView().setBusy(true);

            if (!contextNodeId) {
                oViewModel.setProperty("/title", "Context Node");
                oViewModel.setProperty("/busy", false);
                MessageToast.show("No context node id provided");
                return;
            }

            // OData 请求
            var oModel = this.getView().getModel();
            var sPath = "/ContextNodes(" + contextNodeId + ")";
            oModel.bindContext(sPath).requestObject().then(function (oData) {
                oController.getView().setBusy(false);
                //oViewModel.setProperty("/value", oData.value);
                oViewModel.setProperty("/title", oData.title);

                var htmlContent = marked.parse(oData.value);
                oController.getView().byId("idRichTextEditor").setValue(htmlContent);

            }).catch(function () {
                oController.getView().setBusy(false);
                oViewModel.setProperty("/title", "Context Node");
                oViewModel.setProperty("/value", "加载失败");
                MessageToast.show("加载失败");
            });
        },

        onExit: function () {
            var oViewModel = this.getView().getModel("viewModel");
            if (oViewModel) {
                oViewModel.setProperty("/value", "");
                oViewModel.setProperty("/busy", false);
            }
        }
    });
});