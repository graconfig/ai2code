package customer.ai2code.service.impl.execution;

import java.util.ArrayList;
import java.util.Collection;

import cds.gen.zsrvd_genbdef.ZsrvdGenbdef;
import cds.gen.zsrvd_genbdef.ZcGenbdefTAutoActiveBDEFContext;
import cds.gen.zsrvd_genbdef.ZcGenbdefT_;
import cds.gen.zsrvd_genbdef.ZsgenBdefAction;
import cds.gen.zsrvd_genbdef.ZsgenBdefView;
import cds.gen.zsrvd_genbdef.ZtgenbdefL;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.ServiceException;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataResponseException;
import customer.ai2code.model.execution.annotation.ExecuteMethod;
import customer.ai2code.model.execution.annotation.ExecuteParameter;
import customer.ai2code.service.ContextService;
import customer.ai2code.service.execution.BotExecution;
import customer.ai2code.service.impl.TaskBotCacheManager;

public class CreateGenBdefOdataBotExecution implements BotExecution {

    private final ContextService contextService;
    private final ZsrvdGenbdef zsrvdGenbdef;
    private final ObjectMapper objectMapper;
    private final TaskBotCacheManager taskBotCacheManager;

    public CreateGenBdefOdataBotExecution(
            ContextService contextService,
            ZsrvdGenbdef zsrvdGenbdef,
            ObjectMapper objectMapper,
            TaskBotCacheManager taskBotCacheManager) {
        this.contextService = contextService;
        this.zsrvdGenbdef = zsrvdGenbdef;
        this.objectMapper = objectMapper;
        this.taskBotCacheManager = taskBotCacheManager;
    }

    @ExecuteMethod
    public String execute(
            @ExecuteParameter(name = "botInstanceId", description = "Bot Instance ID") String botInstanceId,
            @ExecuteParameter(name = "autoActiveBDEFContext", description = "Auto Active BDEF Context") ZcGenbdefTAutoActiveBDEFContext autoActiveBDEFContext) {
        // 创建新的上下文并复制参数
        ZcGenbdefTAutoActiveBDEFContext context = ZcGenbdefTAutoActiveBDEFContext.create();
        CqnSelect select = Select.from(ZcGenbdefT_.CDS_NAME);
        context.setCqn(select);

        // 复制基本参数
        context.setBdefname(autoActiveBDEFContext.getBdefname());
        context.setBdefdesc(autoActiveBDEFContext.getBdefdesc());
        context.setBdeftype(autoActiveBDEFContext.getBdeftype());
        context.setWithdraft(autoActiveBDEFContext.getWithdraft());
        context.setWithunmanagedsave(autoActiveBDEFContext.getWithunmanagedsave());
        context.setImpclass(autoActiveBDEFContext.getImpclass());
        context.setReference(autoActiveBDEFContext.getReference());
        context.setDevclass(autoActiveBDEFContext.getDevclass());
        context.setTrkorr(autoActiveBDEFContext.getTrkorr());

        // 处理View集合参数
        Collection<ZsgenBdefView> views = new ArrayList<>();
        autoActiveBDEFContext.getView().forEach(view -> {
            ZsgenBdefView newView = ZsgenBdefView.create();
            newView.setViewname(view.getViewname());
            newView.setDraftTabname(view.getDraftTabname());
            newView.setEtagFieldname(view.getEtagFieldname());
            newView.setMaster(view.getMaster());
            newView.setWithadditionalsave(view.getWithadditionalsave());

            // 处理_ACTION子集合
            Collection<ZsgenBdefAction> actions = new ArrayList<>();
            view.getAction().forEach(action -> {
                ZsgenBdefAction newAction = ZsgenBdefAction.create();
                newAction.setActionname(action.getActionname());
                newAction.setStaticflag(action.getStaticflag());
                newAction.setReqparam(action.getReqparam());
                newAction.setRescardinality(action.getRescardinality());
                newAction.setResparam(action.getResparam());
                actions.add(newAction);
            });
            newView.setAction(actions);

            views.add(newView);
        });
        context.setView(views);

        // 调用ODATA服务
        String result = "";
        try {
            zsrvdGenbdef.emit(context);
        } catch (ServiceException e) {
            // 处理OData服务异常
            ODataResponseException odataEx = (ODataResponseException) e.getCause().getCause();
            String errorMsg = odataEx.getHttpBody().getOrElse("Unknown error");
            return "Error: " + errorMsg;
        } catch (Exception e) {
            return "Error: Parameter serialization failed";
        }

        // 处理返回结果
        try {
            Collection<ZtgenbdefL> results = context.getResult();
            if (results == null || results.isEmpty()) {
                result = "No results returned from service";
            } else {
                result = objectMapper.writeValueAsString(results);
                // 更新上下文信息
                for (ZtgenbdefL item : results) {
                    contextService.updateAdditionInfo(
                            taskBotCacheManager.getCachedBot(botInstanceId),
                            item.getReference(),
                            objectMapper.writeValueAsString(item));
                }
            }
        } catch (JsonProcessingException e) {
            result = "Error: Result processing failed" + e.getMessage();
        }

        return result;
    }
}