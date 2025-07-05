package customer.ai2code.service.impl.rag;

import customer.ai2code.model.execution.annotation.ExecuteMethod;
import customer.ai2code.model.rag.annotation.RAGExtractor;
import customer.ai2code.service.impl.GenericCqnService;
import customer.ai2code.service.rag.RAGExtraction;

import java.util.*;
/**
 * RAGJoinConditionExtractorImpl 实现了 RAGExtractor 接口，用于从视图中提取连接条件信息。
 * 它使用 GenericCqnService 来查询连接条件，并将结果转换为 JSON 格式。
 */
@RAGExtractor(ragSource = "", ragTopK = 10, query = "", language = "ZH", threshold = 0.75)
public class RAGJoinConditionExtractorImpl implements RAGExtraction {

    private final GenericCqnService genericCqnService;

    // 构造函数注入依赖（替代 Spring @Autowired）
    public RAGJoinConditionExtractorImpl(GenericCqnService genericCqnService) {
        this.genericCqnService = genericCqnService;
    }
    
    @ExecuteMethod
    public String extract(String ragSource, int ragTopK, String query, Locale language, double threshold) {
        try {
            // 解析 query 中的 JSON 数组
            List<Map<String, Object>> jsonList = new ArrayList<>();
            jsonList = new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    query,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {
                    });

            // 提取 viewName 列表
            List<String> viewNames = jsonList.stream()
                    .map(entry -> (String) entry.get("viewName"))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            if (viewNames.isEmpty()) {
                return "[]";
            }
            return genericCqnService.findJoinConditionsByViewNames(viewNames);
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Failed to extract CDS Views.\"}";
        }
    }
}
