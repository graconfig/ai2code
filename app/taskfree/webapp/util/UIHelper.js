sap.ui.define(["sap/ui/core/Control"], function (Control) {
    "use strict";

    return {
        scrollToElement: function (element, timeout, behavior) {
            if (timeout === undefined) {
                timeout = 0;
            }
            if (behavior === undefined) {
                behavior = "smooth";
            }
            setTimeout(function () {
                element.scrollIntoView({ behavior: behavior });
            }, timeout);
        }
    };
});