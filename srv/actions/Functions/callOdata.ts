interface odataRequest {
  ProjName: string;
  ProjDesc: string;
  _views: Array<{
    viewName: string;
    sourceCode: string;
  }>;
}

export const callOdata = async function (params: odataRequest) {
  console.log("OData call with params:", params);
};
