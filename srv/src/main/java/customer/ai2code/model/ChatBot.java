package customer.ai2code.model;

import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cds.gen.configservice.BotTypes;
import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstancesExecuteContext;
import cds.gen.mainservice.BotMessages;
import customer.ai2code.service.AIService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatBot implements Bot {

    private BotInstances botInstance;
    private AIModel aiModel;
    private BotTypes botType;

    @Override
    public BotInstancesExecuteContext.ReturnType execute() {
        // 实现聊天机器人的执行逻辑
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
        String response;

        AIService aiService = aiModel.getAIService();
        List<BotMessages> botMessages = botInstance.getMessages();
        List<PromptTexts> promptTexts = botType.getPrompts();

        // 若第一次对话，使用【提示词promptTexts+用户对话内容content】返回应答
        if (botMessages == null || botMessages.size() == 0) {
            response = aiService.chatWithAI(null, promptTexts, content, aiModel);
        } else {
            response = aiService.chatWithAI(botMessages, null, content, aiModel);
        }

        // 实现聊天逻辑
        return "Chat response for: " + response; // 返回聊天响应内容
    }

    // @Override
    // public BotInstances getBotInstance() {
    // // 返回当前Bot实例信息
    // return null; // 需要实现具体的返回逻辑
    // }

    // @Override
    // public AIModel getAIModel() {
    // // TODO Auto-generated method stub
    // throw new UnsupportedOperationException("Unimplemented method 'getAIModel'");
    // }

}
