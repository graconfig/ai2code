    package customer.ai2code.service.impl.rag;

    import customer.ai2code.model.execution.annotation.ExecuteMethod;
    import customer.ai2code.service.impl.GenericCqnService;
    import customer.ai2code.service.rag.RAGExtraction;

    import org.springframework.stereotype.Service;

    import java.util.*;

    /**
     * RAGViewFieldExtractorImpl 实现了 RAGExtractor 接口，用于从视图中提取字段信息。
     * 它使用 GenericCqnService 来查询视图字段，并将结果转换为 JSON 格式。
     */
    @Service
    public class RAGViewFieldExtractorImpl implements RAGExtraction {

        private final GenericCqnService genericCqnService;

        // 构造函数注入依赖（替代 Spring @Autowired）
        public RAGViewFieldExtractorImpl(GenericCqnService genericCqnService) {
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
                return genericCqnService.findViewFieldsByViewNames(viewNames, ragTopK, language);
            } catch (Exception e) {
                e.printStackTrace();
                return "{\"error\": \"Failed to extract CDS Views.\"}";
            }
        }
    }
