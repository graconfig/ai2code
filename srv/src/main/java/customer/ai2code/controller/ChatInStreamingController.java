package customer.ai2code.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import customer.ai2code.model.rest.ChatInStreamingRequest;
import customer.ai2code.service.BotService;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/rest/v1/chat")
public class ChatInStreamingController {

    private BotService botService;

    public ChatInStreamingController(BotService botService) {
        this.botService = botService;
    }

    @PostMapping("/streaming")
    public SseEmitter chatInStreaming(@RequestBody ChatInStreamingRequest request, HttpServletResponse response) {
        // 实现流式聊天逻辑

        response.setHeader("Cache-Control", "no-transform");

        // return new SseEmitter(); // 返回SSE发射器实例
        return botService.chatInStreaming(request.getId(), request.getContent());
    }
}
