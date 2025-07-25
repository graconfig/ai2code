sap.ui.define([
  "sap/ui/core/mvc/Controller",
  "sap/m/MessageToast",
  "sap/ui/model/json/JSONModel",
], function (Controller, MessageToast, JSONModel) {
  "use strict";

  return Controller.extend("ai.orchestration.taskfree.controller.TextAreaNodePage", {
    onInit() {
      // 创建 viewModel 用于页面数据绑定
      const oViewModel = new JSONModel({
        value: "",
        title: "Context Node",
        type: "",
        showReturnButton: false // 添加返回按钮显示控制
      });

      this.getView().setModel(oViewModel, "viewModel");

      // 监听路由
      this.getOwnerComponent()
        .getRouter()
        .getRoute("RouteTextAreaNodePage")
        .attachPatternMatched(this._onRouteMatched, this);

    },

    async _onRouteMatched(oEvent) {
      const oArgs = oEvent.getParameter("arguments");
      const contextNodeId = oArgs?.contextNodeId;
      const oViewModel = this.getView().getModel("viewModel");

      oViewModel?.setProperty("/value", "");
      oViewModel?.setProperty("/type", "");
      oViewModel?.setProperty("/showReturnButton", true);

      if (!contextNodeId) {
        MessageToast.show("No context node id provided");
        oViewModel?.setProperty("/value", "");
        oViewModel?.setProperty("/title", "Context Node");
        oViewModel?.setProperty("/type", "");
        this._setMarkdownContent("");
        return;
      }

      this.getView().setBusy(true);

      // 直接查OData
      const oModel = this.getView().getModel();
      const sPath = "/ContextNodes(" + contextNodeId + ")";

      try {
        const oData = await oModel.bindContext(sPath).requestObject();

        let editorType;
        switch (oData.type?.toLowerCase()) {
          case "markdown":
            editorType = "markdown";
            break;
          case "json":
            editorType = "json";
            break;
          case "string":
            editorType = "markdown";
            break;
          case "code":
            editorType = "abap";
            break;
          default:
            editorType = "json";
        }

        oViewModel?.setProperty("/type", editorType);
        oViewModel?.setProperty("/value", oData.value);
        oViewModel?.setProperty("/title", oData.label);
      } catch (error) {
        oViewModel?.setProperty("/title", "Context Node");
        oViewModel?.setProperty("/type", "");
        oViewModel?.setProperty("/value", "加载失败");
        MessageToast.show("加载失败");
      } finally {
        this.getView().setBusy(false);
      }
    },

    async onReturnToConversation() {
      // 从路由获取contextNodeId
      const oRouter = this.getOwnerComponent().getRouter();
      const oCurrentRoute = oRouter.getHashChanger().getHash();
      const oRouteInfo = oRouter.getRouteInfoByHash(oCurrentRoute);
      const sContextNodeId = oRouteInfo?.arguments?.contextNodeId;

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

        if (!oContextNode.botInstances) {
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
        const sFunctionType = oBotInstance?.type?.functionType_code;

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

    onExit() {
      const oViewModel = this.getView().getModel("viewModel");
      oViewModel?.setProperty("/value", "");
      oViewModel?.setProperty("/title", "");
      oViewModel?.setProperty("/type", "");

    }
  });
});