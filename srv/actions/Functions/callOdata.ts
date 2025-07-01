/**
 * 调用 OData 服务
 */
interface odataRequest {
  /**
   * 项目名称
   */
  ProjName: string;
  /**
   * 项目描述
   */
  ProjDesc: string;
  _views: Array<{
    /**
     * 视图名称
     */
    viewName: string;
    /**
     * 视图描述
     */
    viewDesc: string;
    /**
     * 视图源代码
     */
    sourceCode: string;
  }>;
}

export const callOdata = async function (params: odataRequest) {
  console.log("OData call with params:", params);
};
