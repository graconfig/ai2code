package customer.ai2code.handlers;

import cds.gen.mainservice.BotInstancesChatCompletionContext;
import cds.gen.mainservice.MainService_;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.springframework.stereotype.Component;

@Component
@ServiceName(MainService_.CDS_NAME)
public class MainServiceBotInstancesChatCompletionHandler implements EventHandler {
  @On
  public void handleChatCompletion(BotInstancesChatCompletionContext context) {
    // Your code goes here
    context.setCompleted();
  }
}
