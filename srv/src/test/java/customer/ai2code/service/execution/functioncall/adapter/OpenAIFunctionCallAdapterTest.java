package customer.ai2code.service.execution.functioncall.adapter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import customer.ai2code.service.execution.functioncall.FunctionInfo;
import customer.ai2code.service.execution.functioncall.ParameterInfo;

public class OpenAIFunctionCallAdapterTest {

    private OpenAIFunctionCallAdapter adapter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        adapter = new OpenAIFunctionCallAdapter();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testConvertToOpenAIFormat_EmptyList() {
        // Given
        List<FunctionInfo> emptyList = new ArrayList<>();
        
        // When
        List<Map<String, Object>> result = adapter.convertToOpenAIFormat(emptyList);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testConvertToOpenAIFormat_SingleFunction() {
        // Given
        List<ParameterInfo> parameters = new ArrayList<>();
        parameters.add(ParameterInfo.builder()
            .name("testParam")
            .type(String.class)
            .required(true)
            .description("Test parameter")
            .build());

        FunctionInfo functionInfo = FunctionInfo.builder()
            .name("testFunction")
            .description("Test function description")
            .parameters(parameters)
            .build();

        List<FunctionInfo> functionInfos = List.of(functionInfo);

        // When
        List<Map<String, Object>> result = adapter.convertToOpenAIFormat(functionInfos);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());

        Map<String, Object> function = result.get(0);
        assertEquals("testFunction", function.get("name"));
        assertEquals("Test function description", function.get("description"));
        assertTrue(function.containsKey("parameters"));

        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) function.get("parameters");
        assertEquals("object", params.get("type"));
        assertTrue(params.containsKey("properties"));
        assertTrue(params.containsKey("required"));
    }

    @Test
    void testMapJavaTypeToJsonType() {
        // Given - 创建测试用的函数信息
        List<ParameterInfo> parameters = new ArrayList<>();
        
        // 添加不同类型的参数
        parameters.add(ParameterInfo.builder()
            .name("stringParam")
            .type(String.class)
            .description("String parameter")
            .build());
            
        parameters.add(ParameterInfo.builder()
            .name("intParam")
            .type(Integer.class)
            .description("Integer parameter")
            .build());
            
        parameters.add(ParameterInfo.builder()
            .name("boolParam")
            .type(Boolean.class)
            .description("Boolean parameter")
            .build());
            
        parameters.add(ParameterInfo.builder()
            .name("doubleParam")
            .type(Double.class)
            .description("Double parameter")
            .build());
            
        parameters.add(ParameterInfo.builder()
            .name("listParam")
            .type(List.class)
            .description("List parameter")
            .build());

        FunctionInfo functionInfo = FunctionInfo.builder()
            .name("typeTestFunction")
            .description("Function to test type mapping")
            .parameters(parameters)
            .build();

        // When
        List<Map<String, Object>> result = adapter.convertToOpenAIFormat(List.of(functionInfo));

        // Then
        Map<String, Object> function = result.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) function.get("parameters");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) params.get("properties");

        // 验证类型映射
        @SuppressWarnings("unchecked")
        Map<String, Object> stringProp = (Map<String, Object>) properties.get("stringParam");
        assertEquals("string", stringProp.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> intProp = (Map<String, Object>) properties.get("intParam");
        assertEquals("integer", intProp.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> boolProp = (Map<String, Object>) properties.get("boolParam");
        assertEquals("boolean", boolProp.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> doubleProp = (Map<String, Object>) properties.get("doubleParam");
        assertEquals("number", doubleProp.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> listProp = (Map<String, Object>) properties.get("listParam");
        assertEquals("array", listProp.get("type"));
    }

    @Test
    void testRequiredParameters() {
        // Given
        List<ParameterInfo> parameters = new ArrayList<>();
        parameters.add(ParameterInfo.builder()
            .name("requiredParam")
            .type(String.class)
            .required(true)
            .description("Required parameter")
            .build());
            
        parameters.add(ParameterInfo.builder()
            .name("optionalParam")
            .type(String.class)
            .required(false)
            .description("Optional parameter")
            .build());

        FunctionInfo functionInfo = FunctionInfo.builder()
            .name("testFunction")
            .description("Test function")
            .parameters(parameters)
            .build();

        // When
        List<Map<String, Object>> result = adapter.convertToOpenAIFormat(List.of(functionInfo));

        // Then
        Map<String, Object> function = result.get(0);
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) function.get("parameters");
        
        @SuppressWarnings("unchecked")
        List<String> required = (List<String>) params.get("required");
        assertNotNull(required);
        assertEquals(1, required.size());
        assertTrue(required.contains("requiredParam"));
        assertFalse(required.contains("optionalParam"));
    }
}