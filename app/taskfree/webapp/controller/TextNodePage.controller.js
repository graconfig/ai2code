sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/m/MessageToast",
    "sap/ui/model/json/JSONModel",
], function (Controller, MessageToast, JSONModel) {
    "use strict";

    return Controller.extend("ai.orchestration.taskfree.controller.TextNodePage", {
        onInit() {
            const oViewModel = new JSONModel({
                value: "",
                title: "Context Node",
                type: ""
            });
            this.getView().setModel(oViewModel, "viewModel");

            // 监听路由
            const oRouter = this.getOwnerComponent().getRouter();
            oRouter.getRoute("RouteTextNodePage").attachPatternMatched(this._onRouteMatched, this);

        },

        async _onRouteMatched(oEvent) {
            const oArgs = oEvent.getParameter("arguments");
            const contextNodeId = oArgs && oArgs.contextNodeId;
            const oViewModel = this.getView().getModel("viewModel");


            oViewModel?.setProperty("/value", "");
            oViewModel?.setProperty("/title", "");

            if (!contextNodeId) {
                MessageToast.show("No context node id provided");
                oViewModel.setProperty("/value", "");
                oViewModel.setProperty("/title", "Text Node");
                return;
            }

            // Set busy state
            this.getView().setBusy(true);

            //获取OData
            const oModel = this.getView().getModel();
            const sPath = "/ContextNodes(" + contextNodeId + ")";

            try {
                const oData = await oModel.bindContext(sPath).requestObject();
                oViewModel?.setProperty("/value", oData.value);
                oViewModel?.setProperty("/title", oData.label);
            } catch (error) {
                oViewModel?.setProperty("/value", "加载失败");
                oViewModel?.setProperty("/title", "Text Node");
                MessageToast.show("加载失败");
            } finally {
                this.getView().setBusy(false);
            }
        },
        onExit() {
            const oViewModel = this.getView().getModel("viewModel");
            oViewModel?.setProperty("/value", "");
            oViewModel?.setProperty("/title", "");
        }
    });
});