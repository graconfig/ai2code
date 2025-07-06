package customer.ai2code.service.execution.functioncall;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.persistence.PersistenceService;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultDestinationLoader;
import com.sap.cloud.sdk.cloudplatform.connectivity.DefaultHttpDestination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;

import cds.gen.mainservice.ContextNodes;
// import cds.gen.zsrvd_genddls.ZcGenddlsPAutoActiveCDSContext;
// import cds.gen.zsrvd_genddls.ZcGenddlsP_;
// import cds.gen.zsrvd_genddls.ZsgenDdlsSourceList;
// import cds.gen.zsrvd_genddls.ZsrvdGenddls;
// import cds.gen.zsrvd_genddls.ZsrvdGenddls_;
// import cds.gen.zsrvd_genddls.ZtgenddlsL;
import customer.ai2code.service.impl.CreateCdsOdataBotExecution;
import customer.ai2code.service.impl.GenericCqnService;

//@ExtendWith(MockitoExtension.class)
@SpringBootTest
// @AutoConfigureMockMvc
public class CreateCdsOdataBotExecutionTest {
        // @Mock
        // private GenericCqnService cqnService;
        // @Autowired
        // @Qualifier(ZsrvdGenddls_.CDS_NAME)
        // private ZsrvdGenddls zsrvdGenddlsService;

        @Test
        void Savetableinfo() {//

                String DestinationName = "zsrvd_gentabl";
                String auth = "Basic Q0RTX1VTRVI6SGFuZEAxMjM0NTY=";// 接口认证账号
                DefaultHttpDestination httpDestination = DefaultHttpDestination
                                .builder(
                                                "https://handsap01.hand-china.com/sap/opu/odata4/sap/zsrvb_gentabl/srvd/sap/zsrvd_gentabl/0001")
                                .header("Authorization", auth)
                                .property("sap-client", "310")
                                .property("sap-language", "en")
                                .name(DestinationName).build();

                DefaultDestinationLoader loader = new DefaultDestinationLoader();
                loader.registerDestination(httpDestination);
                DestinationAccessor.prependDestinationLoader(loader);
                // 调用接口
                String actionCode = "01";
                System.out.println(actionCode);
                List<ContextNodes> contextnodes = new ArrayList<>();

                // CreateCdsOdataBotExecution callodata = new
                // CreateCdsOdataBotExecution(zsrvdGenddlsService);
                // ZcGenddlsPAutoActiveCDSContext AutoActiveCDSContext =
                // ZcGenddlsPAutoActiveCDSContext.create();
                // CqnSelect select = (CqnSelect) Select.from(ZcGenddlsP_.CDS_NAME);
                // AutoActiveCDSContext.setCqn(select);
                // AutoActiveCDSContext.setProjname("TESTO703_01");
                // AutoActiveCDSContext.setProjdesc("test report222");
                // AutoActiveCDSContext.setWithadditionalsave(false);
                // AutoActiveCDSContext.setWithdraft(false);
                // AutoActiveCDSContext.setTrkorr("DM2K900068");
                // AutoActiveCDSContext.setDevclass("ZAIREPORT");
                // Collection<ZsgenDdlsSourceList> Sources = new ArrayList<>();
                // ZsgenDdlsSourceList source = ZsgenDdlsSourceList.create();
                // source.setViewname("ZI_TEST2_12");
                // source.setViewdesc("interface view ZI TEST2 1");
                // source.setReference("test.context");
                // source.setSourcecode("@AccessControl.authorizationCheck: #NOT_REQUIRED \\n" +
                // //
                // "@EndUserText.label: 'ZI_TEST2_12' \\n" + //
                // "@Metadata.ignorePropagatedAnnotations: true\\n" + //
                // "@Metadata.allowExtensions: true\\n" + //
                // "@AbapCatalog.viewEnhancementCategory: [#NONE]\\n" + //
                // "define root view entity ZI_TEST2_12\\n" + //
                // " as select from I_PurchaseOrderAPI01 as ds1\\n" + //
                // " composition [0..*] of ZI_Z0810E001_002 as _item\\n" + //
                // "{\\n" + //
                // " key ds1.PurchaseOrder,\\n" + //
                // " ds1.PurchasingOrganization,\\n" + //
                // " ds1.PurchaseOrderType,\\n" + //
                // " ds1.Supplier,\\n" + //
                // " _item\\n" + //
                // "}");
                // AutoActiveCDSContext.setSource(Sources);
                // Collection<ZtgenddlsL> Results = new ArrayList<>();
                // Results = callodata.execute("botid", AutoActiveCDSContext);
                // System.out.println("Result=" + Results.toString());
        }
}
