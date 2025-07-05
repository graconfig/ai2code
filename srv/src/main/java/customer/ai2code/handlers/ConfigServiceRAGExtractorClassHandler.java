package customer.ai2code.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

import com.sap.cds.Result;
import com.sap.cds.ResultBuilder;
import com.sap.cds.ql.cqn.CqnPredicate;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CqnService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import cds.gen.configservice.ConfigService_;
import cds.gen.configservice.RAGExtractorClass_;
import customer.ai2code.service.rag.RAGExtraction;
import customer.ai2code.utils.CheckDataVisitor;
import customer.ai2code.utils.ClassReflection;
import customer.ai2code.utils.UnmanagedReportUtils;

@Component
@ServiceName(ConfigService_.CDS_NAME)
public class ConfigServiceRAGExtractorClassHandler implements EventHandler {

    @On(event = CqnService.EVENT_READ, entity = RAGExtractorClass_.CDS_NAME)
    public void getAllImplementedByClass(CdsReadEventContext context) throws IOException {
        List<Map<String, Object>> implementedByClasses = new ArrayList<>();
        List<Map<String, ? extends Object>> implementedByClasseResult = new ArrayList<>();

        // Get CqnSelect
        CqnSelect select = context.getCqn();
        // ReflectionUtils.
        implementedByClasses = ClassReflection.getClassbyInterface(RAGExtraction.class);

        implementedByClasses.forEach((clazz) -> {
            CheckDataVisitor checkDataVisitor = new CheckDataVisitor(clazz);
            try {
                CqnPredicate cqnPredicate = select.where().get();
                cqnPredicate.accept(checkDataVisitor);
                if (checkDataVisitor.matches()) {
                    implementedByClasseResult.add(clazz);
                }
            } catch (Exception e) {
                // No where conditions
                implementedByClasseResult.add(clazz);
            }
        });

        // sort
        UnmanagedReportUtils.sort(select.orderBy(), implementedByClasseResult);

        long inlineCount = implementedByClasseResult.size();

        List<? extends Map<String, ?>> resultsPaging = UnmanagedReportUtils.getTopSkip(select.top(),
                select.skip(), implementedByClasseResult);
        // set result by ResultBuilder
        Result result = ResultBuilder.selectedRows(resultsPaging).inlineCount(inlineCount).result();
        context.setResult(result);

    }
}
