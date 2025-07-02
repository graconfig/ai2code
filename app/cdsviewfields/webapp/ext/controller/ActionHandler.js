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
          const item = oEvent.getParameter("item");
          this.createEntity(item)
            .then((id) => {
              this.uploadContent(item, id);
            })
            .catch((err) => {
              MessageToast.show("Error adding item: " + err.message);
              console.log(err);
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

        uploadContent: async function (item, id) {

          const csrfToken = await this._apiFetchCsrfToken();

          var url =
            location.hostname === "localhost"
              ? `/embedding/CDSViewFiles(${id})/fileContent`
              : this._getBaseURL() + `/embedding/CDSViewFiles(${id})/fileContent`;

          item.setUploadUrl(url);

          var oUploadSet = byId("uploadSet"); 

          oUploadSet.addHeaderField(
            new sap.ui.core.Item({ key: "x-csrf-token", text: csrfToken })
          );
          oUploadSet.setHttpRequestMethod("PUT");
          oUploadSet.uploadItem(item);
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
      },
    };
  }
);
