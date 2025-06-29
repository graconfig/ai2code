package customer.ai2code.service.impl.rag;

import customer.ai2code.service.execution.RAGExtractor;
import customer.ai2code.service.impl.GenericCqnService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
/**
 * RAGJoinConditionExtractorImpl 实现了 RAGExtractor 接口，用于从视图中提取连接条件信息。
 * 它使用 GenericCqnService 来查询连接条件，并将结果转换为 JSON 格式。
 */
@Service
public class RAGCDSViewExtractorImpl implements RAGExtractor {

    @Autowired
    private GenericCqnService genericCqnService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String extract(String ragSource, int ragTopK, String query, Locale language, double threshold) {
        if (!"cdsViews".equalsIgnoreCase(ragSource)) {
            return "{\"error\":\"Unsupported ragSource: " + ragSource + "\"}";
        }

        try {
            List<Map<String, Object>> views = genericCqnService.findMatchingViewsByScenario(query, threshold , ragTopK);
            // List<Map<String, Object>> views = genericCqnService.findMatchingViewsByScenario(query, threshold / 100.0, ragTopK);
            return objectMapper.writeValueAsString(views);
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Failed to extract CDS Views.\"}";
        }
    }
}
