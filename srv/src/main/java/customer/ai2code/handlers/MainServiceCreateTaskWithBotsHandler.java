package customer.ai2code.handlers;

import cds.gen.mainservice.CreateTaskWithBotsContext;
import cds.gen.mainservice.MainService_;
import customer.ai2code.service.TaskService;

import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.springframework.stereotype.Component;

@Component
@ServiceName(MainService_.CDS_NAME)
public class MainServiceCreateTaskWithBotsHandler implements EventHandler {

  private final TaskService taskService;

  public MainServiceCreateTaskWithBotsHandler(TaskService taskService) {
    // Constructor can be used for dependency injection if needed
    this.taskService = taskService;
  }

  @On
  public void handleCreateTaskWithBots(CreateTaskWithBotsContext context) {
    // Your code goes here
    // context.setCompleted();
    taskService.createTaskWithBots(context);
  }
}
