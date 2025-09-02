package customer.ai2code.service.websocket;

import org.springframework.context.ApplicationEvent;

import customer.ai2code.service.impl.TaskServiceImpl.HierarchyNode;

public class TaskTreeChangedEvent extends ApplicationEvent {
    private final String taskId;
    private final HierarchyNode taskTree;

    public TaskTreeChangedEvent(Object source, String taskId, HierarchyNode taskTree) {
        super(source);
        this.taskId = taskId;
        this.taskTree = taskTree;
    }

    public String getTaskId() {
        return taskId;
    }

    public HierarchyNode getTaskTree() {
        return taskTree;
    }
}
