package customer.ai2code.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sap.cds.reflect.CdsModel;

import customer.ai2code.util.ODataUrlParserEnhanced.ODataUrlInfo.ODataUrlInfoBuilder;
import customer.ai2code.util.ODataUrlParserEnhanced.ODataWhereCondition.ODataWhereConditionBuilder;
import customer.ai2code.util.ODataUrlParserEnhanced.PathSegmentInfo.PathSegmentInfoBuilder;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;

import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.Selectable;
import com.sap.cds.ql.cqn.CqnPredicate;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.reflect.CdsElement;

/**
 * 增强的 OData V4 URL 解析器
 * 使用 SAP CAP CdsModel 来支持更复杂的解析场景
 * 实现从右到左的路径解析策略
 */
public class ODataUrlParserEnhanced {
    private static final Pattern ENTITY_SET_HAS_KEY_PATTERN = Pattern.compile("^([^(]+)(?:\\(([^)]+)\\))?");

    @Getter
    private List<PathSegmentInfo> pathSegments = new ArrayList<>();

    @Getter
    private Select<?> select;

    @Getter
    private BuildingCql buildingCql = new BuildingCql();

    /**
     * OData 请求类型枚举
     */
    // public enum ODataRequestType {
    // ENTITY_SET, // 查询整个实体集 (e.g., /Books)
    // SINGLE_ENTITY, // 查询单个实体 (e.g., /Books('123'))
    // ENTITY_PROPERTY, // 查询实体属性 (e.g., /Books('123')/title)
    // NAVIGATION_PROPERTY, // 查询导航属性 (e.g., /Books('123')/author)
    // NAVIGATION_PROPERTY_VALUE, // 查询导航属性的值 (e.g., /Books('123')/author/name)
    // COMPLEX_NAVIGATION // 复杂导航路径 (e.g., /BotInstances/id/type/outputContextPath)
    // }

    public enum SegmentType {
        ENTITY_SET, // 实体集
        ENTITY_SET_WITH_KEYS, // 带键的实体集
        NAVIGATION_PROPERTY, // 导航属性
        NAVIGATION_PROPERTY_WITH_KEYS, // 带键值的导航属性
        PROPERTY, // 属性
        KEY // 键
    }

    /**
     * 路径段信息
     */
    @Data
    @Builder
    @Getter
    @Setter
    public static class PathSegmentInfo {
        // private String name;
        private Map<String, String> keys;
        // private
        // private boolean isEntitySet; // 是否为实体集
        // private boolean isEntitySetWithKeys; // 是否为带键的实体集
        // private boolean isNavigationProperty; // 是否为导航属性
        // private boolean isNavigationPropertyWithKeys; // 是否为带键值导航属性值
        // private boolean isProperty; // 是否为属性
        // private boolean isKey;
        private SegmentType segmentType; // 段类型
        private CdsEntity entityModel; // 实体集
        private CdsElement elementModel; // 属性/导航属性
        private Integer position; // 在路径中的位置，从右到左
        private String segment; // 原始段
    }

    @Data
    @Getter
    @Setter
    public class BuildingCql {

        private PathSegmentInfo fromSegment;

        private PathSegmentInfo keySegment;

        private List<CqnPredicate> whereConditions = new ArrayList<>();

        private List<String> selectColumns = new ArrayList<>();

        // private CqlE
        public Boolean hasFromSegment() {
            // return fromSegment != null;
            return fromSegment != null;
        }

        public Boolean hasKeySegment() {
            // return keySegment != null;
            return keySegment != null;
        }

        public void addSelectColumn(String column) {
            if (column != null && !column.trim().isEmpty()) {
                selectColumns.add(column);
            }
        }

        public void addWhereCondition(CqnPredicate condition) {
            if (condition != null) {
                whereConditions.add(condition);
            }
        }

    }

    /**
     * 增强的 OData URL 信息类
     */
    // @Builder
    // @Data
    // @Getter
    // @Setter
    // public static class ODataUrlInfo {
    // private String targetEntitySet; // 实体集名称
    // // private
    // @Singular("whereCondition")
    // private List<ODataWhereCondition> whereConditions; // 查询条件
    // @Singular("selectColumn")
    // private List<String> selectColumns;

