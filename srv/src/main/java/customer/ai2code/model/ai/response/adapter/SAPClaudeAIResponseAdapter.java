package customer.ai2code.model.ai.response.adapter;

import customer.ai2code.model.ai.response.AIResponse;
import customer.ai2code.model.aicore.claude.ContentBlock;
import customer.ai2code.model.aicore.claude.ConverseRequestAssistantMessage;
import customer.ai2code.model.aicore.claude.ConverseResponse;
import customer.ai2code.model.aicore.claude.InvokeResponse;
import customer.ai2code.model.aicore.claude.TextResponseContent;
import customer.ai2code.model.aicore.claude.ToolResponseContent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SAPClaudeAIResponseAdapter implements AIResponse {
    private final String content;

    private ObjectMapper objectMapper;
    // private final String stopReason;

    public SAPClaudeAIResponseAdapter(InvokeResponse response) {
        this.content = extractContentFromInvokeResponse(response);
        // this.stopReason = response.getStopReason().getValue();
    }

    public SAPClaudeAIResponseAdapter(ConverseResponse response, ObjectMapper objectMapper)
            throws JsonProcessingException {
        this.objectMapper = objectMapper;
        this.content = extractContentFromConverseResponse(response);
        // this.stopReason = response.getStopReason().getValue();
    }

    private String extractContentFromInvokeResponse(InvokeResponse response) {
        if (response == null || response.getContent() == null || response.getContent().isEmpty()) {
            return "";
        }

        // Handle tool use case
        if (response.getStopReason() == InvokeResponse.StopReasonEnum.TOOL_USE) {
            StringBuilder toolContent = new StringBuilder();
            for (var content : response.getContent()) {
                if (content instanceof ToolResponseContent) {
                    var toolResponse = (ToolResponseContent) content;
                    toolContent.append(toolResponse.getInput());
                }
            }
            return toolContent.toString().trim();
        }

        // Handle normal chat completion case
        if (response.getStopReason() == InvokeResponse.StopReasonEnum.END_TURN ||
                response.getStopReason() == InvokeResponse.StopReasonEnum.STOP_SEQUENCE) {
            var content = response.getContent().get(0);
            if (content instanceof TextResponseContent) {
                var textContent = (TextResponseContent) content;
                return textContent.getText();
            }
        }

        // Default case - try to get text content if available
        for (var content : response.getContent()) {
            if (content instanceof TextResponseContent) {
                return ((TextResponseContent) content)
                        .getText();
            }
        }

        // If we can't properly handle the content, return empty string
        return "";
    }

    private String extractContentFromConverseResponse(ConverseResponse response) throws JsonProcessingException {
        if (response == null) {
            return "";
        }

        // Handle tool use case
        if (ConverseResponse.StopReasonEnum.TOOL_USE.equals(response.getStopReason())) {
            // StringBuilder toolContent = new StringBuilder();
            ConverseRequestAssistantMessage assistantMessage = (ConverseRequestAssistantMessage) response.getOutput()
                    .getMessage();
            ContentBlock toolUse = assistantMessage.getContent().stream()
                    .filter(content -> content.getToolUse() != null)
                    .findFirst().orElse(null);
            if (toolUse != null && toolUse.getToolUse().getInput() != null) {
                // toolContent.append(toolUse.getToolUse().getInput());
                String jsonString = this.objectMapper.writeValueAsString(toolUse.getToolUse().getInput());
                // Remove newline characters
                return jsonString.replace("\\n", "").replace("\n", "");
            }

            // for (var content : assistantMessage.getContent()) {
            // if (content.getToolUse() != null && content.getToolUse().getInput() != null)
            // {
            // toolContent.append(content.getToolUse().getInput());
            // }
            // }
            // return toolContent.toString().trim();
        }

        // Handle normal chat completion case
        if (ConverseResponse.StopReasonEnum.STOP_SEQUENCE.equals(response.getStopReason()) ||
                ConverseResponse.StopReasonEnum.END_TURN.equals(response.getStopReason()) ||
                ConverseResponse.StopReasonEnum.CONTENT_FILTERED.equals(response.getStopReason())) {
            StringBuilder toolContent = new StringBuilder();
            ConverseRequestAssistantMessage assistantMessage = (ConverseRequestAssistantMessage) response.getOutput()
                    .getMessage();
            for (var content : assistantMessage.getContent()) {
                if (content.getText() != null) {
                    toolContent.append(content.getText());
                }
            }
            return toolContent.toString().trim();
        }

        // Default case - try to get any content from output
        if (response.getOutput() != null) {
            return response.getOutput().toString();
        }

        return "";
    }

    @Override
    public String getContent() {
        return content;
    }

    // @Override
    // public String getStopReason() {
    // return stopReason;
    // }
}