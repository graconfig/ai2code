package customer.ai2code.service.variable.impl;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.sap.cds.CdsData;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.cqn.CqnInsert;

import cds.gen.mainservice.CDSViews;
import cds.gen.mainservice.CDSViews_;
import customer.ai2code.service.impl.GenericCqnService;
import customer.ai2code.service.impl.execution.ExtractViewDetailExecution;
import customer.ai2code.service.impl.rag.RAGViewFieldExtractorImpl;

@SpringBootTest
public class ExtractViewDetailExecutionTest {
    private static final Logger logger = LoggerFactory.getLogger(ExtractViewDetailExecutionTest.class);

    private ExtractViewDetailExecution extractor;
    @Autowired
    private GenericCqnService genericCqnService;

    @Test
    public void testExtract_Success() {
        extractor = new ExtractViewDetailExecution(genericCqnService);

        CDSViews view1 = new CDSViewsMock("I_PURCHASEORDERITEMAPI01", "订单");
        CDSViews view2 = new CDSViewsMock("I_CHANGEDOCUMENTITEM", "收货");

        List<CDSViews> views = Arrays.asList(view1, view2);

        String result = extractor.execute(views); // This is just to initialize the extractor, not used in this test

        // Log instead of print
        logger.info("Extract result: {}", result);

        // 设置断点在这里，看 select、vector、similarity 的值
        // Assert
        assertNotNull(result);
    }

}
