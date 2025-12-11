package io.github.hyperliquid.sdk.model.info;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PerpDexStatus typed model.
 *
 * <p>
 * Documentation example: {"totalNetDeposit": "4103492112.4478230476"}
 * This model preserves extension fields to be compatible with future return structures.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerpDexStatus {

    /**
     * Total net deposit/withdrawal (string)
     */
    private String totalNetDeposit;

    /**
     * Other unknown/extension fields
     */
    private Map<String, Object> extensions = new LinkedHashMap<>();

    // Getter and Setter methods
    public String getTotalNetDeposit() {
        return totalNetDeposit;
    }

    public void setTotalNetDeposit(String totalNetDeposit) {
        this.totalNetDeposit = totalNetDeposit;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public void setExtensions(Map<String, Object> extensions) {
        this.extensions = extensions;
    }

    /**
     * Get extension fields
     */
    @JsonAnyGetter
    public Map<String, Object> any() {
        return extensions;
    }

    /**
     * Set extension fields
     */
    @JsonAnySetter
    public void setExtensions(String key, Object value) {
        if (this.extensions == null) {
            this.extensions = new LinkedHashMap<>();
        }
        this.extensions.put(key, value);
    }
}