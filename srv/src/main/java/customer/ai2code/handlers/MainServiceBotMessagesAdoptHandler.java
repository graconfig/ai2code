package customer.ai2code.handlers;

import cds.gen.mainservice.BotMessagesAdoptContext;
import cds.gen.mainservice.BotMessages_;
import cds.gen.mainservice.ContextNodes;
import cds.gen.mainservice.MainService_;
import customer.ai2code.service.BotService;

import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import java.util.Collections;

import org.springframework.stereotype.Component;

@Component
@ServiceName(MainService_.CDS_NAME)
public class MainServiceBotMessagesAdoptHandler implements EventHandler {

  private final BotService botService;

  public MainServiceBotMessagesAdoptHandler(BotService botService) {
    // Constructor can be used for dependency injection if needed
    this.botService = botService;
  }

  @On(entity = BotMessages_.CDS_NAME, event = BotMessagesAdoptContext.CDS_NAME)
  public void handleAdopt(BotMessagesAdoptContext context) {
    ContextNodes node = botService.adopt(context);
    context.setResult(Collections.singletonList(node));
  }
}
