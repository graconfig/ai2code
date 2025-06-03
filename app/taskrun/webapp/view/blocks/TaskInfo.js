sap.ui.define(["sap/ui/core/library", 'sap/uxap/BlockBase'], function (coreLibrary, BlockBase) {
	"use strict";

	var ViewType = coreLibrary.mvc.ViewType;

	var TaskInfo = BlockBase.extend("ai.orchestration.taskrun.view.blocks.TaskInfo", {
		metadata: {
			views: {
				Collapsed: {
					viewName: "ai.orchestration.taskrun.view.blocks.TaskInfo",
					type: ViewType.XML
				},
				Expanded: {
					viewName: "ai.orchestration.taskrun.view.blocks.TaskInfo",
					type: ViewType.XML
				}
			}
		}
	});
	return TaskInfo;
});
