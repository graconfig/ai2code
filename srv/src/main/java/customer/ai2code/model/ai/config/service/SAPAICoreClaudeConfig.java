package customer.ai2code.model.ai.config.service;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;


@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class SAPAICoreClaudeConfig extends SAPAICoreConfig {

    @JsonProperty("maxTokens")
    @Nonnull
    private Integer maxTokens;

    @JsonProperty("temperature")
    @Nonnull
    private BigDecimal temperature;
}
