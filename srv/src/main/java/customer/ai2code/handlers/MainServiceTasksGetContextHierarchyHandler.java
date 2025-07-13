package customer.ai2code.handlers;

import cds.gen.mainservice.MainService_;
import cds.gen.mainservice.TasksGetContextHierarchyContext;
import customer.ai2code.service.ContextService;

import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

// import org.apache.tomcat.util.descriptor.web.ContextService;
import org.springframework.stereotype.Component;

@Component
@ServiceName(MainService_.CDS_NAME)
public class MainServiceTasksGetContextHierarchyHandler implements EventHandler {

  private ContextService contextService;

  public MainServiceTasksGetContextHierarchyHandler(ContextService contextService) {
    this.contextService = contextService;
  }

  @On
  public void handleGetContextHierarchy(TasksGetContextHierarchyContext context) {
    // Your code goes here
    // context.setCompleted();
    context.setResult(
        contextService.buildContextAsHierarchy(context));
  }
}
