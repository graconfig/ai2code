package customer.ai2code.handlers;

import cds.gen.mainservice.MainService_;
import cds.gen.mainservice.TasksGetHierarchyContext;
import customer.ai2code.service.TaskService;

import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.springframework.stereotype.Component;

@Component
@ServiceName(MainService_.CDS_NAME)
public class MainServiceTasksGetHierarchyHandler implements EventHandler {

  private TaskService taskService;

  public MainServiceTasksGetHierarchyHandler(TaskService taskService) {
    this.taskService = taskService;
  }

  @On
  public void handleGetHierarchy(TasksGetHierarchyContext context) {
    // Your code goes here
    // context.setCompleted();
    context.setResult(taskService.getHierarchy(context));
  }
}
