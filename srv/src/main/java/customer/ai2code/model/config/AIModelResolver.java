package customer.ai2code.model.config;

import com.sap.ai.sdk.foundationmodels.openai.OpenAiModel;

/**
 * AI模型解析器
 */
public class AIModelResolver {
    
    /**
     * 根据模型名称获取OpenAI模型
     */
    public static OpenAiModel resolveOpenAiModel(String modelName) {
        if (modelName == null || modelName.isEmpty()) {
            return OpenAiModel.GPT_35_TURBO; // 默认模型
        }
        
        return switch (modelName.toLowerCase()) {
            case "gpt-3.5-turbo", "gpt35turbo" -> OpenAiModel.GPT_35_TURBO;
            case "gpt-4", "gpt4" -> OpenAiModel.GPT_4;
            // case "gpt-4-turbo", "gpt4turbo" -> OpenAiModel.GPT;
            case "gpt-4o", "gpt4o" -> OpenAiModel.GPT_4O;
            case "gpt-4o-mini", "gpt4omini" -> OpenAiModel.GPT_4O_MINI;
            case "text-embedding-ada-002" -> OpenAiModel.TEXT_EMBEDDING_ADA_002;
            case "text-embedding-3-small" -> OpenAiModel.TEXT_EMBEDDING_3_SMALL;
            case "text-embedding-3-large" -> OpenAiModel.TEXT_EMBEDDING_3_LARGE;
            default -> {
                // 记录警告日志
                System.out.println("Unknown model name: " + modelName + ", using default GPT-3.5-turbo");
                yield OpenAiModel.GPT_35_TURBO;
            }
        };
    }
    
    /**
     * 检查模型名称是否支持
     */
    public static boolean isSupportedModel(String modelName) {
        try {
            resolveOpenAiModel(modelName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}