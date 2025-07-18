package customer.ai2code.model.bot;

import java.util.Locale;

// import java.util.concurrent.Executor;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cds.gen.configservice.BotTypes;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstancesExecuteContext;
import customer.ai2code.model.ai.config.model.AIModel;

public interface Bot {
    // public final Executor executor = null;

    public BotInstancesExecuteContext.ReturnType execute();

    public Boolean executeAsync();

    public Boolean stop();

    public Boolean resume();

    public Boolean cancel();

    public SseEmitter chatInStreaming(String content);

    public String chat(String content);

    public BotInstances getBotInstance();

    public BotTypes getBotType();

    public AIModel getAiModel();

    public Locale getLocale();
    
    /**
     * 更新Bot实例状态
     * @param statusCode 状态代码
     * @return 更新后的BotInstances
     */
    public BotInstances updateStatus(String statusCode);
    
    /**
     * 更新Bot实例执行结果
     * @param result 执行结果
     * @return 更新后的BotInstances
     */
    public BotInstances updateResult(String result);
    
    /**
     * 更新Bot实例关联的ContextNode ID
     * @param contextNodeId 上下文节点ID
     * @return 更新后的BotInstances
     */
    public BotInstances updateContextNodeId(String contextNodeId);
    
    /**
     * 更新Bot实例
     * @return 更新后的BotInstances
     */
    public BotInstances update(BotInstances botInstance);
}
