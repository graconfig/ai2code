sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/m/MessageToast",
    "sap/m/MessageBox",
    "sap/ui/model/json/JSONModel",
    "ai/orchestration/taskfree/control/marked"
], function (Controller, MessageToast, MessageBox, JSONModel, marked) {
    "use strict";

    return Controller.extend("ai.orchestration.taskfree.controller.MarkDownNodePage", {
        onInit: function () {
            // 创建 viewModel 用于页面数据绑定
            var oViewModel = new JSONModel({
                value: "",
                title: "Text Node",
                type: "",
                htmlValue: "" // 新增
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
            if (oViewModel) {
                oViewModel.setProperty("/htmlValue", "");
                oViewModel.setProperty("/value", "");
                oViewModel.setProperty("/type", "");
            }

        
            var oController = this;
            if (!contextNodeId) {
                MessageToast.show("No context node id provided");
                oViewModel.setProperty("/value", "");
                oViewModel.setProperty("/title", "Text Node");
                oViewModel.setProperty("/type", "");
                this._setMarkdownContent("");
                return;
            }

            // 直接查OData
            var oModel = this.getView().getModel();
            // 如果 contextNodeId 是字符串主键，需要加引号
            var sPath = "/ContextNodes(" + contextNodeId + ")";
            oModel.bindContext(sPath).requestObject().then(function (oData) {
                oViewModel.setProperty("/type", oData.type);
                oViewModel.setProperty("/value", oData.value);
                oViewModel.setProperty("/title", oData.title);
                if (oData.type === "markdown") {
                    oController._setMarkdownContent(oData.value);
                } else {
                    oController._setMarkdownContent(""); // 清空
                }

                // if (oData.type === "markdown") {
                //   var htmlValue = marked ? marked.parse(oData.value || "") : (oData.value || "");
                //   oViewModel.setProperty("/value", htmlValue);
                // }else{
                //   var value = oData.value;
                //   oViewModel.setProperty("/value", oData.value);
                // }

            }).catch(function () {
                oViewModel.setProperty("/value", "加载失败");
                oViewModel.setProperty("/title", "Text Node");
                oViewModel.setProperty("/type", "");
                oController._setMarkdownContent(""); // 清空
                MessageToast.show("加载失败");
            });
        },

        _setMarkdownContent: function (markdownText) {
            var oViewModel = this.getView().getModel("viewModel");
            var htmlContent = window.marked ? window.marked.parse(markdownText || "") : (markdownText || "");
            console.log("setMarkdownContent called. markdownText:", markdownText, "htmlContent:", htmlContent);
            oViewModel.setProperty("/htmlValue", htmlContent);
        },
        onExit: function () {
            var oViewModel = this.getView().getModel("viewModel");
            if (oViewModel) {
                oViewModel.setProperty("/htmlValue", "");
                oViewModel.setProperty("/value", "");
                oViewModel.setProperty("/type", "");
            }
        }
    });
});