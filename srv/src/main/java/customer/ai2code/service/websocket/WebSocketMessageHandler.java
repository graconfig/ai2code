package customer.ai2code.service.websocket;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketMessageHandler {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketMessageHandler(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // 处理BotInstance状态变更事件
    @EventListener
    public void handleBotStatusChanged(BotInstanceStatusChangedEvent event) {
        // 构建消息对象
        BotStatusMessage message = new BotStatusMessage(
            event.getBotInstanceId(),
            event.getOldStatus(),
            event.getNewStatus(),
            System.currentTimeMillis()
        );
        
        // 推送到/topic/bot-status/{taskId}，让订阅该任务的客户端接收
        messagingTemplate.convertAndSend(
            "/topic/bot-status/" + event.getTaskId(), 
            message
        );
    }

    // 处理ContextNode文本变更事件
    @EventListener
    public void handleContextNodeChanged(ContextNodeChangedEvent event) {
        // 构建消息对象
        ContextNodeMessage message = new ContextNodeMessage(
            event.getNodeId(),
            event.getPath(),
            event.getOldValue(),
            event.getNewValue(),
            System.currentTimeMillis()
        );
        
        // 推送到/topic/context/{taskId}
        messagingTemplate.convertAndSend(
            "/topic/context/" + event.getTaskId(), 
            message
        );
    }

    // 消息类定义
    public static class BotStatusMessage {
        private final String botInstanceId;
        private final String oldStatus;
        private final String newStatus;
        private final long timestamp;

        public BotStatusMessage(String botInstanceId, String oldStatus, String newStatus, long timestamp) {
            this.botInstanceId = botInstanceId;
            this.oldStatus = oldStatus;
            this.newStatus = newStatus;
            this.timestamp = timestamp;
        }
        
        // getters for JSON serialization
    }

    public static class ContextNodeMessage {
        private final String nodeId;
        private final String path;
        private final String oldValue;
        private final String newValue;
        private final long timestamp;

        public ContextNodeMessage(String nodeId, String path, String oldValue, String newValue, long timestamp) {
            this.nodeId = nodeId;
            this.path = path;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.timestamp = timestamp;
        }
        
        // getters for JSON serialization
    }
}
