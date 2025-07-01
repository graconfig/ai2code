package customer.ai2code.service.execution;

import java.util.List;
import java.util.Locale;
import cds.gen.configservice.PromptTexts;

public interface RAGExtractor {
    public String extract(String ragSource, int ragTopK, String query, Locale language, int threshold);
}
