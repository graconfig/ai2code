sap.ui.define(["sap/m/MessageBox", "sap/ui/Device"], function (MessageBox, Device) {
    "use strict";

    // 定义Helper构造函数（这里主要用于挂载静态属性和方法，在JavaScript中类也是函数）
    // function Helper() {}
    return {
        withConfirmation: function (title, text, callback) {
            MessageBox.confirm(text, {
                title: title,
                onClose: function (action) {
                    if (action === MessageBox.Action.OK) {
                        callback();
                    }
                }
            });
        },
        getContentDensityClass: function () {
            if (this.contentDensityClass === undefined) {
                // 检查FLP是否已设置内容密度类，若已设置则不做任何操作
                if (document.body.classList.contains("sapUiSizeCozy") || document.body.classList.contains("sapUiSizeCompact")) {
                    this.contentDensityClass = "";
                } else if (!Device.support.touch) {
                    // 如果不支持触摸，则应用“紧凑”模式
                    this.contentDensityClass = "sapUiSizeCompact";
                } else {
                    // 如果支持触摸，则应用“舒适”模式；这是大多数sap.m控件的默认模式，但对于像sap.ui.table.Table这样的桌面优先控件是需要的
                    this.contentDensityClass = "sapUiSizeCozy";
                }
            }
            return this.contentDensityClass;
        }

    };
});