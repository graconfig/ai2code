package customer.ai2code.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.cds.CdsCreateEventContext;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;

import cds.gen.mainservice.CreateServiceDefinition_;
import cds.gen.mainservice.MainService_;
import cds.gen.zsrvd_gensrvd.ZcGensrvdTAutoActiveSRVDSRVBContext;
import cds.gen.zsrvd_gensrvd.ZcGensrvdT_;
import cds.gen.zsrvd_gensrvd.ZsgenSrvdExposeList;
import cds.gen.zsrvd_gensrvd.ZsrvdGensrvd;
import customer.ai2code.service.ContextService;
import customer.ai2code.service.impl.execution.CreateServiceDefinitionBotExecution;

@Component
@ServiceName(MainService_.CDS_NAME)
public class MainServiceCreateServiceDefinitionHandler {
    private final ZsrvdGensrvd zsrvdGensrvd;
    private CreateServiceDefinitionBotExecution callodata = null;

    MainServiceCreateServiceDefinitionHandler(ContextService contextService, ZsrvdGensrvd zsrvdGensrvd,
            ObjectMapper objectMapper) {
        this.callodata = new CreateServiceDefinitionBotExecution(contextService, zsrvdGensrvd, objectMapper);
        this.zsrvdGensrvd = zsrvdGensrvd;
    }

    @On(event = CqnService.EVENT_READ, entity = CreateServiceDefinition_.CDS_NAME)
    public void Read(CdsReadEventContext context) throws IOException {
        // 填充报文
        ZcGensrvdTAutoActiveSRVDSRVBContext autoActiveSRVDSRVBContext = ZcGensrvdTAutoActiveSRVDSRVBContext.create();
        CqnSelect cqnSelect = Select.from(ZcGensrvdT_.CDS_NAME);
        autoActiveSRVDSRVBContext.setCqn(cqnSelect);
        autoActiveSRVDSRVBContext.setSrvdname("ZSRVD_TEST006");
        autoActiveSRVDSRVBContext.setSrvddesc("SRVD Description");
        autoActiveSRVDSRVBContext.setLabeltext("SRVD Labletext");
        autoActiveSRVDSRVBContext.setLeadingEntity("ZC_RAP100_ATRA2390");
        autoActiveSRVDSRVBContext.setDevclass("ZAIREPORT");
        autoActiveSRVDSRVBContext.setTrkorr("DM2K900068");
        autoActiveSRVDSRVBContext.setReference("ZTESTSRVD0006");
        autoActiveSRVDSRVBContext.setBindingType("ODATA_V4_UI");
        autoActiveSRVDSRVBContext.setSrvbname("ZSRVB_TEST006");
        autoActiveSRVDSRVBContext.setSrvbdesc("SRVB Description");

        Collection<ZsgenSrvdExposeList> exposeList = new ArrayList<>();
        ZsgenSrvdExposeList expose = ZsgenSrvdExposeList.create();
        expose.setDdlsname("ZC_RAP100_ATRA2390");
        exposeList.add(expose);
        autoActiveSRVDSRVBContext.setExpose(exposeList);

        String result = callodata.execute("botid", autoActiveSRVDSRVBContext);
        System.out.println("result=" + result);
    }

    @On(event = CqnService.EVENT_CREATE, entity = CreateServiceDefinition_.CDS_NAME)
    public void Create(CdsCreateEventContext context) throws IOException {

    }

}
