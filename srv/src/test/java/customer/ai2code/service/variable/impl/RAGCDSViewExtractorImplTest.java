package customer.ai2code.service.variable.impl;

import customer.ai2code.service.impl.GenericCqnService;
import customer.ai2code.service.impl.rag.RAGCDSViewExtractorImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.sap.cds.CdsVector;
import com.sap.cds.Row;
import com.sap.cds.ql.CQL;
import com.sap.cds.ql.Select;
import com.sap.cds.ql.cqn.CqnSelect;
import com.sap.cds.ql.cqn.CqnVector;

import cds.gen.mainservice.BusinessScenarios_;
import cds.gen.mainservice.MainService;

@SpringBootTest
public class RAGCDSViewExtractorImplTest {

    private static final Logger logger = LoggerFactory.getLogger(RAGCDSViewExtractorImplTest.class);

    private RAGCDSViewExtractorImpl extractor;
    @Autowired
    private GenericCqnService genericCqnService;

    @Test
    public void testExtract_Success() {
        extractor = new RAGCDSViewExtractorImpl(genericCqnService);
        // Act
        String result = extractor.extract("cdsViews", 5, "采购订单", Locale.ENGLISH, 0.5);

        // Log instead of print
        logger.info("Extract result: {}", result);

        // 设置断点在这里，看 select、vector、similarity 的值
        // Assert
        assertNotNull(result);
    }
}
