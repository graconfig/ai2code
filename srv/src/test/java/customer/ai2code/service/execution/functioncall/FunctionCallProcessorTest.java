package customer.ai2code.service.execution.functioncall;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import customer.ai2code.model.execution.TaskCreationParam;
import customer.ai2code.model.execution.functioncall.FunctionInfo;
import customer.ai2code.model.execution.functioncall.ParameterInfo;
import customer.ai2code.service.TaskService;
import customer.ai2code.service.execution.functioncall.adapter.OpenAIFunctionCallAdapter;
import customer.ai2code.service.impl.CreateTasksBotExecution;

@ExtendWith(MockitoExtension.class)
public class FunctionCallProcessorTest {

    @Mock
    private ApplicationContext applicationContext;
    
    @Mock
    private TaskService taskService;

    private FunctionCallProcessor functionCallProcessor;
    private OpenAIFunctionCallAdapter openAIFunctionCallAdapter;
    private ObjectMapper objectMapper;
    private CreateTasksBotExecution createTasksBotExecution;

    @BeforeEach
    void setUp() {
        // 初始化真实的依赖
        objectMapper = new ObjectMapper();
        functionCallProcessor = new FunctionCallProcessor(applicationContext, objectMapper);
        openAIFunctionCallAdapter = new OpenAIFunctionCallAdapter();
        
        // 创建测试用的 Bot 执行实例
        createTasksBotExecution = new CreateTasksBotExecution(taskService);
    }

    @Test
    void testExtractFunctionInfosFromCreateTasksBotExecution() {
        // Given
        System.out.println("=== Testing Function Info Extraction ===");
        
        // When
        List<FunctionInfo> functionInfos = functionCallProcessor
            .extractFunctionInfosFromInstance(createTasksBotExecution);
        
        // Then
        assertNotNull(functionInfos, "Function infos should not be null");
        assertFalse(functionInfos.isEmpty(), "Should extract at least one function");
        
        FunctionInfo functionInfo = functionInfos.get(0);
        
        // 验证函数基本信息
        System.out.println("Function Name: " + functionInfo.getName());
        System.out.println("Function Description: " + functionInfo.getDescription());
        assertEquals("Create_Tasks_Bot_Execution_execute", functionInfo.getName());
        assertEquals("Implementation for creating tasks in the bot execution framework", functionInfo.getDescription());
        
        // 验证参数信息
        List<ParameterInfo> parameters = functionInfo.getParameters();
        assertNotNull(parameters, "Parameters should not be null");
        assertEquals(2, parameters.size(), "Should have 2 parameters");
        
        // 验证第一个参数 (botInstanceId)
        ParameterInfo param1 = parameters.get(0);
        System.out.println("Parameter 1 - Name: " + param1.getName() + ", Type: " + param1.getType().getSimpleName());
        assertEquals("botInstanceId", param1.getName());
        assertEquals(String.class, param1.getType());
        assertEquals("Bot Instance", param1.getDescription());
        assertFalse(param1.isRequired(), "botInstanceId should not be required by default");
        
        // 验证第二个参数 (taskCreationParam)
        ParameterInfo param2 = parameters.get(1);
        System.out.println("Parameter 2 - Name: " + param2.getName() + ", Type: " + param2.getType().getSimpleName());
        assertEquals("taskCreationParams", param2.getName());
        assertEquals(List.class, param2.getType());
        assertEquals("Array of task parameters", param2.getDescription());
        
        System.out.println("=== Function Info Extraction Test Passed ===\n");
    }

