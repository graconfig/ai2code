package customer.ai2code.service.websocket;

import org.springframework.context.ApplicationEvent;

// BotInstance状态变更事件
public class BotInstanceStatusChangedEvent extends ApplicationEvent {
    private final String botInstanceId;
    private final String oldStatus;
    private final String newStatus;
    private final String taskId;

    public BotInstanceStatusChangedEvent(Object source, String botInstanceId, String oldStatus, 
                                        String newStatus, String taskId) {
        super(source);
        this.botInstanceId = botInstanceId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.taskId = taskId;
    }

    // getters
    public String getBotInstanceId() { return botInstanceId; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public String getTaskId() { return taskId; }
}

