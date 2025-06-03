sap.ui.define(["sap/ui/core/library", 'sap/uxap/BlockBase'], function (coreLibrary, BlockBase) {
	"use strict";

	var ViewType = coreLibrary.mvc.ViewType;

	var BotInstances = BlockBase.extend("ai.orchestration.taskrun.view.blocks.BotInstances", {
		metadata: {
			views: {
				Collapsed: {
					viewName: "ai.orchestration.taskrun.view.blocks.BotInstances",
					type: ViewType.XML
				},
				Expanded: {
					viewName: "ai.orchestration.taskrun.view.blocks.BotInstances",
					type: ViewType.XML
				}
			}
		}
	});
	return BotInstances;
});
