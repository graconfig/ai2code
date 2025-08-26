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

import cds.gen.com.sap.gateway.srvd.zsrvd_genbdef.v0001.V0001;
import cds.gen.com.sap.gateway.srvd.zsrvd_genbdef.v0001.ZcGenbdefTAutoActiveBDEFContext;
import cds.gen.com.sap.gateway.srvd.zsrvd_genbdef.v0001.ZcGenbdefT_;
import cds.gen.com.sap.gateway.srvd.zsrvd_genbdef.v0001.ZsgenBdefAction;
import cds.gen.com.sap.gateway.srvd.zsrvd_genbdef.v0001.ZsgenBdefView;
import cds.gen.com.sap.gateway.srvd.zsrvd_genbdef.v0001.ZtgenbdefL;
import cds.gen.mainservice.CreateGenBdefOdata_;
import cds.gen.mainservice.MainService_;
import customer.ai2code.service.ContextService;
import customer.ai2code.service.impl.TaskBotCacheManager;
import customer.ai2code.service.impl.execution.CreateGenBdefOdataBotExecution;

@Component
@ServiceName(MainService_.CDS_NAME)
public class MainServiceCreateGenbdefHandler implements EventHandler {

    private final V0001 zsrvdGenbdef;
    private  CreateGenBdefOdataBotExecution genbdefExecution = null;
    private final TaskBotCacheManager taskBotCacheManager;

    public MainServiceCreateGenbdefHandler(
            ContextService contextService,
            V0001 zsrvdGenbdef,
            ObjectMapper objectMapper,
            TaskBotCacheManager taskBotCacheManager) {
        this.zsrvdGenbdef = zsrvdGenbdef;
        this.genbdefExecution = new CreateGenBdefOdataBotExecution(
                contextService, zsrvdGenbdef, objectMapper, taskBotCacheManager);
        this.taskBotCacheManager = taskBotCacheManager;
    }

    @On(event = CqnService.EVENT_READ, entity = CreateGenBdefOdata_.CDS_NAME)
    public void Read(CdsReadEventContext context) throws IOException {
        // 创建Genbdef自动激活上下文
        ZcGenbdefTAutoActiveBDEFContext autoActiveContext = ZcGenbdefTAutoActiveBDEFContext.create();
        CqnSelect select = (CqnSelect) Select.from(ZcGenbdefT_.CDS_NAME);
        autoActiveContext.setCqn(select);

        // 设置基本参数
        autoActiveContext.setBdefname("ZR_ZT_PO_29_H");
        autoActiveContext.setBdefdesc("BDEF of ZR_ZT_PO_29_H");
        autoActiveContext.setBdeftype("1");
        autoActiveContext.setWithdraft(true);
        autoActiveContext.setWithunmanagedsave(true);
        autoActiveContext.setImpclass("ZBP_I_PO_29_H");
        autoActiveContext.setReference("TEST CREATE I");
        autoActiveContext.setDevclass("ZRAP_ODATA_TEST");
        autoActiveContext.setTrkorr("DM2K900386");

        // 构建VIEW集合参数
        Collection<ZsgenBdefView> views = new ArrayList<>();

        // 第一个VIEW
        ZsgenBdefView mainView = ZsgenBdefView.create();
        mainView.setViewname("ZR_ZT_PO_29_H");
        mainView.setDraftTabname("ZZT_PO_29_H_D");
        mainView.setEtagFieldname("LOCALLASTCHANGEDAT");
        mainView.setMaster(true);
        mainView.setWithadditionalsave(false);

        // 第一个VIEW的ACTION集合
        Collection<ZsgenBdefAction> mainActions = new ArrayList<>();
        ZsgenBdefAction action1 = ZsgenBdefAction.create();
        action1.setActionname("TEST1");
        action1.setStaticflag(false);
        action1.setReqparam("");
        action1.setRescardinality("1");
        action1.setResparam("$self");
        mainActions.add(action1);

        ZsgenBdefAction action2 = ZsgenBdefAction.create();
        action2.setActionname("TEST2");
        action2.setStaticflag(false);
        action2.setReqparam("");
        action2.setRescardinality("1");
        action2.setResparam("$self");
        mainActions.add(action2);
        mainView.setAction(mainActions);
        views.add(mainView);

        // 第二个VIEW
        ZsgenBdefView itemView = ZsgenBdefView.create();
        itemView.setViewname("ZR_ZT_PO_29_I");
        itemView.setDraftTabname("ZZT_PO_29_I_D");
        itemView.setEtagFieldname("");
        itemView.setMaster(false);
        itemView.setWithadditionalsave(false);

        // 第二个VIEW的ACTION集合
        Collection<ZsgenBdefAction> itemActions = new ArrayList<>();
        ZsgenBdefAction action3 = ZsgenBdefAction.create();
        action3.setActionname("TEST3");
        action3.setStaticflag(false);
        action3.setReqparam("");
        action3.setRescardinality("1");
        action3.setResparam("$self");
        itemActions.add(action3);
        itemView.setAction(itemActions);
        views.add(itemView);

        autoActiveContext.setView(views);

        // 执行OData调用
        String result = genbdefExecution.execute("genbdef-bot-id", autoActiveContext);
        System.out.println("Genbdef Execution Result: " + result);
    }

    @On(event = CqnService.EVENT_CREATE, entity = CreateGenBdefOdata_.CDS_NAME)
    public void Create(CdsCreateEventContext context) throws IOException {
        // 处理创建事件逻辑（如需）
    }
}