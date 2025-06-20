package customer.ai2code.service.variable.impl;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.ql.Select;
import com.sap.cds.services.persistence.PersistenceService;
import customer.ai2code.service.variable.VariableContext;
import customer.ai2code.service.variable.VariableResolver;
import customer.ai2code.util.ODataUrlParserEnhanced;

/**
 * OData 查询变量解析器 - 使用手动解析 OData URL
 */
@Component
public class ODataVariableResolverNew implements VariableResolver {

    private final PersistenceService persistenceService;

    private final CdsModel cdsModel;

    public ODataVariableResolverNew(PersistenceService persistenceService, CdsModel cdsModel) {
        this.persistenceService = persistenceService;
        this.cdsModel = cdsModel;
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
    }    /**
     * 执行OData查询 - 解析OData URL为CQN
     */
    private String executeODataQuery(String odataUrl) {
        try {
            // 解析 OData URL
            ODataUrlParserEnhanced.ODataUrlInfo urlInfo = ODataUrlParserEnhanced.parseUrl(odataUrl, cdsModel);
            if (urlInfo == null) {
                return "";
            }

            // 构建 CQN
            CqnSelect cqnSelect = buildCqnFromUrlInfo(urlInfo);
            if (cqnSelect == null) {
                return "";
            }

            // 执行查询
            Result result = persistenceService.run(cqnSelect);

            // 处理查询结果 - 使用增强版本
            return processEnhancedQueryResult(result, odataUrl, urlInfo);

        } catch (Exception e) {
            System.err.println("Failed to execute OData query: " + odataUrl + ", error: " + e.getMessage());
            return "";
        }
    }    /**
     * 根据解析的 URL 信息构建 CQN - 基于新的右到左解析结果
     */
    private CqnSelect buildCqnFromUrlInfo(ODataUrlParserEnhanced.ODataUrlInfo urlInfo) {
        if (urlInfo.getTargetEntitySet() == null && urlInfo.getEntitySet() == null) {
            return null;
        }

        // 确定查询的目标实体
        String targetEntity = urlInfo.getTargetEntitySet() != null ? 
            urlInfo.getTargetEntitySet() : urlInfo.getEntitySet();

        // 构建基本的 Select 语句
        Select<?> select = Select.from(targetEntity);

        // 处理选择列
        if (urlInfo.getSelectColumn() != null) {
            select = select.columns(urlInfo.getSelectColumn());
        }        // 处理键条件
        if (!urlInfo.getKeys().isEmpty()) {
            for (java.util.Map.Entry<String, String> keyEntry : urlInfo.getKeys().entrySet()) {
                String keyName = keyEntry.getKey();
                String keyValue = keyEntry.getValue();
                select = select.where(b -> b.get(keyName).eq(keyValue));
            }
        }

        // 处理导航条件
        for (ODataUrlParserEnhanced.NavigationCondition condition : urlInfo.getWhereConditions()) {
            String navProperty = condition.getNavigationProperty();
            String keyProperty = condition.getKeyProperty();
            Object keyValue = condition.getKeyValue();
            
            // 简化的导航条件处理
            final String finalNavProperty = navProperty;
            final String finalKeyProperty = keyProperty;
            final Object finalKeyValue = keyValue;
            select = select.where(b -> b.get(finalNavProperty + "_" + finalKeyProperty).eq(finalKeyValue));
        }

        // 处理查询选项
        select = applyQueryOptions(select, urlInfo);

        return select;
    }    /**
     * 应用查询选项
     */
    private Select<?> applyQueryOptions(Select<?> select, ODataUrlParserEnhanced.ODataUrlInfo urlInfo) {
        java.util.Map<String, String> queryOptions = urlInfo.getQueryOptions();

        // $select
        String selectClause = queryOptions.get("$select");
        if (selectClause != null && !selectClause.trim().isEmpty()) {
            String[] fields = selectClause.split(",");
            for (int i = 0; i < fields.length; i++) {
                fields[i] = fields[i].trim();
            }
            select = select.columns(fields);
        }

        // $filter - 这里需要更复杂的解析
        String filter = queryOptions.get("$filter");
        if (filter != null && !filter.trim().isEmpty()) {
            // 简单的 filter 解析示例
            select = applySimpleFilter(select, filter);
        }

        // $top
        String top = queryOptions.get("$top");
        if (top != null) {
            try {
                int topValue = Integer.parseInt(top);
                select = select.limit(topValue);
            } catch (NumberFormatException e) {
                System.err.println("Invalid $top value: " + top);
            }
        }

        // $skip
        String skip = queryOptions.get("$skip");
        if (skip != null) {
            try {
                int skipValue = Integer.parseInt(skip);
                // 注意：CAP 的 skip 实现可能因版本而异
                System.out.println("Skip functionality for value: " + skipValue);
            } catch (NumberFormatException e) {
                System.err.println("Invalid $skip value: " + skip);
            }
        }

        // $orderby
        String orderBy = queryOptions.get("$orderby");
        if (orderBy != null && !orderBy.trim().isEmpty()) {
            // 简单的排序处理
            String[] orderFields = orderBy.split(",");
            for (String orderField : orderFields) {
                String field = orderField.trim();
                if (field.endsWith(" desc")) {
                    final String fieldName = field.substring(0, field.length() - 5).trim();
                    select = select.orderBy(b -> b.get(fieldName).desc());
                } else if (field.endsWith(" asc")) {
                    final String fieldName = field.substring(0, field.length() - 4).trim();
                    select = select.orderBy(b -> b.get(fieldName).asc());
                } else {
                    final String fieldName = field;
                    select = select.orderBy(b -> b.get(fieldName).asc());
                }
            }
        }

        return select;
    }

