sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/m/MessageToast",
    "sap/m/MessageBox",
    "sap/ui/model/json/JSONModel",
    "ai/orchestration/taskfree/control/marked"
], function (Controller, MessageToast, MessageBox, JSONModel, marked) {
    "use strict";

    return Controller.extend("ai.orchestration.taskfree.controller.TextNodePage", {
        onInit: function () {
            var oViewModel = new JSONModel({
                value: "",
                title: "Context Node",
                type: ""
            });
            this.getView().setModel(oViewModel, "viewModel");

            // 监听路由
            var oRouter = this.getOwnerComponent().getRouter();
            oRouter.getRoute("RouteTextNodePage").attachPatternMatched(this._onRouteMatched, this);

        },

        _onRouteMatched: function (oEvent) {
            var oArgs = oEvent.getParameter("arguments");
            var contextNodeId = oArgs && oArgs.contextNodeId;
            var oViewModel = this.getView().getModel("viewModel");
            if (oViewModel) {
                oViewModel.setProperty("/value", "");
                oViewModel.setProperty("/title", "");
            }

            var oController = this;
            if (!contextNodeId) {
                MessageToast.show("No context node id provided");
                oViewModel.setProperty("/value", "");
                oViewModel.setProperty("/title", "Text Node");
                return;
            }

            // Set busy state
            oController.getView().setBusy(true);

            //获取OData
            var oModel = this.getView().getModel();
            var sPath = "/ContextNodes(" + contextNodeId + ")";

            oModel.bindContext(sPath).requestObject().then(function (oData) {
                oController.getView().setBusy(false);
                oViewModel.setProperty("/value", oData.value);
                oViewModel.setProperty("/title", oData.label);
            }).catch(function () {
                oController.getView().setBusy(false);
                oViewModel.setProperty("/value", "加载失败");
                oViewModel.setProperty("/title", "Text Node");
                MessageToast.show("加载失败");
            });
        },
        onExit: function () {
            var oViewModel = this.getView().getModel("viewModel");
            if (oViewModel) {
                oViewModel.setProperty("/value", "");
                oViewModel.setProperty("/title", "");
            }
        }
    });
});