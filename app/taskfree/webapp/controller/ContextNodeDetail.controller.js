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
        onInit: function () {
          var oViewModel = new JSONModel({
            value: "",
            title: "Context Node",
            type: "",
            htmlValue: "",
            busy: false
          });
          this.getView().setModel(oViewModel, "viewModel");

          const oRouter = this.getOwnerComponent().getRouter();
          oRouter.getRoute("RouteContextNodeDetail").attachPatternMatched(this._onRouteMatched, this);
        },

        _onRouteMatched: function (oEvent) {
          const oArguments = oEvent.getParameter("arguments");
          var oViewModel = this.getView().getModel("viewModel");
          var oContextNodeId = oArguments && oArguments.contextNodeId;
          var oController = this;

          oController.getView().setBusy(true);

          if (!oContextNodeId) {
            MessageToast.show("No context node id provided");
            oViewModel.setProperty("/title", "Context Node");
            oViewModel.setProperty("/busy", false);
            return;
          }

          var oModel = this.getView().getModel();
          var sPath = "/ContextNodes(" + oContextNodeId + ")";
          oModel.bindContext(sPath).requestObject().then(function (oData) {
            oController.getView().setBusy(false);
            oViewModel.setProperty("/value", oData.value);
            oViewModel.setProperty("/title", oData.label);
          }).catch(function () {
            oController.getView().setBusy(false);
            oViewModel.setProperty("/value", "加载失败");
            oViewModel.setProperty("/title", "Context Node");
            oViewModel.setProperty("/type", "");
            oViewModel.setProperty("/busy", false);
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
          var oModel = this.getView().getModel();
          var sContextNodePath = "/ContextNodes(" + sContextNodeId + ")";

          var that = this;
          oModel.bindContext(sContextNodePath, null, {
            $expand: "botInstances"
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

            // 跳转到BotInstance页面
            oRouter.navTo("RouteBotInstanceDetail", {
              botInstanceId: sBotInstanceId
            });

          }).catch(function (oError) {
            MessageToast.show("Error loading BotInstance: " + (oError.message || oError.toString()));
          });
        },

        onExit: function () {
          var oViewModel = this.getView().getModel("viewModel");
          if (oViewModel) {
            oViewModel.setProperty("/title", "");
            oViewModel.setProperty("/value", "");
            oViewModel.setProperty("/busy", false);
          }
          if (this._oVBox) {
            this._oVBox.removeAllItems();
          }
        }
      }
    );
  }
); 