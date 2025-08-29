package customer.ai2code.util;

import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * SpEL变量注入助手类
 * 将解析后的变量值注入到SpEL评估上下文中
 */
@Component
public class SpELVariableHelper {

    private final ObjectMapper objectMapper;
    
    // JSON对象模式
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile("\\{[^{}]*\\}");
    
    // JSON数组模式
    private static final Pattern JSON_ARRAY_PATTERN = Pattern.compile("\\[[^\\[\\]]*\\]");

    public SpELVariableHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将变量值注入到SpEL上下文中
     */
    public void injectVariables(StandardEvaluationContext context, String resolvedCondition) {
        // 提取并注入JSON对象
        injectJsonObjects(context, resolvedCondition);
        
        // 提取并注入JSON数组
        injectJsonArrays(context, resolvedCondition);
        
        // 提取并注入简单变量
        injectSimpleVariables(context, resolvedCondition);
    }

    /**
     * 注入变量并返回转换后的SpEL表达式
     */
    public String injectVariablesAndTransform(StandardEvaluationContext context, String resolvedCondition) {
        String transformedExpression = resolvedCondition;
        
        // 处理JSON对象并转换表达式
        transformedExpression = processAndReplaceJsonObjects(context, transformedExpression);
        
        // 处理JSON数组并转换表达式
        transformedExpression = processAndReplaceJsonArrays(context, transformedExpression);
        
        return transformedExpression;
    }

    /**
     * 处理JSON对象并替换表达式中的JSON字符串为变量引用
     */
    private String processAndReplaceJsonObjects(StandardEvaluationContext context, String expression) {
        Matcher matcher = JSON_OBJECT_PATTERN.matcher(expression);
        String result = expression;
        int index = 0;
        
        while (matcher.find()) {
            String jsonStr = matcher.group();
            try {
                Map<String, Object> jsonObject = objectMapper.readValue(jsonStr, 
                    new TypeReference<Map<String, Object>>() {});
                
                // 生成唯一的变量名
                String varName = "jsonVar" + index;
                
                // 注入变量到上下文
                context.setVariable(varName, jsonObject);
                
                // 替换表达式中的JSON字符串为变量引用
                result = result.replace(jsonStr, "#" + varName);
                
                index++;
            } catch (Exception e) {
                // 忽略JSON解析错误，保持原样
            }
        }
        
        return result;
    }

    /**
     * 处理JSON数组并替换表达式中的JSON字符串为变量引用
     */
    private String processAndReplaceJsonArrays(StandardEvaluationContext context, String expression) {
        Matcher matcher = JSON_ARRAY_PATTERN.matcher(expression);
        String result = expression;
        int index = 0;
        
        while (matcher.find()) {
            String jsonStr = matcher.group();
            try {
                List<Object> jsonArray = objectMapper.readValue(jsonStr, 
                    new TypeReference<List<Object>>() {});
                
                // 生成唯一的变量名
                String varName = "jsonArray" + index;
                
                // 注入变量到上下文
                context.setVariable(varName, jsonArray);
                
                // 替换表达式中的JSON字符串为变量引用
                result = result.replace(jsonStr, "#" + varName);
                
                index++;
            } catch (Exception e) {
                // 忽略JSON解析错误，保持原样
            }
        }
        
        return result;
    }

    /**
     * 注入JSON对象变量
     */
    private void injectJsonObjects(StandardEvaluationContext context, String text) {
        Matcher matcher = JSON_OBJECT_PATTERN.matcher(text);
        int index = 0;
        
        while (matcher.find()) {
            String jsonStr = matcher.group();
            try {
                Map<String, Object> jsonObject = objectMapper.readValue(jsonStr, 
                    new TypeReference<Map<String, Object>>() {});
                
                // 注入整个对象
                context.setVariable("jsonObject" + index, jsonObject);
                
                // 注入对象的各个属性
                for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                    context.setVariable(entry.getKey(), entry.getValue());
                }
                
                index++;
            } catch (Exception e) {
                // 忽略JSON解析错误
            }
        }
    }

    /**
     * 注入JSON数组变量
     */
    private void injectJsonArrays(StandardEvaluationContext context, String text) {
        Matcher matcher = JSON_ARRAY_PATTERN.matcher(text);
        int index = 0;
        
        while (matcher.find()) {
            String jsonStr = matcher.group();
            try {
                List<Object> jsonArray = objectMapper.readValue(jsonStr, 
                    new TypeReference<List<Object>>() {});
                
                context.setVariable("jsonArray" + index, jsonArray);
                index++;
            } catch (Exception e) {
                // 忽略JSON解析错误
            }
        }
    }

    /**
     * 注入简单变量（数字、字符串等）
     */
    private void injectSimpleVariables(StandardEvaluationContext context, String text) {
        // 检测数字
        try {
            // 提取可能的数字值
            Pattern numberPattern = Pattern.compile("\\b\\d+(\\.\\d+)?\\b");
            Matcher numberMatcher = numberPattern.matcher(text);
            int index = 0;
            
            while (numberMatcher.find()) {
                String numberStr = numberMatcher.group();
                try {
                    if (numberStr.contains(".")) {
                        Double number = Double.parseDouble(numberStr);
                        context.setVariable("number" + index, number);
                    } else {
                        Integer number = Integer.parseInt(numberStr);
                        context.setVariable("number" + index, number);
                    }
                    index++;
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
        } catch (Exception e) {
            // 忽略错误
        }
    }

    /**
     * 从变量名推断并设置上下文变量
     */
    @SuppressWarnings("unused")
    public void setContextVariable(StandardEvaluationContext context, String varName, Object value) {
        if (value == null) {
            context.setVariable(varName, null);
            return;
        }

        // 尝试解析为JSON
        if (value instanceof String) {
            String strValue = (String) value;
            
            // 尝试解析为JSON对象
            if (strValue.startsWith("{") && strValue.endsWith("}")) {
                try {
                    Map<String, Object> jsonObject = objectMapper.readValue(strValue, 
                        new TypeReference<Map<String, Object>>() {});
                    context.setVariable(varName, jsonObject);
                    return;
                } catch (Exception e) {
                    // 继续作为字符串处理
                }
            }
            
            // 尝试解析为JSON数组
            if (strValue.startsWith("[") && strValue.endsWith("]")) {
                try {
                    List<Object> jsonArray = objectMapper.readValue(strValue, 
                        new TypeReference<List<Object>>() {});
                    context.setVariable(varName, jsonArray);
                    return;
                } catch (Exception e) {
                    // 继续作为字符串处理
                }
            }
        }

        // 直接设置原始值
        context.setVariable(varName, value);
    }

    /**
     * 创建上下文变量的帮助方法
     */
    @SuppressWarnings("unused")
    public StandardEvaluationContext createContextWithVariables(Map<String, Object> variables) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        if (variables != null) {
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                setContextVariable(context, entry.getKey(), entry.getValue());
            }
        }
        
        return context;
    }
}
