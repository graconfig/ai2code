package customer.ai2code.model.rag.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RAGExtractor {
    String ragSource();
    int ragTopK();
    String query();
    String language();
    double threshold();
}