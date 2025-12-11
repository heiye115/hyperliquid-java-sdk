package io.github.hyperliquid.sdk.model.subscription;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * WebSocket subscription base class.
 * <p>
 * All specific subscription types inherit from this class, providing type-safe subscription parameter encapsulation.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Subscription {
    
    /**
     * Get subscription type (implemented by subclasses).
     *
     * @return subscription type string
     */
    public abstract String getType();
    
    /**
     * Generate unique identifier (used for subscription deduplication and message routing).
     * <p>
     * Default implementation returns subscription type, subclasses can override this method as needed.
     * </p>
     *
     * @return unique identifier string
     */
    public String toIdentifier() {
        return getType();
    }
}
