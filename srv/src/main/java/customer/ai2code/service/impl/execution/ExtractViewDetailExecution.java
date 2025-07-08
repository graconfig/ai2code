package customer.ai2code.service.impl.execution;

import customer.ai2code.service.execution.BotExecution;
import customer.ai2code.service.impl.GenericCqnService;

import java.util.ArrayList;
import java.util.List;

import customer.ai2code.exception.BusinessException;
import customer.ai2code.model.execution.annotation.BotExecutor;
import customer.ai2code.model.execution.annotation.ExecuteMethod;
import customer.ai2code.model.execution.annotation.ExecuteParameter;
import com.fasterxml.jackson.databind.ObjectMapper;

import cds.gen.mainservice.CDSViews;

import java.util.*;
import java.util.stream.Collectors;

@BotExecutor(name = "Extract View Details", description = "Extract view fields and join conditions from CDSViews", version = "1.0", enabled = true)
public class ExtractViewDetailExecution implements BotExecution {

    private final GenericCqnService genericCqnService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExtractViewDetailExecution(GenericCqnService genericCqnService) {
        this.genericCqnService = genericCqnService;
    }

    @ExecuteMethod
    public String execute(
            @ExecuteParameter(name = "views", description = "List of CDS Views") List<CDSViews> views
    ) {
        try {
            if (views == null || views.isEmpty()) {
                throw new BusinessException("Views list must not be empty.");
            }

            // 提取 viewName
            List<String> viewNames = views.stream()
                    .map(CDSViews::getViewName)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (viewNames.isEmpty()) {
                throw new BusinessException("No valid viewName found in CDSViews.");
            }

            // 查询 ViewFields 和 JoinConditions
            String viewFieldsJson = genericCqnService.findViewFieldsByViewNames(viewNames, 100, Locale.ENGLISH);
            String joinConditionsJson = genericCqnService.findJoinConditionsByViewNames(viewNames);
            String viewsJson = objectMapper.writeValueAsString(viewNames);
            String result = viewsJson + viewFieldsJson + joinConditionsJson;

            return objectMapper.writeValueAsString(result);

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Failed to extract view details.\"}";
        }
    }
}