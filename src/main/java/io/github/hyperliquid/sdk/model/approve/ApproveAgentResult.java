package io.github.hyperliquid.sdk.model.approve;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * ApproveAgentResult 封装 approveAgent 的返回值：
 * - response: 服务端 /exchange 的 JSON 响应；
 * - agentPrivateKey: 新生成的 Agent 私钥（0x 前缀十六进制字符串）；
 * - agentAddress: 新生成的 Agent 地址（0x 前缀十六进制字符串）。
 */
public class ApproveAgentResult {

    /** 服务端响应 JSON
     */
    private final JsonNode response;

    /** 新生成的 Agent 私钥（0x 前缀）
     */
    private final String agentPrivateKey;

    /** 新生成的 Agent 地址（0x 前缀）
     */
    private final String agentAddress;

    /**
     * 构造结果对象。
     *
     * @param response        服务端响应 JSON
     * @param agentPrivateKey 新生成的 Agent 私钥（0x 前缀）
     * @param agentAddress    新生成的 Agent 地址（0x 前缀）
     */
    public ApproveAgentResult(JsonNode response, String agentPrivateKey, String agentAddress) {
        this.response = response;
        this.agentPrivateKey = agentPrivateKey;
        this.agentAddress = agentAddress;
    }

    // Getter methods
    public JsonNode getResponse() {
        return response;
    }

    public String getAgentPrivateKey() {
        return agentPrivateKey;
    }

    public String getAgentAddress() {
        return agentAddress;
    }
}