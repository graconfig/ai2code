package customer.ai2code.service.execution;

import java.util.Locale;

public interface RAGExtractor {
    public String extract(String ragSource, int ragTopK, String query, Locale language, int threshold);
}
