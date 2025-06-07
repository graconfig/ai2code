sap.ui.define([
    "sap/ui/base/Object",
    "sap/ui/core/Fragment",

], function (Object, Fragment) {
    "use strict";

    // 定义NewEntityDialog构造函数，模拟类的定义
    // function NewEntityDialog(context, fragment, view) {
    //     this.resolve = null;
    //     this.reject = null;
    //     this.dialog = null;
    //     this.model = null;
    //     this.context = context;
    //     this.fragment = fragment;
    //     this.view = view;
    // }
    // var dialog;
    // var model;
    // var context;
    // var fragment;
    // var view;

    return Object.extend("ai.orchestration.taskrun.service.NewEntityDialog", {
        // dialog,
        // model,
        // context,
        // fragment,
        // view,
        constructor: function (context, fragment, view) {
            this.context = context;
            this.fragment = fragment;
            this.view = view;
        },
        open: function () {
            return new Promise((resolve, reject) => {
                // this.resolve = resolve;
                // this.reject = reject;
                this.model = this.context.getModel();
                // this.dialog = await Fragment.load({
                //     id: "newEntityDialog",
                //     name: "report.fragment." + this.fragment,
                //     controller: this
                // });
                Fragment.load({
                    id: "newEntityDialog",
                    name: "report.fragment." + this.fragment,
                    controller: this
                }).then((dialog) => {
                    this.dialog = dialog;
                    this.view.addDependent(dialog);
                    dialog.setBindingContext(this.context);
                    return this.context.created();
                }).then(() => {
                    this.dialog.close();
                    resolve(this.context);
                    this.dialog.open();
                }, reject);

                // this.context.created()
            });
        },
        onCreate: function () {
            return new Promise((resolve, reject) => {
                // try {
                this.model.submitBatch(this.model.getUpdateGroupId()).then(() => {
                    resolve();
                }).catch((error) => {
                    reject(error);
                });
                // resolve();
                // } catch (error) {
                // reject(error);
                // }
            });
        },
        onCancel: function () {
            this.dialog.close();
            this.reject({ error: "User cancelled" });
        }


    });
});