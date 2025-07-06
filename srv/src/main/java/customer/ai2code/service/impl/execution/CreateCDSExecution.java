package customer.ai2code.service.impl.execution;

import org.springframework.beans.factory.support.AutowireCandidateQualifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cloud.sdk.datamodel.odatav4.core.ActionResponseCollection;

import customer.ai2code.model.execution.annotation.BotExecutor;
import customer.ai2code.model.execution.annotation.ExecuteMethod;
import customer.ai2code.model.s4.AutoActiveCDSRequest;
import customer.ai2code.model.s4.namespaces.s4createcdsmetadata.ConsumptionViewForGenerateDdlsp;
import customer.ai2code.model.s4.namespaces.s4createcdsmetadata.ZSGEN_DDLS_RESPONSE;
import customer.ai2code.model.s4.services.DefaultS4CreateCdsMetadataService;
import customer.ai2code.model.s4.services.S4CreateCdsMetadataService;
import customer.ai2code.service.ContextService;
import customer.ai2code.service.execution.BotExecution;
import customer.ai2code.service.impl.GenericCqnService;
import io.vavr.control.Try;

@BotExecutor(name = "Create CDS in S4 HANA", description = "Implementation for creating CDS in S4 HANA", version = "1.0", enabled = true)
public class CreateCDSExecution implements BotExecution {

    private final ContextService contextService;
    private final GenericCqnService genericCqnService;
    private final ObjectMapper objectMapper;

    public CreateCDSExecution(ContextService contextService, GenericCqnService genericCqnService) {
        this.contextService = contextService;
        this.genericCqnService = genericCqnService;
        this.objectMapper = new ObjectMapper();
    }

    @ExecuteMethod
    public String execute(String botInstanceId, AutoActiveCDSRequest autoActiveCDSRequest) {
        // 这里可以实现创建CDS的逻辑
        // 例如调用S4 HANA的API来创建CDS
        // 返回创建结果或状态信息
        StringBuilder builder = new StringBuilder();

        DefaultS4CreateCdsMetadataService cdsService = new DefaultS4CreateCdsMetadataService();

        Try<ActionResponseCollection<ZSGEN_DDLS_RESPONSE>> response = cdsService
                .applyAction(ConsumptionViewForGenerateDdlsp.autoActiveCDS(autoActiveCDSRequest.getProjectName(),
                        autoActiveCDSRequest.getProjectDescription(),
                        autoActiveCDSRequest.getWithDraft(),
                        autoActiveCDSRequest.getWithAdditionalSave(),
                        autoActiveCDSRequest.getDevelopmentClass(),
                        autoActiveCDSRequest.getTransportRequest(), autoActiveCDSRequest.getSourceList()))
                .tryExecute(null);

        response.onSuccess(result -> {
            result.getResponseResult().get().forEach(res -> {
                // 循环res 调用contextService.UpsertContext
                try {
                    // 将结果转换为JSON字符串
                    String resultJson = objectMapper.writeValueAsString(res);
                    builder.append(resultJson).append("\n");
                    // res.gete
                    // 调用contextService.UpsertContext方法
                    contextService.upsertContext(botInstanceId, res.getREFERENCE(), resultJson, "TEXT");

                } catch (Exception e) {
                    System.err.println("Error processing result: " + e.getMessage());
                }
                // return result
            });
            // return
            // result.getResponseResult().forEach(res -> {
            // // 循环res 调用contextService.UpsertContext
            // // try {
            // // // 将结果转换为JSON字符串
            // // String resultJson = objectMapper.writeValueAsString(res);

            // // // 调用contextService.UpsertContext方法
            // // // contextService.upsertContext(botInstanceId, res.getKey(), resultJson);
            // // } catch (Exception e) {
            // // System.err.println("Error processing result: " + e.getMessage());
            // // }
            // });
        }).onFailure(failure -> {
            // failure.
        });
        // response.forEach( res -> {
        // 循环res 调用contextService.UpsertContext
        // res.getResponseResult().forEach(result -> {
        // // try {
        // // // 将结果转换为JSON字符串
        // // String resultJson = objectMapper.writeValueAsString(result);
        // // // 调用contextService.UpsertContext方法
        // // contextService.upsertContext(botInstanceId, result.getKey(), resultJson);
        // // } catch (Exception e) {
        // // System.err.println("Error processing result: " + e.getMessage());
        // // }
        // });
        // response.andThen(res -> {
        // // 循环res 调用contextService.UpsertContext
        // res.getResponseResult().forEach(result -> {
        // try {
        // // 将结果转换为JSON字符串
        // String resultJson = objectMapper.writeValueAsString(result);
        // // 调用contextService.UpsertContext方法
        // // contextService.upsertContext(botInstanceId, result.getKey(), resultJson);
        // // contextService.upsertContext(botInstanceId, result, resultJson,
        // botInstanceId)
        // } catch (Exception e) {
        // System.err.println("Error processing result: " + e.getMessage());
        // }
        // });
        // }).onFailure(throwable -> {
        // // 处理错误情况
        // System.err.println("Error creating CDS: " + throwable.getMessage());
        // });

        // 目前只是返回一个示例字符串
        // return "CDS created successfully in S4 HANA";
        return builder.toString();
    }

}
