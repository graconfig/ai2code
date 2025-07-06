package customer.ai2code.service.impl.rag;

import customer.ai2code.model.execution.annotation.ExecuteMethod;
import customer.ai2code.model.rag.annotation.RAGExtractor;
import customer.ai2code.service.impl.GenericCqnService;
import customer.ai2code.service.rag.RAGExtraction;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
/**
 * RAGJoinConditionExtractorImpl 实现了 RAGExtraction 接口，用于从视图中提取连接条件信息。
 * 它使用 GenericCqnService 来查询连接条件，并将结果转换为 JSON 格式。
 * String ragSource, int ragTopK, String query, Locale language, double threshold
 */
@RAGExtractor(ragSource = "", ragTopK = 10, query = "", language = "ZH", threshold = 0.75)
public class RAGCDSViewExtractorImpl implements RAGExtraction {

    private final GenericCqnService genericCqnService;

    // 构造函数注入依赖（替代 Spring @Autowired）
    public RAGCDSViewExtractorImpl(GenericCqnService genericCqnService) {
        this.genericCqnService = genericCqnService;
    }

    @ExecuteMethod
    public String extract(String ragSource, int ragTopK, String query, Locale language, double threshold) {
        try {
            return genericCqnService.findMatchingViewsByScenario(ragSource, ragTopK , query , language, threshold);
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Failed to extract CDS Views.\"}";
        }
    }
}
