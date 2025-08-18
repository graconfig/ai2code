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
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import cds.gen.com.sap.gateway.srvd.zsrvd_genddls.v0001.ZcGenddlsPAutoActiveCDSContext;
import cds.gen.com.sap.gateway.srvd.zsrvd_genddls.v0001.V0001;
import cds.gen.com.sap.gateway.srvd.zsrvd_genddls.v0001.ZcGenddlsP_;
import cds.gen.com.sap.gateway.srvd.zsrvd_genddls.v0001.ZsgenDdlsSourceList;
import cds.gen.com.sap.gateway.srvd.zsrvd_genddls.v0001.ZtgenddlsL;
import cds.gen.mainservice.CreateCds_;
import cds.gen.mainservice.MainService_;
import customer.ai2code.service.ContextService;
import customer.ai2code.service.impl.GenericCqnService;
import customer.ai2code.service.impl.TaskBotCacheManager;
import customer.ai2code.service.impl.execution.CreateCdsOdataBotExecution;

@Component
@ServiceName(MainService_.CDS_NAME)
public class MainServiceCreateCdsHandler implements EventHandler {
    private final V0001 zsrvdGenddls;
    private CreateCdsOdataBotExecution callodata = null;
    private final TaskBotCacheManager taskBotCacheManager;

    MainServiceCreateCdsHandler(ContextService contextService, V0001 zsrvdGenddls, ObjectMapper objectMapper, TaskBotCacheManager taskBotCacheManager) {
        this.callodata = new CreateCdsOdataBotExecution(contextService, zsrvdGenddls,
                objectMapper, taskBotCacheManager);
        this.zsrvdGenddls = zsrvdGenddls;
        this.taskBotCacheManager = taskBotCacheManager;
    }

    @On(event = CqnService.EVENT_READ, entity = CreateCds_.CDS_NAME)
    public void Read(CdsReadEventContext context) throws IOException {
        ZcGenddlsPAutoActiveCDSContext AutoActiveCDSContext =
        ZcGenddlsPAutoActiveCDSContext.create();
        CqnSelect select = (CqnSelect) Select.from(ZcGenddlsP_.CDS_NAME);
        AutoActiveCDSContext.setCqn(select);
        AutoActiveCDSContext.setProjname("TESTO703_01");
        AutoActiveCDSContext.setProjdesc("test report222");
        AutoActiveCDSContext.setWithadditionalsave(false);
        AutoActiveCDSContext.setWithdraft(false);
        AutoActiveCDSContext.setTrkorr("DM2K900068");
        AutoActiveCDSContext.setDevclass("ZAIREPORT");
        Collection<ZsgenDdlsSourceList> Sources = new ArrayList<>();
        ZsgenDdlsSourceList source = ZsgenDdlsSourceList.create();
        source.setViewname("ZI_TEST2_12");
        // source.setViewdesc(null);
        source.setViewdesc("interface view ZI TEST2 1");
        source.setReference("test.context");
        source.setSourcecode("@AccessControl.authorizationCheck: #NOT_REQUIRED \\n" +
        //
        "@EndUserText.label: 'ZI_TEST2_12' \\n" + //
        "@Metadata.ignorePropagatedAnnotations: true\\n" + //
        "@Metadata.allowExtensions: true\\n" + //
        "@AbapCatalog.viewEnhancementCategory: [#NONE]\\n" + //
        "define root view entity ZI_TEST2_12\\n" + //
        " as select from I_PurchaseOrderAPI01 as ds1\\n" + //
        " composition [0..*] of ZI_Z0810E001_002 as _item\\n" + //
        "{\\n" + //
        " key ds1.PurchaseOrder,\\n" + //
        " ds1.PurchasingOrganization,\\n" + //
        " ds1.PurchaseOrderType,\\n" + //
        " ds1.Supplier,\\n" + //
        " _item\\n" + //
        "}");
        Sources.add(source);
        AutoActiveCDSContext.setSource(Sources);
        System.out.println("CdsSource=" + source.toString());
        Collection<ZtgenddlsL> Results = new ArrayList<>();
        String result = callodata.execute("botid", AutoActiveCDSContext);
        System.out.println("result=" + result);
    }

    @On(event = CqnService.EVENT_CREATE, entity = CreateCds_.CDS_NAME)
    public void Create(CdsCreateEventContext context) throws IOException {
        // context.setResult(callremoteodata.execute(context.getCqn()));
    }

}
