package customer.ai2code.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import cds.gen.configservice.BotTypes;
import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotInstances;
import customer.ai2code.model.bot.Bot;
import customer.ai2code.service.PromptService;
import customer.ai2code.service.impl.rag.RAGExtractionFactoryService;
import customer.ai2code.service.rag.RAGExtraction;
import customer.ai2code.service.variable.VariableContext;
import customer.ai2code.service.variable.VariableParsingService;

@Service
public class PromptServiceImpl implements PromptService {

    private final GenericCqnService genericCqnService;
    private final VariableParsingService variableParsingService;
    private final RAGExtractionFactoryService ragExtractionFactoryService;

    public PromptServiceImpl(GenericCqnService genericCqnService,
            VariableParsingService variableParsingService,
            RAGExtractionFactoryService ragExtractionFactoryService) {
        this.genericCqnService = genericCqnService;
        this.variableParsingService = variableParsingService;
        this.ragExtractionFactoryService = ragExtractionFactoryService;
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
    public List<PromptTexts> getPrompts(Bot bot) {
        try {
            // 1. 查询BotType对应的所有PromptTexts
            List<PromptTexts> prompts = genericCqnService.getPromptTextsByBotType(bot.getBotType().getId());

            // 2. 构建变量解析上下文
            VariableContext context = buildVariableContext(bot.getBotInstance().getId());

            // 3. 解析每个PromptTexts的内容
            for (PromptTexts prompt : prompts) {
                String parsedText = parse(prompt, context);
                prompt.setContent(parsedText);
            }

            return prompts;

        } catch (Exception e) {
            System.err.println("Failed to get and parse prompts for botTypeId: " + bot.getBotType().getId() +
                    ", error: " + e.getMessage());
            return List.of();
        }
    }

    @Override
    public PromptTexts getRagAsPrompts(Bot bot, String query) {
        // 5检查botType.isRAGEnabled，维护true情况下,
        // 5.1.读取botType.ragParameter,再通过promptService.parse获取配置好的表达式，作为RAG输入语句
        // 5.2.与content合并成新的RAG输入语句
        // 5.3获取botType.implementationClass,ragTopK,ragThreshold
        // 5.4.通过接口RAGExtrator实例化implementationClass
        // 5.5.调用RAGExtrator.extract方法获取RAG结果
        // 5.6.将RAG结果添加到prompts中

        PromptTexts ragPrompt = PromptTexts.create();

        BotTypes botType = bot.getBotType();

        if (botType.getIsRAGEnabled() != null && botType.getIsRAGEnabled()) {
            try {
                // 5.1 读取botType.ragParameter,再通过promptService.parse获取配置好的表达式，作为RAG输入语句
                String ragParameter = botType.getRagParameter();
                // 2. 构建变量解析上下文
                VariableContext context = buildVariableContext(bot.getBotInstance().getId());

                PromptTexts ragInput = PromptTexts.create();
                ragInput.setContent(ragParameter);

                String ragInputStatement = parse(ragInput, context);

                // 5.2 与content合并成新的RAG输入语句
                String combinedRagInput = ragInputStatement + "/" + query;

                // 5.3 获取botType.implementationClass,ragTopK,ragThreshold
                String implementationClass = botType.getRagClass();
                Integer ragTopK;
                double ragThreshold;
                try{
                    ragThreshold = botType.getRagThreshold();
                } catch (Exception e) {
                    // System.err.println("Failed to get RAG threshold: " + e.getMessage());
                    ragThreshold = 0.65; // 默认值
                }
                try {
                    ragTopK = botType.getRagTopK();
                } catch (Exception e) {
                    // TODO: handle exception
                    ragTopK = 5; // 默认值
                }
                if (ragTopK == null || ragTopK <= 0) {
                    ragTopK = 5; // 默认值
                }
                
                String ragSource = botType.getRagSource();

                // 5.4 通过接口RAGExtractor实例化implementationClass
                // Class<?> clazz = Class.forName(implementationClass);
                // RAGExtraction ragExtraction = (RAGExtraction) clazz.getDeclaredConstructor().newInstance();
                RAGExtraction ragExtraction = ragExtractionFactoryService.createRAGExtractionInstance(implementationClass);

                // 5.5 调用RAGExtractor.extract方法获取RAG结果
                String ragContent = ragExtraction.extract(ragSource, ragTopK, combinedRagInput, bot.getLocale(),
                        ragThreshold);

                // 5.6 将RAG结果添加到prompts中
                if (ragContent != null && !ragContent.isEmpty()) {
                    // PromptTexts ragPrompt = new PromptTexts();
                    ragPrompt.setContent(ragContent);
                    // prompts.add(ragPrompt);
                }

            } catch (Exception e) {
                System.err.println("RAG processing failed: " + e.getMessage());
                // RAG失败时继续正常流程，不中断聊天
            }
        }
        return ragPrompt; // 返回包含RAG结果的单个PromptTexts列表
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

            builder.currentInstance(currentInstance);

        } catch (Exception e) {
            System.err.println("Failed to build complete variable context: " + e.getMessage());
        }

        return builder.build();
    }
}
