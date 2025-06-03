sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/m/Dialog",
    "sap/m/SelectDialog",
    "sap/ui/layout/form/SimpleForm",
    "sap/m/Button",
    "sap/m/ButtonType",
    "sap/m/MessageToast",
    "sap/m/Label",
    "sap/m/Input",
    "sap/m/TextArea",
    "sap/m/DialogType",
    "sap/ui/core/Element"
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
        Label, 
        Input, 
        TextArea, 
        DialogType,
        Element) {
        "use strict";

        return Controller.extend("ai.orchestration.taskrun.controller.TaskRunList", {
            onInit: function () {
                const oRouter = this.getOwnerComponent().getRouter();
                const oModel = this.getOwnerComponent().getModel();
                oRouter.attachRouteMatched(function (oEvent) {
                    oModel.refresh();
                }.bind(this));
            },

            onCreate: function () {
                if (!this.oSubmitDialog) {
                    this.oSubmitDialog = new Dialog({
                        type: DialogType.Message,
                        title: "Create",
                        content: [
                            this._createTaskForm()
                        ],
                        beginButton: new Button({
                            type: ButtonType.Emphasized,
                            text: "Create",
                            enabled: false,
                            press: function () {
                                this._createTask();
                                this.oSubmitDialog.close();
                            }.bind(this)
                        }),
                        endButton: new Button({
                            text: "Cancel",
                            press: function () {
                                this.oSubmitDialog.close();
                            }.bind(this)
                        })
                    });
                }
    
                this.oSubmitDialog.open();
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
                this.getOwnerComponent().getRouter().navTo("RouteTaskRunDetail", {
                    taskRunId: sTaskRunId
                });
            },

            _createSelectTaskTypeDialog: function () {
                return this.oSelectTypeDialog ? this.oSelectTypeDialog : new SelectDialog({
                    noDataText: "No task types found",
                    title: "Select Task Type",
                    items: {
                        path: "/TaskType",
                        template: new sap.m.StandardListItem({
                            title: "{name}",
                            description: "{description}"
                        })
                    }
                });
            },

            _createTaskForm: function () {
                return new SimpleForm({
                    content: [
                        new Label({ text: "Task name" }),
                        new Input("taskName", {
                            placeholder: "Enter task name",
                            required: true,
                            liveChange: function (oEvent) {
                                var sText = oEvent.getParameter("value");
                                this.oSubmitDialog.getBeginButton().setEnabled(sText.length > 0);
                            }.bind(this)
                        }),
                        new Label({ text: "Description" }),
                        new TextArea("taskDescription", {
                            placeholder: "Enter task description",
                            rows: 3
                        }),
                        new Label({ text: "Type id" }),
                        new Input("taskTypeId", {
                            showValueHelp: true,
                            valueHelpOnly: true,
                            valueHelpRequest: function () {
                                this.oSelectTypeDialog = this._createSelectTaskTypeDialog();
                                this.oSelectTypeDialog.open();
                                MessageToast.show("Value help requested");
                            }.bind(this)
                        }),
                    ]
                })
            },

            _createTask: function () {
                const sTaskName = Element.getElementById("taskName").getValue();
                const sTaskDescription = Element.getElementById("taskDescription").getValue();
                const sTaskTypeId = Element.getElementById("taskTypeId").getValue();
                const oNewTask = {
                    name: sTaskName,
                    description: sTaskDescription,
                    type_ID: sTaskTypeId == "" ? null : sTaskTypeId,
                };
                const oModel = this.getOwnerComponent().getModel();
                const oListBinding = oModel.bindList("/Tasks", oNewTask);
                const oContext = oListBinding.create(oNewTask);
                oContext.created().then(function () {
                    // Task created successfully
                    oModel.refresh();
                    this._navToTaskRunDetail(oContext.getProperty("ID"));
                    MessageToast.show("Task created successfully");
                }.bind(this)).catch(function (oError) {
                    // Handle error
                    MessageToast.show("Error creating task: " + oError.message);
                });
            }
        });
    });
