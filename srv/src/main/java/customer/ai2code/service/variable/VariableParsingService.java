package customer.ai2code.service.variable;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

/**
 * 变量解析服务
 */
@Service
public class VariableParsingService {

    private final List<VariableResolver> resolvers;

    // 匹配{{expression}}的正则表达式，支持嵌套
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^{}]+(?:\\{\\{[^{}]*\\}\\}[^{}]*)*)\\}\\}");

    public VariableParsingService(List<VariableResolver> resolvers) {
        // 按优先级排序
        this.resolvers = resolvers.stream()
                .sorted((r1, r2) -> Integer.compare(r1.getPriority(), r2.getPriority()))
                .collect(Collectors.toList());
    }

    /**
     * 解析文本中的所有变量
     */
    public String parseVariables(String text, VariableContext context) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return parseVariablesRecursively(text, context, 5); // 最大递归深度5层
    }

    /**
     * 递归解析变量（支持嵌套引用）
     */
    private String parseVariablesRecursively(String text, VariableContext context, int maxDepth) {
        if (maxDepth <= 0) {
            System.err.println("Variable parsing reached maximum recursion depth");
            return text;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        boolean hasReplacement = false;

        while (matcher.find()) {
            String variableExpression = matcher.group(1).trim();
            
            // 先递归解析内部的嵌套变量
            String resolvedExpression = parseVariablesRecursively(variableExpression, context, maxDepth - 1);
            
            // 然后解析当前变量
            String resolvedValue = resolveVariable(resolvedExpression, context);
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(resolvedValue));
            hasReplacement = true;
        }

        matcher.appendTail(result);
        
        // 如果有替换发生，可能还需要再次解析（处理解析结果中可能包含的新变量）
        if (hasReplacement && maxDepth > 1) {
            String newResult = result.toString();
            if (VARIABLE_PATTERN.matcher(newResult).find()) {
                return parseVariablesRecursively(newResult, context, maxDepth - 1);
            }
        }
        
        return result.toString();
    }

    /**
     * 解析单个变量
     */
    private String resolveVariable(String variableExpression, VariableContext context) {
        for (VariableResolver resolver : resolvers) {
            if (resolver.supports(variableExpression)) {
                try {
                    return resolver.resolve(variableExpression, context);
                } catch (Exception e) {
                    System.err.println("Variable resolver failed: " + resolver.getClass().getSimpleName() + 
                                     ", expression: " + variableExpression + ", error: " + e.getMessage());
                }
            }
        }
        
        // 如果没有解析器支持，返回原始表达式
        return "{{" + variableExpression + "}}";
    }

    /**
     * 检查文本中是否包含变量
     */
    public boolean containsVariables(String text) {
        return text != null && VARIABLE_PATTERN.matcher(text).find();
    }

    /**
     * 提取文本中的所有变量表达式
     */
    public List<String> extractVariables(String text) {
        if (text == null) {
            return List.of();
        }

        return VARIABLE_PATTERN.matcher(text)
                .results()
                .map(matchResult -> matchResult.group(1).trim())
                .collect(Collectors.toList());
    }
}