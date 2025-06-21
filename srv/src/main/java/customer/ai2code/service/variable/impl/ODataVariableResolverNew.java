package customer.ai2code.service.variable.impl;

import org.springframework.stereotype.Component;
import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsModel;
import cds.gen.mainservice.MainService;
import customer.ai2code.service.impl.EntityService;
import customer.ai2code.service.variable.VariableContext;
import customer.ai2code.service.variable.VariableResolver;
import customer.ai2code.util.ODataUrlParserEnhanced;

/**
 * OData 查询变量解析器 - 使用手动解析 OData URL
 */
@Component
public class ODataVariableResolverNew implements VariableResolver {

    private final CdsModel cdsModel;
    private final EntityService entityService;
    private final MainService mainService;

    public ODataVariableResolverNew(CdsModel cdsModel, EntityService entityService, MainService mainService) {
        this.cdsModel = cdsModel;
        this.entityService = entityService;
        this.mainService = mainService;
    }

    @Override
    public boolean supports(String variableExpression) {
        return variableExpression.startsWith("OData:");
    }

    @Override
    public String resolve(String variableExpression, VariableContext context) {
        try {
            String odataUrl = variableExpression.replace("OData:", "").trim();
            return executeODataQuery(odataUrl);
        } catch (Exception e) {
            System.err
                    .println("Failed to resolve OData variable: " + variableExpression + ", error: " + e.getMessage());
            return "";
        }
    }

    @Override
    public int getPriority() {
        return 3;
    }

    /**
     * 执行OData查询 - 解析OData URL为CQN
     */
    private String executeODataQuery(String odataUrl) {
        try {
            // 解析 OData URL
            ODataUrlParserEnhanced urlInfo = new ODataUrlParserEnhanced();
            CqnSelect select = urlInfo.parseUrl(odataUrl, cdsModel);
            if (select == null) {
                System.err.println("Failed to parse OData URL: " + odataUrl);
                return "";
            }

            // 执行查询
            Result result = entityService.select(mainService, select);

            // 处理查询结果 - 使用增强版本
            return processEnhancedQueryResult(result, urlInfo.getBuildingCql());

        } catch (Exception e) {
            System.err.println("Failed to execute OData query: " + odataUrl + ", error: " + e.getMessage());
            return "";
        }
    }

    /**
     * 增强的查询结果处理
     */
    private String processEnhancedQueryResult(Result result, ODataUrlParserEnhanced.BuildingCql buildingCql) {
        if (result == null || !result.iterator().hasNext()) {
            return "";
        }

        // 根据请求类型处理结果
        if (buildingCql != null) {
            return processResultByRequestType(result, buildingCql);
        }
        return null;
    }

    /**
     * 根据请求类型处理结果
     */
    private String processResultByRequestType(Result result, ODataUrlParserEnhanced.BuildingCql buildingCql) {
        switch (buildingCql.getRequestType()) {
            case SINGLE_ENTITY:
                // 单个实体，返回完整信息或特定字段
                return processSingleEntity(result, buildingCql);

            case ENTITY_SET:
                // 实体集合，可能需要分页
                return processEntitySet(result, buildingCql);

            case ENTITY_PROPERTY:
                // 单个属性值
                return processEntityProperty(result, buildingCql);

            case ENTITY_PROERTIES:
                // 导航属性，可能是集合或单个对象
                // return processNavigationProperty(result, urlInfo);
                return processSingleEntity(result, buildingCql);

            default:
                return processEntitySet(result, buildingCql);
        }
    }

    /**
     * 处理单个实体
     */
    private String processSingleEntity(Result result, ODataUrlParserEnhanced.BuildingCql buildingCql) {

        Row row = result.first().orElse(null);
        if (row == null) {
            return "";
        }
        return row.toJson();
    }

    /**
     * 处理实体集合
     */
    private String processEntitySet(Result result, ODataUrlParserEnhanced.BuildingCql buildingCql) {
        return result.toJson();
    }

    /**
     * 处理实体属性
     */
    private String processEntityProperty(Result result, ODataUrlParserEnhanced.BuildingCql buildingCql) {
        // if (!result.iterator().hasNext()) {
        // return "";
        // }

        Row row = result.first().orElse(null);
        if (row == null) {
            return "";
        }

        String propertyName = buildingCql.getSelectColumns().getFirst();
        if (propertyName != null) {
            Object value = row.get(propertyName);
            return value != null ? value.toString() : "";
        }

        return "";
    }
}
