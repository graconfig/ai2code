sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/m/MessageToast",
    "sap/ui/model/json/JSONModel"
], function (Controller, MessageToast, JSONModel) {
    "use strict";

    return Controller.extend("ai.orchestration.taskfree.controller.MarkDownNodePage", {
        onInit() {
            // 创建 viewModel 用于页面数据绑定
            const oViewModel = new JSONModel({
                value: "",
                title: "Context Node"
            });
            this.getView().setModel(oViewModel, "viewModel");

            // 监听路由
            this.getOwnerComponent()
                .getRouter()
                .getRoute("RouteMarkDownNodePage")
                .attachPatternMatched(this._onRouteMatched, this);
        },

        async _onRouteMatched(oEvent) {
            const oArgs = oEvent.getParameter("arguments");
            const contextNodeId = oArgs?.contextNodeId;
            const oViewModel = this.getView().getModel("viewModel");

            this.getView().setBusy(true);

            if (!contextNodeId) {
                oViewModel?.setProperty("/title", "Context Node");
                oViewModel?.setProperty("/busy", false);
                MessageToast.show("No context node id provided");
                return;
            }

            // OData 请求
            const oModel = this.getView().getModel();
            const sPath = "/ContextNodes(" + contextNodeId + ")";
            try {
                const oData = await oModel.bindContext(sPath).requestObject();
                oViewModel.setProperty("/title", oData.label);

                const htmlContent = marked.parse(oData.value);
                this.getView().byId("idRichTextEditor").setValue(htmlContent);
            } catch (error) {
                oViewModel.setProperty("/title", "Context Node");
                oViewModel.setProperty("/value", "加载失败");
                MessageToast.show("加载失败");
            } finally {
                this.getView().setBusy(false);
            }
        },

        async onReturnToBot() {
            // 从路由获取contextNodeId
            const oRouter = this.getOwnerComponent().getRouter();
            const oCurrentRoute = oRouter.getHashChanger().getHash();
            const oRouteInfo = oRouter.getRouteInfoByHash(oCurrentRoute);
            const sContextNodeId = oRouteInfo && oRouteInfo.arguments && oRouteInfo.arguments.contextNodeId;

            if (!sContextNodeId) {
                MessageToast.show("ContextNode ID not available");
                return;
            }

            const oModel = this.getView().getModel();
            const sContextNodePath = "/ContextNodes(" + sContextNodeId + ")";

            try {
                const oContextNode = await oModel.bindContext(sContextNodePath, null, {
                    $expand: "botInstances($expand=task,type)"
                }).requestObject();

                if (!oContextNode) {
                    MessageToast.show("ContextNode not found");
                    return;
                }

                if (!oContextNode.botInstances || oContextNode.botInstances.length === 0) {
                    MessageToast.show("No associated BotInstance found for this ContextNode");
                    return;
                }

                // 在BotInstances中找到contextID等于sContextNodeId的特定记录
                const oBotInstance = oContextNode.botInstances.find(function (botInstance) {
                    return botInstance.contextID === sContextNodeId;
                });

                if (!oBotInstance) {
                    MessageToast.show("No matching BotInstance found with contextID: " + sContextNodeId);
                    return;
                }

                const sBotInstanceId = oBotInstance.ID;
                const sTaskId = oBotInstance.task ? oBotInstance.task.ID : oBotInstance.task_ID;

                if (!sTaskId) {
                    MessageToast.show("Task ID not available");
                    return;
                }

                // 获取botInstance的functionType来决定跳转路由
                const sFunctionType = oBotInstance.type && oBotInstance.type.functionType_code;

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

            } catch (error) {
                MessageToast.show("Error loading BotInstance: " + (error.message || error.toString()));
            }
        },
        // 新增：查找 BotInstance 的工具函数
        async _findBotInstanceIdByContextNodeId(sContextNodeId) {
            const oModel = this.getView().getModel();
            const sContextNodePath = "/ContextNodes(" + sContextNodeId + ")";
            const oContextNode = await oModel.bindContext(sContextNodePath, null, {
                $expand: "botInstances($expand=task,type)"
            }).requestObject();

            if (!oContextNode || !oContextNode.botInstances || oContextNode.botInstances.length === 0) {
                return null;
            }
            const oBotInstance = oContextNode.botInstances.find(function (botInstance) {
                return botInstance.contextID === sContextNodeId;
            });
            return oBotInstance ? oBotInstance.ID : null;
        },

        // 新增：创建 BotMessage 的工具函数
        async _createBotMessage(sBotInstanceId, sValue) {
            const oModel = this.getView().getModel();
            const oBotMessagesBinding = oModel.bindList("/BotMessages", null, null, null, { $$updateGroupId: "$auto" });
            await oBotMessagesBinding.create({
                role: "user",
                message: sValue,
                botInstance_ID: sBotInstanceId
            });
        },

        onSave: async function () {
            const oView = this.getView();
            const oModel = oView.getModel();
            const oRouter = this.getOwnerComponent().getRouter();
            const oCurrentRoute = oRouter.getHashChanger().getHash();
            const oRouteInfo = oRouter.getRouteInfoByHash(oCurrentRoute);
            const sContextNodeId = oRouteInfo && oRouteInfo.arguments && oRouteInfo.arguments.contextNodeId;
            const sValue = oView.byId("idRichTextEditor").getValue();

            if (!sContextNodeId) {
                MessageToast.show("ContextNodeId 不存在，无法保存");
                return;
            }

            oView.setBusy(true);
            const sPath = "/ContextNodes(" + sContextNodeId + ")";

            try {
                // 1. 保存 ContextNode
                const oBinding = oModel.bindContext(sPath, null, { $$updateGroupId: "$auto" });
                await oBinding.requestObject();
                const oContext = oBinding.getBoundContext();
                oContext.setProperty("value", sValue);
                //await oModel.submitBatch("saveGroup");

                // 2. 查找 BotInstanceId
                const sBotInstanceId = await this._findBotInstanceIdByContextNodeId(sContextNodeId);
                if (!sBotInstanceId) {
                    MessageToast.show("No associated BotInstance found for this ContextNode");
                    return;
                }

                // 3. 新增 BotMessage
                await this._createBotMessage(sBotInstanceId, sValue);

                MessageToast.show("保存成功，并已同步到 BotInstance");
            } catch (e) {
                MessageToast.show("保存失败");
            } finally {
                oView.setBusy(false);
            }
        },
        onExit() {
            const oViewModel = this.getView().getModel("viewModel");
            oViewModel?.setProperty("/value", "");
            oViewModel?.setProperty("/busy", false);
        }
    });
});