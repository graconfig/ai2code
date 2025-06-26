package customer.ai2code.handlers;

import cds.gen.mainservice.BotInstancesExecuteContext;
import cds.gen.mainservice.MainService_;
import customer.ai2code.service.BotService;

import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.springframework.stereotype.Component;

@Component
@ServiceName(MainService_.CDS_NAME)
public class MainServiceBotInstancesExecuteHandler implements EventHandler {

  private final BotService botService;

  public MainServiceBotInstancesExecuteHandler(BotService botService) {
    // Constructor can be used for dependency injection if needed
    this.botService = botService;
  }

  @On
  public void handleExecute(BotInstancesExecuteContext context) {
    // Your code goes here
    // context.setCompleted();
    context.setResult(botService.execute(context));
  }
}
