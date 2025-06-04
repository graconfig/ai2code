package customer.ai2code.service.impl;

import customer.ai2code.service.execution.BotExecution;
import customer.ai2code.service.execution.annotation.BotExecutor;
import customer.ai2code.service.execution.annotation.ExecuteMethod;
import customer.ai2code.service.execution.annotation.ExecuteParameter;

@BotExecutor(name = "Create Tasks Bot Execution", description = "Implementation for creating tasks in the bot execution framework", version = "1.0", enabled = true)
public class CreateTasksBotExecution implements BotExecution {
    @ExecuteMethod
    public String execute(String param) {
        throw new UnsupportedOperationException("Unimplemented method 'execute'");
    }

}
