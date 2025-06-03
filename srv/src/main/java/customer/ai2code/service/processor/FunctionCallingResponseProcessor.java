package customer.ai2code.service.processor;

import com.fasterxml.jackson.databind.JsonNode;

@FunctionalInterface
public interface FunctionCallingResponseProcessor<T> {
    T process(JsonNode functionCallNode);
}
