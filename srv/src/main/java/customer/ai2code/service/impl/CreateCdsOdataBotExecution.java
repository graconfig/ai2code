package customer.ai2code.service.impl;

import java.util.ArrayList;
import java.util.Collection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cds.gen.com.sap.gateway.srvd.zsrvd_genddls.v0001.V0001;
import cds.gen.com.sap.gateway.srvd.zsrvd_genddls.v0001.ZcGenddlsPAutoActiveCDSContext;
import cds.gen.com.sap.gateway.srvd.zsrvd_genddls.v0001.ZtgenddlsL;
import cds.gen.mainservice.ContextNodes;
import customer.ai2code.model.execution.annotation.BotExecutor;
import customer.ai2code.model.execution.annotation.ExecuteMethod;
import customer.ai2code.model.execution.annotation.ExecuteParameter;
import customer.ai2code.service.ContextService;
import customer.ai2code.service.execution.BotExecution;

@BotExecutor(name = "Call Remote Odata", description = "Implementation for Call S4/HANA OP Odata", version = "1.0", enabled = true)
public class CreateCdsOdataBotExecution implements BotExecution {
    // private final GenericCqnService genericCqnService;
    private final ContextService contextService;
    private final V0001 zsrvdGenddls;
    private final ObjectMapper objectMapper;

    // public CreateCdsOdataBotExecution(GenericCqnService genericCqnService,
    // ZsrvdGenddls zsrvdGenddls) {
    // public CreateCdsOdataBotExecution(V0001 zsrvdGenddls) {
    public CreateCdsOdataBotExecution(ContextService contextService,
            V0001 zsrvdGenddls, ObjectMapper objectMapper) {
        // 默认构造函数
        // this.genericCqnService = genericCqnService;
        this.contextService = contextService;
        this.zsrvdGenddls = zsrvdGenddls;
        this.objectMapper = objectMapper;
    }

    // String DestinationName,String Request,String Response,，List<ContextNodes>
    @ExecuteMethod
    public Collection<ZtgenddlsL> execute(
            @ExecuteParameter(name = "botInstanceId", description = "Bot Instance") String botInstanceId,
            @ExecuteParameter(description = "ZcGenddlsPAutoActiveCDSContext", name = "ZcGenddlsPAutoActiveCDSContext") ZcGenddlsPAutoActiveCDSContext AutoActiveCDSContext) {
        // zsrvdGenddls.run();

        Collection<ZtgenddlsL> Results = new ArrayList<>();// 返回结果
        // 打印CDS
        System.out.println("CdsSource=" + AutoActiveCDSContext.getSource().toString());
        // 调用action
        zsrvdGenddls.emit(AutoActiveCDSContext);
        Results = AutoActiveCDSContext.getResult();
        // 打印返回消息
        for (ZtgenddlsL item : Results) {
            String type = item.getType();
            String message = item.getMessage();
            String REFERENCE = item.getReference();
            System.out.println("Result=" + item.toString());

            System.out.println("type=" + type);
            System.out.println("message=" + message);
            System.out.println("REFERENCE=" + REFERENCE);

            // contextService.upsertContext(botInstanceId, REFERENCE, , type)
            try {
                contextService.updateAdditionInfo(botInstanceId, REFERENCE, objectMapper.writeValueAsString(item));
            } catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
                contextService.updateAdditionInfo(botInstanceId, REFERENCE, e.getMessage());
            }

        }

        // return 
        // // 更新context
        // String mainTaskId = genericCqnService.getMainTaskId(botInstanceId);

        // for (ZtgenddlsL item : Results) {
        // String type = item.getType();
        // String message = item.getMessage();
        // String REFERENCE = item.getReference();
        // ContextNodes contextnodes =
        // genericCqnService.getContextNodeByTaskAndPath(mainTaskId, REFERENCE);
        // // ContextNodes existingNode, String label, String type, String contextValue)
        // {
        // // genericCqnService.updateContextNode(contextnodes,label,type,contextValue);

        // }
        return Results;

    }

}
