package customer.ai2code.service.impl.rag;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import customer.ai2code.service.impl.GenericCqnService;

/**
 * RAGCDSViewExtractorImpl 集成测试
 * 使用真实的数据库和服务进行测试，不使用mock
 */
@SpringBootTest
@ActiveProfiles("test")
class RAGCDSViewExtractorImplTest {

    @Autowired
    private GenericCqnService genericCqnService;

    private RAGCDSViewExtractorImpl ragExtractor;

    @BeforeEach
    void setUp() {
        // 使用真实的服务创建测试对象
        ragExtractor = new RAGCDSViewExtractorImpl(genericCqnService);
    }

    @Test
    void testExtract_WithValidQuery() {
        // 测试使用有效查询"采购订单"
        String ragSource = ""; // 空字符串，使用默认值
        int ragTopK = 10;
        String query = "采购订单";
        Locale language = Locale.CHINESE;
        double threshold = 0.75;

        String result = ragExtractor.extract(ragSource, ragTopK, query, language, threshold);

        // 验证结果
        assertNotNull(result, "提取结果不应该为空");
        assertFalse(result.trim().isEmpty(), "提取结果不应该为空字符串");
        
        // 验证返回的是有效的JSON格式
        assertTrue(result.startsWith("[") && result.endsWith("]") || 
                  result.startsWith("{") && result.endsWith("}"),
                  "结果应该是有效的JSON格式");
        
        // 如果有匹配结果，验证不是错误信息
        if (!result.equals("[]")) {
            assertFalse(result.contains("\"error\""), "结果不应该包含错误信息");
        }
        
        System.out.println("查询\"采购订单\"的结果: " + result);
    }

    @Test
    void testExtract_WithDifferentTopK() {
        // 测试不同的topK值
        String ragSource = "";
        int ragTopK = 5; // 使用较小的topK值
        String query = "采购订单";
        Locale language = Locale.CHINESE;
        double threshold = 0.75;

        String result = ragExtractor.extract(ragSource, ragTopK, query, language, threshold);

        assertNotNull(result, "提取结果不应该为空");
        System.out.println("TopK=5时查询\"采购订单\"的结果: " + result);
    }

    @Test
    void testExtract_WithDifferentThreshold() {
        // 测试不同的阈值
        String ragSource = "";
        int ragTopK = 10;
        String query = "采购订单";
        Locale language = Locale.CHINESE;
        double threshold = 0.5; // 使用较低的阈值

        String result = ragExtractor.extract(ragSource, ragTopK, query, language, threshold);

        assertNotNull(result, "提取结果不应该为空");
        System.out.println("阈值=0.5时查询\"采购订单\"的结果: " + result);
    }

    @Test
    void testExtract_WithEnglishLocale() {
        // 测试英语环境
        String ragSource = "";
        int ragTopK = 10;
        String query = "采购订单";
        Locale language = Locale.ENGLISH;
        double threshold = 0.75;

        String result = ragExtractor.extract(ragSource, ragTopK, query, language, threshold);

        assertNotNull(result, "提取结果不应该为空");
        System.out.println("英语环境下查询\"采购订单\"的结果: " + result);
    }

    @Test
    void testExtract_WithEmptyQuery() {
        // 测试空查询
        String ragSource = "";
        int ragTopK = 10;
        String query = "";
        Locale language = Locale.CHINESE;
        double threshold = 0.75;

        String result = ragExtractor.extract(ragSource, ragTopK, query, language, threshold);

        assertNotNull(result, "即使查询为空，结果也不应该为null");
        // 空查询可能返回空数组或所有结果，取决于实现
        System.out.println("空查询的结果: " + result);
    }

    @Test
    void testExtract_WithVeryHighThreshold() {
        // 测试很高的阈值，可能没有匹配结果
        String ragSource = "";
        int ragTopK = 10;
        String query = "采购订单";
        Locale language = Locale.CHINESE;
        double threshold = 0.99; // 很高的阈值

        String result = ragExtractor.extract(ragSource, ragTopK, query, language, threshold);

        assertNotNull(result, "提取结果不应该为空");
        // 很高的阈值可能返回空数组
        System.out.println("高阈值(0.99)时查询\"采购订单\"的结果: " + result);
    }

    @Test
    void testExtract_WithDifferentBusinessQueries() {
        // 测试不同的业务查询
        String[] queries = {"采购订单", "销售订单", "库存管理", "财务报表"};
        String ragSource = "";
        int ragTopK = 10;
        Locale language = Locale.CHINESE;
        double threshold = 0.75;

        for (String query : queries) {
            String result = ragExtractor.extract(ragSource, ragTopK, query, language, threshold);
            
            assertNotNull(result, "查询\"" + query + "\"的结果不应该为空");
            System.out.println("查询\"" + query + "\"的结果: " + result);
        }
    }

    @Test
    void testExtract_WithSpecificRagSource() {
        // 测试指定ragSource的情况
        String ragSource = "PURCHASING"; // 假设有特定的ragSource
        int ragTopK = 10;
        String query = "采购订单";
        Locale language = Locale.CHINESE;
        double threshold = 0.75;

        String result = ragExtractor.extract(ragSource, ragTopK, query, language, threshold);

        assertNotNull(result, "指定ragSource时的结果不应该为空");
        System.out.println("指定ragSource=\"" + ragSource + "\"时查询\"采购订单\"的结果: " + result);
    }

    @Test
    void testExtract_PerformanceTest() {
        // 性能测试
        String ragSource = "";
        int ragTopK = 10;
        String query = "采购订单";
        Locale language = Locale.CHINESE;
        double threshold = 0.75;

        long startTime = System.currentTimeMillis();
        String result = ragExtractor.extract(ragSource, ragTopK, query, language, threshold);
        long endTime = System.currentTimeMillis();

        assertNotNull(result, "性能测试的结果不应该为空");
        
        long executionTime = endTime - startTime;
        System.out.println("查询执行时间: " + executionTime + "ms");
        System.out.println("性能测试结果: " + result);
        
        // 验证执行时间在合理范围内（例如小于10秒）
        assertTrue(executionTime < 10000, "查询执行时间应该在合理范围内");
    }

    @Test
    void testExtract_ExceptionHandling() {
        // 测试极端参数值的异常处理
        String ragSource = "";
        int ragTopK = -1; // 负数topK
        String query = "采购订单";
        Locale language = Locale.CHINESE;
        double threshold = 0.75;

        // 即使参数不正常，方法也应该返回有效结果而不是抛异常
        assertDoesNotThrow(() -> {
            String result = ragExtractor.extract(ragSource, ragTopK, query, language, threshold);
            assertNotNull(result, "即使参数异常，结果也不应该为null");
            System.out.println("异常参数测试结果: " + result);
        });
    }
}
