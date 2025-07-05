sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/m/MessageToast",
    "sap/m/MessageBox",
    "sap/ui/model/json/JSONModel",
    "ai/orchestration/taskfree/control/marked"
], function (Controller, MessageToast, MessageBox, JSONModel, marked) {
    "use strict";

    return Controller.extend("ai.orchestration.taskfree.controller.TextAreaNodePage", {
        onInit: function () {
            // 创建 viewModel 用于页面数据绑定
            var oViewModel = new JSONModel({
                value: "",
                title: "Text Node",
                type: "",
                htmlValue: "", // 新增
                showReturnButton: true // 添加返回按钮显示控制
            });
            this.getView().setModel(oViewModel, "viewModel");

            // 监听路由
            var oRouter = this.getOwnerComponent().getRouter();
            oRouter.getRoute("RouteTextAreaNodePage").attachPatternMatched(this._onRouteMatched, this);

        },

        _onRouteMatched: function (oEvent) {
            var oArgs = oEvent.getParameter("arguments");
            var contextNodeId = oArgs && oArgs.contextNodeId;
            var oViewModel = this.getView().getModel("viewModel");
            if (oViewModel) {
                oViewModel.setProperty("/htmlValue", "");
                oViewModel.setProperty("/value", "");
                oViewModel.setProperty("/type", "");
                oViewModel.setProperty("/showReturnButton", true);
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

            // Set busy state
            oController.getView().setBusy(true);

            // 直接查OData
            var oModel = this.getView().getModel();
            // 如果 contextNodeId 是字符串主键，需要加引号
            var sPath = "/ContextNodes(" + contextNodeId + ")";
            oModel.bindContext(sPath).requestObject().then(function (oData) {
                oController.getView().setBusy(false);
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
                oController.getView().setBusy(false);
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
            //console.log("setMarkdownContent called. markdownText:", markdownText, "htmlContent:", htmlContent);
            oViewModel.setProperty("/htmlValue", htmlContent);
        },

        onReturnToConversation: function() {
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
          var oModel = this.getView().getModel();
          var sContextNodePath = "/ContextNodes(" + sContextNodeId + ")";
          
          var that = this;
          oModel.bindContext(sContextNodePath, null, {
            $expand: "botInstances($expand=task)"
          }).requestObject().then(function(oContextNode) {
            
            if (!oContextNode) {
              MessageToast.show("ContextNode not found");
              return;
            }
            
            if (!oContextNode.botInstances || oContextNode.botInstances.length === 0) {
              MessageToast.show("No associated BotInstance found for this ContextNode");
              return;
            }

            // 在BotInstances中找到contextID等于sContextNodeId的特定记录
            var oBotInstance = oContextNode.botInstances.find(function(botInstance) {
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

            // 切换左侧导航到Tasks视图

            // 跳转到AIConversation页面
            oRouter.navTo("RouteAIConversation", {
              taskRunId: sTaskId,
              botInstanceId: sBotInstanceId
            });
            
          }).catch(function(oError) {
            MessageToast.show("Error loading BotInstance: " + (oError.message || oError.toString()));
          });
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