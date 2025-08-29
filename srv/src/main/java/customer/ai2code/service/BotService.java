package customer.ai2code.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import cds.gen.configservice.BotTypes;
import cds.gen.mainservice.BotInstances;
import cds.gen.mainservice.BotInstancesChatCompletionContext;
import cds.gen.mainservice.BotInstancesExecuteContext;
import cds.gen.mainservice.BotMessages;
import cds.gen.mainservice.BotMessagesAdoptContext;
import cds.gen.mainservice.ContextNodes;
import customer.ai2code.model.bot.Bot;

public interface BotService {
    /**
     * Get Current Bot(with subtasks) by botInstanceId.
     * @param botInstanceId
     * @return
     */
    public Bot getCurrentBot(String botInstanceId);

    /**
     * Get Current Bot(no subtasks) by taskId(as parent) and sequence.
     * @param taskId
     * @param sequence
     * @return
     */
    public Bot getCurrentBot(String taskId, int sequence);

    public BotMessages chat(BotInstancesChatCompletionContext context);

    public BotMessages chat(String botInstanceId, String content);

    public SseEmitter chatInStreaming(String botInstanceId, String content);

    public Boolean executeAsync(BotInstancesExecuteContext context);

    public Boolean executeAsync(String botInstanceId);

    public BotInstancesExecuteContext.ReturnType execute(BotInstancesExecuteContext context);

    public BotInstancesExecuteContext.ReturnType execute(String botInstanceId);

    public ContextNodes adopt(BotMessagesAdoptContext context);

    public ContextNodes adopt(String botInstanceId, String messageId);

    /**
     * Update bot instance status
     * This method updates both database and cache
     * @param bot Bot object containing the instance to update
     * @param status New status value (e.g., "RUNNING", "SUCCESS", "FAILED", "SKIPPED")
     */
    public void updateBotInstanceStatus(Bot bot, String status);

    // public Bot createBotInstance(BotInstances botInstance, BotTypes botType);

    // /**
    // * 根据BotInstance ID获取主任务ID
    // * 通过任务层级关系向上查找，直到找到isMain=true的任务
    // *
    // * @param botInstanceId Bot实例ID
    // * @return 主任务ID
    // * @throws IllegalStateException 如果找不到主任务或数据不一致
    // */
    // public String getMainTaskId(String botInstanceId);
}
