package io.github.hyperliquid.sdk.model.approve;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * ApproveAgentResult wraps the return value of approveAgent:
 * - response: server /exchange JSON response;
 * - agentPrivateKey: newly generated Agent private key (0x prefix hexadecimal string);
 * - agentAddress: newly generated Agent address (0x prefix hexadecimal string).
 */
public class ApproveAgentResult {

    /** Server response JSON
     */
    private final JsonNode response;

    /** Newly generated Agent private key (0x prefix)
     */
    private final String agentPrivateKey;

    /** Newly generated Agent address (0x prefix)
     */
    private final String agentAddress;

    /**
     * Construct result object.
     *
     * @param response        server response JSON
     * @param agentPrivateKey newly generated Agent private key (0x prefix)
     * @param agentAddress    newly generated Agent address (0x prefix)
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