    // private Integer skip;

    // private Integer top;
    // }

    // @Data
    // @Getter
    // @Setter
    // @Builder
    // public static class ODataWhereCondition {
    // private String propertyName; // 属性名称
    // private String operator; // 操作符
    // private Object value; // 属性值
    // }

    public ODataUrlParserEnhanced() {
        // 初始化
    }

    /**
     * 解析 OData URL - 主要入口方法
     * 
     * @param oDataUrl 完整的 OData URL 或路径部分
     * @param cdsModel CDS 模型用于验证和增强解析
     * @return 解析后的 URL 信息
     */
    public Select<?> parseUrl(String oDataUrl, CdsModel cdsModel) {
        if (oDataUrl == null || oDataUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("OData URL cannot be null or empty");
        }
        // select.columns()
        // ODataUrlInfo urlInfo = new ODataUrlInfo();
        // ODataUrlInfoBuilder builder = ODataUrlInfo.builder();

        // 分离路径和查询参数
        String[] urlParts = oDataUrl.split("\\?", 2);
        String pathPart = urlParts[0];
        String queryPart = urlParts.length > 1 ? urlParts[1] : "";

        parsePathLefttoRight(pathPart, cdsModel);

        // 从右到左解析路径部分
        parsePathRightToLeft();
        // 解析查询参数
        parseQueryParameters(queryPart);

        // 确定请求类型
        // determineRequestType(builder);

        // return builder.build();
        return select;
    }

    /**
     * 从左到右解析路径 - 将每个segment的定义和前后关系解析出来
     */
    private void parsePathLefttoRight(String pathPart, CdsModel cdsModel) {
        if (pathPart.startsWith("/")) {
            pathPart = pathPart.substring(1);
        }

        String[] segments = pathPart.split("/");

        for (int idx = 0; idx < segments.length; idx++) {
            final int i = idx;
            Optional<PathSegmentInfo> segmentInfo = resolveEntitySetWithKeys(segments, i, cdsModel)
                    .or(() -> resolveEntitySet(segments, i, cdsModel))
                    .or(() -> resolveNavigationPropertywithKeys(segments, i, cdsModel))
                    .or(() -> resolveNavigationProperty(segments, i, cdsModel))
                    .or(() -> resolveProperty(segments, i, cdsModel))
                    .or(() -> resolveKey(segments, i, cdsModel));

            // 只有当解析成功时才添加
            segmentInfo.ifPresent(pathSegments::add);
        }
    }

    /**
     * 从右到左解析路径 - 核心方法
     * 按照设计文档的要求：
     * 1. 通过问号将path和query分开
     * 2. 将path通过/符号分割成若干段，从最右侧开始往左侧依次解析
     */
    private void parsePathRightToLeft() {

        Boolean stop = false;

        // 从右到左解析
        // 如果找到Where条件，或者解析结束
        Integer i = pathSegments.size() - 1;
        while (!stop && i >= 0) {

            PathSegmentInfo segmentInfo = pathSegments.get(i);

            // 解析当前段
            switch (segmentInfo.getSegmentType()) {
                case NAVIGATION_PROPERTY:
                case ENTITY_SET:
                    if (!buildingCql.hasFromSegment()) {
                        buildingCql.setFromSegment(segmentInfo);
                    }
                    if (buildingCql.hasKeySegment()) {
                        // buildingCql.addWhereCondition();
                        buildWhereCondition(segmentInfo).forEach(buildingCql::addWhereCondition);
                        stop = true; // 如果上一段是键值,构建完whereCondition就结束
                        break;
                    }

                    break;

                case NAVIGATION_PROPERTY_WITH_KEYS:
                case ENTITY_SET_WITH_KEYS:
                    if (!buildingCql.hasFromSegment()) {
                        // 没有from segment，则设置当前段为fromSegment
                        buildingCql.setFromSegment(segmentInfo);
                        buildWhereCondition(segmentInfo).forEach(buildingCql::addWhereCondition);
                    }
                    stop = true; // 找到带键值导航属性/EntitySet，结束解析
                    break;

                case PROPERTY:
                    buildingCql.addSelectColumn(segmentInfo.getSegment());
                    break;
                case KEY:
                    buildingCql.setKeySegment(segmentInfo);
                    break;
                default:
                    break;
            }

        }
    }

