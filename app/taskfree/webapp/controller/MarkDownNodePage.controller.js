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
                oViewModel.setProperty("/title", oData.label);

                var htmlContent = marked.parse(oData.value);
                oController.getView().byId("idRichTextEditor").setValue(htmlContent);

            }).catch(function () {
                oController.getView().setBusy(false);
                oViewModel.setProperty("/title", "Context Node");
                oViewModel.setProperty("/value", "加载失败");
                MessageToast.show("加载失败");
            });
        },

        onReturnToBot: function () {
            // 从路由获取contextNodeId
            var oRouter = this.getOwnerComponent().getRouter();
            var oCurrentRoute = oRouter.getHashChanger().getHash();
            var oRouteInfo = oRouter.getRouteInfoByHash(oCurrentRoute);
            var sContextNodeId = oRouteInfo && oRouteInfo.arguments && oRouteInfo.arguments.contextNodeId;

            if (!sContextNodeId) {
                MessageToast.show("ContextNode ID not available");
                return;
            }

            // 根据数据模型关联关系：通过ContextNode的botInstances导航属性获取关联的BotInstance
            // 同时展开type关联以获取functionType信息
            var oModel = this.getView().getModel();
            var sContextNodePath = "/ContextNodes(" + sContextNodeId + ")";

            var that = this;
            oModel.bindContext(sContextNodePath, null, {
                $expand: "botInstances($expand=task,type)"
            }).requestObject().then(function (oContextNode) {

                if (!oContextNode) {
                    MessageToast.show("ContextNode not found");
                    return;
                }

                if (!oContextNode.botInstances || oContextNode.botInstances.length === 0) {
                    MessageToast.show("No associated BotInstance found for this ContextNode");
                    return;
                }

                // 在BotInstances中找到contextID等于sContextNodeId的特定记录
                var oBotInstance = oContextNode.botInstances.find(function (botInstance) {
                    return botInstance.contextID === sContextNodeId;
                });

                if (!oBotInstance) {
                    MessageToast.show("No matching BotInstance found with contextID: " + sContextNodeId);
                    return;
                }

                var sBotInstanceId = oBotInstance.ID;
                var sTaskId = oBotInstance.task ? oBotInstance.task.ID : oBotInstance.task_ID;

                if (!sTaskId) {
                    MessageToast.show("Task ID not available");
                    return;
                }

                // 获取botInstance的functionType来决定跳转路由
                var sFunctionType = oBotInstance.type && oBotInstance.type.functionType_code;

                if (!sFunctionType) {
                    MessageToast.show("BotInstance function type not available");
                    return;
                }

                // 根据functionType决定跳转路由
                if (sFunctionType === "A") {
                    // A类型（AI CHAT）跳转到AIConversation页面
                    oRouter.navTo("RouteAIConversation", {
                        taskRunId: sTaskId,
                        botInstanceId: sBotInstanceId
                    });
                } else {
                    // 其他类型（FUNCTION_CALL、CODE）跳转到BotInstanceDetail页面
                    oRouter.navTo("RouteBotInstanceDetail", {
                        botInstanceId: sBotInstanceId
                    });
                }

            }).catch(function (oError) {
                MessageToast.show("Error loading BotInstance: " + (oError.message || oError.toString()));
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