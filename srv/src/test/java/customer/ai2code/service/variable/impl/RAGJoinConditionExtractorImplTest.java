package customer.ai2code.service.variable.impl;

import customer.ai2code.service.impl.GenericCqnService;
import customer.ai2code.service.impl.rag.RAGJoinConditionExtractorImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class RAGJoinConditionExtractorImplTest {

    private GenericCqnService genericCqnService;
    private RAGJoinConditionExtractorImpl extractor;

    @BeforeEach
    void setUp() {
        genericCqnService = mock(GenericCqnService.class);
        extractor = new RAGJoinConditionExtractorImpl(genericCqnService);
    }

    @Test
    void testExtract_Success() {
        String query = "I_PURCHASEORDERITEMAPI01,I_CHANGEDOCUMENTITEM";
        String expectedJson = "[{\"view\":\"ZCDS_VIEW_1\"},{\"view\":\"ZCDS_VIEW_2\"}]";

        try {
            when(genericCqnService.findJoinConditionsByViewNames(anyList()))
                    .thenReturn(expectedJson);
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String result = extractor.extract("test", 5, query, Locale.ENGLISH, 0.75);

        System.out.println("JoinCondition result: " + result);

        System.out.println("Input viewNames: " + expectedJson);
        System.out.println("Returned JSON: " + result);

        assertEquals(expectedJson, result);
        try {
            verify(genericCqnService, times(1))
                    .findJoinConditionsByViewNames(List.of("I_PURCHASEORDERITEMAPI01", "I_CHANGEDOCUMENTITEM"));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
