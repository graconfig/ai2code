sap.ui.define(["sap/ui/base/Object", "sap/f/library", "sap/ui/model/json/JSONModel"], function (Object, library, JSONModel) {
    "use strict";
    var LayoutManager = Object.extend("ai.orchestration.taskrun.util.LayoutManager", {
        constructor: function () {

        },
        setModel: function (model) {
            this.model = model;
        },
        setLayout: function (layout) {
            this.model.setData({
                currentLayout: layout,
                oldLayout: this.getLayout(),
                isFullScreen: layout === library.LayoutType.MidColumnFullScreen || layout === library.LayoutType.EndColumnFullScreen
            });
        },
        getLayout: function () {
            return this.model.getProperty("/currentLayout");
        },
        setMidColumnFullScreen: function () {
            this.setLayout(library.LayoutType.MidColumnFullScreen);
        },
        setEndColumnFullScreen: function () {
            this.setLayout(library.LayoutType.EndColumnFullScreen);
        },
        exitFullScreen: function () {
            this.setLayout(this.model.getProperty("/oldLayout"));
        }
    });




    return {
        getInstance: function () {
            this.instance ??= new LayoutManager();
            return this.instance;
        }
    };
});