package customer.ai2code.service.websocket;

import org.springframework.context.ApplicationEvent;

// ContextNode文本变更事件
public class ContextNodeChangedEvent extends ApplicationEvent {
    private final String nodeId;
    private final String oldValue;
    private final String newValue;
    private final String taskId;
    private final String path;

    public ContextNodeChangedEvent(Object source, String nodeId, String oldValue, 
                                  String newValue, String taskId, String path) {
        super(source);
        this.nodeId = nodeId;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.taskId = taskId;
        this.path = path;
    }

    // getters
    public String getNodeId() { return nodeId; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public String getTaskId() { return taskId; }
    public String getPath() { return path; }
}