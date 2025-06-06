package customer.ai2code.service.execution;

@FunctionalInterface
public interface BotExecutionFactory {
    public BotExecution create(String botName, String description, String version, boolean enabled);
}
