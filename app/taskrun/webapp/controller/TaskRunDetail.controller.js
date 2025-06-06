sap.ui.define([
    "sap/ui/core/mvc/Controller"
],
    /**
     * @param {typeof sap.ui.core.mvc.Controller} Controller
     */
    function (
        Controller) {
        "use strict";

        return Controller.extend("ai.orchestration.taskrun.controller.TaskRunDetail", {
            onInit: function () {
                const oRouter = this.getOwnerComponent().getRouter();
                oRouter.attachRouteMatched(function (oEvent) {
                    const oArgs = oEvent.getParameter("arguments");
                    if (oArgs && oArgs.taskRunId) {
                        this.getView().bindElement({
                            path: "/Tasks(" + oArgs.taskRunId + ")" 
                        })
                    }
                }.bind(this));
            }
        });
    });