    @Test
    void testConvertToOpenAIFormat() {
        // Given
        System.out.println("=== Testing OpenAI Format Conversion ===");
        
        List<FunctionInfo> functionInfos = functionCallProcessor
            .extractFunctionInfosFromInstance(createTasksBotExecution);
        
        // When
        List<Map<String, Object>> openAIFunctions = openAIFunctionCallAdapter
            .convertToOpenAIFormat(functionInfos);
        
        // Then
        assertNotNull(openAIFunctions, "OpenAI functions should not be null");
        assertEquals(1, openAIFunctions.size(), "Should have one function");
        
        Map<String, Object> function = openAIFunctions.get(0);
        
        // 验证函数基本结构
        System.out.println("OpenAI Function Structure:");
        System.out.println("Name: " + function.get("name"));
        System.out.println("Description: " + function.get("description"));
        
        assertEquals("Create_Tasks_Bot_Execution_execute", function.get("name"));
        assertEquals("Implementation for creating tasks in the bot execution framework", function.get("description"));
        assertTrue(function.containsKey("parameters"), "Should contain parameters");
        
        // 验证参数结构
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) function.get("parameters");
        assertNotNull(parameters, "Parameters should not be null");
        assertEquals("object", parameters.get("type"));
        assertTrue(parameters.containsKey("properties"), "Should contain properties");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) parameters.get("properties");
        assertEquals(2, properties.keySet().size(), "Should have 2 properties");
        
        // 验证 botInstanceId 参数
        assertTrue(properties.containsKey("botInstanceId"), "Should contain botInstanceId parameter");
        @SuppressWarnings("unchecked")
        Map<String, Object> botInstanceIdParam = (Map<String, Object>) properties.get("botInstanceId");
        assertEquals("string", botInstanceIdParam.get("type"));
        assertEquals("Bot Instance", botInstanceIdParam.get("description"));
        
        // 验证 taskCreationParam 参数
        assertTrue(properties.containsKey("taskCreationParams"), "Should contain taskCreationParams parameter");
        @SuppressWarnings("unchecked")
        Map<String, Object> taskCreationParamParam = (Map<String, Object>) properties.get("taskCreationParams");
        assertEquals("array", taskCreationParamParam.get("type"));
        assertEquals("Array of task parameters", taskCreationParamParam.get("description"));
        
        System.out.println("Properties: " + properties);
        System.out.println("=== OpenAI Format Conversion Test Passed ===\n");
    }

    @Test
    void testCompleteOpenAIParametersGeneration() {
        // Given
        System.out.println("=== Testing Complete OpenAI Parameters Generation ===");
        
        // When
        List<FunctionInfo> functionInfos = functionCallProcessor
            .extractFunctionInfosFromInstance(createTasksBotExecution);
        List<Map<String, Object>> openAIFunctions = openAIFunctionCallAdapter
            .convertToOpenAIFormat(functionInfos);
        
        // Then
        Map<String, Object> function = openAIFunctions.get(0);
        
        // 打印完整的 OpenAI 参数结构
        System.out.println("Complete OpenAI Function Definition:");
        try {
            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(function);
            System.out.println(jsonOutput);
            
            // 验证 JSON 结构是否完整
            assertTrue(jsonOutput.contains("\"name\""), "Should contain name field");
            assertTrue(jsonOutput.contains("\"description\""), "Should contain description field");
            assertTrue(jsonOutput.contains("\"parameters\""), "Should contain parameters field");
            assertTrue(jsonOutput.contains("\"properties\""), "Should contain properties field");
            assertTrue(jsonOutput.contains("\"botInstanceId\""), "Should contain botInstanceId parameter");
            assertTrue(jsonOutput.contains("\"taskCreationParams\""), "Should contain taskCreationParams parameter");
            
        } catch (Exception e) {
            fail("Failed to serialize to JSON: " + e.getMessage());
        }
        
        System.out.println("=== Complete OpenAI Parameters Generation Test Passed ===\n");
    }

    @Test
    void testParameterTypeMapping() {
        // Given
        System.out.println("=== Testing Parameter Type Mapping ===");
        
        List<FunctionInfo> functionInfos = functionCallProcessor
            .extractFunctionInfosFromInstance(createTasksBotExecution);
        List<Map<String, Object>> openAIFunctions = openAIFunctionCallAdapter
            .convertToOpenAIFormat(functionInfos);
        
        // When
        Map<String, Object> function = openAIFunctions.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) function.get("parameters");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) parameters.get("properties");
        
        // Then - 验证类型映射
        @SuppressWarnings("unchecked")
        Map<String, Object> botInstanceIdProperty = (Map<String, Object>) properties.get("botInstanceId");
        assertEquals("string", botInstanceIdProperty.get("type"), 
            "String parameter should map to 'string' type");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> taskCreationParamProperty = (Map<String, Object>) properties.get("taskCreationParams");
        assertEquals("array", taskCreationParamProperty.get("type"), 
            "List parameter should map to 'array' type");
        
        System.out.println("Type Mapping Results:");
        System.out.println("- String -> " + botInstanceIdProperty.get("type"));
        System.out.println("- List<TaskCreationParams> -> " + taskCreationParamProperty.get("type"));
        
        System.out.println("=== Parameter Type Mapping Test Passed ===\n");
    }

    @Test
    void testTaskCreationParamCompatibility() {
        // Given - 创建一个模拟的 TaskCreationParam 来验证兼容性
        System.out.println("=== Testing TaskCreationParam Compatibility ===");
        
        TaskCreationParam testParam = new TaskCreationParam();
        testParam.setSequence(1);
        testParam.setName("Test Task");
        testParam.setDescription("Test Description");
        testParam.setContextPath("/test/context");
        
        // When - 验证对象可以正确序列化
        try {
            String json = objectMapper.writeValueAsString(testParam);
            System.out.println("TaskCreationParam JSON: " + json);
            
            // 验证可以反序列化
            TaskCreationParam deserializedParam = objectMapper.readValue(json, TaskCreationParam.class);
            
            // Then
            assertEquals(testParam.getSequence(), deserializedParam.getSequence());
            assertEquals(testParam.getName(), deserializedParam.getName());
            assertEquals(testParam.getDescription(), deserializedParam.getDescription());
            assertEquals(testParam.getContextPath(), deserializedParam.getContextPath());
            
            System.out.println("TaskCreationParam serialization/deserialization successful");
            
        } catch (Exception e) {
            fail("TaskCreationParam should be serializable: " + e.getMessage());
        }
        
        System.out.println("=== TaskCreationParam Compatibility Test Passed ===\n");
    }
}