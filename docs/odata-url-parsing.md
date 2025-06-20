# OData V4 URL 手动解析方案

## 概述

由于 SAP CAP Java 框架主要通过 EventContext 和 CQN 自动处理 OData URL 解析，**不直接提供手动解析 OData V4 URL 的公开 API**，我们提供了以下几种解决方案：

## 方案1: Apache Olingo (推荐)

### 依赖配置
```xml
<dependency>
    <groupId>org.apache.olingo</groupId>
    <artifactId>odata-server-api</artifactId>
    <version>4.10.0</version>
</dependency>
<dependency>
    <groupId>org.apache.olingo</groupId>
    <artifactId>odata-server-core</artifactId>
    <version>4.10.0</version>
</dependency>
```

### 使用示例
```java
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.core.uri.parser.Parser;

public class OlingoUrlParser {
    
    public void parseODataUrl(String oDataUrl, EdmProvider edmProvider) {
        try {
            // 创建 EDM 模型
            Edm edm = new EdmProviderImpl(edmProvider);
            
            // 解析 URL
            UriInfo uriInfo = new Parser(edm, oDataService).parseUri(
                odataPath, queryParameters, null, baseUri);
            
            // 获取资源路径
            List<UriResource> resourceParts = uriInfo.getUriResourceParts();
            
            // 获取查询选项
            FilterOption filterOption = uriInfo.getFilterOption();
            SelectOption selectOption = uriInfo.getSelectOption();
            ExpandOption expandOption = uriInfo.getExpandOption();
            
        } catch (UriParserException e) {
            // 处理解析异常
        }
    }
}
```

## 方案2: 自定义轻量级解析器

我们已经为您的项目创建了两个文件：

### 1. `ODataUrlParser.java` - 通用解析器
- 位置：`d:\code\AI\ai2code\srv\src\main\java\customer\ai2code\util\ODataUrlParser.java`
- 功能：解析 OData V4 URL 的各个组件
- 支持：实体集、键、查询参数、过滤条件等

### 2. `ODataVariableResolverNew.java` - CAP 集成版本
- 位置：`d:\code\AI\ai2code\srv\src\main\java\customer\ai2code\service\variable\impl\ODataVariableResolverNew.java`
- 功能：将解析的 OData URL 转换为 SAP CAP CQN 查询

## 使用示例

### 基本 URL 解析
```java
import customer.ai2code.util.ODataUrlParser;

public void parseExample() {
    String[] testUrls = {
        "Books",
        "Books('123')",
        "Books(ID='123',Version=1)",
        "Books('123')/author",
        "Books?$filter=title eq 'Java'&$select=title,author&$top=10",
        "Books('123')?$expand=author,reviews"
    };
    
    for (String url : testUrls) {
        ODataUrlParser.ODataUrlInfo info = ODataUrlParser.parseUrl(url);
        System.out.println("Entity Set: " + info.getEntitySet());
        System.out.println("Keys: " + info.getKeys());
        System.out.println("Filter: " + info.getFilter());
        System.out.println("Select: " + info.getSelect());
    }
}
```

### 在变量解析器中使用
```java
// 使用新的变量解析器
@Component
public class MyService {
    
    @Autowired
    private ODataVariableResolverNew oDataResolver;
    
    public String resolveODataVariable(String expression) {
        VariableContext context = new VariableContext();
        return oDataResolver.resolve("OData:Books?$filter=status eq 'ACTIVE'", context);
    }
}
```

## 支持的 OData V4 功能

### 当前支持：
- ✅ 实体集识别
- ✅ 简单键和复合键
- ✅ 基本查询参数 ($filter, $select, $top, $skip)
- ✅ 简单的等式过滤条件
- ✅ 导航属性

### 可扩展：
- 🔧 复杂的 $filter 表达式
- 🔧 $expand 解析
- 🔧 $orderby 解析
- 🔧 函数调用

## 其他开源选项

### Microsoft OData Java 库
```xml
<dependency>
    <groupId>com.microsoft.odata</groupId>
    <artifactId>odata-client-core</artifactId>
    <version>4.0.0</version>
</dependency>
```

### OData4j (较老但仍可用)
```xml
<dependency>
    <groupId>org.odata4j</groupId>
    <artifactId>odata4j-core</artifactId>
    <version>0.8.0</version>
</dependency>
```

## 测试

运行 `ODataUrlParser.main()` 方法可以测试各种 URL 解析情况：

```bash
cd d:\code\AI\ai2code
mvn compile exec:java -Dexec.mainClass="customer.ai2code.util.ODataUrlParser"
```

## 注意事项

1. **Apache Olingo** 是最成熟和功能完整的方案，但需要额外的依赖
2. **自定义解析器** 轻量级，适合简单场景，可根据需要扩展
3. 如果只是偶尔需要解析 URL，建议使用自定义解析器
4. 如果需要完整的 OData V4 支持，建议使用 Apache Olingo

选择哪种方案取决于您的具体需求和项目约束。

## 答案：是的，完全支持！

我们的 OData URL 解析器**完全支持**您提供的这种复杂导航路径：

```
/BotInstances/b631b9de-24ba-439c-afb3-f6a8002ddc9c/type/outputContextPath
```

### 解析结果：
- **实体集**: `BotInstances`
- **键**: `{ID=b631b9de-24ba-439c-afb3-f6a8002ddc9c}`
- **导航属性**: `type`
- **完整导航路径**: `[type, outputContextPath]`
- **所有路径段**: `[BotInstances, b631b9de-24ba-439c-afb3-f6a8002ddc9c, type, outputContextPath]`

### 支持的复杂路径格式：

1. **直接键格式**（您的例子）：
   ```
   /BotInstances/b631b9de-24ba-439c-afb3-f6a8002ddc9c/type/outputContextPath
   ```

2. **标准 OData 键格式**：
   ```
   BotInstances('b631b9de-24ba-439c-afb3-f6a8002ddc9c')/type/outputContextPath
   ```

3. **多级导航路径**：
   ```
   /BotInstances/someId/type/prompts/content
   /Tasks/taskId/botInstances/results
   ```

### 智能键识别

解析器能够智能识别以下键格式：
- ✅ **GUID**: `b631b9de-24ba-439c-afb3-f6a8002ddc9c`
- ✅ **数字**: `123`, `456`
- ✅ **引号字符串**: `'some-key'`
- ✅ **标准 OData 键**: `EntitySet(key)`

### 实际使用示例

```java
// 解析您的 URL
String url = "/BotInstances/b631b9de-24ba-439c-afb3-f6a8002ddc9c/type/outputContextPath";
ODataUrlParser.ODataUrlInfo info = ODataUrlParser.parseUrl(url);

// 获取解析结果
String entitySet = info.getEntitySet();        // "BotInstances"
String botId = info.getKeys().get("ID");        // "b631b9de-24ba-439c-afb3-f6a8002ddc9c"
String firstNav = info.getNavigationProperty(); // "type"
List<String> fullPath = info.getNavigationPath(); // ["type", "outputContextPath"]

// 在 SAP CAP 中构建 CQN 查询
// 这将被自动转换为相应的数据库查询
```