    private List<CqnPredicate> buildWhereCondition(PathSegmentInfo segmentInfo) {
        switch (segmentInfo.getSegmentType()) {
            case NAVIGATION_PROPERTY_WITH_KEYS:
            case ENTITY_SET_WITH_KEYS:
                //从当前segment开始，往右一直找
            
                break;

            case NAVIGATION_PROPERTY:
            case ENTITY_SET:

                break;    
            default:
                break;
        }
        // CQL.to    

        return List.of(CQL.get("a").eq(segmentInfo));
    }


    private List<String> buildingNavigationChain(PathSegmentInfo segmentInfo) {
        List<String> chain = new ArrayList<>();
        // segmentInfo.
        Integer position = segmentInfo.getPosition();
        Integer positionBehind = position + 1;
        while (position <= buildingCql.getFromSegment().getPosition()) {
            PathSegmentInfo info = pathSegments.get(position);
            PathSegmentInfo infoBehind = pathSegments.get(positionBehind);
            //先简单用反射方式
            
            if( infoBehind.getEntityModel() != null ) {
                infoBehind.getEntityModel().associations()
                    .filter(asso -> asso.getType().as(CdsEntity.class).getName().equals(info.getEntityModel().getName()))
                    .findFirst()
                    .ifPresent(asso -> chain.add(asso.getName()));
                

                // 如果当前段和后面一段的实体模型相同，则添加到链中
                chain.add(info.getSegment());
            } else {
                // 否则，停止查找
                break;

            }

            // info.getEntityModel().associations().forEach( asso -> {
            //     if (asso.getType().as(CdsEntity.class).getName().equals(info.getSegment())) ) {
            //         chain.add(asso.getName());
            //     }
            // });

            // if (info.getSegmentType() == SegmentType.NAVIGATION_PROPERTY
            //         || info.getSegmentType() == SegmentType.NAVIGATION_PROPERTY_WITH_KEYS) {
            //     chain.add(info.getSegment());
            // } else {
            //     break; // 遇到非导航属性的段，停止
            // }
            position++;
            
        }

        // 从当前segment开始，往右一直找
        // for (int i = segmentInfo.getPosition(); i < pathSegments.size(); i++) {
        //     PathSegmentInfo info = pathSegments.get(i);
        //     if (info.getSegmentType() == SegmentType.NAVIGATION_PROPERTY
        //             || info.getSegmentType() == SegmentType.NAVIGATION_PROPERTY_WITH_KEYS) {
        //         chain.add(info.getSegment());
        //     } else {
        //         break; // 遇到非导航属性的段，停止
        //     }
        // }
        // return chain;
    }




    /**
     * 分析单个路径段 - 解析键和实体集
     */
    public Optional<PathSegmentInfo> resolveEntitySetWithKeys(String[] segments, Integer position, CdsModel cdsModel) {
        String segment = segments[position];

        // 解析路径段，提取键、实体集等信息
        Matcher matcher = ENTITY_SET_HAS_KEY_PATTERN.matcher(segment);

        if (matcher.matches()) {
            if (matcher.group(2) == null || matcher.group(2).isEmpty()) {
                // 第二个分组空，则表示是一个实体集，没有键
                return Optional.empty();
            }

            String entitySetName = matcher.group(1);
            return cdsModel.findEntity(entitySetName)
                    .map(entity -> {
                        String keyPart = matcher.group(2);

                        // 再通过逗号分隔key
                        List<String> keys = keyPart != null ? Arrays.asList(keyPart.split(","))
                                : Collections.emptyList();
                        Map<String, String> keyMap = new HashMap<>();

                        keys.forEach(key -> {
                            // 处理每个键
                            String[] keyParts = key.split("=");
                            if (keyParts.length == 2) {
                                String keyName = keyParts[0].trim();
                                String keyValue = unquote(keyParts[1].trim());
                                keyMap.put(keyName, keyValue);
                            } else if (keyParts.length == 1) {
                                // 如果只有一个部分，可能是单个键值
                                keyMap.put("ID", unquote(keyParts[0].trim()));
                            }
                        });

                        // 返回
                        return PathSegmentInfo.builder()
                                .segment(segment)
                                .position(position)
                                .segmentType(SegmentType.ENTITY_SET_WITH_KEYS)
                                .entityModel(entity)
                                .keys(keyMap)
                                .build();
                    });
        }
        return Optional.empty();
    }

