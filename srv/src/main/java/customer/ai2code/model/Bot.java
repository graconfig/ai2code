package customer.ai2code.model;

import java.util.concurrent.Executor;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cds.gen.mainservice.BotInstancesExecuteContext;

public interface Bot {
    public final Executor executor = null;

    public BotInstancesExecuteContext.ReturnType execute();

    public Boolean executeAsync();

    public Boolean stop();

    public Boolean resume();

    public Boolean cancel();

    public SseEmitter chatInStreaming(String content);

    public String chat(String content);
}
