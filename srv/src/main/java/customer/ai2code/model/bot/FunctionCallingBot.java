package customer.ai2code.model.bot;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cds.gen.configservice.BotTypes;
import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstancesExecuteContext;
import customer.ai2code.model.config.AIModel;
import customer.ai2code.model.config.AIModelResolver;
import customer.ai2code.service.AIService;
import customer.ai2code.service.PromptService;
import customer.ai2code.service.impl.GenericCqnService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FunctionCallingBot implements Bot {

    private BotInstances botInstance;
    private AIModel aiModel;
    private BotTypes botType;

    // 服务依赖（通过构造函数注入）
    private GenericCqnService genericCqnService;
    private PromptService promptService;
    private AIModelResolver aiModelResolver;

    @Override
    public BotInstancesExecuteContext.ReturnType execute() {
        // 实现函数调用机器人的执行逻辑
        List<PromptTexts> prompts = new ArrayList<>();
        try {
            // 1. 根据AIModel类型，获取到不同AIService服务
            AIService aiService = aiModelResolver.resolveAIService(aiModel.getModelConfigs());
            // 2. 使用genericCqnService.getMainTaskId，再获取Prompt
            String mainTaskId = genericCqnService.getMainTaskId(botInstance.getId());
            prompts = promptService.getPrompts(botType.getId(), mainTaskId, botInstance.getId());

            // 3. 根据botType.getImplementationClass()获取到函数调用的实现类

            // 4. 传入类

            // String response = aiService.functionCalling(new ArrayList<>(), prompts, botType.getImplementationClass(), aiModel);

        } catch (Exception e) {
            // TODO: handle exception
        }

        return null; // 返回执行结果
    }

    @Override
    public Boolean executeAsync() {
        // 实现异步执行逻辑
        return true; // 返回是否成功
    }

    @Override
    public Boolean stop() {
        // 实现停止逻辑
        return true; // 返回是否成功
    }

    @Override
    public Boolean resume() {
        // 实现恢复逻辑
        return true; // 返回是否成功
    }

    @Override
    public Boolean cancel() {
        // 实现取消逻辑
        return true; // 返回是否成功
    }

    @Override
    public SseEmitter chatInStreaming(String content) {
        // 实现流式聊天逻辑
        return new SseEmitter(); // 返回SSE发射器实例
    }

    @Override
    public String chat(String content) {
        // 实现聊天逻辑
        return "Function calling response for: " + content; // 返回聊天响应内容
    }

}
