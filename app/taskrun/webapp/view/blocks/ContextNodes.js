sap.ui.define(["sap/ui/core/library", 'sap/uxap/BlockBase'], function (coreLibrary, BlockBase) {
	"use strict";

	var ViewType = coreLibrary.mvc.ViewType;

	var ContextNodes = BlockBase.extend("ai.orchestration.taskrun.view.blocks.ContextNodes", {
		metadata: {
			views: {
				Collapsed: {
					viewName: "ai.orchestration.taskrun.view.blocks.ContextNodes",
					type: ViewType.XML
				},
				Expanded: {
					viewName: "ai.orchestration.taskrun.view.blocks.ContextNodes",
					type: ViewType.XML
				}
			}
		}
	});
	return ContextNodes;
});