    /**
     * 解析单个路径 - EntitySet
     */
    public Optional<PathSegmentInfo> resolveEntitySet(String[] segments, Integer position, CdsModel cdsModel) {
        String segment = segments[position];
        // 解析路径段，提取键、实体集等信息
        Matcher matcher = ENTITY_SET_HAS_KEY_PATTERN.matcher(segment);

        if (matcher.matches()) {
            if (matcher.groupCount() >= 1) {
                String entitySetName = matcher.group(1);
                return cdsModel.findEntity(entitySetName)
                        .map(entity -> PathSegmentInfo.builder()
                                .segment(segment)
                                .position(position)
                                .segmentType(SegmentType.ENTITY_SET)
                                .entityModel(entity)
                                .build());
            }
        }
        return Optional.empty();
    }

    /**
     * 解析单个路径 - 带键导航属性
     */
    public Optional<PathSegmentInfo> resolveNavigationPropertywithKeys(String[] segments, Integer position,
            CdsModel cdsModel) {
        String segment = segments[position];
        // 解析路径段，提取键、实体集等信息
        Matcher matcher = ENTITY_SET_HAS_KEY_PATTERN.matcher(segment);

        if (matcher.matches() && matcher.groupCount() >= 2) {
            String navigationPropertyName = matcher.group(1);

            // 检查是否为导航属性
            if (cdsModel != null) {
                return cdsModel.entities()
                        .map(entity -> entity.findAssociation(navigationPropertyName))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .findFirst()
                        .map(element -> {
                            CdsEntity targetEntity = element.getType().as(CdsEntity.class);

                            // 再通过逗号分隔key
                            String keyPart = matcher.group(2);
                            List<String> keys = keyPart != null ? Arrays.asList(keyPart.split(","))
                                    : Collections.emptyList();
                            Map<String, String> keyMap = new HashMap<>();

                            keys.forEach(key -> {
                                // 处理每个键
                                String[] keyParts = key.split("=");
                                if (keyParts.length == 2) {
                                    String keyName = keyParts[0].trim();
                                    String keyValue = unquote(keyParts[1].trim());
                                    keyMap.put(keyName, keyValue);
                                } else if (keyParts.length == 1) {
                                    // 如果只有一个部分，可能是单个键值
                                    keyMap.put("ID", unquote(keyParts[0].trim()));
                                }
                            });

                            return PathSegmentInfo.builder()
                                    .segment(segment)
                                    .position(position)
                                    .segmentType(SegmentType.NAVIGATION_PROPERTY_WITH_KEYS)
                                    .elementModel(element)
                                    .entityModel(targetEntity)
                                    .keys(keyMap)
                                    .build();
                        });
            }
        }
        return Optional.empty();
    }

    /**
     * 解析单个路径 - 导航属性
     */
    public Optional<PathSegmentInfo> resolveNavigationProperty(String[] segments, Integer position, CdsModel cdsModel) {
        String segment = segments[position];

        // 解析路径段，提取导航属性信息
        if (cdsModel != null) {
            return cdsModel.entities()
                    .map(entity -> entity.findAssociation(segment))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
                    .map(element -> {
                        CdsEntity targetEntity = element.getType().as(CdsEntity.class);
                        return PathSegmentInfo.builder()
                                .segment(segment)
                                .position(position)
                                .segmentType(SegmentType.NAVIGATION_PROPERTY)
                                .elementModel(element)
                                .entityModel(targetEntity)
                                .build();
                    });
        }
        return Optional.empty();
    }

