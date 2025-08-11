package customer.ai2code.service.impl.execution;

import java.util.ArrayList;
import java.util.Collection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.ServiceException;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataResponseException;

import cds.gen.zsrvd_genclas.ZsrvdGenclas;
import cds.gen.zsrvd_genclas.ZcGenclasTAutoActiveCLASContext;
import cds.gen.zsrvd_genclas.ZcGenclasT_;
import cds.gen.zsrvd_genclas.ZsgenClasAttrList;
import cds.gen.zsrvd_genclas.ZsgenClasMethodsList;
import cds.gen.zsrvd_genclas.ZsgenClasTypesList;
import cds.gen.zsrvd_genclas.ZtgenclasL;
import customer.ai2code.model.execution.annotation.BotExecutor;
import customer.ai2code.model.execution.annotation.ExecuteMethod;
import customer.ai2code.model.execution.annotation.ExecuteParameter;
import customer.ai2code.service.ContextService;
import customer.ai2code.service.execution.BotExecution;
import customer.ai2code.service.impl.TaskBotCacheManager;

@BotExecutor(name = "Call Genclas Odata", description = "Implementation for Call SRVD_GENCLAS Odata", version = "1.0", enabled = true)
public class CreateGenclasOdataBotExecution implements BotExecution {

    private final ContextService contextService;
    private final ZsrvdGenclas zsrvdGenclas;
    private final ObjectMapper objectMapper;
    private final TaskBotCacheManager taskBotCacheManager;

    public CreateGenclasOdataBotExecution(ContextService contextService,
                                          ZsrvdGenclas zsrvdGenclas,
                                          ObjectMapper objectMapper,
                                          TaskBotCacheManager taskBotCacheManager) {
        this.contextService = contextService;
        this.zsrvdGenclas = zsrvdGenclas;
        this.objectMapper = objectMapper;
        this.taskBotCacheManager = taskBotCacheManager;
    }

    @ExecuteMethod
    public String execute(
            @ExecuteParameter(name = "botInstanceId", description = "Bot Instance ID") String botInstanceId,
            @ExecuteParameter(name = "autoActiveCLASContext", description = "Auto Active CLAS Context") ZcGenclasTAutoActiveCLASContext autoActiveCLASContext) {

        // 1. 创建新的上下文并复制参数
        ZcGenclasTAutoActiveCLASContext newContext = ZcGenclasTAutoActiveCLASContext.create();
        CqnSelect select = Select.from(ZcGenclasT_.CDS_NAME);
        newContext.setCqn(select);

        // 复制基础参数
        newContext.setClasname(autoActiveCLASContext.getClasname());
        newContext.setClasdesc(autoActiveCLASContext.getClasdesc());
        newContext.setBdefname(autoActiveCLASContext.getBdefname());
        newContext.setManaged(autoActiveCLASContext.getManaged());
        newContext.setAdditionalsave(autoActiveCLASContext.getAdditionalsave());
        newContext.setDevclass(autoActiveCLASContext.getDevclass());
        newContext.setTrkorr(autoActiveCLASContext.getTrkorr());
        newContext.setReference(autoActiveCLASContext.getReference());

        // 复制集合参数（Methods/Types/Attr）
        Collection<ZsgenClasMethodsList> methods = new ArrayList<>();
        autoActiveCLASContext.getMethods().forEach(method -> {
            ZsgenClasMethodsList newMethod = ZsgenClasMethodsList.create();
            newMethod.setMethodsname(method.getMethodsname());
            newMethod.setEntityname(method.getEntityname());
            newMethod.setMethodstype(method.getMethodstype());
            newMethod.setActionname(method.getActionname());
            newMethod.setSource(method.getSource());
            methods.add(newMethod);
        });
        newContext.setMethods(methods);

        Collection<ZsgenClasTypesList> types = new ArrayList<>();
        autoActiveCLASContext.getTypes().forEach(type -> {
            ZsgenClasTypesList newType = ZsgenClasTypesList.create();
            newType.setClasname(type.getClasname());
            newType.setTypename(type.getTypename());
            newType.setDatatype(type.getDatatype());
            types.add(newType);
        });
        newContext.setTypes(types);

        Collection<ZsgenClasAttrList> attrs = new ArrayList<>();
        autoActiveCLASContext.getAttr().forEach(attr -> {
            ZsgenClasAttrList newAttr = ZsgenClasAttrList.create();
            newAttr.setAttname(attr.getAttname());
            newAttr.setDatatype(attr.getDatatype());
            attrs.add(newAttr);
        });
        newContext.setAttr(attrs);

        // 2. 调用OData服务
        String result = "";
        try {
            zsrvdGenclas.emit(newContext);
        } catch (ServiceException e) {
            // 处理OData异常
            ODataResponseException odataEx = (ODataResponseException) e.getCause().getCause();
            result = String.format("Error (HTTP %d): %s", odataEx.getHttpCode(), odataEx.getHttpBody());
            return result;
        }

        // 3. 处理返回结果
        Collection<ZtgenclasL> results = newContext.getResult();
        if (results == null) {
            return "No results returned from OData service";
        }

        try {
            result = objectMapper.writeValueAsString(results);
            // 更新上下文信息
            for (ZtgenclasL item : results) {
                contextService.updateAdditionInfo(
                        taskBotCacheManager.getCachedBot(botInstanceId),
                        item.getReference(),
                        objectMapper.writeValueAsString(item)
                );
            }
        } catch (JsonProcessingException e) {
            result = "Failed to process results: " + e.getMessage();
        }

        return result;
    }
}