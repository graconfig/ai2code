package customer.ai2code.service.variable.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import customer.ai2code.service.impl.GenericCqnService;
import customer.ai2code.service.impl.rag.RAGViewFieldExtractorImpl;

@SpringBootTest
public class RAGViewFieldExtractorImplTest {
    private static final Logger logger = LoggerFactory.getLogger(RAGViewFieldExtractorImplTest.class);

    private RAGViewFieldExtractorImpl extractor;
    @Autowired
    private GenericCqnService genericCqnService;

    @Test
    public void testExtract_Success() {
        extractor = new RAGViewFieldExtractorImpl(genericCqnService);
        // Act

        String query = "[{\"viewName\":\"I_RFQLIFECYCLESTATUSTEXT\",\"viewDesc\":\"订单\"},{\"viewName\":\"I_CADUNNINGLEVELCATEGORY\",\"viewDesc\":\"收货\"}]/BotInstances/123/messages/请问哪些字段可查";
        String result = extractor.extract("cdsViews", 5, query, Locale.ENGLISH, 0.5);

        // Log instead of print
        logger.info("Extract result: {}", result);

        // 设置断点在这里，看 select、vector、similarity 的值
        // Assert
        assertNotNull(result);
    }
}
