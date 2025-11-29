package io.github.hyperliquid.sdk.model.info;

/** 更新杠杆操作返回 */
public class UpdateLeverage {
    /** 顶层状态（如 "ok"/"error"） */
    private String status;
    /** 响应体（类型等） */
    private Response response;

    // Getter and Setter methods
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public static class Response {
        /** 响应类型描述 */
        private String type;

        // Getter and Setter methods
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}