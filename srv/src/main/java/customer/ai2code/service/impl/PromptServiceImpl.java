package customer.ai2code.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotInstances;
import customer.ai2code.service.PromptService;
import customer.ai2code.service.variable.VariableContext;
import customer.ai2code.service.variable.VariableParsingService;

@Service
public class PromptServiceImpl implements PromptService {

    private final GenericCqnService genericCqnService;
    private final VariableParsingService variableParsingService;

    public PromptServiceImpl(GenericCqnService genericCqnService,
            VariableParsingService variableParsingService) {
        this.genericCqnService = genericCqnService;
        this.variableParsingService = variableParsingService;
    }

    @Override
    public String parse(PromptTexts prompt, VariableContext context) {
        String promptText = prompt.getContent();
        if (promptText == null || promptText.isEmpty()) {
            return promptText;
        }

        try {
            // 构建变量解析上下文
            // VariableContext context = buildVariableContext(botInstanceId);

            // 使用统一的变量解析服务
            return variableParsingService.parseVariables(promptText, context);

        } catch (Exception e) {
            System.err.println("Failed to parse prompt variables for " +
                    " botInstanceId: " + context.getBotInstanceId() + ", error: " + e.getMessage());
            return promptText;
        }
    }

    @Override
    public List<PromptTexts> getPrompts(String botTypeId, String botInstanceId) {
        try {
            // 1. 查询BotType对应的所有PromptTexts
            List<PromptTexts> prompts = genericCqnService.getPromptTextsByBotType(botTypeId);

            // 2. 构建变量解析上下文
            VariableContext context = buildVariableContext(botInstanceId);

            // 3. 解析每个PromptTexts的内容
            for (PromptTexts prompt : prompts) {
                String parsedText = parse(prompt, context);
                prompt.setContent(parsedText);
            }

            return prompts;

        } catch (Exception e) {
            System.err.println("Failed to get and parse prompts for botTypeId: " + botTypeId +
                    ", error: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 解析单个变量表达式（用于调试和测试）
     */
    public String parseVariableExpression(String expression, String botInstanceId) {
        try {
            VariableContext context = buildVariableContext(botInstanceId);
            return variableParsingService.parseVariables("{{" + expression + "}}", context)
                    .replace("{{" + expression + "}}", ""); // 移除包装的大括号
        } catch (Exception e) {
            System.err.println("Failed to parse variable expression: " + expression + ", error: " + e.getMessage());
            return "";
        }
    }

    /**
     * 检查Prompt中是否包含变量
     */
    public boolean hasVariables(PromptTexts prompt) {
        return prompt.getContent() != null &&
                variableParsingService.containsVariables(prompt.getContent());
    }

    /**
     * 提取Prompt中的所有变量
     */
    public List<String> extractVariables(PromptTexts prompt) {
        if (prompt.getContent() == null) {
            return List.of();
        }
        return variableParsingService.extractVariables(prompt.getContent());
    }

    /**
     * 批量解析多个Prompt
     */
    public List<PromptTexts> parsePrompts(List<PromptTexts> prompts, String botInstanceId) {
        VariableContext context = buildVariableContext(botInstanceId);

        for (PromptTexts prompt : prompts) {
            if (hasVariables(prompt)) {
                String parsedContent = variableParsingService.parseVariables(prompt.getContent(), context);
                prompt.setContent(parsedContent);
            }
        }

        return prompts;
    }

    /**
     * 构建变量解析上下文
     */
    private VariableContext buildVariableContext(String botInstanceId) {
        VariableContext.VariableContextBuilder builder = VariableContext.builder()
                // .taskId(taskId)
                .botInstanceId(botInstanceId);

        try {
            // 获取主任务ID
            String mainTaskId = genericCqnService.getMainTaskId(botInstanceId);
            builder.mainTaskId(mainTaskId);

            // 确定当前实例类型和对象
            Object currentInstance = null;

            if (botInstanceId != null) {
                try {
                    BotInstances botInstance = genericCqnService.getBotInstanceById(botInstanceId);
                    if (botInstance != null) {
                        currentInstance = botInstance;
                    }
                } catch (Exception e) {
                    // BotInstance不存在，尝试Task
                }
            }

            // if (currentInstance == null) {
            // try {
            // Tasks task = genericCqnService.getTaskById(taskId);
            // if (task != null) {
            // currentInstance = task;
            // }
            // } catch (Exception e) {
            // // Task也不存在
            // }
            // }

            builder.currentInstance(currentInstance);

        } catch (Exception e) {
            System.err.println("Failed to build complete variable context: " + e.getMessage());
        }

        return builder.build();
    }
}
