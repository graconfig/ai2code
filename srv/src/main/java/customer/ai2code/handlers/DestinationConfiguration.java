package customer.ai2code.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;
import com.sap.cds.services.application.ApplicationLifecycleService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.Before;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultDestinationLoader;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;

@Component
@ServiceName(ApplicationLifecycleService.DEFAULT_NAME)
public class DestinationConfiguration implements EventHandler {

	@Value("${s4.apikey:}")
	private String apiKey;
	@Value("${s4.localapi:}")
	private String localapi;
	@Autowired
	private ConfigurableEnvironment environment;

	@Before(event = ApplicationLifecycleService.EVENT_APPLICATION_PREPARED)
	void initializeDestinations() {
		String auth = "Basic Q0RTX1VTRVI6SGFuZEAxMjM0NTY=";// 接口认证账号
		String activepro = null;
		// MutablePropertySources zpro = environment.getPropertySources();

		String[] ActiveProfiles = environment.getActiveProfiles();
		if (ActiveProfiles.length > 0) {
			activepro = ActiveProfiles[0];
		} else {
			ActiveProfiles = environment.getDefaultProfiles();
			activepro = ActiveProfiles[0];
		}
		if (activepro == "default") {
			DefaultHttpDestination httpDestination = DefaultHttpDestination
					.builder(
							"https://handsap01.hand-china.com")
					.header("Authorization", auth)
					.property("sap-client", "310")
					.property("sap-language", "en")
					.name("zsrvd_genddls").build();

			DefaultDestinationLoader loader = new DefaultDestinationLoader();
			loader.registerDestination(httpDestination);
			DestinationAccessor.prependDestinationLoader(loader);

			loader = new DefaultDestinationLoader();
			loader.registerDestination(httpDestination);
			DestinationAccessor.prependDestinationLoader(loader);

			httpDestination = DefaultHttpDestination
					.builder(
							"https://handsap01.hand-china.com")
					.header("Authorization", auth)
					.property("sap-client", "310")
					.property("sap-language", "en")
					.name("zsrvd_gensrvd").build();

			loader = new DefaultDestinationLoader();
			loader.registerDestination(httpDestination);
			DestinationAccessor.prependDestinationLoader(loader);

		}

	}

	// @EventListener
	// void applicationReady(ApplicationReadyEvent ready) {
	// 	int port = Integer.valueOf(environment.getProperty("local.server.port"));
	// 	DefaultHttpDestination mockDestination = DefaultHttpDestination
	// 			.builder("http://localhost:" + port)
	// 			.name("s4-business-partner-api-mocked").build();

	// 	DefaultDestinationLoader loader = new DefaultDestinationLoader();
	// 	loader.registerDestination(mockDestination);
	// 	DestinationAccessor.prependDestinationLoader(loader);
	// }

}
