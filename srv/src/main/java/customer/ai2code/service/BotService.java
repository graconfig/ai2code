package customer.ai2code.service;

import cds.gen.mainservice.BotInstancesExecuteContext;
import customer.ai2code.model.Bot;

public interface BotService {

    public Bot getCurrentBot(String botInstanceId);

    public Bot getCurrentBot(String taskId, int sequence);

    public Boolean executeAsync(BotInstancesExecuteContext context);
    public Boolean executeAsync(String botInstanceId);

    public BotInstancesExecuteContext.ReturnType execute(BotInstancesExecuteContext context);
    public BotInstancesExecuteContext.ReturnType execute(String botInstanceId);
}