    /**
     * 应用简单的过滤条件
     */
    private Select<?> applySimpleFilter(Select<?> select, String filter) {
        // 简单的过滤器解析，支持基本的 eq, ne, gt, lt 等操作
        // 示例：name eq 'test' 或 sequence gt 5
        
        try {
            // 解析简单的 eq 操作
            if (filter.contains(" eq ")) {
                String[] parts = filter.split(" eq ");
                if (parts.length == 2) {
                    String fieldName = parts[0].trim();
                    String value = parts[1].trim();
                    
                    // 移除引号
                    if (value.startsWith("'") && value.endsWith("'")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    final String finalValue = value;
                    select = select.where(b -> b.get(fieldName).eq(finalValue));
                }
            }
            // 可以添加更多的过滤操作支持
        } catch (Exception e) {
            System.err.println("Failed to parse filter: " + filter + ", error: " + e.getMessage());
        }

        return select;
    }/**
     * 增强的查询结果处理
     */
    private String processEnhancedQueryResult(Result result, String originalUrl, ODataUrlParserEnhanced.ODataUrlInfo urlInfo) {
        if (result == null || !result.iterator().hasNext()) {
            return "";
        }

        // 根据请求类型处理结果
        if (urlInfo != null) {
            return processResultByRequestType(result, urlInfo);
        }

        // 回退到原始处理方式
        return processResultByEntityType(result, originalUrl);
    }/**
     * 根据请求类型处理结果
     */
    private String processResultByRequestType(Result result, ODataUrlParserEnhanced.ODataUrlInfo urlInfo) {
        switch (urlInfo.getRequestType()) {
            case SINGLE_ENTITY:
                // 单个实体，返回完整信息或特定字段
                return processSingleEntity(result, urlInfo);

            case ENTITY_SET:
                // 实体集合，可能需要分页
                return processEntitySet(result, urlInfo);

            case ENTITY_PROPERTY:
                // 单个属性值
                return processEntityProperty(result, urlInfo);

            case NAVIGATION_PROPERTY:
                // 导航属性，可能是集合或单个对象
                return processNavigationProperty(result, urlInfo);

            case NAVIGATION_PROPERTY_VALUE:
                // 导航属性的特定值
                return processNavigationPropertyValue(result, urlInfo);

            case COMPLEX_NAVIGATION:
                // 复杂导航路径
                return processComplexNavigation(result, urlInfo);

            default:
                return processEntitySet(result, urlInfo);
        }
    }

    /**
     * 处理单个实体
     */
    private String processSingleEntity(Result result, ODataUrlParserEnhanced.ODataUrlInfo urlInfo) {
        if (!result.iterator().hasNext()) {
            return "";
        }

        Row row = result.first().orElse(null);
        if (row == null) {
            return "";
        }

        // 如果指定了 $select，只返回选定的字段
        java.util.List<String> selectFields = urlInfo.getSelectFields();
        if (!selectFields.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String field : selectFields) {
                if (sb.length() > 0) sb.append(", ");
                Object value = row.get(field);
                sb.append(field).append(": ").append(value != null ? value.toString() : "null");
            }
            return sb.toString();
        }

        // 返回实体的主要信息
        return formatEntityRow(row, urlInfo.getEntitySet());
    }

    /**
     * 处理实体集合
     */
    private String processEntitySet(Result result, ODataUrlParserEnhanced.ODataUrlInfo urlInfo) {
        StringBuilder resultBuilder = new StringBuilder();
        int count = 0;
        
        for (Row row : result) {
            if (count > 0) {
                resultBuilder.append("; ");
            }
            resultBuilder.append(formatEntityRow(row, urlInfo.getEntitySet()));
            count++;
        }

        // 如果有 $top 限制，添加计数信息
        Integer topValue = urlInfo.getTopValue();
        if (topValue != null && count >= topValue) {
            resultBuilder.append(" (limited to ").append(topValue).append(" results)");
        }

        return resultBuilder.toString();
    }

