package customer.ai2code.model.aicore.deepseek;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import customer.ai2code.exception.BusinessException;

// 保留核心通信能力，支持同步和流式
public class DeepseekClient {
    private final String apiUrl;
    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();
    private final WebClient webClient;

    public DeepseekClient(String apiUrl, String apiKey) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl(apiUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    // 非流式调用
    public DeepseekChatResponse chatCompletion(DeepseekChatRequest request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<DeepseekChatRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<DeepseekChatResponse> response = restTemplate.exchange(
                "/chat/completions", // 简化URL，基于baseUrl
                HttpMethod.POST,
                entity,
                DeepseekChatResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                throw new BusinessException("API请求失败: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new BusinessException("客户端错误", e);
        }
    }

    // 流式调用（返回增量内容）
    public Flux<String> streamChatCompletion(DeepseekChatRequest request) {
        request.setStream(true);
        
        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .map(this::extractContent)
                .onErrorMap(e -> new BusinessException("流式请求失败", e));
    }

    // 提取流式响应内容（简化解析）
    private String extractContent(String chunk) {
        if (chunk.contains("\"content\":\"")) {
            return chunk.split("\"content\":\"")[1].split("\"")[0];
        }
        return "";
    }
}