package customer.ai2code.handlers;

import cds.gen.mainservice.BotMessagesAdoptContext;
import cds.gen.mainservice.MainService_;
import customer.ai2code.service.BotService;

import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import org.springframework.stereotype.Component;

@Component
@ServiceName(MainService_.CDS_NAME)
public class MainServiceBotMessagesAdoptHandler implements EventHandler {

  private final BotService botService;

  public MainServiceBotMessagesAdoptHandler(BotService botService) {
    // Constructor can be used for dependency injection if needed
    this.botService = botService;
  }

  @On
  public void handleAdopt(BotMessagesAdoptContext context) {
    botService.adopt(context);
  }
}
