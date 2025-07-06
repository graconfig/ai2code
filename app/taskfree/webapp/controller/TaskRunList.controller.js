sap.ui.define(
  [
    "sap/ui/core/mvc/Controller",
    "sap/m/Dialog",
    "sap/m/SelectDialog",
    "sap/ui/layout/form/SimpleForm",
    "sap/m/Button",
    "sap/m/ButtonType",
    "sap/m/MessageToast",
    "sap/m/MessageBox",
    "sap/m/Label",
    "sap/m/Input",
    "sap/m/TextArea",
    "sap/m/DialogType",
    "sap/ui/core/Element",
    "sap/ui/model/Filter",
    "sap/ui/model/FilterOperator"
  ],
  /**
   * @param {typeof sap.ui.core.mvc.Controller} Controller
   */
  function (
    Controller,
    Dialog,
    SelectDialog,
    SimpleForm,
    Button,
    ButtonType,
    MessageToast,
    MessageBox,
    Label,
    Input,
    TextArea,
    DialogType,
    Element,
    Filter,
    FilterOperator
  ) {
    "use strict";

    return Controller.extend(
      "ai.orchestration.taskfree.controller.TaskRunList",
      {
        onInit: function () {
          const oRouter = this.getOwnerComponent().getRouter();
          const oModel = this.getOwnerComponent().getModel();
          oRouter.attachRouteMatched(
            function (oEvent) {
              oModel.refresh();
            }.bind(this)
          );
          
          // Initialize view model for UI state
          this._initializeViewModel();
        },

        _initializeViewModel: function () {
          var oViewModel = new sap.ui.model.json.JSONModel({
            showCards: false,
            searchValue: "",
            statusFilter: "all"
          });
          this.getView().setModel(oViewModel, "view");
        },

        onSearch: function (oEvent) {
          var sQuery = oEvent.getParameter("query") || oEvent.getParameter("newValue");
          var oTable = this.byId("taskTable");
          var oBinding = oTable.getBinding("items");
          
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

        onStatusFilterChange: function (oEvent) {
          var sKey = oEvent.getParameter("selectedItem").getKey();
          var oTable = this.byId("taskTable");
          var oBinding = oTable.getBinding("items");
          var aFilters = [];
          
          switch (sKey) {
            case "main":
              aFilters.push(new sap.ui.model.Filter("isMain", sap.ui.model.FilterOperator.EQ, true));
              break;
            case "sub":
              aFilters.push(new sap.ui.model.Filter("isMain", sap.ui.model.FilterOperator.EQ, false));
              break;
            default:
              // Show all tasks
              break;
          }
          
          oBinding.filter(aFilters);
        },

        onExport: function () {
          MessageToast.show("Export functionality to be implemented");
        },

        onSettings: function () {
          MessageToast.show("Settings functionality to be implemented");
        },

        onCreate: function () {
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

        onDelete: function (oEvent) {
          var oTable = this.byId("taskTable");
          var aSelectedItems = oTable.getSelectedItems();
          var that = this;

          if (aSelectedItems.length === 0) {
            MessageToast.show("No items selected for deletion");
            return;   
          }

          // Show confirmation dialog
          MessageBox.confirm(
            "Are you sure you want to delete the selected " + aSelectedItems.length + " item(s)?",
            {
              title: "Confirm Deletion",
              onClose: function (oAction) {
                if (oAction === MessageBox.Action.OK) {
                  that._deleteSelectedItems(aSelectedItems);
                }
              }
            }
          );
        },

        _deleteSelectedItems: function (aSelectedItems) {
          var oTable = this.byId("taskTable");
          var that = this;
          var aPromises = [];

          oTable.setBusy(true);

          // Get contexts from selected items and delete them
          aSelectedItems.forEach(function (oItem) {
            try {
              var oContext = oItem.getBindingContext();
              if (oContext) {
                var sPath = oContext.getPath();

                var oDeletePromise = oContext.delete("$auto").then(function () {
                }).catch(function (oError) {
                  throw oError;
                });

                aPromises.push(oDeletePromise);
              }
            } catch (error) {
              // Silent error handling
            }
          });

          Promise.all(aPromises).then(function (aResults) {
            MessageToast.show(aSelectedItems.length + " item(s) deleted successfully");

            // Clear selection after successful deletion
            oTable.removeSelections(true);

          }).catch(function (oError) {
            MessageToast.show("Error deleting items: " + (oError.message || oError.toString()));

          }).finally(function () {
            oTable.setBusy(false);
          });
        },

        onItemPress: function (oEvent) {
          // Handle item press event
          const oItem = oEvent.getSource();
          const oContext = oItem.getBindingContext();
          if (oContext) {
            this._navToTaskRunDetail(oContext.getProperty("ID"));
          } else {
            MessageToast.show("No context available for the selected item.");
          }
        },

        _navToTaskRunDetail: function (sTaskRunId) {
          this.getOwnerComponent().getRouter().navTo("RouteTaskRunNav", {
            taskRunId: sTaskRunId,
          });
        },

        _createSelectTaskTypeDialog: function () {
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
                confirm: function (oEvent) {
                  const oSelectedItem = oEvent.getParameter("selectedItem");
                  if (oSelectedItem) {
                    const sTaskTypeName = oSelectedItem.getTitle();
                    Element.getElementById("taskTypeName").setValue(sTaskTypeName);
                    Element.getElementById("taskTypeId").setValue(
                      oSelectedItem.getHighlightText()
                    );
                    // Auto-fill task name with task type name
                    Element.getElementById("taskName").setValue(sTaskTypeName);
                    // Enable the Create button if task name is now filled
                    this.oSubmitDialog.getBeginButton().setEnabled(sTaskTypeName.length > 0);
                  }
                }.bind(this),
              });
        },

        _createTaskForm: function () {
          return new SimpleForm({
            content: [
              new Label({ text: "Task Type" }),
              new Input("taskTypeName", {
                showValueHelp: true,
                valueHelpOnly: true,
                valueHelpRequest: function () {
                  this.oSelectTypeDialog = this._createSelectTaskTypeDialog();
                  this.oSelectTypeDialog.setModel(
                    this.getOwnerComponent().getModel()
                  );
                  this.oSelectTypeDialog.open();
                }.bind(this),
              }),
              new Input("taskTypeId", {
                editable: false,
                visible: false,
              }),
              new Label({ text: "Task Type name" }),
              new Input("taskName", {
                placeholder: "Enter task name",
                required: true,
                liveChange: function (oEvent) {
                  var sText = oEvent.getParameter("value");
                  this.oSubmitDialog
                    .getBeginButton()
                    .setEnabled(sText.length > 0);
                }.bind(this),
              }),
              new Label({ text: "Description" }),
              new TextArea("taskDescription", {
                placeholder: "Enter task description",
                rows: 16,
              }),
            ],
          });
        },

        _createTask: function () {
          const sTaskName = Element.getElementById("taskName").getValue();
          const sTaskDescription =
            Element.getElementById("taskDescription").getValue();
          const sTaskTypeId = Element.getElementById("taskTypeId").getValue();
          const oNewTask = {
            name: sTaskName,
            description: sTaskDescription,
            type_ID: sTaskTypeId == "" ? null : sTaskTypeId,
          };
          const oModel = this.getOwnerComponent().getModel();
          const sPath = "/createTaskWithBots(...)";
          const oContextBinding = oModel.bindContext(sPath);
          oContextBinding.setParameter("name", oNewTask.name);
          oContextBinding.setParameter("description", oNewTask.description);
          oContextBinding.setParameter("typeId", oNewTask.type_ID);
          this.getView().setBusy(true);
          oContextBinding
            .invoke()
            .then(
              function (oContext) {
                MessageToast.show("Task created successfully");
                this._navToTaskRunDetail(
                  oContextBinding.getBoundContext().getProperty("ID")
                );
                oModel.refresh();
              }.bind(this)
            )
            .catch(
              function (oError) {
                MessageToast.show("Error creating task: " + oError.message);
              }.bind(this)
            )
            .finally(
              function () {
                this.getView().setBusy(false);
              }.bind(this)
            );
        },

        _clearFormFields: function () {
          // Clear all form fields
          var oTaskTypeNameField = Element.getElementById("taskTypeName");
          var oTaskTypeIdField = Element.getElementById("taskTypeId");
          var oTaskNameField = Element.getElementById("taskName");
          var oTaskDescriptionField = Element.getElementById("taskDescription");
          
          if (oTaskTypeNameField) oTaskTypeNameField.setValue("");
          if (oTaskTypeIdField) oTaskTypeIdField.setValue("");
          if (oTaskNameField) oTaskNameField.setValue("");
          if (oTaskDescriptionField) oTaskDescriptionField.setValue("");
          
          // Disable the Create button
          if (this.oSubmitDialog) {
            this.oSubmitDialog.getBeginButton().setEnabled(false);
          }
        },
      }
    );
  }
);
