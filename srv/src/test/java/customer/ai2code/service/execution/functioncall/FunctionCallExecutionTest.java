package customer.ai2code.service.execution.functioncall;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import cds.gen.mainservice.Tasks;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.task.GenericTask;
import customer.ai2code.model.task.Task;
import customer.ai2code.service.TaskService;
import customer.ai2code.service.impl.CreateTasksBotExecution;

@ExtendWith(MockitoExtension.class)
class FunctionCallExecutionTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private TaskService taskService;

    private FunctionCallProcessor functionCallProcessor;
    private ObjectMapper objectMapper;
    private CreateTasksBotExecution createTasksBotExecution;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        functionCallProcessor = new FunctionCallProcessor(applicationContext, objectMapper);
        createTasksBotExecution = new CreateTasksBotExecution(taskService);
    }

    // @Test
    // void testExecuteFunctionCallOnInstance_Success() throws Exception {
    //     System.out.println("=== Testing executeFunctionCallOnInstance - Success Case ===");

    //     // Given - 准备测试数据
    //     String functionName = "Create_Tasks_Bot_Execution_execute";
    //     String argumentsJson = """
    //             {
    //                 "botInstanceId": "test-bot-123",
    //                 "taskCreationParams": [
    //                     {
    //                         "sequence": 1,
    //                         "name": "Test Task 1",
    //                         "description": "First test task",
    //                         "contextPath": "SubContext:test.context[1]"
    //                     },
    //                     {
    //                         "sequence": 2,
    //                         "name": "Test Task 2",
    //                         "description": "Second test task",
    //                         "contextPath": "SubContext:test.context[2]"
    //                     }
    //                 ]
    //             }
    //             """;

    //     // Mock TaskService 返回结果
    //     Map<String, Object> taskParam1 = new HashMap<>();
    //     taskParam1.put("ID", "task-1");
    //     taskParam1.put("sequence", 1);
    //     taskParam1.put("name", "Test Task 1");
    //     taskParam1.put("description", "First test task");
    //     taskParam1.put("contextPath", "/test/context1");
    //     Task mockTask1 = new GenericTask(Tasks.of(taskParam1));

    //     Map<String, Object> taskParam2 = new HashMap<>();
    //     taskParam2.put("ID", "task-2");
    //     taskParam2.put("sequence", 2);
    //     taskParam2.put("name", "Test Task 2");
    //     taskParam2.put("description", "Second test task");
    //     taskParam2.put("contextPath", "/test/context2");
    //     Task mockTask2 = new GenericTask(Tasks.of(taskParam2));

    //     when(taskService.createTaskWithBots(eq("test-bot-123"), eq("Test Task 1"),
    //             eq("First test task"), eq("/test/context1"), eq(1)))
    //             .thenReturn(mockTask1);

    //     when(taskService.createTaskWithBots(eq("test-bot-123"), eq("Test Task 2"),
    //             eq("Second test task"), eq("/test/context2"), eq(2)))
    //             .thenReturn(mockTask2);

    //     // When - 执行函数调用
    //     Object result = functionCallProcessor.executeFunctionCallOnInstance(
    //             functionName, argumentsJson, createTasksBotExecution);

    //     // Then - 验证结果
    //     assertNotNull(result, "Function call result should not be null");
    //     assertTrue(result instanceof List, "Result should be a List");

    //     @SuppressWarnings("unchecked")
    //     List<Task> tasks = (List<Task>) result;
    //     assertEquals(2, tasks.size(), "Should create 2 tasks");

    //     // 验证第一个任务
    //     Task task1 = tasks.get(0);
    //     assertEquals("task-1", task1.getTask().getId());
    //     assertEquals("Test Task 1", task1.getTask().getName());
    //     assertEquals("First test task", task1.getTask().getDescription());

    //     // 验证第二个任务
    //     Task task2 = tasks.get(1);
    //     assertEquals("task-2", task2.getTask().getId());
    //     assertEquals("Test Task 2", task2.getTask().getName());
    //     assertEquals("Second test task", task2.getTask().getDescription());

    //     // 验证 TaskService 被正确调用
    //     verify(taskService).createTaskWithBots("test-bot-123", "Test Task 1",
    //             "First test task", "/test/context1", 1);
    //     verify(taskService).createTaskWithBots("test-bot-123", "Test Task 2",
    //             "Second test task", "/test/context2", 2);

    //     System.out.println("✅ Function call executed successfully:");
    //     System.out.println("- Created " + tasks.size() + " tasks");
    //     System.out.println("- Task 1 ID: " + task1.getTask().getId());
    //     System.out.println("- Task 2 ID: " + task2.getTask().getId());
    //     System.out.println("=== Success Case Test Passed ===\n");
    // }

    @Test
    void testExecuteFunctionCallOnInstance_WithNullBotInstanceId() throws Exception {
        System.out.println("=== Testing executeFunctionCallOnInstance - Null BotInstanceId ===");

        // Given - botInstanceId 为 null
        String functionName = "Create_Tasks_Bot_Execution_execute";
        String argumentsJson = """
                {
                    "botInstanceId": null,
                    "taskCreationParams": [
                        {
                            "sequence": 1,
                            "name": "Test Task",
                            "description": "Test task with null bot instance",
                            "contextPath": "/test/context"
                        }
                    ]
                }
                """;

        // 移除不必要的 Mock 设置，因为会在验证阶段就抛出异常

        // When & Then - 应该抛出 BusinessException (可能被包装在其他异常中)
        Exception exception = assertThrows(Exception.class, () -> {
            functionCallProcessor.executeFunctionCallOnInstance(
                    functionName, argumentsJson, createTasksBotExecution);
        });

        // 检查异常链，找到 BusinessException
        Throwable cause = exception;
        BusinessException businessException = null;
        while (cause != null) {
            if (cause instanceof BusinessException) {
                businessException = (BusinessException) cause;
                break;
            }
            cause = cause.getCause();
        }

        assertNotNull(businessException, "Should contain a BusinessException in the exception chain");
        assertEquals("Bot instance ID cannot be null or empty", businessException.getMessage());
    }

    @Test
    void testExecuteFunctionCallOnInstance_EmptyTaskList() throws Exception {
        System.out.println("=== Testing executeFunctionCallOnInstance - Empty Task List ===");

        // Given - 空的任务列表（应该抛出异常）
        String functionName = "Create_Tasks_Bot_Execution_execute";
        String argumentsJson = """
                {
                    "botInstanceId": "test-bot-123",
                    "taskCreationParams": []
                }
                """;

        // When & Then - 应该抛出 BusinessException (可能被包装在其他异常中)
        Exception exception = assertThrows(Exception.class, () -> {
            functionCallProcessor.executeFunctionCallOnInstance(
                    functionName, argumentsJson, createTasksBotExecution);
        });

        // 检查异常链，找到 BusinessException
        Throwable cause = exception;
        BusinessException businessException = null;
        while (cause != null) {
            if (cause instanceof BusinessException) {
                businessException = (BusinessException) cause;
                break;
            }
            cause = cause.getCause();
        }

        assertNotNull(businessException, "Should contain a BusinessException in the exception chain");
        assertEquals("Task creation parameters cannot be null or empty", businessException.getMessage());

        // 验证 TaskService 没有被调用
        verify(taskService, never()).createTaskWithBots(anyString(), anyString(), anyString(), anyString(), anyInt());

        System.out.println("✅ Empty task list validation works correctly");
        System.out.println("- Exception message: " + businessException.getMessage());
        System.out.println("=== Empty Task List Test Passed ===\n");
    }

    @Test
    void testExecuteFunctionCallOnInstance_InvalidJson() throws Exception {
        System.out.println("=== Testing executeFunctionCallOnInstance - Invalid JSON ===");

        // Given - 无效的 JSON
        String functionName = "Create_Tasks_Bot_Execution_execute";
        String invalidJson = """
                {
                    "botInstanceId": "test",
                    "taskCreationParams": [
                        invalid json here
                    ]
                }
                """;

        // When & Then - 应该抛出 BusinessException (可能被包装在其他异常中)
        Exception exception = assertThrows(Exception.class, () -> {
            functionCallProcessor.executeFunctionCallOnInstance(
                    functionName, invalidJson, createTasksBotExecution);
        });

        // 检查异常链，找到包含解析错误信息的异常
        boolean foundParseError = false;
        Throwable cause = exception;
        while (cause != null) {
            if (cause.getMessage() != null && 
                (cause.getMessage().contains("Failed to parse function arguments") ||
                 cause.getMessage().contains("Failed to execute function call") ||
                 cause.getMessage().contains("parse") ||
                 cause.getMessage().contains("JSON"))) {
                foundParseError = true;
                break;
            }
            cause = cause.getCause();
        }

        assertTrue(foundParseError, "Should contain parse error information in exception chain");

        System.out.println("✅ Invalid JSON handled correctly");
        System.out.println("- Exception message: " + exception.getMessage());
        System.out.println("=== Invalid JSON Test Passed ===\n");
    }

    @Test
    void testExecuteFunctionCallOnInstance_TaskServiceException() throws Exception {
        System.out.println("=== Testing executeFunctionCallOnInstance - TaskService Exception ===");

        // Given
        String functionName = "Create_Tasks_Bot_Execution_execute";
        String argumentsJson = """
                {
                    "botInstanceId": "test-bot-123",
                    "taskCreationParams": [
                        {
                            "sequence": 1,
                            "name": "Failing Task",
                            "description": "This task will fail",
                            "contextPath": "/test/context"
                        }
                    ]
                }
                """;

        // Mock TaskService 抛出异常
        when(taskService.createTaskWithBots(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then - 应该抛出异常 (可能被包装)
        Exception exception = assertThrows(Exception.class, () -> {
            functionCallProcessor.executeFunctionCallOnInstance(
                    functionName, argumentsJson, createTasksBotExecution);
        });

        // 检查异常链，找到相关的错误信息
        boolean foundTaskError = false;
        boolean foundDatabaseError = false;
        Throwable cause = exception;
        while (cause != null) {
            if (cause.getMessage() != null) {
                if (cause.getMessage().contains("Failed to create task: Failing Task")) {
                    foundTaskError = true;
                }
                if (cause.getMessage().contains("Database connection failed")) {
                    foundDatabaseError = true;
                }
            }
            cause = cause.getCause();
        }

        assertTrue(foundTaskError || foundDatabaseError, 
                   "Should contain task creation or database error information");

        System.out.println("✅ TaskService exception handled correctly");
        System.out.println("- Exception message: " + exception.getMessage());
        System.out.println("=== TaskService Exception Test Passed ===\n");
    }

    @Test
    void testExecuteFunctionCallOnInstance_ComplexParameterTypes() throws Exception {
        System.out.println("=== Testing executeFunctionCallOnInstance - Complex Parameter Types ===");

        // Given - 包含复杂嵌套数据的参数
        String functionName = "Create_Tasks_Bot_Execution_execute";
        String argumentsJson = """
                {
                    "botInstanceId": "complex-bot-456",
                    "taskCreationParams": [
                        {
                            "sequence": 1,
                            "name": "Complex Task 1",
                            "description": "Task with special characters: áéíóú & <html>",
                            "contextPath": "/complex/path/with/special-chars_123"
                        },
                        {
                            "sequence": 10,
                            "name": "High Priority Task",
                            "description": "Task with high sequence number",
                            "contextPath": "/high/priority/context"
                        }
                    ]
                }
                """;

        // Mock TaskService
        Map<String, Object> taskParam1 = new HashMap<>();
        taskParam1.put("ID", "complex-task-1");
        taskParam1.put("sequence", 1);
        taskParam1.put("name", "Complex Task 1");
        taskParam1.put("description", "Task with special characters: áéíóú & <html>");
        taskParam1.put("contextPath", "/complex/path/with/special-chars_123");
        Task mockTask1 = new GenericTask(Tasks.of(taskParam1));

        Map<String, Object> taskParam2 = new HashMap<>();
        taskParam2.put("ID", "complex-task-2");
        taskParam2.put("sequence", 10);
        taskParam2.put("name", "High Priority Task");
        taskParam2.put("description", "Task with high sequence number");
        taskParam2.put("contextPath", "/high/priority/context");
        Task mockTask2 = new GenericTask(Tasks.of(taskParam2));

        when(taskService.createTaskWithBots(eq("complex-bot-456"), eq("Complex Task 1"),
                eq("Task with special characters: áéíóú & <html>"), eq("/complex/path/with/special-chars_123"), eq(1)))
                .thenReturn(mockTask1);

        when(taskService.createTaskWithBots(eq("complex-bot-456"), eq("High Priority Task"),
                eq("Task with high sequence number"), eq("/high/priority/context"), eq(10)))
                .thenReturn(mockTask2);

        // When
        Object result = functionCallProcessor.executeFunctionCallOnInstance(
                functionName, argumentsJson, createTasksBotExecution);

        // Then
        assertNotNull(result);
        assertTrue(result instanceof List);

        @SuppressWarnings("unchecked")
        List<Task> tasks = (List<Task>) result;
        assertEquals(2, tasks.size());

        // 验证复杂参数正确传递
        verify(taskService).createTaskWithBots("complex-bot-456", "Complex Task 1",
                "Task with special characters: áéíóú & <html>", "/complex/path/with/special-chars_123", 1);
        verify(taskService).createTaskWithBots("complex-bot-456", "High Priority Task",
                "Task with high sequence number", "/high/priority/context", 10);

        System.out.println("✅ Complex parameter types handled correctly");
        System.out.println("- Special characters preserved");
        System.out.println("- High sequence numbers handled");
        System.out.println("- Complex paths processed correctly");
        System.out.println("=== Complex Parameter Types Test Passed ===\n");
    }

    @Test
    void testExecuteFunctionCallOnInstance_LoggingEnabled() throws Exception {
        System.out.println("=== Testing executeFunctionCallOnInstance - Logging Enabled ===");

        // Given
        String functionName = "Create_Tasks_Bot_Execution_execute";
        String argumentsJson = """
                {
                    "botInstanceId": "logging-test-bot",
                    "taskCreationParams": [
                        {
                            "sequence": 1,
                            "name": "Logged Task",
                            "description": "Task for logging test",
                            "contextPath": "/logging/test"
                        }
                    ]
                }
                """;

        // Mock TaskService
        Map<String, Object> taskParam1 = new HashMap<>();
        taskParam1.put("ID", "logged-task");
        taskParam1.put("sequence", 1);
        taskParam1.put("name", "Logged Task");
        taskParam1.put("description", "Task for logging test");
        taskParam1.put("contextPath", "/logging/test");
        Task mockTask = new GenericTask(Tasks.of(taskParam1));

        when(taskService.createTaskWithBots(anyString(), anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(mockTask);

        // Capture system output (简单示例，实际项目中可能需要更复杂的日志捕获)
        System.out.println("Executing function call with logging enabled...");

        // When
        Object result = functionCallProcessor.executeFunctionCallOnInstance(
                functionName, argumentsJson, createTasksBotExecution);

        // Then
        assertNotNull(result);
        assertTrue(result instanceof List);

        @SuppressWarnings("unchecked")
        List<Task> tasks = (List<Task>) result;
        assertEquals(1, tasks.size());

        System.out.println("✅ Function executed with logging enabled");
        System.out.println("- Check console output for logging messages");
        System.out.println("=== Logging Enabled Test Passed ===\n");
    }

    @Test
    void testExecuteFunctionCallOnInstance_ParameterTypeConversion() throws Exception {
        System.out.println("=== Testing executeFunctionCallOnInstance - Parameter Type Conversion ===");

        // Given - 测试不同数据类型的转换
        String functionName = "Create_Tasks_Bot_Execution_execute";
        String argumentsJson = """
                {
                    "botInstanceId": "type-conversion-bot",
                    "taskCreationParams": [
                        {
                            "sequence": "5",
                            "name": "Type Conversion Task",
                            "description": "Testing parameter type conversion",
                            "contextPath": "/conversion/test"
                        }
                    ]
                }
                """;

        // Mock TaskService
        Map<String, Object> taskParam1 = new HashMap<>();
        taskParam1.put("ID", "conversion-task");
        taskParam1.put("sequence", 5);
        taskParam1.put("name", "Type Conversion Task");
        taskParam1.put("description", "Testing parameter type conversion");
        taskParam1.put("contextPath", "/conversion/test");
        Task mockTask = new GenericTask(Tasks.of(taskParam1));

        when(taskService.createTaskWithBots(eq("type-conversion-bot"), eq("Type Conversion Task"),
                eq("Testing parameter type conversion"), eq("/conversion/test"), eq(5)))
                .thenReturn(mockTask);

        // When
        Object result = functionCallProcessor.executeFunctionCallOnInstance(
                functionName, argumentsJson, createTasksBotExecution);

        // Then
        assertNotNull(result);
        assertTrue(result instanceof List);

        @SuppressWarnings("unchecked")
        List<Task> tasks = (List<Task>) result;
        assertEquals(1, tasks.size());

        // 验证字符串 "5" 被正确转换为 Integer 5
        verify(taskService).createTaskWithBots("type-conversion-bot", "Type Conversion Task",
                "Testing parameter type conversion", "/conversion/test", 5);

        System.out.println("✅ Parameter type conversion works correctly");
        System.out.println("- String '5' converted to Integer 5");
        System.out.println("=== Parameter Type Conversion Test Passed ===\n");
    }

    @Test
    void testExecuteFunctionCallOnInstance_Performance() throws Exception {
        System.out.println("=== Testing executeFunctionCallOnInstance - Performance ===");

        // Given - 大量任务创建
        String functionName = "Create_Tasks_Bot_Execution_execute";
        StringBuilder jsonBuilder = new StringBuilder();
        jsonBuilder.append("{\n");
        jsonBuilder.append("  \"botInstanceId\": \"performance-test-bot\",\n");
        jsonBuilder.append("  \"taskCreationParams\": [\n");

        int taskCount = 50; // 测试50个任务
        for (int i = 1; i <= taskCount; i++) {
            jsonBuilder.append("    {\n");
            jsonBuilder.append("      \"sequence\": ").append(i).append(",\n");
            jsonBuilder.append("      \"name\": \"Performance Task ").append(i).append("\",\n");
            jsonBuilder.append("      \"description\": \"Performance test task number ").append(i).append("\",\n");
            jsonBuilder.append("      \"contextPath\": \"/performance/test/").append(i).append("\"\n");
            jsonBuilder.append("    }");
            if (i < taskCount) {
                jsonBuilder.append(",");
            }
            jsonBuilder.append("\n");
        }

        jsonBuilder.append("  ]\n");
        jsonBuilder.append("}");

        String argumentsJson = jsonBuilder.toString();

        // Mock TaskService - 每次调用返回一个模拟任务
        for (int i = 1; i <= taskCount; i++) {
            Map<String, Object> taskParam = new HashMap<>();
            taskParam.put("ID", "perf-task-" + i);
            taskParam.put("sequence", i);
            taskParam.put("name", "Performance Task " + i);
            taskParam.put("description", "Performance test task number " + i);
            taskParam.put("contextPath", "/performance/test/" + i);
            Task mockTask = new GenericTask(Tasks.of(taskParam));

            when(taskService.createTaskWithBots(eq("performance-test-bot"), eq("Performance Task " + i),
                    eq("Performance test task number " + i), eq("/performance/test/" + i), eq(i)))
                    .thenReturn(mockTask);
        }

        // When - 测量执行时间
        long startTime = System.currentTimeMillis();

        Object result = functionCallProcessor.executeFunctionCallOnInstance(
                functionName, argumentsJson, createTasksBotExecution);

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // Then
        assertNotNull(result);
        assertTrue(result instanceof List);

        @SuppressWarnings("unchecked")
        List<Task> tasks = (List<Task>) result;
        assertEquals(taskCount, tasks.size());

        // 验证所有任务都被创建
        for (int i = 1; i <= taskCount; i++) {
            verify(taskService).createTaskWithBots("performance-test-bot", "Performance Task " + i,
                    "Performance test task number " + i, "/performance/test/" + i, i);
        }

        System.out.println("✅ Performance test completed successfully");
        System.out.println("- Created " + taskCount + " tasks");
        System.out.println("- Execution time: " + executionTime + " ms");
        System.out.println("- Average time per task: " + (executionTime / (double) taskCount) + " ms");

        // 性能断言（可根据实际需求调整）
        assertTrue(executionTime < 5000, "Execution should complete within 5 seconds for " + taskCount + " tasks");

        System.out.println("=== Performance Test Passed ===\n");
    }
}