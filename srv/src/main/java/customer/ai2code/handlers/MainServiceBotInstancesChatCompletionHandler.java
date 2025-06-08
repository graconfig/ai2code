package customer.ai2code.handlers;

import cds.gen.mainservice.BotInstancesChatCompletionContext;
import cds.gen.mainservice.BotInstances_;
import cds.gen.mainservice.MainService_;
import customer.ai2code.service.BotService;

import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.springframework.stereotype.Component;

@Component
@ServiceName(MainService_.CDS_NAME)
public class MainServiceBotInstancesChatCompletionHandler implements EventHandler {

  private final BotService botService;

  public MainServiceBotInstancesChatCompletionHandler(BotService botService) {
    // Constructor can be used for dependency injection if needed
    this.botService = botService;
  }

  @On(entity = BotInstances_.CDS_NAME, event = BotInstancesChatCompletionContext.CDS_NAME)
  public void handleChatCompletion(BotInstancesChatCompletionContext context) {
    // Your code goes here
    context.setResult(botService.chat(context));
    // context.setCompleted();
    // context.setResult();
  }
}
