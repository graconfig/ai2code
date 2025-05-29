package customer.ai2code.model;

import java.util.concurrent.Executor;

import cds.gen.mainservice.BotInstancesExecuteContext;

public interface Bot {
    public final Executor executor = null;

    public BotInstancesExecuteContext.ReturnType execute();

    public Boolean executeAsync();

    public Boolean stop();

    public Boolean resume();

    public Boolean cancel();
}
