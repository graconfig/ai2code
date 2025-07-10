sap.ui.define(
    [
        "sap/ui/core/mvc/Controller"
    ],
    function(BaseController) {
      "use strict";
  
      return BaseController.extend("ai.orchestration.taskfree.controller.TaskRunList", {
        onInit() {
        },

        onItemPress: function(oEvent) {
          const oItem = oEvent.getSource();
          const oBindingContext = oItem.getBindingContext();
          if (oBindingContext) {
              const sTaskRunId = oBindingContext.getProperty("ID");
              if (sTaskRunId) {
                  // Publish event to notify other controllers of the task change
                  sap.ui.getCore().getEventBus().publish("TaskRun", "TaskSelectionChanged", {
                      taskId: sTaskRunId
                  });

                  // Navigate to the detail page
                  this.getOwnerComponent().getRouter().navTo("RouteTaskRunNav", {
                      taskRunId: sTaskRunId,
                  });
              }
          }
        }
      });
    }
  );
  
