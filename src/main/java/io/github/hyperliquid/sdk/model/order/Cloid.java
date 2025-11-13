package io.github.hyperliquid.sdk.model.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.hyperliquid.sdk.utils.HypeError;
import lombok.Getter;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * 客户端订单 ID（Cloid）Java 实现，对齐 Python 版本。
 *
 * <p>
 * - 字符串必须以 "0x" 开头且后续十六进制字符长度为 32（即 16 字节）；
 * - 非符合上述格式时抛出 TypeError：
 * - 前缀不为 0x -> "cloid is not a hex string"
 * - 长度不为 32 -> "cloid is not 16 bytes"
 * - from_int(cloid: int) 会格式化为 0x 前缀、宽度 34（含 0x），不足位左侧补零；
 * - from_str(cloid: str) 直接封装；
 * - to_raw() 返回原始字符串。
 */
@Getter
public class Cloid {

    /**
     * 原始 Cloid 字符串（0x + 32 hex chars）
     * -- GETTER --
     * 兼容旧接口：获取原始字符串。
     */
    private final String raw;

    /**
     * 构造函数：校验并保存原始 Cloid 字符串。
     *
     * @param raw 原始字符串（必须以 0x 开头且长度为 34，包括 0x + 32 位十六进制字符）
     * @throws HypeError 当不以 0x 开头或长度不为 16 字节（32 hex）时抛出
     */
    @JsonCreator
    public Cloid(String raw) {
        if (raw == null) {
            throw new HypeError("cloid is not a hex string");
        }
        this.raw = raw;
        validate();
    }

    /**
     * 内部校验逻辑
     */
    private void validate() {
        if (!raw.startsWith("0x")) {
            throw new HypeError("cloid is not a hex string");
        }
        String hex = raw.substring(2);
        if (hex.length() != 32) {
            throw new HypeError("cloid is not 16 bytes");
        }
    }

    /**
     * 工厂方法：从整型构造 Cloid（对齐 Python from_int）。
     *
     * @param cloid 整型值（Java int）
     * @return Cloid 实例
     * @throws HypeError 当格式化结果长度不为 16 字节时抛出（例如数值超出 32 hex 长度）
     */
    public static Cloid fromInt(Integer cloid) {
        String hex = Integer.toHexString(cloid);
        String padded = leftPad(hex);
        return new Cloid("0x" + padded);
    }

    /**
     * 工厂方法：从长整型构造 Cloid，便于覆盖更大范围（可选扩展）。
     *
     * @param cloid 长整型值
     * @return Cloid 实例
     */
    public static Cloid fromLong(Long cloid) {
        String hex = Long.toHexString(cloid);
        String padded = leftPad(hex);
        return new Cloid("0x" + padded);
    }

    /**
     * 工厂方法：从 BigInteger 构造 Cloid（覆盖超出 64 位的场景）。
     *
     * @param cloid BigInteger 值
     * @return Cloid 实例
     */
    public static Cloid fromBigInt(BigInteger cloid) {
        if (cloid == null) {
            throw new HypeError("cloid is not a hex string");
        }
        String hex = cloid.toString(16);
        String padded = leftPad(hex);
        return new Cloid("0x" + padded);
    }

    /**
     * 工厂方法：从字符串构造 Cloid（对齐 Python from_str）。
     *
     * @param cloid 字符串（必须满足 0x + 32 hex 规则）
     * @return Cloid 实例
     */
    public static Cloid fromStr(String cloid) {
        return new Cloid(cloid);
    }

    public static Cloid auto() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        String hex = Numeric.toHexStringNoPrefix(bytes);
        String padded = leftPad(hex);
        return new Cloid("0x" + padded);
    }

    /**
     * 工厂方法：从十进制字符串构造 Cloid。
     * <p>
     * 使用场景：当上游系统生成的是纯数字 ID（如 "123456789"），可通过该方法直接转换为
     * 满足后端要求的 0x 前缀 + 32 位十六进制格式。
     * 规则：
     * - 仅允许由数字字符组成的非负整数（正则 ^\d+$）；
     * - 自动左侧补零至 32 位十六进制字符；
     * - 超出 32 位十六进制范围时会截断高位（与 fromBigInt/leftPad 行为保持一致）。
     *
     * @param s 十进制数字字符串（例如 "123456"）
     * @return Cloid 实例
     * @throws HypeError 当输入为空、包含非数字字符或为负数时抛出
     */
    public static Cloid fromDecimalString(String s) {
        if (s == null) {
            throw new HypeError("cloid is not a hex string");
        }
        String trimmed = s.trim();
        if (!trimmed.matches("^\\d+$")) {
            throw new HypeError("cloid is not a hex string");
        }
        // BigInteger 构造确保非负十进制
        BigInteger bi = new BigInteger(trimmed);
        if (bi.signum() < 0) {
            throw new HypeError("cloid is not a hex string");
        }
        String hex = bi.toString(16);
        String padded = leftPad(hex);
        return new Cloid("0x" + padded);
    }

    /**
     * 返回原始字符串（对齐 Python to_raw）。
     *
     * @return 原始字符串（0x + 32 hex）
     */
    @JsonValue
    public String toRaw() {
        return raw;
    }

    @Override
    public String toString() {
        return raw;
    }

    /**
     * 左侧补齐到指定长度。
     */
    private static String leftPad(String s) {
        if (s == null)
            s = "";
        if (s.length() >= 32)
            return s.substring(s.length() - 32);
        return String.valueOf('0').repeat(32 - s.length()) + s;
    }
}
