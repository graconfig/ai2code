package customer.ai2code.service.execution.functioncall.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;

import com.fasterxml.jackson.databind.ObjectMapper;

import customer.ai2code.model.execution.TaskCreationParam;
import customer.ai2code.model.execution.functioncall.FunctionInfo;
import customer.ai2code.service.TaskBotDataService;
import customer.ai2code.service.TaskService;
import customer.ai2code.service.execution.functioncall.FunctionCallProcessor;
import customer.ai2code.service.execution.functioncall.adapter.OpenAIFunctionCallAdapter;
import customer.ai2code.service.impl.execution.CreateTasksBotExecution;

@ExtendWith(MockitoExtension.class)
public class OpenAIFunctionCallIntegrationTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private TaskService taskService;

    private FunctionCallProcessor functionCallProcessor;
    private OpenAIFunctionCallAdapter openAIFunctionCallAdapter;
    private ObjectMapper objectMapper;
    private CreateTasksBotExecution createTasksBotExecution;
    private ApplicationEventPublisher eventPublisher;
    private TaskBotDataService taskBotDataService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        functionCallProcessor = new FunctionCallProcessor(applicationContext, objectMapper);
        openAIFunctionCallAdapter = new OpenAIFunctionCallAdapter();
        createTasksBotExecution = new CreateTasksBotExecution(taskService, eventPublisher, taskBotDataService);
    }

    @Test
    void testEndToEndOpenAIFunctionCallGeneration() {
        System.out.println("=== End-to-End OpenAI Function Call Generation Test ===");

        // Step 1: Extract function info from bot execution instance
        List<FunctionInfo> functionInfos = functionCallProcessor
                .extractFunctionInfosFromInstance(createTasksBotExecution);

        System.out.println("Step 1 - Extracted " + functionInfos.size() + " functions");
        assertFalse(functionInfos.isEmpty(), "Should extract at least one function");

        // Step 2: Convert to OpenAI format
        List<Map<String, Object>> openAIFunctions = openAIFunctionCallAdapter
                .convertToOpenAIFormat(functionInfos);

        System.out.println("Step 2 - Converted to OpenAI format");
        assertEquals(1, openAIFunctions.size(), "Should have one function");

        // Step 3: Verify the OpenAI format structure (without using SAP SDK classes)
        Map<String, Object> function = openAIFunctions.get(0);

        // Verification of basic structure
        assertEquals("Create_Tasks_Bot_Execution_execute", function.get("name"));
        assertEquals("Implementation for creating tasks in the bot execution framework",
                function.get("description"));

        assertNotNull(function.get("parameters"), "Should have parameters");

        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) function.get("parameters");
        assertEquals("object", parameters.get("type"));

        assertTrue(parameters.containsKey("properties"), "Should contain properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) parameters.get("properties");
        assertTrue(properties.containsKey("botInstanceId"));
        assertTrue(properties.containsKey("taskCreationParams"));

        System.out.println("Final OpenAI Function Configuration:");
        System.out.println("- Function Name: " + function.get("name"));
        System.out.println("- Parameters Type: " + parameters.get("type"));
        System.out.println("- Properties Count: " + properties.size());

        try {
            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(function);
            System.out.println("Complete Function JSON:");
            System.out.println(jsonOutput);
        } catch (Exception e) {
            System.out.println("Failed to serialize function to JSON: " + e.getMessage());
        }

        System.out.println("=== End-to-End Test Passed ===\n");
    }

    @Test
    void testOpenAIFunctionStructureValidation() {
        System.out.println("=== OpenAI Function Structure Validation Test ===");

        // Extract and convert function info
        List<FunctionInfo> functionInfos = functionCallProcessor
                .extractFunctionInfosFromInstance(createTasksBotExecution);
        List<Map<String, Object>> openAIFunctions = openAIFunctionCallAdapter
                .convertToOpenAIFormat(functionInfos);

        Map<String, Object> function = openAIFunctions.get(0);

        // Validate required OpenAI function calling structure
        assertTrue(function.containsKey("name"), "Must have 'name' field");
        assertTrue(function.containsKey("description"), "Must have 'description' field");
        assertTrue(function.containsKey("parameters"), "Must have 'parameters' field");

        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) function.get("parameters");
        assertEquals("object", parameters.get("type"), "Parameters type must be 'object'");
        assertTrue(parameters.containsKey("properties"), "Parameters must have 'properties'");

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) parameters.get("properties");

        // Validate specific parameters
        assertTrue(properties.containsKey("botInstanceId"), "Must have botInstanceId parameter");
        assertTrue(properties.containsKey("taskCreationParams"), "Must have taskCreationParams parameter");

        // Validate botInstanceId parameter structure
        @SuppressWarnings("unchecked")
        Map<String, Object> botInstanceIdParam = (Map<String, Object>) properties.get("botInstanceId");
        assertEquals("string", botInstanceIdParam.get("type"));
        assertNotNull(botInstanceIdParam.get("description"));

        // Validate taskCreationParam parameter structure
        @SuppressWarnings("unchecked")
        Map<String, Object> taskCreationParamParam = (Map<String, Object>) properties.get("taskCreationParams");
        assertEquals("array", taskCreationParamParam.get("type"));
        assertNotNull(taskCreationParamParam.get("description"));

        System.out.println("OpenAI Function Structure Validation:");
        System.out.println("✓ Has required fields: name, description, parameters");
        System.out.println("✓ Parameters type is 'object'");
        System.out.println("✓ Has properties with correct parameter definitions");
        System.out.println("✓ Parameter types are correctly mapped");

        System.out.println("=== Structure Validation Test Passed ===\n");
    }

    // @Test
    // void testSimulatedFunctionCallExecution() throws Exception {
    // System.out.println("=== Simulated Function Call Execution Test ===");

    // // Simulate OpenAI function call response
    // String functionName = "Create_Tasks_Bot_Execution_execute";
    // Map<String, Object> arguments = new HashMap<>();
    // arguments.put("botInstanceId", "test-bot-123");

    // // Create test task parameters
    // List<Map<String, Object>> taskParamMaps = new ArrayList<>();
    // Map<String, Object> taskParam1 = new HashMap<>();
    // taskParam1.put("sequence", 1);
    // taskParam1.put("name", "Task 1");
    // taskParam1.put("description", "First test task");
    // taskParam1.put("contextPath", "/test/context1");
    // taskParamMaps.add(taskParam1);

    // Map<String, Object> taskParam2 = new HashMap<>();
    // taskParam2.put("sequence", 2);
    // taskParam2.put("name", "Task 2");
    // taskParam2.put("description", "Second test task");
    // taskParam2.put("contextPath", "/test/context2");
    // taskParamMaps.add(taskParam2);

    // arguments.put("taskCreationParam", taskParamMaps);

    // System.out.println("Simulated arguments: " + arguments);

    // // Mock TaskService behavior
    // Task mockTask1 = new GenericTask(Tasks.of(taskParam1));
    // // mockTask1.setId("task-1");
    // // mockTask1.setName("Task 1");
    // // mockTask1

    // Task mockTask2 = new GenericTask(Tasks.of(taskParam2));
    // // mockTask2.setId("task-2");
    // // mockTask2.setName("Task 2");

    // when(taskService.createTaskWithBots(eq("test-bot-123"), eq("Task 1"),
    // eq("First test task"), eq("/test/context1"), eq(1)))
    // .thenReturn(mockTask1);

    // when(taskService.createTaskWithBots(eq("test-bot-123"), eq("Task 2"),
    // eq("Second test task"), eq("/test/context2"), eq(2)))
    // .thenReturn(mockTask2);

    // // Execute function call
    // Object result = functionCallProcessor.executeFunctionCallOnInstance(
    // functionName, arguments, createTasksBotExecution);

    // // Verify result
    // assertNotNull(result, "Function call should return a result");
    // assertTrue(result instanceof List, "Result should be a List");

    // @SuppressWarnings("unchecked")
    // List<Task> tasks = (List<Task>) result;
    // assertEquals(2, tasks.size(), "Should create 2 tasks");

    // System.out.println("Function execution result: " + tasks.size() + " tasks
    // created");
    // System.out.println("Task 1 ID: " + tasks.get(0).getTask().getId());
    // System.out.println("Task 2 ID: " + tasks.get(1).getTask().getId());

    // // Verify TaskService was called correctly
    // verify(taskService).createTaskWithBots("test-bot-123", "Task 1",
    // "First test task", "/test/context1", 1);
    // verify(taskService).createTaskWithBots("test-bot-123", "Task 2",
    // "Second test task", "/test/context2", 2);

    // System.out.println("=== Simulated Function Call Execution Test Passed
    // ===\n");
    // }

    @Test
    void testComplexParameterConversion() throws Exception {
        System.out.println("=== Complex Parameter Conversion Test ===");

        // Test conversion of complex objects from Map to TaskCreationParam
        Map<String, Object> taskParamMap = new HashMap<>();
        taskParamMap.put("sequence", 1);
        taskParamMap.put("name", "Complex Task");
        taskParamMap.put("description", "A task with complex parameters");
        taskParamMap.put("contextPath", "/complex/context");

        // Convert using ObjectMapper (same logic as in FunctionCallProcessor)
        String json = objectMapper.writeValueAsString(taskParamMap);
        TaskCreationParam convertedParam = objectMapper.readValue(json, TaskCreationParam.class);

        // Verify conversion
        assertEquals(1, convertedParam.getSequence());
        assertEquals("Complex Task", convertedParam.getName());
        assertEquals("A task with complex parameters", convertedParam.getDescription());
        assertEquals("/complex/context", convertedParam.getContextPath());

        System.out.println("Complex parameter conversion successful:");
        System.out.println("- Original Map: " + taskParamMap);
        System.out.println("- Converted Object: " + convertedParam);

        System.out.println("=== Complex Parameter Conversion Test Passed ===");
    }

    @Test
    void testOpenAIParametersCompatibility() {
        System.out.println("=== OpenAI Parameters Compatibility Test ===");

        // Extract and convert function info
        List<FunctionInfo> functionInfos = functionCallProcessor
                .extractFunctionInfosFromInstance(createTasksBotExecution);
        List<Map<String, Object>> openAIFunctions = openAIFunctionCallAdapter
                .convertToOpenAIFormat(functionInfos);

        // Test that the generated parameters are compatible with OpenAI API format
        Map<String, Object> function = openAIFunctions.get(0);

        try {
            // Test serialization - this should work for OpenAI API
            String functionJson = objectMapper.writeValueAsString(function);
            assertNotNull(functionJson);
            assertTrue(functionJson.length() > 0);

            // Test deserialization back to Map
            @SuppressWarnings("unchecked")
            Map<String, Object> deserializedFunction = objectMapper.readValue(functionJson, Map.class);
            assertEquals(function.get("name"), deserializedFunction.get("name"));
            assertEquals(function.get("description"), deserializedFunction.get("description"));

            System.out.println("OpenAI Parameters Compatibility verified:");
            System.out.println("✓ Serializable to JSON");
            System.out.println("✓ Deserializable from JSON");
            System.out.println("✓ Maintains structure integrity");

            // Print the final JSON that would be sent to OpenAI
            System.out.println("Final OpenAI Function JSON:");
            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(function));

        } catch (Exception e) {
            fail("OpenAI parameters should be serializable: " + e.getMessage());
        }

        System.out.println("=== OpenAI Parameters Compatibility Test Passed ===");
    }
}