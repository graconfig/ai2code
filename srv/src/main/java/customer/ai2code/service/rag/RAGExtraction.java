package customer.ai2code.service.rag;

import java.util.List;
import java.util.Locale;
import cds.gen.configservice.PromptTexts;

public interface RAGExtraction {
    public String extract(String ragSource, int ragTopK, String query, Locale language, double threshold);
}
