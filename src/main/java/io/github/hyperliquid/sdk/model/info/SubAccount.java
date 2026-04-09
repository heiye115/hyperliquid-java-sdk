package io.github.hyperliquid.sdk.model.info;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
public class SubAccount {
    private String name;
    private String subAccountUser;
    private ClearinghouseState clearinghouseState;
    private SpotClearinghouseState spotState;
}
