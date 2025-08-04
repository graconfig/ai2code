package customer.ai2code.service.execution.functioncall.adapter;

import org.springframework.stereotype.Component;

import customer.ai2code.model.execution.functioncall.FunctionInfo;
import customer.ai2code.model.execution.functioncall.ParameterInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Function Calling 格式适配器 - 增强版
 */
@Component
public class GeminiAIFunctionCallAdapter {
    
    /**
     * 将通用的函数信息转换为 OpenAI Function Calling 格式
     */
    public List<Map<String, Object>> convertToOpenAIFormat(List<FunctionInfo> functionInfos) {
        List<Map<String, Object>> openAIFunctions = new ArrayList<>();
        
        for (FunctionInfo functionInfo : functionInfos) {
            Map<String, Object> function = new HashMap<>();
            function.put("name", functionInfo.getName());
            function.put("description", functionInfo.getDescription());
            
            // 构建参数定义
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("type", "object");
            
            Map<String, Object> properties = new HashMap<>();
            List<String> required = new ArrayList<>();
            
            for (ParameterInfo paramInfo : functionInfo.getParameters()) {
                Map<String, Object> paramDef = new HashMap<>();
                
                // 使用增强的类型信息
                String schemaType = paramInfo.getJsonSchemaType() != null ? 
                    paramInfo.getJsonSchemaType() : mapJavaTypeToJsonType(paramInfo.getType());
                paramDef.put("type", schemaType);
                paramDef.put("description", paramInfo.getDescription());
                
                // 处理数组类型
                if ("array".equals(schemaType)) {
                    Map<String, Object> items = new HashMap<>();
                    if (paramInfo.getItemType() != null) {
                        items.put("type", mapJavaTypeToJsonType(paramInfo.getItemType()));
                        
                        // 如果数组元素是复杂对象，添加属性定义
                        if (paramInfo.getItemProperties() != null && !paramInfo.getItemProperties().isEmpty()) {
                            items.put("type", "object");
                            items.put("properties", paramInfo.getItemProperties());
                        }
                    } else {
                        items.put("type", "object");
                    }
                    paramDef.put("items", items);
                }
                
                // 处理对象类型
                if ("object".equals(schemaType) && paramInfo.getProperties() != null) {
                    paramDef.put("properties", paramInfo.getProperties());
                }
                
                properties.put(paramInfo.getName(), paramDef);
                
                if (paramInfo.isRequired()) {
                    required.add(paramInfo.getName());
                }
            }
            
            parameters.put("properties", properties);
            if (!required.isEmpty()) {
                parameters.put("required", required);
            }
            
            function.put("parameters", parameters);
            openAIFunctions.add(function);
        }
        
        return openAIFunctions;
    }
    
    /**
     * 将 Java 类型映射为 JSON Schema 类型
     */
    private String mapJavaTypeToJsonType(Class<?> javaType) {
        if (javaType == String.class) {
            return "string";
        } else if (javaType == Integer.class || javaType == int.class ||
                   javaType == Long.class || javaType == long.class) {
            return "integer";
        } else if (javaType == Boolean.class || javaType == boolean.class) {
            return "boolean";
        } else if (javaType == Double.class || javaType == double.class ||
                   javaType == Float.class || javaType == float.class) {
            return "number";
        } else if (List.class.isAssignableFrom(javaType)) {
            return "array";
        } else {
            return "object";
        }
    }
}