sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/m/MessageToast",
    "sap/m/MessageBox",
    "sap/ui/model/json/JSONModel",
    "ai/orchestration/taskfree/control/marked",
    "sap/ui/core/HTML"
], function (Controller, MessageToast, MessageBox, JSONModel, marked, HTML) {
    "use strict";

    return Controller.extend("ai.orchestration.taskfree.controller.MarkDownNodePage", {
        onInit: function () {
            // 创建 viewModel 用于页面数据绑定
            var oViewModel = new JSONModel({
                value: "",
                title: "Text Node",
                type: "",
                htmlValue: "",
                busy: false
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
            var oPage = this.getView().byId("MarkDownNodePage");
            var oVBox;

            // 动态创建 VBox 容器（只创建一次）
            if (!this._oVBox) {
                oVBox = new sap.m.VBox("markdownVBox");
                oPage.removeAllContent();
                oPage.addContent(oVBox);
                this._oVBox = oVBox;
            } else {
                oVBox = this._oVBox;
                oVBox.removeAllItems();
            }

            // 立即 busy，立即移除 HTML 控件
            oController.getView().setBusy(true);

            if (!contextNodeId) {
                MessageToast.show("No context node id provided");
                oViewModel.setProperty("/title", "Text Node");
                oViewModel.setProperty("/busy", false);
                return;
            }

            // OData 请求
            var oModel = this.getView().getModel();
            var sPath = "/ContextNodes(" + contextNodeId + ")";
            oModel.bindContext(sPath).requestObject().then(function (oData) {
                oController.getView().setBusy(false);
                oData.type = (oData.type || "").toLowerCase()
                oViewModel.setProperty("/type", oData.type);
                oViewModel.setProperty("/value", oData.value);
                oViewModel.setProperty("/title", oData.title);

                // 加载完成后再创建 HTML 控件
                if (oData.type === "markdown") {
                    var htmlContent = window.marked ? window.marked.parse(oData.value || "") : (oData.value || "");
                    var oHtml = new HTML({
                        content: htmlContent
                    });
                    oVBox.addItem(oHtml);
                }
                oViewModel.setProperty("/busy", false);
            }).catch(function () {
                oController.getView().setBusy(false);
                oViewModel.setProperty("/value", "加载失败");
                oViewModel.setProperty("/title", "Text Node");
                oViewModel.setProperty("/type", "");
                oVBox.removeAllItems();
                oViewModel.setProperty("/busy", false);
                MessageToast.show("加载失败");
            });
        },

        onExit: function () {
            var oViewModel = this.getView().getModel("viewModel");
            if (oViewModel) {
                oViewModel.setProperty("/htmlValue", "");
                oViewModel.setProperty("/value", "");
                oViewModel.setProperty("/type", "");
                oViewModel.setProperty("/busy", false);
            }
            if (this._oVBox) {
                this._oVBox.removeAllItems();
            }
        }
    });
});