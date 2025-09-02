package customer.ai2code.model.rest;

import lombok.Data;

@Data
public class ChatInStreamingRequest {
    private String id;
    private String content;
}
