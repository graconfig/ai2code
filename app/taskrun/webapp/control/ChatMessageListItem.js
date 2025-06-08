sap.ui.define(["sap/m/ListItemBase", "./ChatMessageListItemRenderer"], function (ListItemBase, ChatMessageListItemRenderer) {
    return ListItemBase.extend("ai.orchestration.taskrun.control.ChatMessageListItem", {
        metadata: {
            properties: {
                message: { type: "string", group: "Misc", defaultValue: "" },
                sender: { type: "string", group: "Misc", defaultValue: "" },
                date: { type: "string", group: "Misc", defaultValue: "" },
                isAdopted: { type: "boolean", group: "Misc", defaultValue: false },
                loading: { type: "boolean", group: "Misc", defaultValue: false }  // 添加 busy 属性
            },
            aggregations: {
                avatar: { type: "sap.m.Avatar", multiple: false },
                adopt: { type: "sap.m.Button", multiple: false },
                display: { type: "sap.m.Button", multiple: false }
            }
        },
        renderer: ChatMessageListItemRenderer
    });
});