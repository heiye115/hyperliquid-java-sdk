package io.github.hyperliquid.sdk.model.info;

import lombok.Data;

/***
 * 资金费率历史记录。
 **/
@Data
public class FundingHistory {

    /**
     * 币种名称
     **/
    private String coin;

    /***
     * 资金费率
     **/
    private String fundingRate;

    /***
     * 溢价率
     **/
    private String premium;

    /***
     * 时间戳（毫秒）
     **/
    private Long time;
}
