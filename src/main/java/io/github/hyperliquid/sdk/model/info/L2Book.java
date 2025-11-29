package io.github.hyperliquid.sdk.model.info;

import java.util.List;

/** L2 订单簿快照（前 10 档买卖盘） */
public class L2Book {

    /** 币种名称（如 "BTC"） */
    private String coin;
    /** 快照时间戳（毫秒） */
    private Long time;
    /** 买卖盘列表：索引 0 为买盘，索引 1 为卖盘 */
    private List<List<Levels>> levels;

    // Getter and Setter methods
    public String getCoin() {
        return coin;
    }

    public void setCoin(String coin) {
        this.coin = coin;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public List<List<Levels>> getLevels() {
        return levels;
    }

    public void setLevels(List<List<Levels>> levels) {
        this.levels = levels;
    }

    public static class Levels {
        /** 该档位价格（字符串） */
        private String px;
        /** 该档位挂单总数量（字符串） */
        private String sz;
        /** 该价位的挂单笔数/档位计数 */
        private Integer n;

        // Getter and Setter methods
        public String getPx() {
            return px;
        }

        public void setPx(String px) {
            this.px = px;
        }

        public String getSz() {
            return sz;
        }

        public void setSz(String sz) {
            this.sz = sz;
        }

        public Integer getN() {
            return n;
        }

        public void setN(Integer n) {
            this.n = n;
        }
    }
}