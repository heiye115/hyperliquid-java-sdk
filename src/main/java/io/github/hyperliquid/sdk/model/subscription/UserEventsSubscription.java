package io.github.hyperliquid.sdk.model.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * User events subscription.
 * <p>
 * Subscribe to all trading events of the current user, including order status changes, trade notifications, etc.
 * This subscription does not require specifying a user address, automatically uses the currently logged-in user.
 * </p>
 */
public class UserEventsSubscription extends Subscription {
    
    @JsonProperty("type")
    private final String type = "userEvents";
    
    /**
     * Construct user events subscription.
     */
    public UserEventsSubscription() {
    }
    
    /**
     * Static factory method: create user events subscription.
     *
     * @return UserEventsSubscription instance
     */
    public static UserEventsSubscription create() {
        return new UserEventsSubscription();
    }
    
    @Override
    public String getType() {
        return type;
    }
}
