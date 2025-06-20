package customer.ai2code.service.variable.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import com.sap.cds.Result;
import com.sap.cds.Row;
import com.sap.cds.reflect.CdsModel;
import com.sap.cds.reflect.CdsEntity;
import com.sap.cds.services.persistence.PersistenceService;

import com.sap.cds.ql.cqn.CqnSelect;
import customer.ai2code.service.variable.VariableContext;
import customer.ai2code.util.ODataUrlParserEnhanced;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class ODataVariableResolverNewTest {
    
    @Autowired
    private CdsModel cdsModel;
    
    @Mock
    private PersistenceService persistenceService;
    
    @Autowired
    private ODataVariableResolverNew resolver;

    @Test
    void testSupports() {
        assertTrue(resolver.supports("OData:BotInstances"));
        assertTrue(resolver.supports("OData:Tasks"));
        assertFalse(resolver.supports("Variable:test"));
        assertFalse(resolver.supports("test"));
    }

    @Test
    void testGetPriority() {
        assertEquals(3, resolver.getPriority());
    }

    // 1.1 测试查询整个实体集合
    @Test
    void testEntitySetQuery() {
        // Mock返回结果
        Result mockResult = mock(Result.class);
        Row mockRow1 = mock(Row.class);
        Row mockRow2 = mock(Row.class);
        
        when(mockRow1.get("ID")).thenReturn("bot-1");
        when(mockRow1.get("status_code")).thenReturn("RUNNING");
        when(mockRow1.get("sequence")).thenReturn(1);
        when(mockRow1.keySet()).thenReturn(Set.of("ID", "status_code", "sequence"));
        
        when(mockRow2.get("ID")).thenReturn("bot-2");
        when(mockRow2.get("status_code")).thenReturn("COMPLETED");
        when(mockRow2.get("sequence")).thenReturn(2);
        when(mockRow2.keySet()).thenReturn(Set.of("ID", "status_code", "sequence"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow1, mockRow2).iterator());
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:BotInstances", context);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("bot-1") || result.contains("bot-2"));
    }

    @Test
    void testEntitySetWithQueryOptions() {
        // Mock返回结果
        Result mockResult = mock(Result.class);
        Row mockRow = mock(Row.class);
        
        when(mockRow.get("ID")).thenReturn("bot-1");
        when(mockRow.get("status_code")).thenReturn("RUNNING");
        when(mockRow.keySet()).thenReturn(Set.of("ID", "status_code"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow).iterator());
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:BotInstances?$select=ID,status_code&$filter=status_code eq 'RUNNING'&$top=10", context);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("bot-1"));
    }

    // 1.2 测试查询单个实体
    @Test
    void testSingleEntityQuery() {
        Result mockResult = mock(Result.class);
        Row mockRow = mock(Row.class);
        
        when(mockRow.get("ID")).thenReturn("bot-123");
        when(mockRow.get("status_code")).thenReturn("RUNNING");
        when(mockRow.get("sequence")).thenReturn(1);
        when(mockRow.keySet()).thenReturn(Set.of("ID", "status_code", "sequence"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow).iterator());
        when(mockResult.first()).thenReturn(Optional.of(mockRow));
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:BotInstances('bot-123')", context);
        
        assertNotNull(result);
        assertTrue(result.contains("bot-123"));
    }

    @Test
    void testSingleEntityWithIdKey() {
        Result mockResult = mock(Result.class);
        Row mockRow = mock(Row.class);
        
        when(mockRow.get("ID")).thenReturn("bot-123");
        when(mockRow.get("status_code")).thenReturn("RUNNING");
        when(mockRow.keySet()).thenReturn(Set.of("ID", "status_code"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow).iterator());
        when(mockResult.first()).thenReturn(Optional.of(mockRow));
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:BotInstances(ID='bot-123')", context);
        
        assertNotNull(result);
        assertTrue(result.contains("bot-123"));
    }

    // 1.3 测试访问实体属性
    @Test
    void testEntityPropertyAccess() {
        Result mockResult = mock(Result.class);
        Row mockRow = mock(Row.class);
        
        when(mockRow.get("status_code")).thenReturn("RUNNING");
        when(mockRow.keySet()).thenReturn(Set.of("status_code"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow).iterator());
        when(mockResult.first()).thenReturn(Optional.of(mockRow));
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:BotInstances('bot-123')/status_code", context);
        
        assertNotNull(result);
        assertEquals("RUNNING", result);
    }

    @Test
    void testTaskNameProperty() {
        Result mockResult = mock(Result.class);
        Row mockRow = mock(Row.class);
        
        when(mockRow.get("name")).thenReturn("Data Processing Task");
        when(mockRow.keySet()).thenReturn(Set.of("name"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow).iterator());
        when(mockResult.first()).thenReturn(Optional.of(mockRow));
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:Tasks('task-456')/name", context);
        
        assertNotNull(result);
        assertEquals("Data Processing Task", result);
    }

    // 1.4 测试导航属性查询
    @Test
    void testNavigationPropertyQuery() {
        Result mockResult = mock(Result.class);
        Row mockRow1 = mock(Row.class);
        Row mockRow2 = mock(Row.class);
        
        when(mockRow1.get("ID")).thenReturn("msg-1");
        when(mockRow1.get("role")).thenReturn("user");
        when(mockRow1.get("message")).thenReturn("Hello");
        when(mockRow1.keySet()).thenReturn(Set.of("ID", "role", "message"));
        
        when(mockRow2.get("ID")).thenReturn("msg-2");
        when(mockRow2.get("role")).thenReturn("assistant");
        when(mockRow2.get("message")).thenReturn("Hi there!");
        when(mockRow2.keySet()).thenReturn(Set.of("ID", "role", "message"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow1, mockRow2).iterator());
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:BotInstances('bot-123')/messages", context);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("msg-1") || result.contains("msg-2"));
    }

    @Test
    void testTaskNavigationProperty() {
        Result mockResult = mock(Result.class);
        Row mockRow = mock(Row.class);
        
        when(mockRow.get("ID")).thenReturn("task-123");
        when(mockRow.get("name")).thenReturn("Main Task");
        when(mockRow.get("isMain")).thenReturn(true);
        when(mockRow.keySet()).thenReturn(Set.of("ID", "name", "isMain"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow).iterator());
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:BotInstances('bot-123')/task", context);
        
        assertNotNull(result);
        assertTrue(result.contains("task-123"));
    }

    // 1.5 测试复杂导航路径
    @Test
    void testComplexNavigationPath() {
        Result mockResult = mock(Result.class);
        Row mockRow = mock(Row.class);
        
        when(mockRow.get("name")).thenReturn("Complex Task");
        when(mockRow.keySet()).thenReturn(Set.of("name"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow).iterator());
        when(mockResult.first()).thenReturn(Optional.of(mockRow));
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:BotInstances('bot-123')/task/name", context);
        
        assertNotNull(result);
        assertEquals("Complex Task", result);
    }

    @Test
    void testReverseNavigationPath() {
        Result mockResult = mock(Result.class);
        Row mockRow1 = mock(Row.class);
        Row mockRow2 = mock(Row.class);
        
        when(mockRow1.get("ID")).thenReturn("msg-1");
        when(mockRow1.get("role")).thenReturn("user");
        when(mockRow1.get("message")).thenReturn("First message");
        when(mockRow1.keySet()).thenReturn(Set.of("ID", "role", "message"));
        
        when(mockRow2.get("ID")).thenReturn("msg-2");
        when(mockRow2.get("role")).thenReturn("assistant");
        when(mockRow2.get("message")).thenReturn("Second message");
        when(mockRow2.keySet()).thenReturn(Set.of("ID", "role", "message"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow1, mockRow2).iterator());
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:Tasks('task-456')/botInstances/messages", context);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    // 1.6 测试复合键实体
    @Test
    void testCompositeKeyEntity() {
        Result mockResult = mock(Result.class);
        Row mockRow = mock(Row.class);
        
        when(mockRow.get("taskId")).thenReturn("task-123");
        when(mockRow.get("path")).thenReturn("context.result");
        when(mockRow.get("type")).thenReturn("text");
        when(mockRow.get("value")).thenReturn("Success");
        when(mockRow.keySet()).thenReturn(Set.of("taskId", "path", "type", "value"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow).iterator());
        when(mockResult.first()).thenReturn(Optional.of(mockRow));
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:ContextNodes(taskId='task-123',path='context.result')", context);
        
        assertNotNull(result);
        assertTrue(result.contains("task-123") || result.contains("context.result"));
    }

    // 测试各种查询选项
    @Test
    void testSelectQueryOption() {
        Result mockResult = mock(Result.class);
        Row mockRow = mock(Row.class);
        
        when(mockRow.get("ID")).thenReturn("bot-1");
        when(mockRow.get("status_code")).thenReturn("RUNNING");
        when(mockRow.get("sequence")).thenReturn(1);
        when(mockRow.keySet()).thenReturn(Set.of("ID", "status_code", "sequence"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow).iterator());
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:BotInstances?$select=ID,status_code,sequence", context);
        
        assertNotNull(result);
        assertTrue(result.contains("bot-1"));
    }

    @Test
    void testFilterQueryOption() {
        Result mockResult = mock(Result.class);
        Row mockRow = mock(Row.class);
        
        when(mockRow.get("ID")).thenReturn("bot-1");
        when(mockRow.get("status_code")).thenReturn("RUNNING");
        when(mockRow.keySet()).thenReturn(Set.of("ID", "status_code"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow).iterator());
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:BotInstances?$filter=status_code eq 'RUNNING'", context);
        
        assertNotNull(result);
        assertTrue(result.contains("RUNNING"));
    }

    @Test
    void testComplexFilterQueryOption() {
        Result mockResult = mock(Result.class);
        Row mockRow = mock(Row.class);
        
        when(mockRow.get("ID")).thenReturn("task-1");
        when(mockRow.get("name")).thenReturn("Main Task");
        when(mockRow.get("isMain")).thenReturn(true);
        when(mockRow.get("sequence")).thenReturn(10);
        when(mockRow.keySet()).thenReturn(Set.of("ID", "name", "isMain", "sequence"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow).iterator());
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:Tasks?$filter=isMain eq true and sequence gt 5", context);
        
        assertNotNull(result);
        assertTrue(result.contains("Main Task"));
    }

    @Test
    void testOrderByQueryOption() {
        Result mockResult = mock(Result.class);
        Row mockRow1 = mock(Row.class);
        Row mockRow2 = mock(Row.class);
        
        when(mockRow1.get("ID")).thenReturn("bot-1");
        when(mockRow1.get("sequence")).thenReturn(1);
        when(mockRow1.keySet()).thenReturn(Set.of("ID", "sequence"));
        
        when(mockRow2.get("ID")).thenReturn("bot-2");
        when(mockRow2.get("sequence")).thenReturn(2);
        when(mockRow2.keySet()).thenReturn(Set.of("ID", "sequence"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow1, mockRow2).iterator());
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:BotInstances?$orderby=sequence asc,createdAt desc", context);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testTopQueryOption() {
        Result mockResult = mock(Result.class);
        Row mockRow = mock(Row.class);
        
        when(mockRow.get("ID")).thenReturn("bot-1");
        when(mockRow.get("status_code")).thenReturn("RUNNING");
        when(mockRow.keySet()).thenReturn(Set.of("ID", "status_code"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow).iterator());
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:BotInstances?$top=10", context);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testSkipQueryOption() {
        Result mockResult = mock(Result.class);
        Row mockRow = mock(Row.class);
        
        when(mockRow.get("ID")).thenReturn("bot-21");
        when(mockRow.get("status_code")).thenReturn("RUNNING");
        when(mockRow.keySet()).thenReturn(Set.of("ID", "status_code"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow).iterator());
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:BotInstances?$skip=20", context);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testExpandQueryOption() {
        Result mockResult = mock(Result.class);
        Row mockRow = mock(Row.class);
        
        when(mockRow.get("ID")).thenReturn("bot-1");
        when(mockRow.get("status_code")).thenReturn("RUNNING");
        when(mockRow.keySet()).thenReturn(Set.of("ID", "status_code"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow).iterator());
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:BotInstances?$expand=messages,task", context);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    // 测试组合查询
    @Test
    void testComplexCombinedQuery() {
        Result mockResult = mock(Result.class);
        Row mockRow1 = mock(Row.class);
        Row mockRow2 = mock(Row.class);
        
        when(mockRow1.get("ID")).thenReturn("bot-1");
        when(mockRow1.get("status_code")).thenReturn("RUNNING");
        when(mockRow1.get("sequence")).thenReturn(1);
        when(mockRow1.keySet()).thenReturn(Set.of("ID", "status_code", "sequence"));
        
        when(mockRow2.get("ID")).thenReturn("bot-2");
        when(mockRow2.get("status_code")).thenReturn("COMPLETED");
        when(mockRow2.get("sequence")).thenReturn(2);
        when(mockRow2.keySet()).thenReturn(Set.of("ID", "status_code", "sequence"));
        
        when(mockResult.iterator()).thenReturn(List.of(mockRow1, mockRow2).iterator());
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:BotInstances?$select=ID,status_code,sequence&$filter=status_code ne 'FAILED'&$orderby=sequence asc&$top=20&$skip=10&$expand=task", context);
        
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.contains("limited to 20 results") || result.contains("bot-1") || result.contains("bot-2"));
    }

    // 基础功能测试
    @Test
    void testParseUrlWithCdsModel() {
        // 验证CdsModel中包含预期的实体
        assertNotNull(cdsModel, "CdsModel should be injected");
        
        // 检查MainService实体是否存在
        Optional<CdsEntity> botInstancesEntity = cdsModel.findEntity("MainService.BotInstances");
        Optional<CdsEntity> tasksEntity = cdsModel.findEntity("MainService.Tasks");
        
        assertTrue(botInstancesEntity.isPresent() || 
                  cdsModel.findEntity("BotInstances").isPresent(), 
                  "BotInstances entity should exist in CdsModel");
        assertTrue(tasksEntity.isPresent() || 
                  cdsModel.findEntity("Tasks").isPresent(), 
                  "Tasks entity should exist in CdsModel");
        
        // 测试URL解析
        ODataUrlParserEnhanced.ODataUrlInfo urlInfo = ODataUrlParserEnhanced.parseUrl("BotInstances", cdsModel);
        assertNotNull(urlInfo);
        assertEquals("BotInstances", urlInfo.getEntitySet());
        assertEquals(ODataUrlParserEnhanced.ODataRequestType.ENTITY_SET, urlInfo.getRequestType());
    }

    // @Test
    // void testErrorHandling() {
    //     // 测试错误处理
    //     when(persistenceService.run(any())).thenThrow(new RuntimeException("Database error"));

    //     VariableContext context = new VariableContext();
    //     String result = resolver.resolve("OData:BotInstances", context);
        
    //     // 应该返回空字符串而不是抛出异常
    //     assertEquals("", result);
    // }

    // @Test
    // void testEmptyResult() {
    //     // 测试空结果
    //     Result mockResult = mock(Result.class);
    //     when(mockResult.iterator()).thenReturn(Collections.emptyIterator());
    //     when(persistenceService.run(any())).thenReturn(mockResult);

    //     VariableContext context = new VariableContext();
    //     String result = resolver.resolve("OData:BotInstances", context);
        
    //     assertEquals("", result);
    // }

    @Test
    void testUrlParserIntegration() {
        // 测试URL解析器集成
        
        // 测试实体集合
        ODataUrlParserEnhanced.ODataUrlInfo info1 = ODataUrlParserEnhanced.parseUrl("BotInstances", cdsModel);
        assertNotNull(info1);
        assertEquals("BotInstances", info1.getEntitySet());
        assertEquals(ODataUrlParserEnhanced.ODataRequestType.ENTITY_SET, info1.getRequestType());
        assertTrue(info1.isCollection());

        // 测试单个实体
        ODataUrlParserEnhanced.ODataUrlInfo info2 = ODataUrlParserEnhanced.parseUrl("BotInstances('bot-123')", cdsModel);
        assertNotNull(info2);
        assertEquals("BotInstances", info2.getEntitySet());
        assertEquals(ODataUrlParserEnhanced.ODataRequestType.SINGLE_ENTITY, info2.getRequestType());
        assertFalse(info2.getKeys().isEmpty());
        assertEquals("bot-123", info2.getKeys().get("ID"));

        // 测试查询参数
        ODataUrlParserEnhanced.ODataUrlInfo info3 = ODataUrlParserEnhanced.parseUrl("BotInstances?$select=ID,status_code&$top=10", cdsModel);
        assertNotNull(info3);
        assertEquals("ID,status_code", info3.getSelect());
        assertEquals("10", info3.getTop());
        assertEquals(Integer.valueOf(10), info3.getTopValue());

        // 测试导航路径
        ODataUrlParserEnhanced.ODataUrlInfo info4 = ODataUrlParserEnhanced.parseUrl("BotInstances/bot-123/messages", cdsModel);
        assertNotNull(info4);
        assertEquals("BotInstances", info4.getEntitySet());
        assertFalse(info4.getNavigationPath().isEmpty());
    }

    // 测试设计文档中的具体示例
    @Test
    void testDocumentationExamples() {
        // 示例1: 获取导航属性 /BotInstances/bot-123/task/task-123/messages
        Result mockResult1 = mock(Result.class);
        Row mockRow1 = mock(Row.class);
        when(mockRow1.get("ID")).thenReturn("msg-1");
        when(mockRow1.get("content")).thenReturn("Message content");
        when(mockRow1.keySet()).thenReturn(Set.of("ID", "content"));
        when(mockResult1.iterator()).thenReturn(List.of(mockRow1).iterator());
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult1);

        VariableContext context = new VariableContext();
        String result1 = resolver.resolve("OData:BotInstances/bot-123/task/task-123/messages", context);
        assertNotNull(result1);
        assertFalse(result1.isEmpty());

        // 示例2: 获取单个导航属性 /BotInstances/bot-123/task/task-123/messages/message-1
        Result mockResult2 = mock(Result.class);
        Row mockRow2 = mock(Row.class);
        when(mockRow2.get("ID")).thenReturn("message-1");
        when(mockRow2.get("content")).thenReturn("Specific message");
        when(mockRow2.keySet()).thenReturn(Set.of("ID", "content"));
        when(mockResult2.iterator()).thenReturn(List.of(mockRow2).iterator());
        when(mockResult2.first()).thenReturn(Optional.of(mockRow2));
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult2);

        String result2 = resolver.resolve("OData:BotInstances/bot-123/task/task-123/messages/message-1", context);
        assertNotNull(result2);
        assertTrue(result2.contains("message-1"));

        // 示例3: 获取字段 /BotInstances/bot-123/tasks/task-123/messages/message-1/content
        Result mockResult3 = mock(Result.class);
        Row mockRow3 = mock(Row.class);
        when(mockRow3.get("content")).thenReturn("Message content value");
        when(mockRow3.keySet()).thenReturn(Set.of("content"));
        when(mockResult3.iterator()).thenReturn(List.of(mockRow3).iterator());
        when(mockResult3.first()).thenReturn(Optional.of(mockRow3));
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult3);

        String result3 = resolver.resolve("OData:BotInstances/bot-123/tasks/task-123/messages/message-1/content", context);
        assertNotNull(result3);
        assertEquals("Message content value", result3);
    }

    @Test
    void testOneToOneNavigationExamples() {
        // 示例4: 获取一对一导航属性 /BotInstances/bot-123/type/model
        Result mockResult = mock(Result.class);
        Row mockRow = mock(Row.class);
        when(mockRow.get("model")).thenReturn("GPT-4");
        when(mockRow.keySet()).thenReturn(Set.of("model"));
        when(mockResult.iterator()).thenReturn(List.of(mockRow).iterator());
        when(mockResult.first()).thenReturn(Optional.of(mockRow));
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult);

        VariableContext context = new VariableContext();
        String result = resolver.resolve("OData:BotInstances/bot-123/type/model", context);
        assertNotNull(result);
        assertEquals("GPT-4", result);

        // 示例5: 获取一对一导航属性下的字段 /BotInstances/bot123/type/model/field1
        Result mockResult2 = mock(Result.class);
        Row mockRow2 = mock(Row.class);
        when(mockRow2.get("field1")).thenReturn("Field value");
        when(mockRow2.keySet()).thenReturn(Set.of("field1"));
        when(mockResult2.iterator()).thenReturn(List.of(mockRow2).iterator());
        when(mockResult2.first()).thenReturn(Optional.of(mockRow2));
        when(persistenceService.run(any(CqnSelect.class))).thenReturn(mockResult2);

        String result2 = resolver.resolve("OData:BotInstances/bot123/type/model/field1", context);
        assertNotNull(result2);
        assertEquals("Field value", result2);
    }
}
