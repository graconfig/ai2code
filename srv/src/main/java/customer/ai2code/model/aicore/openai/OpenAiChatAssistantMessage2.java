package customer.ai2code.model.aicore.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import com.sap.ai.sdk.foundationmodels.openai.model.OpenAiChatMessage;

@Accessors(chain = true)
@EqualsAndHashCode
@ToString
public class OpenAiChatAssistantMessage2 implements OpenAiChatMessage{
    @JsonProperty("role")
    @Getter(onMethod_ = @Nonnull)
    private final String role = "assistant";
    /** The contents of the system message. */
    @JsonProperty("content")
    @Getter(onMethod_ = @Nullable)
    @Setter(onParam_ = @Nonnull)
    private String content;
}
