package customer.ai2code.model.bot;

// import java.util.concurrent.Executor;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cds.gen.configservice.BotTypes;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstancesExecuteContext;
// import cds.gen.mainservice.BotType;
import customer.ai2code.model.config.AIModel;

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
}