    /**
     * 解析单个路径-字段
     */
    public Optional<PathSegmentInfo> resolveProperty(String[] segments, Integer position, CdsModel cdsModel) {
        String segment = segments[position];

        // 解析路径段，提取属性信息
        if (cdsModel != null) {
            return cdsModel.entities()
                    .map(entity -> entity.findElement(segment))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .findFirst()
                    .map(element -> PathSegmentInfo.builder()
                            .segment(segment)
                            .position(position)
                            .segmentType(SegmentType.PROPERTY)
                            .elementModel(element)
                            .build());
        }
        return Optional.empty();
    }

    /**
     * 解析单个路径 - 键
     */
    public Optional<PathSegmentInfo> resolveKey(String[] segments, Integer position, CdsModel cdsModel) {
        String segment = segments[position];

        // 简单的键值检查，可以根据需要增强
        if (isGuidOrKey(segment)) {
            Map<String, String> keys = new HashMap<>();
            keys.put("ID", unquote(segment));

            return Optional.of(PathSegmentInfo.builder()
                    .segment(segment)
                    .position(position)
                    .segmentType(SegmentType.KEY)
                    .keys(keys)
                    .build());
        }
        return Optional.empty();
    }

    /**
     * 检查字符串是否看起来像 GUID 或键值
     */
    private static boolean isGuidOrKey(String segment) {
        // GUID 格式检查
        Pattern guidPattern = Pattern
                .compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
        if (guidPattern.matcher(segment).matches()) {
            return true;
        }

        // 数字键检查
        try {
            Integer.parseInt(segment);
            return true;
        } catch (NumberFormatException e) {
            // 不是数字
        }

        // 引号包围的键
        if (segment.startsWith("'") && segment.endsWith("'") && segment.length() > 2) {
            return true;
        }

        return false;
    }

    /**
     * 解析查询参数
     */
    private static void parseQueryParameters(String queryPart) {
        if (queryPart == null || queryPart.trim().isEmpty()) {
            return;
        }

        String[] params = queryPart.split("&");
        for (String param : params) {
            String[] keyValue = param.split("=", 2);
            if (keyValue.length == 2) {
                String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                // urlInfo.getQueryOptions().put(key, value);
                // 只处理$select,$top,$skip
                switch (key) {
                    case "$select":
                        // urlInfo.getSelectColumns().addAll(Arrays.asList(value.split(",")));
                        urlInfoBuilder.selectColumns(Arrays.asList(value.split(",")));
                        break;
                    case "$top":
                        try {
                            urlInfoBuilder.top(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            // 忽略无效的 $top 值
                        }
                        break;
                    case "$skip":
                        try {
                            urlInfoBuilder.skip(Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            // 忽略无效的 $skip 值
                        }
                        break;
                    case "$filter":
                        // 解析过滤条件（简化处理）
                        // ODataWhereCondition condition = ODataWhereCondition.builder()
                        // .propertyName("status_code")
                        // .operator("eq")
                        // .value(unquote(value))
                        // .build();
                        // urlInfo.getWhereConditions().add(condition);
                        List<String> expressions = Arrays.asList(value.split(" and "));
                        expressions.forEach(expr -> {
                            // 根据空格 拆分
                            String[] parts = expr.split(" ");
                            if (parts.length >= 3) {
                                ODataWhereCondition condition = ODataWhereCondition.builder()
                                        .propertyName(parts[0].trim())
                                        .operator(parts[1].trim())
                                        .value(unquote(parts[2].trim()))
                                        .build();
                                urlInfoBuilder.whereCondition(condition);

                            }
                        });
                        break;
                    default:
                        // 其他查询参数可以忽略或记录
                        break;
                }
            }
        }
    }

    /**
     * 移除字符串两端的引号
     */
    private static String unquote(String value) {
        if (value != null && value.length() >= 2) {
            if ((value.startsWith("'") && value.endsWith("'")) ||
                    (value.startsWith("\"") && value.endsWith("\""))) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

}
