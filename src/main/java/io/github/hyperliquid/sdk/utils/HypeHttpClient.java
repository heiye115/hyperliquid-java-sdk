package io.github.hyperliquid.sdk.utils;

import com.fasterxml.jackson.databind.JsonNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class HypeHttpClient {

    private static final Logger log = LoggerFactory.getLogger(HypeHttpClient.class);

    private final String baseUrl;

    private final OkHttpClient client;

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    public HypeHttpClient(String baseUrl, OkHttpClient client) {
        this.baseUrl = baseUrl;
        this.client = client;
    }

    /**
     * Get base URL.
     *
     * @return Base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Send POST request to specified path and return JSON result.
     *
     * @param path    Relative path, e.g., "/info", "/exchange"
     * @param payload JSON serializable object (will be serialized by ObjectMapper)
     * @return Returned JSON node
     * @throws HypeError.ClientHypeError When response is 4xx
     * @throws HypeError.ServerHypeError When response is 5xx
     */
    public JsonNode post(String path, Object payload) {
        String url = baseUrl + path;
        String requestJson = "";
        try {
            requestJson = JSONUtil.writeValueAsString(payload);
            log.debug("POST: {} ", url);
            log.debug("Request: {}", requestJson);
            RequestBody body = RequestBody.create(requestJson, JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                ResponseBody responseBodyObj = response.body();
                String responseBody = responseBodyObj.string();
                log.debug("Response: {}", responseBody);
                if (!response.isSuccessful()) {
                    int code = response.code();
                    String errorMsg = String.format("HTTP %d: %s", code, responseBody);

                    if (code >= 400 && code < 500) {
                        throw new HypeError.ClientHypeError(code, errorMsg);
                    } else {
                        throw new HypeError.ServerHypeError(code, errorMsg);
                    }
                }
                return JSONUtil.readTree(responseBody);
            }
        } catch (IOException e) {
            throw new HypeError("Network error for POST " + path + ": " + e.getMessage(), e);
        }
    }
}
