package customer.ai2code.model.config;

import org.springframework.stereotype.Service;

import com.sap.ai.sdk.foundationmodels.openai.OpenAiModel;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;

import cds.gen.configservice.ModelConfigs;
import cds.gen.configservice.ModelConfigs_;
import customer.ai2code.model.AIModel;
import customer.ai2code.service.AIService;
import customer.ai2code.service.impl.EntityService;
import customer.ai2code.service.impl.GenericCqnService;
import customer.ai2code.service.impl.SAClaudeAIServiceImpl;
import customer.ai2code.service.impl.SAPOpenAIServiceImpl;

/**
 * AI模型解析器
 */
@Service
public class AIModelResolver {
    

    private final GenericCqnService genericCqnService;

    private final SAPOpenAIServiceImpl sapOpenAIService;
    private final SAClaudeAIServiceImpl sapClaudeAIService;

    public AIModelResolver(GenericCqnService genericCqnService 
            , SAPOpenAIServiceImpl sapOpenAIService
            , SAClaudeAIServiceImpl sapClaudeAIService) {
        this.genericCqnService = genericCqnService;
        this.sapOpenAIService = sapOpenAIService;
        this.sapClaudeAIService = sapClaudeAIService;
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
        // return new AIModel(resolveOpenAiModel(null))
        // TODO: 这里可以根据需要返回一个默认的AIModel实例
        
        // 1. 从配置服务中获取模型配置
        // ModelConfigs modelConfig = 
        //     genericCqnService.getModelConfig(modelConfigId);

        // switch (modelConfig.getProvider()) {
        //     case 'SAPAICore-OpenAI':
                 

        //         break;
        //     default:
        //         break;
        // }

        return null;

    }

    /**
     * 
     * @param modelConfigId
     * @return
     */
    public AIService resolveAIService(String modelConfigId) {
        
        //TODO: 这里可以根据需要返回一个默认的AIService实例
        // return null;
        switch (modelConfigId) {
            case "SAPAICore-OpenAI":
                return sapOpenAIService;
                // break;
            case "SAPAICore-Claude":
                return sapClaudeAIService;
                // break;
            default:
                return sapOpenAIService;
                // break;
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