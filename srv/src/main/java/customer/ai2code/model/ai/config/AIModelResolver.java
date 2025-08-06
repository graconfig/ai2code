package customer.ai2code.model.ai.config;

import org.springframework.stereotype.Service;
import cds.gen.configservice.ModelConfigs;
import customer.ai2code.model.ai.config.model.AIModel;
import customer.ai2code.model.ai.config.model.SAPAICoreClaudeAI35Sonnet;
import customer.ai2code.model.ai.config.model.SAPAICoreClaudeAI37Sonnet;
import customer.ai2code.model.ai.config.model.SAPAICoreDeepSeek;
import customer.ai2code.model.ai.config.model.SAPAICoreOpenAIGPT35;
import customer.ai2code.model.ai.config.model.SAPAICoreOpenAIgpt4o;
import customer.ai2code.service.AIService;
import customer.ai2code.service.impl.GenericCqnService;
import customer.ai2code.service.impl.SAClaudeAIServiceImpl;
import customer.ai2code.service.impl.SAPDeepseekAIServiceImpl;
import customer.ai2code.service.impl.SAPOpenAIServiceImpl;

/**
 * AI模型解析器
 */
@Service
public class AIModelResolver {
    

    private final GenericCqnService genericCqnService;

    private final SAPOpenAIServiceImpl sapOpenAIService;
    private final SAClaudeAIServiceImpl sapClaudeAIService;
    private final SAPDeepseekAIServiceImpl sapDeepseekAIService;

    public AIModelResolver(GenericCqnService genericCqnService 
            , SAPOpenAIServiceImpl sapOpenAIService
            , SAClaudeAIServiceImpl sapClaudeAIService
            , SAPDeepseekAIServiceImpl sapDeepseekAIService) {
        this.genericCqnService = genericCqnService;
        this.sapOpenAIService = sapOpenAIService;
        this.sapClaudeAIService = sapClaudeAIService;
        this.sapDeepseekAIService = sapDeepseekAIService;
    }


    // /**
    //  * 根据模型名称获取OpenAI模型
    //  */
    // public  OpenAiModel resolveOpenAiModel(String modelName) {
    //     if (modelName == null || modelName.isEmpty()) {
    //         return OpenAiModel.GPT_35_TURBO; // 默认模型
    //     }
        
    //     return switch (modelName.toLowerCase()) {
    //         case "gpt-3.5-turbo", "gpt35turbo" -> OpenAiModel.GPT_35_TURBO;
    //         case "gpt-4", "gpt4" -> OpenAiModel.GPT_4;
    //         // case "gpt-4-turbo", "gpt4turbo" -> OpenAiModel.GPT;
    //         case "gpt-4o", "gpt4o" -> OpenAiModel.GPT_4O;
    //         case "gpt-4o-mini", "gpt4omini" -> OpenAiModel.GPT_4O_MINI;
    //         case "text-embedding-ada-002" -> OpenAiModel.TEXT_EMBEDDING_ADA_002;
    //         case "text-embedding-3-small" -> OpenAiModel.TEXT_EMBEDDING_3_SMALL;
    //         case "text-embedding-3-large" -> OpenAiModel.TEXT_EMBEDDING_3_LARGE;
    //         default -> {
    //             // 记录警告日志
    //             System.out.println("Unknown model name: " + modelName + ", using default GPT-3.5-turbo");
    //             yield OpenAiModel.GPT_35_TURBO;
    //         }
    //     };
    // }
    

    public AIModel resolveAIModel(String modelConfigId){
        
        // 1. 从配置服务中获取模型配置
        ModelConfigs modelConfig = 
            genericCqnService.getModelConfig(modelConfigId);
        
        return resolveAIModel(modelConfig);

    }

    public AIModel resolveAIModel(ModelConfigs modelConfigs){
        // 2. 根据模型配置的提供者和模型名称创建对应的AIModel实例
        switch (modelConfigs.getProvider()) {
            // case "SAPAICore-OpenAI":
            //     switch (modelConfigs.getModelName()) {
            //         case "gpt-4o":
            //             return new SAPAICoreOpenAIgpt4o(modelConfigs);
            //         default:
            //             break;
            //     }    
            //     break;
            case "SAPAICore-OpenAI": // OpenAI系列模型
                switch (modelConfigs.getModelName()) {
                    case "gpt-4o":
                        return new SAPAICoreOpenAIgpt4o(modelConfigs);
                    case "gpt-3.5-turbo": // 新增GPT-3.5支持
                        return new SAPAICoreOpenAIGPT35(modelConfigs);
                }
                break;
            case "SAPAICore-Claude": // Claude系列模型
                switch (modelConfigs.getModelName()) {
                    case "claude-3.5-sonnet": // 新增Claude 3.5支持
                        return new SAPAICoreClaudeAI35Sonnet(modelConfigs);
                    case "claude-3.7-sonnet": // 新增Claude 3.7支持
                        return new SAPAICoreClaudeAI37Sonnet(modelConfigs);
                    // 其他Claude模型...
                }
                break;
            case "SAPAICore-DeepSeek":
                return new SAPAICoreDeepSeek(modelConfigs);
            default:
                break;
        }


        return null;
    }


    /**
     * 
     * @param modelConfigId
     * @return
     */
    public AIService resolveAIService(String modelConfigId) {
        
        // 1. 从配置服务中获取模型配置
        ModelConfigs modelConfigs = 
            genericCqnService.getModelConfig(modelConfigId);
        return resolveAIService(modelConfigs);
    }

    public AIService resolveAIService(ModelConfigs modelConfigs) {
        // 2. 根据模型配置的提供者和模型名称创建对应的AIService实例
        switch (modelConfigs.getProvider()) {
            case "SAPAICore-OpenAI":
                return sapOpenAIService;
            case "SAPAICore-Claude":
                return sapClaudeAIService;
            case "SAPAICore-DeepSeek":
                return sapDeepseekAIService;
            default:
                return sapOpenAIService; // 默认返回OpenAI服务
        }
    }

    // /**
    //  * 检查模型名称是否支持
    //  */
    // public boolean isSupportedModel(String modelName) {
    //     try {
    //         resolveOpenAiModel(modelName);
    //         return true;
    //     } catch (Exception e) {
    //         return false;
    //     }
    // }
}