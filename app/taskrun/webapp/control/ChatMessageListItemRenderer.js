sap.ui.define([
    "sap/ui/core/Renderer",
    "sap/m/Text",
    "sap/m/BusyIndicator",
    "./marked"
],
    function (Renderer, Text, BusyIndicator, markedImport) {
        return Renderer.extend("ai.orchestration.taskrun.control.ChatMessageListItemRenderer", {
            renderLIContent: function (rm, control) {
                rm.openStart("div").class("sapMMessageListItem").openEnd();


                // 消息内容区域
                rm.openStart("div").class("sapMMessageListItemText").openEnd();
                if (control.getLoading()) {
                    // 显示 busy 指示器
                    rm.renderControl(new BusyIndicator({
                        size: "1rem",
                        text: "AI is thinking..."
                    }));
                    // } else {

                }
                rm.unsafeHtml(this.markdownToHtml(control.getMessage()));
                rm.close("div");

                rm.openStart("div").class("sapMMessageListItemHeader").openEnd();
                rm.renderControl(control.getAggregation("avatar"));

                rm.openStart("div").class("sapMMessageListItemInfo").openEnd();
                rm.renderControl(new Text({ text: control.getSender() }));
                rm.renderControl(new Text({ text: "|" }));
                rm.renderControl(new Text({ text: control.getDate() }));
                rm.openStart("div").class("customMessageListItemRightAlign").openEnd();
                rm.renderControl(control.getAggregation("adopt"));
                rm.renderControl(control.getAggregation("display"));
                rm.close("div");
                rm.close("div");


                rm.close("div");
                rm.close("div");
            },
            markdownToHtml: function (text) {
                return marked.parse(text);
            }
        });
    });