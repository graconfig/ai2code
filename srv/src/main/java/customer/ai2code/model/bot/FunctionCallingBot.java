package customer.ai2code.model.bot;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cds.gen.configservice.BotTypes;
import cds.gen.configservice.PromptTexts;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstancesExecuteContext;
import cds.gen.mainservice.BotMessages;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.config.AIModel;
import customer.ai2code.model.config.AIModelResolver;
import customer.ai2code.service.AIService;
import customer.ai2code.service.impl.BotExecutionFactoryService;
import customer.ai2code.service.PromptService;
import customer.ai2code.service.execution.BotExecution;
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
    private Locale locale;

    // 服务依赖（通过构造函数注入）
    private GenericCqnService genericCqnService;
    private PromptService promptService;
    private AIModelResolver aiModelResolver;
    private BotExecutionFactoryService botExecutionFactoryService; // 新增：BotExecution 工厂服务

    @Override
    public BotInstancesExecuteContext.ReturnType execute() {
        List<PromptTexts> prompts = new ArrayList<>();
        List<BotMessages> messages = new ArrayList<>();

        try {
            System.out.println("=== Starting FunctionCallingBot execution ===");
            System.out.println("- Bot Instance ID: " + botInstance.getId());
            System.out.println("- Bot Type: " + botType.getName());
            System.out.println("- Implementation Class: " + botType.getImplementationClass());

            // 1. 根据AIModel类型，获取到不同AIService服务
            AIService aiService = aiModelResolver.resolveAIService(aiModel.getModelConfigs());


            // 2. 使用genericCqnService.getMainTaskId，再获取Prompt
            // String mainTaskId = genericCqnService.getMainTaskId(botInstance.getId());


            prompts = promptService.getPrompts(this);
            System.out.println("- Retrieved " + prompts.size() + " prompts for execution");

            // 3. 获取历史消息
            // messages = botMessageService.getMessagesByBotInstanceId(botInstance.getId());
            System.out.println("- Retrieved " + messages.size() + " historical messages");

            // 4. 根据botType.getImplementationClass()获取到函数调用的实现类
            String implementationClass = botType.getImplementationClass();


            // 使用工厂服务创建 BotExecution 实例
            BotExecution botExecution = botExecutionFactoryService.createBotExecutionInstance(implementationClass);

            System.out.println("- Created bot execution instance: " + implementationClass);
            System.out.println(botExecutionFactoryService.getBotExecutionInstanceInfo(botExecution));

            // 5. 调用 AI Function Calling
            System.out.println("- Calling AI service function calling...");
            Object result = aiService.functionCalling(messages, prompts, botExecution, aiModel);

            // 6. 处理执行结果
            BotInstancesExecuteContext.ReturnType returnResult = processExecutionResult(result);

            System.out.println("- Function calling execution completed successfully");
            System.out.println("=== FunctionCallingBot execution finished ===");

            return returnResult;

        } catch (BusinessException e) {
            System.err.println("Business error in FunctionCallingBot execution: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 处理执行结果
     */
    private BotInstancesExecuteContext.ReturnType processExecutionResult(Object result) {
        try {
            BotInstancesExecuteContext.ReturnType returnResult = BotInstancesExecuteContext.ReturnType.create();

            // 判断是否为Collection<String> tasks
            if (result instanceof Collection<?>) {
                Collection<?> tasks = (Collection<?>) result;
                if (!tasks.isEmpty() && tasks.iterator().next() instanceof String) {
                    List<String> taskList = new ArrayList<>();
                    for (Object task : tasks) {
                        taskList.add((String) task);
                    }
                    returnResult.setTasks(taskList);
                    System.out.println("- Function call returned " + taskList.size() + " tasks");
                } 
            }

            if (result instanceof String) {
                String resultString = convertResultToString(result);
                returnResult.setResult(resultString);
                System.out.println("- Execution result processed successfully");
                System.out.println("- Result length: " + resultString.length() + " characters");
            }

            return returnResult;

        } catch (Exception e) {
            System.err.println("Error processing execution result: " + e.getMessage());
            BotInstancesExecuteContext.ReturnType errorResult = BotInstancesExecuteContext.ReturnType.create();
            errorResult.setResult("Error processing result: " + e.getMessage());
            // errorResult.setSuccess(false);
            return errorResult;
        }
    }

    /**
     * 将结果转换为字符串
     */
    private String convertResultToString(Object result) {
        try {
            if (result instanceof String) {
                return (String) result;
            } else if (result instanceof List) {
                List<?> list = (List<?>) result;
                StringBuilder sb = new StringBuilder();
                sb.append("Function call returned ").append(list.size()).append(" items:\n");
                for (int i = 0; i < list.size(); i++) {
                    sb.append((i + 1)).append(". ").append(list.get(i).toString()).append("\n");
                }
                return sb.toString();
            } else {
                return result.toString();
            }
        } catch (Exception e) {
            return "Result conversion error: " + e.getMessage();
        }
    }

    @Override
    public Boolean executeAsync() {
        try {
            new Thread(() -> {
                try {
                    execute();
                } catch (Exception e) {
                    System.err.println("Async execution failed: " + e.getMessage());
                }
            }).start();
            return true;
        } catch (Exception e) {
            System.err.println("Failed to start async execution: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Boolean stop() {
        System.out.println("Stopping FunctionCallingBot execution...");
        return true;
    }

    @Override
    public Boolean resume() {
        System.out.println("Resuming FunctionCallingBot execution...");
        return true;
    }

    @Override
    public Boolean cancel() {
        System.out.println("Cancelling FunctionCallingBot execution...");
        return true;
    }

    @Override
    public SseEmitter chatInStreaming(String content) {
        throw new UnsupportedOperationException("FunctionCallingBot does not support chatInStreaming method directly. Use execute() instead.");
    }

    @Override
    public String chat(String content) {
        throw new UnsupportedOperationException("FunctionCallingBot does not support chat method directly. Use execute() instead.");
    }
}
