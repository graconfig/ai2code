sap.ui.define([
    "sap/m/MessageToast",
    "sap/m/MessageBox",
    "sap/ui/core/Fragment"
], function (MessageToast, MessageBox, Fragment) {
    'use strict';

    function _createUploadController(oExtensionAPI) {
        var oUploadDialog;

        function setOkButtonEnabled(bOk) {
            oUploadDialog && oUploadDialog.getBeginButton().setEnabled(bOk);
        }

        function setDialogBusy(bBusy) {
            oUploadDialog.setBusy(bBusy)
        }

        function closeDialog() {
            oUploadDialog && oUploadDialog.close()
        }

        function showError(sMessage) {
            MessageBox.error(sMessage)
        }

        // TODO: Better option for this?
        function byId(sId) {
            return sap.ui.core.Fragment.byId("uploadDialogId", sId);
        }

        return {
            onBeforeOpen: function (oEvent) {
                oUploadDialog = oEvent.getSource();
                oExtensionAPI.addDependent(oUploadDialog);
            },

            onAfterClose: function (oEvent) {
                oExtensionAPI.removeDependent(oUploadDialog);
                oUploadDialog.destroy();
                oUploadDialog = undefined;
            },
            onFileChange: function (oEvent) {
                const files = oEvent.getParameter("files");
                if (files && files.length > 0) {
                    this._selectedFile = files[0];

                    // 启用 OK 按钮
                    const oDialog = oEvent.getSource().getParent(); // FileUploader → Dialog
                    const oOkButton = oDialog.getBeginButton();
                    if (oOkButton) {
                        oOkButton.setEnabled(true);
                    }
                } else {
                    this._selectedFile = null;
                }
            },
            onOk: async function (oEvent) {
                setDialogBusy(true)
                const oModel = oExtensionAPI.getModel();

                const base64Content = await this.fileToBase64(this._selectedFile);

                const oContextBinding = oModel.bindContext("/uploadCDSViews(...)");
                oContextBinding.setParameter("excel", base64Content);

                try {
                    await oContextBinding.execute();
                    const result = oContextBinding.getBoundContext().getObject();
                    const message = result?.value;
                    MessageToast.show(result.value);
                    oExtensionAPI.refresh()
                    closeDialog();
                } catch (e) {
                    MessageBox.error("上传失败: " + e.message);
                }
                setDialogBusy(false)
            },
            fileToBase64: function (file) {
                return new Promise((resolve, reject) => {
                    const reader = new FileReader();
                    reader.onload = () => {
                        const base64 = reader.result.split(',')[1]; // 去掉 data: 前缀
                        resolve(base64);
                    };
                    reader.onerror = reject;
                    reader.readAsDataURL(file);
                });
            },
            onCancel: function (oEvent) {
                closeDialog();
            },

            onTypeMismatch: function (oEvent) {
                var sSupportedFileTypes = oEvent
                    .getSource()
                    .getFileType()
                    .map(function (sFileType) {
                        return "*." + sFileType;
                    })
                    .join(", ");

                showError(
                    "The file type *." +
                    oEvent.getParameter("fileType") +
                    " is not supported. Choose one of the following types: " +
                    sSupportedFileTypes
                );
            },

            onFileAllowed: function (oEvent) {
                setOkButtonEnabled(true)
            },

            onFileEmpty: function (oEvent) {
                setOkButtonEnabled(false)
            },



            onUploadComplete: function (oEvent) {
                //获取返回状态
                var iStatus = oEvent.getParameter("status");
                var oFileUploader = oEvent.getSource()

                oFileUploader.clear();
                setOkButtonEnabled(false)
                setDialogBusy(false)

                if (iStatus >= 400) {
                    var oRawResponse = JSON.parse(oEvent.getParameter("responseRaw"));
                    showError(oRawResponse.error.message);
                } else {
                    MessageToast.show("Uploaded successfully");
                    oExtensionAPI.refresh()
                    closeDialog();
                }
            }
        };
    }

    return {
        onUpload: function (oEvent) {
            //MessageToast.show("Custom handler invoked.");
            Fragment.load({
                id: "uploadDialogId",
                name: "ragcdsviews.ext.fragment.upload",
                controller: _createUploadController(this)
            }).then(function (oDialog) {
                oDialog.open();
            });
        },
        uploadExcelViaODataAction: function (file) {
            return new Promise((resolve, reject) => {
                const reader = new FileReader();

                reader.onload = async function (e) {
                    // 读取 ArrayBuffer
                    const arrayBuffer = e.target.result;
                    const uint8Array = new Uint8Array(arrayBuffer);

                    // 转base64字符串
                    let binary = '';
                    for (let i = 0; i < uint8Array.length; i++) {
                        binary += String.fromCharCode(uint8Array[i]);
                    }
                    const base64 = btoa(binary);

                    // 构造OData请求体，$binary字段必须用base64字符串
                    const payload = {
                        excel: {
                            $binary: base64
                        }
                    };

                    try {
                        const response = await fetch("/odata/v4/MainService/uploadCDSViews", {
                            method: "POST",
                            headers: {
                                "Content-Type": "application/json"
                            },
                            body: JSON.stringify(payload)
                        });

                        if (response.ok) {
                            const data = await response.json();
                            resolve(data.value || data);
                        } else {
                            const errorText = await response.text();
                            reject(new Error(errorText));
                        }
                    } catch (err) {
                        reject(err);
                    }
                };

                reader.onerror = function (err) {
                    reject(err);
                };

                reader.readAsArrayBuffer(file);
            });
        }
    };
});
