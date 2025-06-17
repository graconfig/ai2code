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

import customer.ai2code.service.TaskService;
import customer.ai2code.service.execution.functioncall.adapter.OpenAIFunctionCallAdapter;
import customer.ai2code.service.impl.CreateTasksBotExecution;

@ExtendWith(MockitoExtension.class)
class OpenAIParametersFormatTest {

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
        objectMapper = new ObjectMapper();
        functionCallProcessor = new FunctionCallProcessor(applicationContext, objectMapper);
        openAIFunctionCallAdapter = new OpenAIFunctionCallAdapter();
        createTasksBotExecution = new CreateTasksBotExecution(taskService);
    }

    @Test
    void testOpenAIFunctionCallParametersFormat() {
        System.out.println("=== Testing OpenAI Function Call Parameters Format ===");
        
        // Step 1: Extract function info
        List<FunctionInfo> functionInfos = functionCallProcessor
            .extractFunctionInfosFromInstance(createTasksBotExecution);
        
        assertFalse(functionInfos.isEmpty(), "Should have at least one function");
        
        // Step 2: Convert to OpenAI format
        List<Map<String, Object>> openAIFunctions = openAIFunctionCallAdapter
            .convertToOpenAIFormat(functionInfos);
        
        assertEquals(1, openAIFunctions.size(), "Should have exactly one function");
        
        Map<String, Object> function = openAIFunctions.get(0);
        
        // Step 3: Print and validate the exact format that will be sent to OpenAI
        try {
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(function);
            
            System.out.println("OpenAI Function Definition (Pretty Format):");
            System.out.println(prettyJson);
            
            // Validate the JSON structure matches OpenAI requirements
            assertTrue(prettyJson.contains("\"name\" : \"Create Tasks Bot Execution_execute\""));
            assertTrue(prettyJson.contains("\"description\""));
            assertTrue(prettyJson.contains("\"parameters\""));
            assertTrue(prettyJson.contains("\"type\" : \"object\""));
            assertTrue(prettyJson.contains("\"properties\""));
            assertTrue(prettyJson.contains("\"botInstanceId\""));
            assertTrue(prettyJson.contains("\"taskCreationParams\""));
            assertTrue(prettyJson.contains("\"type\" : \"string\""));
            assertTrue(prettyJson.contains("\"type\" : \"array\""));
            
            // Also test compact format (what actually gets sent)
            String compactJson = objectMapper.writeValueAsString(function);
            System.out.println("\nOpenAI Function Definition (Compact Format):");
            System.out.println(compactJson);
            
            // Validate compact format
            assertNotNull(compactJson);
            assertTrue(compactJson.length() > 0);
            assertFalse(compactJson.contains("\n")); // Should be compact
            
        } catch (Exception e) {
            fail("Failed to serialize OpenAI function parameters: " + e.getMessage());
        }
        
        System.out.println("\n=== OpenAI Parameters Format Test Passed ===");
    }

    @Test
    void testOpenAIParametersStructureValidation() {
        System.out.println("=== Testing OpenAI Parameters Structure Validation ===");
        
        List<FunctionInfo> functionInfos = functionCallProcessor
            .extractFunctionInfosFromInstance(createTasksBotExecution);
        List<Map<String, Object>> openAIFunctions = openAIFunctionCallAdapter
            .convertToOpenAIFormat(functionInfos);
        
        Map<String, Object> function = openAIFunctions.get(0);
        
        // Validate top-level structure
        assertEquals("Create Tasks Bot Execution_execute", function.get("name"));
        assertEquals("Implementation for creating tasks in the bot execution framework", function.get("description"));
        assertTrue(function.containsKey("parameters"));
        
        // Validate parameters structure
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) function.get("parameters");
        assertEquals("object", parameters.get("type"));
        assertTrue(parameters.containsKey("properties"));
        
        // Validate properties structure
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) parameters.get("properties");
        assertEquals(2, properties.size(), "Should have exactly 2 properties");
        
        // Validate botInstanceId property
        assertTrue(properties.containsKey("botInstanceId"));
        @SuppressWarnings("unchecked")
        Map<String, Object> botInstanceIdProp = (Map<String, Object>) properties.get("botInstanceId");
        assertEquals("string", botInstanceIdProp.get("type"));
        assertEquals("Bot Instance", botInstanceIdProp.get("description"));
        
        // Validate taskCreationParams property
        assertTrue(properties.containsKey("taskCreationParams"));
        @SuppressWarnings("unchecked")
        Map<String, Object> taskCreationParamProp = (Map<String, Object>) properties.get("taskCreationParams");
        assertEquals("array", taskCreationParamProp.get("type"));
        assertEquals("Array of task parameters", taskCreationParamProp.get("description"));
        
        System.out.println("Structure Validation Results:");
        System.out.println("✓ Function name: " + function.get("name"));
        System.out.println("✓ Function description: " + function.get("description"));
        System.out.println("✓ Parameters type: " + parameters.get("type"));
        System.out.println("✓ Properties count: " + properties.size());
        System.out.println("✓ botInstanceId type: " + botInstanceIdProp.get("type"));
        System.out.println("✓ taskCreationParams type: " + taskCreationParamProp.get("type"));
        
        System.out.println("=== Parameters Structure Validation Test Passed ===");
    }

    @Test
    void testParametersFormatMatchesOpenAISpec() {
        System.out.println("=== Testing Parameters Format Matches OpenAI Specification ===");
        
        List<FunctionInfo> functionInfos = functionCallProcessor
            .extractFunctionInfosFromInstance(createTasksBotExecution);
        List<Map<String, Object>> openAIFunctions = openAIFunctionCallAdapter
            .convertToOpenAIFormat(functionInfos);
        
        Map<String, Object> function = openAIFunctions.get(0);
        
        // According to OpenAI API spec, a function should have:
        // - name (string, required)
        // - description (string, optional but recommended)  
        // - parameters (object, required) - JSON Schema object
        
        // Validate according to OpenAI spec
        assertTrue(function.get("name") instanceof String, "name must be string");
        assertTrue(function.get("description") instanceof String, "description must be string");
        assertTrue(function.get("parameters") instanceof Map, "parameters must be object");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> parameters = (Map<String, Object>) function.get("parameters");
        
        // Parameters should follow JSON Schema format
        assertEquals("object", parameters.get("type"), "parameters.type must be 'object'");
        assertTrue(parameters.containsKey("properties"), "parameters must have 'properties'");
        assertTrue(parameters.get("properties") instanceof Map, "properties must be object");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) parameters.get("properties");
        
        // Each property should have a type
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            assertTrue(entry.getValue() instanceof Map, "Each property must be an object");
            @SuppressWarnings("unchecked")
            Map<String, Object> property = (Map<String, Object>) entry.getValue();
            assertTrue(property.containsKey("type"), "Each property must have a 'type'");
            assertTrue(property.get("type") instanceof String, "Property type must be string");
        }
        
        System.out.println("OpenAI Specification Compliance:");
        System.out.println("✓ Function has required 'name' field (string)");
        System.out.println("✓ Function has 'description' field (string)");
        System.out.println("✓ Function has required 'parameters' field (object)");
        System.out.println("✓ Parameters follow JSON Schema format");
        System.out.println("✓ Parameters have 'type': 'object'");
        System.out.println("✓ Parameters have 'properties' object");
        System.out.println("✓ Each property has valid 'type' field");
        
        System.out.println("=== OpenAI Specification Compliance Test Passed ===");
    }
}