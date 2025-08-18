sap.ui.define(
  [
    "sap/ui/core/mvc/Controller",
    "sap/m/Dialog",
    "sap/m/DialogType",
    "sap/m/Button",
    "sap/m/ButtonType",
    "sap/m/MessageToast",
    "sap/m/MessageBox",
    "sap/ui/layout/form/SimpleForm",
    "sap/m/Label",
    "sap/m/Input",
    "sap/m/TextArea",
    "sap/m/SelectDialog",
    "sap/ui/model/Filter",
    "sap/ui/model/FilterOperator"
  ],
  function (
    BaseController,
    Dialog,
    DialogType,
    Button,
    ButtonType,
    MessageToast,
    MessageBox,
    SimpleForm,
    Label,
    Input,
    TextArea,
    SelectDialog,
    Filter,
    FilterOperator
  ) {
    "use strict";

    return BaseController.extend("ai.orchestration.taskfree.controller.TaskRunList", {
      onInit() {
        this.oSubmitDialog = null;
        this.oSelectTypeDialog = null;
        this.oTaskTypeNameInput = null;
        this.oTaskTypeIdInput = null;
        this.oTaskNameInput = null;
        this.oTaskDescriptionInput = null;
      },

      onExit() {
        if (this.oSubmitDialog) {
          this.oSubmitDialog.destroy();
          this.oSubmitDialog = null;
          this.oTaskTypeNameInput = null;
          this.oTaskTypeIdInput = null;
          this.oTaskNameInput = null;
          this.oTaskDescriptionInput = null;
        }
        if (this.oSelectTypeDialog) {
          this.oSelectTypeDialog.destroy();
          this.oSelectTypeDialog = null;
        }
      },

      onCreate() {
        if (!this.oSubmitDialog) {
          this.oSubmitDialog = new Dialog({
            type: DialogType.Message,
            title: "Create",
            contentWidth: "600px",
            contentHeight: "600px",
            content: [this._createTaskForm()],
            beginButton: new Button({
              type: ButtonType.Emphasized,
              text: "Create",
              enabled: false,
              press: function () {
                this._createTask();
                this.oSubmitDialog.close();
              }.bind(this),
            }),
            endButton: new Button({
              text: "Cancel",
              press: function () {
                this.oSubmitDialog.close();
              }.bind(this),
            }),
          });
        }

        // Clear all input fields before opening
        this._clearFormFields();
        this.oSubmitDialog.open();
      },

      onDelete(oEvent) {
        const oTable = this.byId("taskTable");
        const aSelectedItems = oTable.getSelectedItems();

        if (aSelectedItems.length === 0) {
          MessageToast.show("No items selected for deletion");
          return;
        }

        // Show confirmation dialog
        MessageBox.confirm(
          "Are you sure you want to delete the selected " + aSelectedItems.length + " item(s)?",
          {
            title: "Confirm Deletion",
            onClose: (oAction) => {
              if (oAction === MessageBox.Action.OK) {
                this._deleteSelectedItems(aSelectedItems);
              }
            }
          }
        );
      },

      _deleteSelectedItems(aSelectedItems) {
        const oTable = this.byId("taskTable");
        let aPromises = [];

        oTable.setBusy(true);

        // Get contexts from selected items and delete them
        aSelectedItems.forEach((oItem) => {
          const oContext = oItem.getBindingContext();
          if (oContext) {
            aPromises.push(oContext.delete("$auto"));
          }
        });

        Promise.all(aPromises)
          .then(() => {
            MessageToast.show(aSelectedItems.length + " item(s) deleted successfully");
            // Clear selection after successful deletion
            oTable.removeSelections(true);
          }).catch((oError) => {
            MessageToast.show("Error deleting items: " + (oError.message || oError.toString()));
          }).finally(() => {
            oTable.setBusy(false);
          });
      },

      onItemPress(oEvent) {
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
      },
      _createSelectTaskTypeDialog() {
        return this.oSelectTypeDialog
          ? this.oSelectTypeDialog
          : new SelectDialog({
            noDataText: "No task types found",
            title: "Select Task Type",
            items: {
              path: "/TaskType",
              filters: [new Filter("isMain", FilterOperator.EQ, true)],
              template: new sap.m.StandardListItem({
                title: "{name}",
                description: "{description}",
                highlightText: "{ID}",
              }),
            },
            confirm: (oEvent) => {
              const oSelectedItem = oEvent.getParameter("selectedItem");
              if (oSelectedItem) {
                const sTaskTypeName = oSelectedItem.getTitle();
                this.oTaskTypeNameInput.setValue(sTaskTypeName);
                this.oTaskTypeIdInput.setValue(
                  oSelectedItem.getHighlightText()
                );
                // Auto-fill task name with task type name
                this.oTaskNameInput.setValue(sTaskTypeName);
                // Enable the Create button if task name is now filled
                this.oSubmitDialog.getBeginButton().setEnabled(sTaskTypeName.length > 0);
              }
            },
          });
      },

      _createTaskForm() {
        this.oTaskTypeNameInput = new Input({
          showValueHelp: true,
          valueHelpOnly: true,
          valueHelpRequest: () => {
            this.oSelectTypeDialog = this._createSelectTaskTypeDialog();
            this.oSelectTypeDialog.setModel(
              this.getOwnerComponent().getModel()
            );
            this.oSelectTypeDialog.open();
          },
        });

        this.oTaskTypeIdInput = new Input({
          editable: false,
          visible: false,
        });

        this.oTaskNameInput = new Input({
          placeholder: "Enter task name",
          required: true,
          liveChange: (oEvent) => {
            var sText = oEvent.getParameter("value");
            this.oSubmitDialog
              .getBeginButton()
              .setEnabled(sText.length > 0);
          },
        });

        this.oTaskDescriptionInput = new TextArea({
          placeholder: "Enter task description",
          rows: 16,
        });

        return new SimpleForm({
          content: [
            new Label({ text: "Task Type" }),
            this.oTaskTypeNameInput,
            this.oTaskTypeIdInput,
            new Label({ text: "Task Name" }),
            this.oTaskNameInput,
            new Label({ text: "Description" }),
            this.oTaskDescriptionInput,
          ],
        });
      },

      _createTask() {
        const sTaskName = this.oTaskNameInput.getValue();
        const sTaskDescription = this.oTaskDescriptionInput.getValue();
        const sTaskTypeId = this.oTaskTypeIdInput.getValue();
        const oNewTask = {
          name: sTaskName,
          description: sTaskDescription,
          type_ID: sTaskTypeId === "" ? null : sTaskTypeId,
        };
        const oModel = this.getOwnerComponent().getModel();
        const sPath = "/createTaskWithBots(...)";
        const oContextBinding = oModel.bindContext(sPath);

        oContextBinding?.setParameter("name", oNewTask.name);
        oContextBinding?.setParameter("description", oNewTask.description);
        oContextBinding?.setParameter("typeId", oNewTask.type_ID);
        this.getView().setBusy(true);
        oContextBinding
          .invoke()
          .then(
            (oContext) => {
              MessageToast.show("Task created successfully");
              this._navToTaskRunDetail(
                oContextBinding.getBoundContext().getProperty("ID")
              );
              oModel.refresh();
            }
          )
          .catch(
            (oError) => {
              MessageToast.show("Error creating task: " + oError.message);
            }
          )
          .finally(
            () => {
              this.getView().setBusy(false);
            }
          );
      },

      _clearFormFields() {
        // Clear all form fields
        if (this.oTaskTypeNameInput) this.oTaskTypeNameInput.setValue("");
        if (this.oTaskTypeIdInput) this.oTaskTypeIdInput.setValue("");
        if (this.oTaskNameInput) this.oTaskNameInput.setValue("");
        if (this.oTaskDescriptionInput) this.oTaskDescriptionInput.setValue("");

        // Disable the Create button
        if (this.oSubmitDialog) {
          this.oSubmitDialog.getBeginButton().setEnabled(false);
        }
      },

      _navToTaskRunDetail(sTaskRunId) {
        // Publish event to notify other controllers of the task change
        sap.ui.getCore().getEventBus().publish("TaskRun", "TaskSelectionChanged", {
          taskId: sTaskRunId
        });

        this.getOwnerComponent().getRouter().navTo("RouteTaskRunNav", {
          taskRunId: sTaskRunId,
        });
      },

      onSearch(oEvent) {
        const sQuery = oEvent.getParameter("query") || oEvent.getParameter("newValue");
        const oTable = this.byId("taskTable");
        const oBinding = oTable.getBinding("items");

        if (sQuery && sQuery.length > 0) {
          var oFilter = new sap.ui.model.Filter([
            new sap.ui.model.Filter("name", sap.ui.model.FilterOperator.Contains, sQuery),
            new sap.ui.model.Filter("description", sap.ui.model.FilterOperator.Contains, sQuery)
          ], false);
          oBinding.filter([oFilter]);
        } else {
          oBinding.filter([]);
        }
      },
    });
  }
);

