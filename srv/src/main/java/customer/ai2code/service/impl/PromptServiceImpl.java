package customer.ai2code.service.impl;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.ContextNodes;
import customer.ai2code.service.PromptService;
import customer.ai2code.service.ContextService;

@Service
public class PromptServiceImpl implements PromptService {

    private final GenericCqnService genericCqnService;
    private final ContextService contextService;

    // 匹配{{ContextPath}}的正则表达式
    private static final Pattern CONTEXT_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");

    public PromptServiceImpl(GenericCqnService genericCqnService, ContextService contextService) {
        this.genericCqnService = genericCqnService;
        this.contextService = contextService;
    }

    @Override
    public String parse(PromptTexts prompt, String taskId, String botInstanceId) {
        // 重载方法，支持指定taskId来查询context
        String promptText = prompt.getContent();
        if (promptText == null || promptText.isEmpty()) {
            return promptText;
        }

        // 查找所有{{ContextPath}}模式
        Matcher matcher = CONTEXT_PATTERN.matcher(promptText);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String contextPath = matcher.group(1).trim(); // 获取大括号内的内容
            contextPath = contextService.getContextFullPath(botInstanceId, contextPath); // 获取完整的context路径

            try {
                // 根据taskId和contextPath获取ContextNode的值
                String contextValue = getContextValue(taskId, contextPath);

                // 替换{{ContextPath}}为实际的context值
                matcher.appendReplacement(result, Matcher.quoteReplacement(contextValue != null ? contextValue : ""));
            } catch (Exception e) {
                // 如果获取context失败，保留原始的{{ContextPath}}
                System.err.println("Failed to get context value for taskId: " + taskId + ", path: " + contextPath
                        + ", error: " + e.getMessage());
                matcher.appendReplacement(result, Matcher.quoteReplacement("{{" + contextPath + "}}"));
            }
        }

        matcher.appendTail(result);
        return result.toString();
    }

    @Override
    public List<PromptTexts> getPrompts(String botTypeId, String taskId, String botInstanceId) {
        // 重载方法，支持指定taskId来解析context
        // 1. 查询BotType对应的所有PromptTexts
        List<PromptTexts> prompts = genericCqnService.getPromptTextsByBotType(botTypeId);

        // 2. 调用parse方法解析每个PromptTexts的内容，使用指定的taskId
        for (PromptTexts prompt : prompts) {
            String parsedText = parse(prompt, taskId, botInstanceId);
            prompt.setContent(parsedText);
        }

        // 3. 返回解析后的PromptTexts列表
        return prompts;
    }

    /**
     * 根据taskId和contextPath获取ContextNode的值
     */
    private String getContextValue(String taskId, String contextPath) {
        try {
            ContextNodes contextNode = genericCqnService.getContextNodeByTaskAndPath(taskId, contextPath);
            return contextNode != null ? contextNode.getValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

}
