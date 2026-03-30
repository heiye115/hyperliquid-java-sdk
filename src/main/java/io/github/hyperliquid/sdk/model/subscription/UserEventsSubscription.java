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

    @JsonProperty("user")
    private String user;


    /**
     * Default constructor.
     */
    public UserEventsSubscription() {
    }

    /**
     * Construct user events subscription.
     */
    public UserEventsSubscription(String user) {
        this.user = user;
    }

    /**
     * Static factory method: create user events subscription.
     *
     * @return UserEventsSubscription instance
     */
    public static UserEventsSubscription create(String user) {
        return new UserEventsSubscription(user);
    }

    @Override
    public String getType() {
        return type;
    }

    public String getUser() {
        return user;
    }

    @Override
    public String toIdentifier() {
        return type + ":" + user;
    }
}
