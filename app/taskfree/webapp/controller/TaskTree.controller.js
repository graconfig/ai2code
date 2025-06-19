sap.ui.define([
    "sap/ui/core/mvc/Controller",
    "sap/ui/model/json/JSONModel",
    "sap/ui/model/Filter",
    "sap/ui/model/FilterOperator",
    "sap/ui/model/Sorter",
    "sap/m/MessageToast",
    "sap/m/MessageBox",
    "sap/tnt/NavigationListItem",
    "sap/f/library"
], function (Controller, JSONModel, Filter, FilterOperator, Sorter, MessageToast, MessageBox, NavigationListItem, fioriLibrary) {
    "use strict";

    return Controller.extend("tasktree.ext.controller.TaskTree", {

        onInit: function () {
            this._currentMode = "tasks";
            this._editMode = false;
            this._originalData = null;

            this._oModel = this.getOwnerComponent().getModel();
            this._initializeViewModel();
            this._initializeNavigationList();
            this._handleRouteMatched();
            this._initializeFlexibleColumnLayout();

            // Load initial data will be handled by route matching
            // If no route parameters, load all tasks as fallback
            setTimeout(() => {
                const oViewModel = this.getView().getModel("viewModel");
                const sSelectedTaskId = oViewModel.getProperty("/selectedTaskId");

                if (!sSelectedTaskId) {
                    // No specific task selected from routing, load all tasks
                    this._loadTasksForNavigation().then(() => {
                        this._showDefaultTaskDetails();
                    }).catch(error => {
                        console.error("Failed to load initial tasks:", error);
                    });
                }
            }, 100);

            setTimeout(() => {
                this._checkViewComponents();
            }, 1000);
        },

        _checkViewComponents: function () {
            const aFormsToCheck = [
                "taskDetailForm",
                "botInstanceDetailForm",
                "contextNodeDetailForm"
            ];

            aFormsToCheck.forEach(sFormId => {
                const oForm = this.byId(sFormId);
                if (!oForm) {
                    console.warn(`Form ${sFormId} not found`);
                }
            });
        },

        _initializeViewModel: function () {
            const oViewModel = new JSONModel({
                currentMode: "tasks",
                viewMode: "tasks",  // current view mode
                editMode: false,
                showEditButton: true,
                showAiChatButton: false,
                treeTitle: "Tasks and Bot Instances",
                detailTitle: "Details",
                selectedItemType: null,
                navigationItems: [],
                busy: false,
                layout: fioriLibrary.LayoutType.TwoColumnsMidExpanded,  // Default layout
                selectedFixedKey: "viewMode_tasks",  // default selected fixed item
                currentTaskName: "Task Management",  // default title
                selectedTaskId: null  // currently selected task ID
            });
            this.getView().setModel(oViewModel, "viewModel");
        },

        _initializeNavigationList: function () {
            const oNavigationList = this.byId("navigationList");
            if (oNavigationList) {
                oNavigationList.setModel(this._oModel);
            }

            this._oNavigationModel = new JSONModel({
                navigation: [],
                selectedKey: null
            });
            this.getView().setModel(this._oNavigationModel, "navigation");
        },

        async _loadTasksForNavigation(sSelectedTaskId) {
            if (!this._oModel) {
                this._oNavigationModel.setProperty("/navigation", []);
                return;
            }

            try {
                this._setBusy(true);

                // If a specific task is selected, only load that task
                if (sSelectedTaskId) {
                    const sPath = `/Tasks(ID=${sSelectedTaskId})`;
                    const oContext = this._oModel.bindContext(sPath, null, {
                        $expand: "botInstances($expand=type,status,messages,tasks)"
                    });

                    const oTaskData = await oContext.requestObject();
                    if (oTaskData) {
                        this._buildNavigationStructure([{ getObject: () => oTaskData }]);
                    } else {
                        this._oNavigationModel.setProperty("/navigation", []);
                    }
                } else {
                    // Load all main tasks if no specific task is selected
                    const oTasksListBinding = this._oModel.bindList("/Tasks", null, null, null, {
                        $expand: "botInstances($expand=type,status,messages,tasks)"
                    });

                    const aContexts = await oTasksListBinding.requestContexts();

                    if (aContexts.length === 0) {
                        this._oNavigationModel.setProperty("/navigation", []);
                    } else {
                        const aMainTasks = aContexts
                            .filter(oContext => {
                                const oTask = oContext.getObject();
                                return oTask.isMain === true || !oTask.botInstance;
                            })
                            .sort((a, b) => {
                                const oTaskA = a.getObject();
                                const oTaskB = b.getObject();
                                const seqA = oTaskA.sequence || 0;
                                const seqB = oTaskB.sequence || 0;
                                if (seqA !== seqB) {
                                    return seqA - seqB;
                                }
                                return (oTaskA.name || '').localeCompare(oTaskB.name || '');
                            });

                        this._buildNavigationStructure(aMainTasks);
                    }
                }
            } catch (error) {
                console.error("Error loading tasks for navigation:", error);
                this._oNavigationModel.setProperty("/navigation", []);
                MessageToast.show("Failed to load tasks");
            } finally {
                this._setBusy(false);
            }
        },

        async _loadContextNodesForNavigation(sSelectedTaskId) {
            if (!this._oModel) {
                this._oNavigationModel.setProperty("/navigation", []);
                return;
            }

            try {
                this._setBusy(true);

                let oContextNodesListBinding;

                // If a specific task is selected, only load context nodes for that task
                if (sSelectedTaskId) {
                    oContextNodesListBinding = this._oModel.bindList("/ContextNodes", null, null, [
                        new Filter("task/ID", FilterOperator.EQ, sSelectedTaskId)
                    ], {
                        $expand: "task"
                    });
                } else {
                    // Load all context nodes if no specific task is selected
                    oContextNodesListBinding = this._oModel.bindList("/ContextNodes", null, null, null, {
                        $expand: "task"
                    });
                }

                const aContexts = await oContextNodesListBinding.requestContexts();

                if (aContexts && aContexts.length > 0) {
                    // Filter out any contexts with invalid data
                    const aValidContexts = aContexts.filter(oContext => {
                        const oNode = oContext.getObject();
                        return oNode && oNode.ID && oNode.ID.trim() !== "";
                    });

                    if (aValidContexts.length > 0) {
                        this._buildContextNodesNavigation(aValidContexts);
                    } else {
                        this._oNavigationModel.setProperty("/navigation", []);
                    }
                } else {
                    this._oNavigationModel.setProperty("/navigation", []);
                }
            } catch (error) {
                console.error("Error loading context nodes for navigation:", error);
                this._oNavigationModel.setProperty("/navigation", []);
                MessageToast.show("Failed to load context nodes");
            } finally {
                this._setBusy(false);
            }
        },

        _buildContextNodesNavigation: function (aContexts) {
            const aContextNodes = aContexts.map(ctx => ctx.getObject());
            const oGroupedNodes = {};

            aContextNodes.forEach(oNode => {
                const oTask = oNode.task;
                let sTaskName = "未分配";
                let sGroupKey = "task_unassigned";

                if (oTask && oTask.name) {
                    sTaskName = oTask.name;
                    sGroupKey = `task_${oTask.ID}`;
                }

                if (!oGroupedNodes[sGroupKey]) {
                    oGroupedNodes[sGroupKey] = {
                        taskId: oTask ? oTask.ID : null,
                        taskName: sTaskName,
                        nodes: []
                    };
                }

                oGroupedNodes[sGroupKey].nodes.push(oNode);
            });

            const aNavigationData = [];

            Object.keys(oGroupedNodes).forEach(sGroupKey => {
                const oGroup = oGroupedNodes[sGroupKey];

                const oTaskGroupItem = {
                    key: sGroupKey,
                    title: oGroup.taskName,
                    icon: "sap-icon://task",
                    expanded: true,  // Default expanded for ContextNodes groups
                    hasExpander: true,
                    type: "Task",
                    data: {
                        ID: oGroup.taskId,
                        name: oGroup.taskName,
                        contextNodes: oGroup.nodes
                    },
                    items: oGroup.nodes
                        .filter(oNode => oNode.ID && oNode.ID.trim() !== "") // Filter out nodes without valid IDs
                        .map(oNode => ({
                            key: `contextnode_${oNode.ID}`,
                            title: oNode.label || oNode.path || 'Unnamed Context Node',
                            icon: "sap-icon://detail-view",
                            expanded: false,
                            hasExpander: false,
                            type: "ContextNode",
                            data: oNode,
                            items: []
                        }))
                };

                aNavigationData.push(oTaskGroupItem);
            });

            this._oNavigationModel.setProperty("/navigation", aNavigationData);
        },

        onSwitchToTasks: function () {
            this._currentMode = "tasks";
            const oViewModel = this.getView().getModel("viewModel");
            const sSelectedTaskId = oViewModel.getProperty("/selectedTaskId");

            oViewModel.setProperty("/currentMode", "tasks");
            oViewModel.setProperty("/treeTitle", "Tasks and Bot Instances");

            // If no task is selected, reset to default title
            if (!sSelectedTaskId) {
                oViewModel.setProperty("/currentTaskName", "Task Management");
            }

            this._loadTasksForNavigation(sSelectedTaskId).then(() => {
                this._showDefaultTaskDetails();
            });
        },

        onSwitchToContextNodes: function () {
            this._currentMode = "contextNodes";
            const oViewModel = this.getView().getModel("viewModel");
            const sSelectedTaskId = oViewModel.getProperty("/selectedTaskId");

            oViewModel.setProperty("/currentMode", "contextNodes");
            oViewModel.setProperty("/treeTitle", "Context Nodes");

            // If no task is selected, reset to default title
            if (!sSelectedTaskId) {
                oViewModel.setProperty("/currentTaskName", "Context Nodes");
            }

            this._loadContextNodesForNavigation(sSelectedTaskId).then(() => {
                this._showDefaultContextNodeDetails();
            });
        },

        onFocusNavigation: function () {
            // Focus on the navigation panel
            const oNavigationList = this.byId("navigationList");
            if (oNavigationList) {
                oNavigationList.focus();
            }
        },

        onNavigationItemSelect: function (oEvent) {
            const oListItem = oEvent.getParameter("item");
            const sKey = oListItem.getKey();
            const oViewModel = this.getView().getModel("viewModel");

            console.log("Navigation item selected:", sKey);

            // Handle fixed navigation items
            if (this._isFixedNavigationItem(sKey)) {
                this._handleFixedNavigationItem(sKey);
                return;
            }

            // Update selected key for main navigation
            this._oNavigationModel.setProperty("/selectedKey", sKey);

            // Parse the key to determine item type and ID
            const aParts = sKey.split("_");
            if (aParts.length < 2) {
                console.warn("Invalid key format:", sKey);
                return;
            }

            const sType = aParts[0];
            const sId = aParts.slice(1).join("_");

            console.log("Parsed type:", sType, "ID:", sId);

            // Set layout to show detail column
            oViewModel.setProperty("/layout", fioriLibrary.LayoutType.TwoColumnsMidExpanded);

            // Load detail based on type
            this._loadDetailForSelectedItem(sType, sId);
        },

        _handleContextNodesSelection: function (sKey) {
            if (sKey.startsWith("contextnode_")) {
                // First try to get data from navigation model to avoid unnecessary network requests
                const aNavigationData = this._oNavigationModel.getProperty("/navigation");
                const oContextNodeItem = this._findItemByKey(aNavigationData, sKey);

                if (oContextNodeItem && oContextNodeItem.data && oContextNodeItem.data.ID) {
                    // Use the data directly from navigation model
                    this._updateDetailPanel(oContextNodeItem.data, "contextNode");
                } else {
                    // Fallback to network request if data not available
                    const sNodeId = sKey.split("_")[1];
                    if (sNodeId && sNodeId.trim() !== "") {
                        this._loadItemDetails("ContextNodes", sNodeId, "contextNode");
                    } else {
                        console.error("Invalid ContextNode ID:", sNodeId);
                        MessageToast.show("Invalid ContextNode ID");
                    }
                }
            } else if (sKey.startsWith("task_")) {
                const sTaskId = sKey.split("_")[1];

                if (sTaskId && sTaskId !== "unassigned") {
                    this._loadItemDetails("Tasks", sTaskId, "task");
                } else {
                    const aNavigationData = this._oNavigationModel.getProperty("/navigation");
                    const oTaskGroup = aNavigationData.find(item => item.key === sKey);
                    if (oTaskGroup) {
                        this._showUnassignedContextInfo(oTaskGroup);
                    } else {
                        this._clearDetailPanel();
                    }
                }
            } else {
                this._clearDetailPanel();
            }
        },

        async _loadBotInstancesForTask(sTaskId) {
            if (!this._oModel) {
                throw new Error("Model not available");
            }

            try {
                const oBinding = this._oModel.bindList("/BotInstances", null, null, [
                    new Filter("task/ID", FilterOperator.EQ, sTaskId)
                ], {
                    $expand: "status,type,tasks($expand=botInstances($expand=status,type))"
                });

                const aContexts = await oBinding.requestContexts();

                const aFilteredBotInstances = aContexts.map(oContext => {
                    const oBotInstance = oContext.getObject();

                    if (oBotInstance.tasks && Array.isArray(oBotInstance.tasks)) {
                        oBotInstance.tasks = oBotInstance.tasks.filter(oSubTask => {
                            return oSubTask.botInstance && oSubTask.botInstance.ID === oBotInstance.ID;
                        });
                    }

                    return oBotInstance;
                });

                return aFilteredBotInstances;
            } catch (error) {
                console.error("Error loading bot instances for task:", error);
                throw error;
            }
        },

        _loadAndShowItemDetails: function (sKey) {
            if (sKey.startsWith("task_")) {
                const sTaskId = sKey.split("_")[1];
                if (sTaskId && sTaskId !== "unassigned") {
                    this._loadItemDetails("Tasks", sTaskId, "task");
                }
            } else if (sKey.startsWith("bot_")) {
                const sBotInstanceId = sKey.split("_")[1];
                this._loadItemDetails("BotInstances", sBotInstanceId, "botInstance");
            } else if (sKey.startsWith("subtask_")) {
                const sSubTaskId = sKey.split("_")[1];
                this._loadItemDetails("Tasks", sSubTaskId, "task");
            } else if (sKey.startsWith("botmessage_")) {
                const sBotMessageId = sKey.split("_")[1];
                this._loadItemDetails("BotMessages", sBotMessageId, "botMessage");
            } else if (sKey.startsWith("contextnode_")) {
                const sContextNodeId = sKey.split("_")[1];
                this._loadItemDetails("ContextNodes", sContextNodeId, "contextNode");
            }
        },

        _findItemByKey: function (aItems, sKey) {
            for (let i = 0; i < aItems.length; i++) {
                const oItem = aItems[i];

                if (oItem.key === sKey) {
                    return oItem;
                }

                if (oItem.items && oItem.items.length > 0) {
                    const oFound = this._findItemByKey(oItem.items, sKey);
                    if (oFound) {
                        return oFound;
                    }
                }
            }

            return null;
        },

        async _loadItemDetails(sEntitySet, sId, sItemType) {
            try {
                this._setBusy(true);

                // Validate input parameters
                if (!sId || sId.trim() === "") {
                    throw new Error(`Invalid or empty ID provided for ${sItemType}: '${sId}'`);
                }

                let sPath;

                // All entities in this model use cuid (ID field)
                sPath = `/${sEntitySet}(ID=${sId})`;

                if (sItemType === "botInstance") {
                    sPath += "?$expand=status,type,tasks($expand=botInstances($expand=status,type)),messages";
                }

                const oContext = this._oModel.bindContext(sPath);
                const oData = await oContext.requestObject();

                this._updateDetailPanel(oData, sItemType);
            } catch (error) {
                console.error(`Failed to load ${sItemType} details:`, error);

                // Try alternative UUID formats
                try {
                    // Try without quotes around the UUID
                    let sAlternativePath = `/${sEntitySet}(${sId})`;

                    if (sItemType === "botInstance") {
                        sAlternativePath += "?$expand=status,type,tasks($expand=botInstances($expand=status,type)),messages";
                    }

                    const oAlternativeContext = this._oModel.bindContext(sAlternativePath);
                    const oData = await oAlternativeContext.requestObject();

                    this._updateDetailPanel(oData, sItemType);
                } catch (alternativeError) {
                    // Try format with single quotes around UUID
                    try {
                        let sQuotedPath = `/${sEntitySet}('${sId}')`;

                        if (sItemType === "botInstance") {
                            sQuotedPath += "?$expand=status,type,tasks($expand=botInstances($expand=status,type)),messages";
                        }

                        const oQuotedContext = this._oModel.bindContext(sQuotedPath);
                        const oData = await oQuotedContext.requestObject();

                        this._updateDetailPanel(oData, sItemType);
                    } catch (quotedError) {
                        if (sItemType === "contextNode") {
                            // Try to find ContextNode through search
                            this._findContextNodeById(sId);
                        } else {
                            console.error(`Failed to load ${sItemType} details with ID: ${sId}`);
                            MessageToast.show(`Failed to load ${sItemType} details`);
                        }
                    }
                }
            } finally {
                this._setBusy(false);
            }
        },

        async _findContextNodeById(sId) {
            try {
                if (!sId || sId.trim() === "") {
                    MessageToast.show("Invalid ContextNode ID");
                    return;
                }

                const oContextNodesListBinding = this._oModel.bindList("/ContextNodes", null, null, null, {
                    $expand: "task"
                });
                const aContexts = await oContextNodesListBinding.requestContexts();

                const oFoundContext = aContexts.find(oContext => {
                    const oNode = oContext.getObject();
                    return oNode.ID === sId || String(oNode.ID) === String(sId);
                });

                if (oFoundContext) {
                    const oData = oFoundContext.getObject();
                    this._updateDetailPanel(oData, "contextNode");
                } else {
                    MessageToast.show(`ContextNode with ID '${sId}' not found`);
                }
            } catch (error) {
                console.error("Failed to search for ContextNode:", error);
                MessageToast.show("Failed to search for ContextNode");
            }
        },

        _updateDetailPanel: function (oData, sItemType) {
            if (this._editMode) {
                this.onCancelEdit();
            }

            const oDetailModel = new JSONModel(oData);
            this.getView().setModel(oDetailModel, "detailModel");

            const oViewModel = this.getView().getModel("viewModel");

            // Ensure correct casing for view binding
            let sNormalizedItemType = sItemType;
            if (sItemType && sItemType.toLowerCase() === "contextnode") {
                sNormalizedItemType = "contextNode";
            } else if (sItemType && sItemType.toLowerCase() === "botinstance") {
                sNormalizedItemType = "botInstance";
            } else if (sItemType && sItemType.toLowerCase() === "botmessage") {
                sNormalizedItemType = "botMessage";
            }

            // Update navigation page title and selected task ID when Task is selected
            if (sItemType && (sItemType.toLowerCase() === "task" || sItemType.toLowerCase() === "subtask")) {
                const sTaskName = oData.name || 'Unnamed Task';
                oViewModel.setProperty("/currentTaskName", sTaskName);
                oViewModel.setProperty("/selectedTaskId", oData.ID);
            }

            //botInstance如果是Chat开头，显示AI Chat按钮
            if (sItemType && (sItemType.toLowerCase() === "botinstance")) {
                if (oData.type?.name.startsWith("Chat")) {
                    oViewModel.setProperty("/showAiChatButton", true);
                } else {
                    oViewModel.setProperty("/showAiChatButton", false);
                }
            }

            oViewModel.setProperty("/selectedItemType", sNormalizedItemType);
            oViewModel.setProperty("/detailTitle", this._getDetailTitle(sItemType, oData));
            oViewModel.setProperty("/showEditButton", true);
        },

        _getDetailTitle: function (sItemType, oData) {
            const sType = sItemType ? sItemType.toLowerCase() : "";
            switch (sType) {
                case "task":
                case "subtask":
                    return oData.name || 'Unnamed Task';
                case "botinstance":
                    return oData.type?.name || `Bot Instance ${oData.sequence || ''}` || oData.ID || 'Unknown';
                case "contextnode":
                    return oData.label || oData.path || 'Unknown Context Node';
                case "botmessage":
                    return `Message (${oData.role || 'unknown'})`;
                default:
                    return "Details";
            }
        },

        _clearDetailPanel: function () {
            this.getView().setModel(null, "detailModel");
            const oViewModel = this.getView().getModel("viewModel");
            oViewModel.setProperty("/selectedItemType", null);
            oViewModel.setProperty("/detailTitle", "Details");
            oViewModel.setProperty("/showEditButton", false);
            oViewModel.setProperty("/showAiChatButton", false);
            oViewModel.setProperty("/selectedTaskId", null);
            // Reset navigation title to default
            oViewModel.setProperty("/currentTaskName", "Task Management");
        },

        _showDefaultTaskDetails: function () {
            // Show the first main task details by default
            const aNavigationData = this._oNavigationModel.getProperty("/navigation");
            if (aNavigationData && aNavigationData.length > 0) {
                const oFirstItem = aNavigationData[0];
                if (oFirstItem && oFirstItem.data && oFirstItem.data.ID) {
                    // Select the first item in navigation
                    this._oNavigationModel.setProperty("/selectedKey", oFirstItem.key);

                    // Set layout to show detail column
                    const oViewModel = this.getView().getModel("viewModel");
                    oViewModel.setProperty("/layout", fioriLibrary.LayoutType.TwoColumnsMidExpanded);

                    // Show its details
                    let sItemType = oFirstItem.type;
                    if (sItemType === "Task") {
                        sItemType = "task";
                    }
                    this._updateDetailPanel(oFirstItem.data, sItemType);
                }
            }
        },

        _showDefaultContextNodeDetails: function () {
            // Show the first task details by default (not context node details)
            const aNavigationData = this._oNavigationModel.getProperty("/navigation");
            if (aNavigationData && aNavigationData.length > 0) {
                // Find the first task group
                const oFirstTaskGroup = aNavigationData[0];
                if (oFirstTaskGroup && oFirstTaskGroup.data && oFirstTaskGroup.data.ID) {
                    // Select the first task group
                    this._oNavigationModel.setProperty("/selectedKey", oFirstTaskGroup.key);

                    // Show task details (not context node details)
                    this._updateDetailPanel(oFirstTaskGroup.data, "task");
                }
            }
        },

        _showUnassignedContextInfo: function (oTaskGroup) {
            const oData = {
                name: "未分配的上下文节点",
                description: `包含 ${oTaskGroup.data.contextNodes.length} 个未分配给任何任务的上下文节点`,
                contextNodes: oTaskGroup.data.contextNodes
            };

            this._updateDetailPanel(oData, "task");
        },

        _setBusy: function (bBusy) {
            const oViewModel = this.getView().getModel("viewModel");
            if (oViewModel) {
                oViewModel.setProperty("/busy", bBusy);
            }
        },

        _handleRouteMatched: function () {
            const oRouter = this.getOwnerComponent().getRouter();
            const oTaskTreeRoute = oRouter.getRoute("TaskTreePage");
            if (oTaskTreeRoute) {
                oTaskTreeRoute.attachPatternMatched(this._onTaskTreePatternMatched, this);
            }
        },

        _onTaskTreePatternMatched: function (oEvent) {
            const oArguments = oEvent.getParameter("arguments");
            const sTaskKey = oArguments.key;

            if (sTaskKey) {
                const sTaskId = this._extractIdFromKey(sTaskKey);

                // Set the selected task ID in view model
                const oViewModel = this.getView().getModel("viewModel");
                oViewModel.setProperty("/selectedTaskId", sTaskId);

                // Load navigation data for the specific task
                this._loadTasksForNavigation(sTaskId).then(() => {
                    // Show the task details
                    this._selectAndExpandTask(sTaskId);
                }).catch(error => {
                    console.error("Failed to load task for navigation:", error);
                });

                if (this._currentMode !== "tasks") {
                    this.onSwitchToTasks();
                }
            }
        },

        _extractIdFromKey: function (sKey) {
            if (sKey.includes("ID=")) {
                const sIdPart = sKey.split("ID=")[1];
                return sIdPart.split(",")[0];
            }
            return sKey;
        },

        _selectAndExpandTask: function (sTaskId) {
            // Set the navigation selection to the task
            const sTaskKey = `task_${sTaskId}`;
            this._oNavigationModel.setProperty("/selectedKey", sTaskKey);

            // Load and show task details
            this._loadItemDetails("Tasks", sTaskId, "task");
        },

        // Formatter methods
        formatStatusState: function (sStatus, oStatusObject) {
            let sActualStatus;

            if (oStatusObject && oStatusObject.code) {
                sActualStatus = oStatusObject.code;
            } else if (oStatusObject && typeof oStatusObject === 'string') {
                sActualStatus = oStatusObject;
            } else if (sStatus) {
                sActualStatus = sStatus;
            } else {
                return "Information";
            }

            switch (sActualStatus) {
                case "CREATED": return "Information";
                case "RUNNING": return "Success";
                case "SUCCESS": return "Success";
                case "FAILED": return "Error";
                case "SKIPPED": return "Warning";
                case "CANCELLED": return "None";
                case "A": case "R": case "Active": case "Running": return "Success";
                case "E": case "F": case "Error": case "Failed": return "Error";
                case "W": case "Warning": return "Warning";
                case "C": case "Completed": return "Information";
                case "I": case "S": case "Inactive": case "Stopped": return "None";
                default: return "Information";
            }
        },

        formatMultiLineText: function (sValue) {
            if (!sValue) return "";

            // Ensure sValue is a string
            const sStringValue = String(sValue);
            return sStringValue.replace(/\n/g, "<br/>");
        },

        getRouter: function () {
            return this.getOwnerComponent().getRouter();
        },

        onSubTaskSelect: function (oEvent) {
            const oSelectedItem = oEvent.getSource();
            const oContext = oSelectedItem.getBindingContext("detailModel");
            const oSubTask = oContext.getObject();

            this._loadItemDetails("Tasks", oSubTask.ID, "task");
        },

        onFocusNavigation: function () {
            const oNavigationList = this.byId("navigationList");
            if (oNavigationList) {
                oNavigationList.focus();
            }
        },

        // Edit mode functionality with OData V4 context-based operations
        onEditMode: function () {
            this._editMode = true;
            const oDetailModel = this.getView().getModel("detailModel");
            if (oDetailModel) {
                this._originalData = JSON.parse(JSON.stringify(oDetailModel.getData()));
            }

            const oViewModel = this.getView().getModel("viewModel");
            oViewModel.setProperty("/editMode", true);
            oViewModel.setProperty("/showEditButton", false);
        },

        onCancelEdit: function () {
            this._editMode = false;
            if (this._originalData) {
                const oDetailModel = this.getView().getModel("detailModel");
                if (oDetailModel) {
                    oDetailModel.setData(this._originalData);
                }
                this._originalData = null;
            }

            const oViewModel = this.getView().getModel("viewModel");
            oViewModel.setProperty("/editMode", false);
            oViewModel.setProperty("/showEditButton", true);
            oViewModel.setProperty("/showAiChatButton", true);
        },

        onRefreshDetail: function () {
            // Get the currently selected item type and refresh its details
            const oViewModel = this.getView().getModel("viewModel");
            const sSelectedItemType = oViewModel.getProperty("/selectedItemType");
            const oDetailModel = this.getView().getModel("detailModel");

            if (!sSelectedItemType || !oDetailModel) {
                MessageToast.show("No item selected to refresh");
                return;
            }

            const oCurrentData = oDetailModel.getData();
            if (!oCurrentData || !oCurrentData.ID) {
                MessageToast.show("No valid data to refresh");
                return;
            }

            // Map item type to correct entity set
            let sEntitySet = "";
            switch (sSelectedItemType) {
                case "task":
                    sEntitySet = "Tasks";
                    break;
                case "botInstance":
                    sEntitySet = "BotInstances";
                    break;
                case "contextNode":
                    sEntitySet = "ContextNodes";
                    break;
                case "botMessage":
                    sEntitySet = "BotMessages";
                    break;
                default:
                    MessageToast.show("Unknown item type");
                    return;
            }

            // Reload the details for the current item
            this._loadItemDetails(sEntitySet, oCurrentData.ID, sSelectedItemType)
                .then(() => {
                    MessageToast.show("Details refreshed successfully");
                })
                .catch(error => {
                    console.error("Failed to refresh details:", error);
                    MessageToast.show("Failed to refresh details");
                });
        },

        onAiChat: function () {
            const oDetailModel = this.getView().getModel("detailModel");
            const oViewModel = this.getView().getModel("viewModel");
            const sItemType = oViewModel.getProperty("/selectedItemType");

            if (!oDetailModel || !sItemType) {
                MessageToast.show("No data to chat");
                return;
            }

            const oData = oDetailModel.getData();
            const sEntitySet = "BotInstances";
        },

        async onSaveChanges() {
            const oDetailModel = this.getView().getModel("detailModel");
            const oViewModel = this.getView().getModel("viewModel");
            const sItemType = oViewModel.getProperty("/selectedItemType");

            if (!oDetailModel || !sItemType) {
                MessageToast.show("No data to save");
                return;
            }

            const oData = oDetailModel.getData();
            let sEntitySet;

            switch (sItemType) {
                case "task":
                    sEntitySet = "Tasks";
                    break;
                case "botInstance":
                    sEntitySet = "BotInstances";
                    break;
                case "contextNode":
                    sEntitySet = "ContextNodes";
                    break;
                case "botMessage":
                    sEntitySet = "BotMessages";
                    break;
                default:
                    MessageToast.show("Unknown item type");
                    return;
            }

            try {
                this._setBusy(true);

                const sPath = `/${sEntitySet}(ID='${oData.ID}')`;
                const oContext = this._oModel.bindContext(sPath);

                // Use OData V4 context-based updates
                await oContext.requestObject(); // Ensure context is loaded

                // Update properties through context
                Object.keys(oData).forEach(sKey => {
                    if (sKey !== "ID" && oData.hasOwnProperty(sKey)) {
                        oContext.setProperty(sKey, oData[sKey]);
                    }
                });

                // Submit batch automatically handled by OData V4 model
                await this._oModel.submitBatch();

                MessageToast.show("Changes saved successfully");
                this._editMode = false;
                oViewModel.setProperty("/editMode", false);
                oViewModel.setProperty("/showEditButton", true);
                oViewModel.setProperty("/showAiChatButton", true);
                this._originalData = null;

                // Refresh navigation data
                if (this._currentMode === "tasks") {
                    await this._loadTasksForNavigation();
                } else {
                    await this._loadContextNodesForNavigation();
                }

            } catch (error) {
                console.error("Failed to save changes:", error);
                MessageBox.error("Failed to save changes: " + (error.message || error));
            } finally {
                this._setBusy(false);
            }
        },

        _buildNavigationStructure: function (aTaskContexts) {
            const aNavigationData = [];

            aTaskContexts.forEach(oContext => {
                const oTask = oContext.getObject();
                const oTaskItem = this._buildTaskItemRecursively(oTask);
                aNavigationData.push(oTaskItem);
            });

            this._oNavigationModel.setProperty("/navigation", aNavigationData);
        },

        _buildTaskItemRecursively: function (oTask) {
            const oTaskItem = {
                key: `task_${oTask.ID}`,
                title: oTask.name || 'Unnamed Task',
                icon: "sap-icon://task",
                expanded: true,  // Default expanded for Tasks
                hasExpander: false,
                type: "Task",
                data: oTask,
                items: []
            };

            // Add BotInstances as child items (no ContextNodes in Tasks navigation)
            const aBotInstances = oTask.botInstances;
            if (aBotInstances && aBotInstances.length > 0) {
                oTaskItem.hasExpander = true;
                aBotInstances.forEach(oBotInstance => {
                    const oBotItem = this._buildBotInstanceItem(oBotInstance);
                    oTaskItem.items.push(oBotItem);
                });
            }

            return oTaskItem;
        },

        _buildBotInstanceItem: function (oBotInstance) {
            const oBotItem = {
                key: `bot_${oBotInstance.ID}`,
                title: oBotInstance.type?.name || `Bot Instance ${oBotInstance.sequence || ''}`,
                icon: "sap-icon://robot",
                expanded: true,  // Default expanded for BotInstances
                hasExpander: false,
                type: "BotInstance",
                data: oBotInstance,
                items: []
            };

            // Add SubTasks
            const aSubTasks = oBotInstance.tasks;
            if (aSubTasks && aSubTasks.length > 0) {
                oBotItem.hasExpander = true;
                aSubTasks.forEach(oSubTask => {
                    const oSubTaskItem = this._buildTaskItemRecursively(oSubTask);
                    oSubTaskItem.title = `${oSubTask.name || 'Unnamed SubTask'}`;
                    oBotItem.items.push(oSubTaskItem);
                });
            }

            // Add BotMessages
            const aBotMessages = oBotInstance.messages;
            if (aBotMessages && aBotMessages.length > 0) {
                if (!oBotItem.hasExpander) {
                    oBotItem.hasExpander = true;
                }
                aBotMessages.forEach((oBotMessage, index) => {
                    const oBotMessageItem = {
                        key: `botmessage_${oBotMessage.ID}`,
                        title: `Message ${index + 1} (${oBotMessage.role || 'unknown'})`,
                        icon: "sap-icon://email",
                        expanded: false,
                        hasExpander: false,
                        type: "BotMessage",
                        data: oBotMessage,
                        items: []
                    };
                    oBotItem.items.push(oBotMessageItem);
                });
            }

            return oBotItem;
        },

        createNavigationItem: function (sId, oContext) {
            const oData = oContext.getObject();
            return new sap.tnt.NavigationListItem({
                text: oData.title,
                icon: oData.icon,
                key: oData.key,
                expanded: oData.expanded,
                hasExpander: oData.hasExpander,
                items: {
                    path: 'navigation>items',
                    factory: this.createNavigationItem.bind(this)
                }
            });
        },

        formatMessageRoleState: function (sRole) {
            if (!sRole) {
                return "None";
            }

            switch (sRole.toLowerCase()) {
                case "user":
                    return "Information";
                case "assistant":
                    return "Success";
                case "system":
                    return "Warning";
                default:
                    return "None";
            }
        },

        formatContextValue: function (sValue, sType) {
            if (!sValue) {
                return "";
            }

            // Ensure sValue is a string
            const sStringValue = String(sValue);

            // Format based on context type
            switch (sType) {
                case "code":
                case "CODE":
                    return `<pre><code>${this._escapeHtml(sStringValue)}</code></pre>`;
                case "markdown":
                case "MARKDOWN":
                    // Basic markdown to HTML conversion
                    return this._convertMarkdownToHtml(sStringValue);
                case "json":
                case "JSON":
                    try {
                        const oJsonObject = JSON.parse(sStringValue);
                        return `<pre><code>${JSON.stringify(oJsonObject, null, 2)}</code></pre>`;
                    } catch (e) {
                        return `<pre><code>${this._escapeHtml(sStringValue)}</code></pre>`;
                    }
                default:
                    return this.formatMultiLineText(sStringValue);
            }
        },

        _convertMarkdownToHtml: function (sMarkdown) {
            if (!sMarkdown) {
                return "";
            }

            // Ensure sMarkdown is a string
            let sHtml = String(sMarkdown);

            // Headers
            sHtml = sHtml.replace(/^### (.*$)/gim, '<h3>$1</h3>');
            sHtml = sHtml.replace(/^## (.*$)/gim, '<h2>$1</h2>');
            sHtml = sHtml.replace(/^# (.*$)/gim, '<h1>$1</h1>');

            // Bold
            sHtml = sHtml.replace(/\*\*(.*)\*\*/gim, '<strong>$1</strong>');

            // Italic
            sHtml = sHtml.replace(/\*(.*)\*/gim, '<em>$1</em>');

            // Code blocks
            sHtml = sHtml.replace(/```([\s\S]*?)```/gim, '<pre><code>$1</code></pre>');

            // Inline code
            sHtml = sHtml.replace(/`(.*)`/gim, '<code>$1</code>');

            // Line breaks
            sHtml = sHtml.replace(/\n/g, '<br/>');

            return sHtml;
        },

        _escapeHtml: function (sText) {
            if (!sText) {
                return "";
            }

            // Ensure sText is a string
            const sStringText = String(sText);
            const div = document.createElement('div');
            div.textContent = sStringText;
            return div.innerHTML;
        },

        // New methods for FlexibleColumnLayout support

        onLayoutStateChange: function (oEvent) {
            const sLayout = oEvent.getParameter("layout");
            const bIsNavigationColumnVisible = oEvent.getParameter("isNavigationColumnVisible");
            const bIsMidColumnVisible = oEvent.getParameter("isMidColumnVisible");
            const bIsEndColumnVisible = oEvent.getParameter("isEndColumnVisible");

            const oViewModel = this.getView().getModel("viewModel");
            oViewModel.setProperty("/layout", sLayout);

            console.log("Layout changed to:", sLayout);
        },

        onNavigationSearch: function (oEvent) {
            const sQuery = oEvent.getParameter("query") || oEvent.getParameter("newValue") || "";
            this._filterNavigationItems(sQuery);
        },

        _filterNavigationItems: function (sQuery) {
            const oNavigationList = this.byId("navigationList");
            if (!oNavigationList) {
                return;
            }

            const oBinding = oNavigationList.getBinding("items");
            if (!oBinding) {
                return;
            }

            const aFilters = [];
            if (sQuery && sQuery.trim()) {
                const oFilter = new Filter({
                    path: "title",
                    operator: FilterOperator.Contains,
                    value1: sQuery,
                    caseSensitive: false
                });
                aFilters.push(oFilter);
            }

            oBinding.filter(aFilters);
        },

        onRefreshNavigation: function () {
            const oViewModel = this.getView().getModel("viewModel");
            const sCurrentMode = oViewModel.getProperty("/viewMode");

            if (sCurrentMode === "tasks") {
                this._loadTasksForNavigation();
            } else if (sCurrentMode === "contextNodes") {
                this._loadContextNodesForNavigation();
            }

            MessageToast.show("Navigation refreshed");
        },

        onExpandAll: function () {
            this._setNavigationExpansion(true);
        },

        onCollapseAll: function () {
            this._setNavigationExpansion(false);
        },

        _setNavigationExpansion: function (bExpanded) {
            const aNavigation = this._oNavigationModel.getProperty("/navigation") || [];

            const setExpansionRecursive = (aItems) => {
                aItems.forEach(oItem => {
                    if (oItem.hasExpander) {
                        oItem.expanded = bExpanded;
                    }
                    if (oItem.items && oItem.items.length > 0) {
                        setExpansionRecursive(oItem.items);
                    }
                });
            };

            setExpansionRecursive(aNavigation);
            this._oNavigationModel.setProperty("/navigation", aNavigation);
        },

        onCloseDetail: function () {
            // Keep the two-column layout but clear the detail content
            this._clearDetailView();
        },

        _clearDetailView: function () {
            const oViewModel = this.getView().getModel("viewModel");
            oViewModel.setProperty("/selectedItemType", null);
            oViewModel.setProperty("/detailTitle", "Details");
            oViewModel.setProperty("/showEditButton", false);
            oViewModel.setProperty("/showAiChatButton", false);
            oViewModel.setProperty("/editMode", false);

            // Clear detail model
            const oDetailModel = this.getView().getModel("detailModel");
            if (oDetailModel) {
                oDetailModel.setData({});
            }

            // Clear navigation selection but keep fixed item selection
            this._oNavigationModel.setProperty("/selectedKey", null);
        },

        onNavigationBreadcrumb: function () {
            this.onCloseDetail();
        },

        onFocusNavigation: function () {
            const oNavigationList = this.byId("navigationList");
            if (oNavigationList) {
                oNavigationList.focus();
            }
        },

        // Override the existing onNavigationItemSelect to handle new layout
        onNavigationItemSelect: function (oEvent) {
            const oListItem = oEvent.getParameter("item");
            const sKey = oListItem.getKey();
            const oViewModel = this.getView().getModel("viewModel");

            console.log("Navigation item selected:", sKey);

            // Handle fixed navigation items
            if (this._isFixedNavigationItem(sKey)) {
                this._handleFixedNavigationItem(sKey);
                return;
            }

            // Update selected key for main navigation
            this._oNavigationModel.setProperty("/selectedKey", sKey);

            // Parse the key to determine item type and ID
            const aParts = sKey.split("_");
            if (aParts.length < 2) {
                console.warn("Invalid key format:", sKey);
                return;
            }

            const sType = aParts[0];
            const sId = aParts.slice(1).join("_");

            console.log("Parsed type:", sType, "ID:", sId);

            // Set layout to show detail column
            oViewModel.setProperty("/layout", fioriLibrary.LayoutType.TwoColumnsMidExpanded);

            // Load detail based on type
            this._loadDetailForSelectedItem(sType, sId);
        },

        _isFixedNavigationItem: function (sKey) {
            return sKey && sKey.startsWith("viewMode_");
        },

        _handleFixedNavigationItem: function (sKey) {
            if (sKey.startsWith("viewMode_")) {
                const sViewMode = sKey.replace("viewMode_", "");
                this._switchViewMode(sViewMode);
            } else {
                console.warn("Unknown fixed navigation item:", sKey);
            }
        },

        _switchViewMode: function (sViewMode) {
            const oViewModel = this.getView().getModel("viewModel");
            const sCurrentMode = oViewModel.getProperty("/viewMode");
            const sSelectedTaskId = oViewModel.getProperty("/selectedTaskId");

            // Don't switch if already in the same mode
            if (sCurrentMode === sViewMode) {
                return;
            }

            // Update view mode
            oViewModel.setProperty("/viewMode", sViewMode);
            this._currentMode = sViewMode;

            // Load appropriate data based on mode
            if (sViewMode === "tasks") {
                this._loadTasksForNavigation(sSelectedTaskId);
                //MessageToast.show("Switched to Tasks view");
            } else if (sViewMode === "contextNodes") {
                this._loadContextNodesForNavigation(sSelectedTaskId);
                //MessageToast.show("Switched to Context Nodes view");
            }

            // Reset detail selection when switching views
            this._clearDetailView();

            // Update the selected key to reflect the current view mode
            oViewModel.setProperty("/selectedFixedKey", "viewMode_" + sViewMode);
        },

        async _loadDetailForSelectedItem(sType, sId) {
            try {
                this._setBusy(true);
                console.log("Loading detail for type:", sType, "ID:", sId);

                let oItemData = null;
                let sItemType = null;
                let sEntitySet = null;

                switch (sType) {
                    case "task":
                        sEntitySet = "Tasks";
                        sItemType = "task";
                        break;
                    case "bot":
                        sEntitySet = "BotInstances";
                        sItemType = "botInstance";
                        break;
                    case "botmessage":
                        sEntitySet = "BotMessages";
                        sItemType = "botMessage";
                        break;
                    case "contextnode":
                        oItemData = await this._findContextNodeById(sId);
                        sItemType = "contextNode";
                        break;
                    default:
                        console.warn("Unknown item type:", sType);
                        return;
                }

                if (sEntitySet) {
                    oItemData = await this._loadItemDetails(sEntitySet, sId, sItemType);
                }

                console.log("Loaded item data:", oItemData);

                if (oItemData) {
                    this._updateDetailPanel(oItemData, sItemType);
                    console.log("Detail panel updated successfully");
                } else {
                    console.warn("No item data found for:", sType, sId);
                }

            } catch (error) {
                console.error("Error loading detail for selected item:", error);
                MessageToast.show("Failed to load item details");
            } finally {
                this._setBusy(false);
            }
        },

        _initializeFlexibleColumnLayout: function () {
            // FlexibleColumnLayout will use its native default responsive behavior
            const oFlexibleColumnLayout = this.byId("flexibleColumnLayout");
            if (oFlexibleColumnLayout) {
                console.log("FlexibleColumnLayout initialized with native default layout");
            }
        }
    });
}); 