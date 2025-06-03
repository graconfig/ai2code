package customer.ai2code.handlers;

import cds.gen.mainservice.CreateTaskWithBotsContext;
import cds.gen.mainservice.MainService_;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.springframework.stereotype.Component;

@Component
@ServiceName(MainService_.CDS_NAME)
public class MainServiceCreateTaskWithBotsHandler implements EventHandler {
  @On
  public void handleCreateTaskWithBots(CreateTaskWithBotsContext context) {
    // Your code goes here
    context.setCompleted();
  }
}
