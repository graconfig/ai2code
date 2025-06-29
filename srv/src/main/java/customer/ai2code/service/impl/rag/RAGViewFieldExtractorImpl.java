package customer.ai2code.service.impl.rag;

import customer.ai2code.service.execution.RAGExtractor;
import customer.ai2code.service.impl.GenericCqnService;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * RAGViewFieldExtractorImpl 实现了 RAGExtractor 接口，用于从视图中提取字段信息。
 * 它使用 GenericCqnService 来查询视图字段，并将结果转换为 JSON 格式。
 */
@Service
public class RAGViewFieldExtractorImpl implements RAGExtractor {

    @Autowired
    private GenericCqnService genericCqnService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String extract(String ragSource, int ragTopK, String query, Locale language, double threshold) {
        if (!"viewFields".equalsIgnoreCase(ragSource)) {
            return "{\"error\":\"Unsupported ragSource: " + ragSource + "\"}";
        }

        try {
            List<String> viewNames = Arrays.asList(query.split(",")); // query为CSV形式
            List<Map<String, Object>> fields = genericCqnService.findViewFieldsByViewNames(viewNames, language);
            return objectMapper.writeValueAsString(fields);
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"error\": \"Failed to extract View Fields.\"}";
        }
    }
}