    /**
     * 处理实体属性
     */
    private String processEntityProperty(Result result, ODataUrlParserEnhanced.ODataUrlInfo urlInfo) {
        if (!result.iterator().hasNext()) {
            return "";
        }

        Row row = result.first().orElse(null);
        if (row == null) {
            return "";
        }

        String propertyName = urlInfo.getTargetProperty();
        if (propertyName != null) {
            Object value = row.get(propertyName);
            return value != null ? value.toString() : "";
        }

        return "";
    }

    /**
     * 处理导航属性
     */
    private String processNavigationProperty(Result result, ODataUrlParserEnhanced.ODataUrlInfo urlInfo) {
        // 导航属性的处理取决于目标实体类型
        return processEntitySet(result, urlInfo);
    }

    /**
     * 处理导航属性值
     */
    private String processNavigationPropertyValue(Result result, ODataUrlParserEnhanced.ODataUrlInfo urlInfo) {
        return processEntityProperty(result, urlInfo);
    }

    /**
     * 处理复杂导航
     */
    private String processComplexNavigation(Result result, ODataUrlParserEnhanced.ODataUrlInfo urlInfo) {
        // 复杂导航的处理，可能需要根据具体路径定制
        return processEntitySet(result, urlInfo);
    }

    /**
     * 根据实体类型处理结果（原始方法）
     */
    private String processResultByEntityType(Result result, String originalUrl) {
        StringBuilder resultBuilder = new StringBuilder();

        for (Row row : result) {
            if (resultBuilder.length() > 0) {
                resultBuilder.append(", ");
            }

            // 根据URL中的实体类型确定处理方式
            if (originalUrl.toLowerCase().contains("botinstances")) {
                resultBuilder.append(formatBotInstanceRow(row));
            } else if (originalUrl.toLowerCase().contains("tasks")) {
                resultBuilder.append(formatTaskRow(row));
            } else if (originalUrl.toLowerCase().contains("contextnodes")) {
                resultBuilder.append(formatContextNodeRow(row));
            } else {
                // 默认处理：返回第一个非空字段的值
                for (String key : row.keySet()) {
                    Object value = row.get(key);
                    if (value != null) {
                        resultBuilder.append(value.toString());
                        break;
                    }
                }
            }
        }

        return resultBuilder.toString();
    }

    /**
     * 格式化实体行数据
     */
    private String formatEntityRow(Row row, String entityType) {
        if (entityType == null) {
            return formatGenericRow(row);
        }

        switch (entityType.toLowerCase()) {
            case "botinstances":
                return formatBotInstanceRow(row);
            case "tasks":
                return formatTaskRow(row);
            case "contextnodes":
                return formatContextNodeRow(row);
            default:
                return formatGenericRow(row);
        }
    }

    /**
     * 格式化 BotInstance 行
     */
    private String formatBotInstanceRow(Row row) {
        String id = Optional.ofNullable(row.get("ID")).map(Object::toString).orElse("");
        String status = Optional.ofNullable(row.get("status_code")).map(Object::toString).orElse("");
        String sequence = Optional.ofNullable(row.get("sequence")).map(Object::toString).orElse("");
        return String.format("BotInstance[ID: %s, Status: %s, Sequence: %s]", id, status, sequence);
    }

    /**
     * 格式化 Task 行
     */
    private String formatTaskRow(Row row) {
        String id = Optional.ofNullable(row.get("ID")).map(Object::toString).orElse("");
        String name = Optional.ofNullable(row.get("name")).map(Object::toString).orElse("");
        String isMain = Optional.ofNullable(row.get("isMain")).map(Object::toString).orElse("");
        return String.format("Task[ID: %s, Name: %s, IsMain: %s]", id, name, isMain);
    }

    /**
     * 格式化 ContextNode 行
     */
    private String formatContextNodeRow(Row row) {
        String id = Optional.ofNullable(row.get("ID")).map(Object::toString).orElse("");
        String path = Optional.ofNullable(row.get("path")).map(Object::toString).orElse("");
        String type = Optional.ofNullable(row.get("type")).map(Object::toString).orElse("");
        return String.format("ContextNode[ID: %s, Path: %s, Type: %s]", id, path, type);
    }

    /**
     * 格式化通用行数据
     */
    private String formatGenericRow(Row row) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        
        for (String key : row.keySet()) {
            Object value = row.get(key);
            if (value != null) {
                if (!first) sb.append(", ");
                sb.append(key).append(": ").append(value.toString());
                first = false;
            }
        }
        
        return sb.length() > 0 ? sb.toString() : "";
    }
}
