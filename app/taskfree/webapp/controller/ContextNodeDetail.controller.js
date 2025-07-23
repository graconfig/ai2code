sap.ui.define(
  ["sap/ui/core/mvc/Controller", "sap/m/MessageToast", "sap/ui/model/json/JSONModel"],
  /**
   * @param {typeof sap.ui.core.mvc.Controller} Controller
   */
  function (Controller, MessageToast, JSONModel) {
    "use strict";

    return Controller.extend(
      "ai.orchestration.taskfree.controller.ContextNodeDetail",
      {
        onInit() {
          const oViewModel = new JSONModel({
            value: "",
            title: "Context Node",
            type: "",
            htmlValue: "",
            busy: false
          });
          this.getView().setModel(oViewModel, "viewModel");

          this.getOwnerComponent()
            .getRouter()
            .getRoute("RouteContextNodeDetail")
            .attachPatternMatched(this._onRouteMatched, this);
        },

        async _onRouteMatched(oEvent) {
          const oArguments = oEvent.getParameter("arguments");
          const oViewModel = this.getView().getModel("viewModel");
          const oContextNodeId = oArguments?.contextNodeId;

          this.getView().setBusy(true);

          if (!oContextNodeId) {
            MessageToast.show("No context node id provided");
            oViewModel.setProperty("/title", "Context Node");
            oViewModel.setProperty("/busy", false);
            return;
          }

          const oModel = this.getView().getModel();
          const sPath = "/ContextNodes(" + oContextNodeId + ")";

          try {
            const oData = await oModel.bindContext(sPath).requestObject();
            oViewModel?.setProperty("/value", oData.value);
            oViewModel?.setProperty("/title", oData.label);
          } catch (error) {
            oViewModel?.setProperty("/value", "加载失败");
            oViewModel?.setProperty("/title", "Context Node");
            oViewModel?.setProperty("/type", "");
            oViewModel?.setProperty("/busy", false);
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

          // 根据数据模型关联关系：通过ContextNode的botInstances导航属性获取关联的BotInstance
          const oModel = this.getView().getModel();
          const sContextNodePath = "/ContextNodes(" + sContextNodeId + ")";

          try {
            const oContextNode = await oModel.bindContext(sContextNodePath, null, {
              $expand: "botInstances"
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

            // 跳转到BotInstance页面
            oRouter.navTo("RouteBotInstanceDetail", {
              botInstanceId: sBotInstanceId
            });

          } catch (error) {
            MessageToast.show("Error loading BotInstance: " + (error.message || error.toString()));
          }
        },

        onExit() {
          const oViewModel = this.getView().getModel("viewModel");
          oViewModel?.setProperty("/title", "");
          oViewModel?.setProperty("/value", "");
          oViewModel?.setProperty("/busy", false);
        }
      }
    );
  }
); 