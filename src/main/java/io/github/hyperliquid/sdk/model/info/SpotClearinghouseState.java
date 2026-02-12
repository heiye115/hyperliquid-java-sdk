package io.github.hyperliquid.sdk.model.info;

import lombok.*;

import java.util.List;

/** Spot clearinghouse state: user token balance list */

public class SpotClearinghouseState {
    /** Balance list */
    @Getter
    @Setter
    private List<Balance> balances;

    @Data
    @ToString
    @EqualsAndHashCode
    public static class Balance {
        /** Token name or index prefix form (e.g., "@107") */
        private String coin;
        /** Token integer ID */
        private Integer token;
        /** Frozen/occupied quantity (string) */
        private String hold;
        /** Total balance quantity (string) */
        private String total;
        /** Nominal USD value (string) */
        private String entryNtl;
    }
}