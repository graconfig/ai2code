sap.ui.define(
  ["sap/m/MessageToast", "sap/ui/core/Fragment"],
  function (MessageToast, Fragment) {
    "use strict";

    function _uploadController(oExtensionAPI) {
      var oUploadDialog;

      function byId(sId) {
        return sap.ui.core.Fragment.byId("fileUploadFragment", sId);
      }

      function closeDialog() {
        oUploadDialog && oUploadDialog.close();
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
          oExtensionAPI.refresh();
          // Reset the parent controller's dialog reference
          oExtensionAPI._oDialog = null;
        },

        onAfterItemAdded: function (oEvent) {
          const oItem = oEvent.getParameter("item");
          const oFile = oItem.getFileObject();
          this.uploadContent(oFile, "ZH").then(() => {
            oItem.setUploadState("Complete");
          });
        },

        onUploadCompleted: function (oEvent) {
          MessageToast.show("Upload Completed.");
          closeDialog();
        },

        _apiFetchCsrfToken: async function () {
          if (!this._sCsrfToken) {
            const res = await fetch(`${this._getBaseURL()}/index.html`, {
              method: "HEAD",
              headers: {
                "X-CSRF-Token": "fetch",
              },
              credentials: "same-origin",
            });
            this._sCsrfToken = res.headers.get("x-csrf-token");
          }
          return this._sCsrfToken;
        },

        createEntity: async function (item) {
          try {
            // Generate UUID using standard browser API
            const uuid = crypto.randomUUID();

            const data = {
              ID: uuid,
              mediaType: item.getMediaType(),
              fileName: item.getFileName(),
              size: item.getFileObject().size.toString(),
              isGenerated: false,
            };

            // Use OData V4 model operations
            const oModel = oExtensionAPI.getModel();
            const oListBinding = oModel.bindList("/CDSViewFiles");
            const oContext = oListBinding.create(data);

            // Wait for the entity to be created on the server
            await oContext.created();
            return data.ID;
          } catch (oError) {
            console.error("Error creating entity:", oError);
            throw oError;
          }
        },

        // uploadContent: async function (item, id) {
        //   const oFile = item.getFileObject();
        //   const sMediaType = oFile.type;

        //   const oModel = oExtensionAPI.getModel();
        //   const sUploadUrl = `${oModel.sServiceUrl}/CDSViewFiles(${id})/fileContent/$value`;

        //   try {
        //     await fetch(sUploadUrl, {
        //       method: "PUT",
        //       headers: {
        //         "Content-Type": sMediaType,
        //       },
        //       body: oFile,
        //     });

        //     item.setUploadState("Complete");
        //   } catch (err) {
        //     console.error("Upload failed", err);
        //     item.setUploadState("Error");
        //   }
        // },
        uploadContent: async function (oFile, langu) {

          const oModel = oExtensionAPI.getModel();
          const binaryBuffer = await this.fileToArrayBuffer(oFile);
          const sBase64 = await this.fileToBase64(oFile);  // base64 string, no data URI
          const oContextBinding = oModel.bindContext("/uploadCDSViewFields(...)");

          oContextBinding.setParameter("txt", sBase64);
          oContextBinding.setParameter("langu", langu);

          try {
            const resultContext = await oContextBinding.execute();
            const resultValue = resultContext.getObject(); // result: { value: "成功导入 38 条视图字段" }

            sap.m.MessageToast.show(resultValue.value);
          } catch (err) {
            console.error("上传失败", err);
            sap.m.MessageBox.error("上传失败: " + err.message);
          }
        },
        fileToBase64: function (file) {
          return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => {
              const base64 = reader.result.split(",")[1]; // remove "data:text/plain;base64,..."
              resolve(base64);
            };
            reader.onerror = reject;
            reader.readAsDataURL(file);
          });
        },
        fileToArrayBuffer: function (oFile) {
          return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = function () {
              resolve(reader.result); // This is ArrayBuffer!
            };
            reader.onerror = reject;
            reader.readAsArrayBuffer(oFile);
          });
        },
        _getBaseURL: function () {
          //var appId = this.getOwnerComponent().getManifestEntry("/sap.app/id");
          var appPath = "cdsviewfields";
          var appModulePath = jQuery.sap.getModulePath(appPath);
          return appModulePath;
        },

        onCloseUploadFileFragment: function () {
          closeDialog();
        },
      };
    }

    return {
      onUpload: function (oBindingContext, aSelectedContexts) {
        // Always create a new dialog instance to avoid issues with destroyed dialogs
        this._oDialog = null;

        Fragment.load({
          id: "fileUploadFragment",
          name: "cdsviewfields.ext.fragment.fileUpload",
          controller: _uploadController(this),
        })
          .then((oDialog) => {
            this._oDialog = oDialog;
            const oView = this.getView
              ? this.getView()
              : this.editFlow.getView();
            oView.addDependent(this._oDialog);
            this._oDialog.open();
          })
          .catch((oError) => {
            console.error("Error loading fragment:", oError);
            MessageToast.show("Could not open upload dialog");
          });
      }
    };
  }
);
