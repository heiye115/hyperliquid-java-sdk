package io.github.hyperliquid.sdk.model.order;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.hyperliquid.sdk.utils.HypeError;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Client order ID (Cloid) Java implementation, aligned with Python version.
 *
 * <p>
 * - String must start with "0x" and the subsequent hexadecimal characters must be 32 in length (i.e., 16 bytes);
 * - Throws TypeError when not conforming to the above format:
 * - Prefix not 0x -> "cloid is not a hex string"
 * - Length not 32 -> "cloid is not 16 bytes"
 * - from_int(cloid: int) will format with 0x prefix, width 34 (including 0x), left-padded with zeros if insufficient;
 * - from_str(cloid: str) directly wraps;
 * - to_raw() returns the raw string.
 */
public class Cloid {

    /**
     * Raw Cloid string (0x + 32 hex chars)
     */
    private final String raw;

    /**
     * Compatible with old interface: get raw string.
     *
     * @return raw Cloid string
     */
    public String getRaw() {
        return raw;
    }

    /**
     * Constructor: validate and save raw Cloid string.
     *
     * @param raw raw string (must start with 0x and have length 34, including 0x + 32 hex characters)
     * @throws HypeError when not starting with 0x or length not 16 bytes (32 hex)
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
     * Internal validation logic
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
     * Factory method: construct Cloid from integer (aligned with Python from_int).
     *
     * @param cloid integer value (Java int)
     * @return Cloid instance
     * @throws HypeError when formatted result length is not 16 bytes (e.g., value exceeds 32 hex length)
     */
    public static Cloid fromInt(Integer cloid) {
        String hex = Integer.toHexString(cloid);
        String padded = leftPad(hex);
        return new Cloid("0x" + padded);
    }

    /**
     * Factory method: construct Cloid from long (optional extension to cover larger range).
     *
     * @param cloid long value
     * @return Cloid instance
     */
    public static Cloid fromLong(Long cloid) {
        String hex = Long.toHexString(cloid);
        String padded = leftPad(hex);
        return new Cloid("0x" + padded);
    }

    /**
     * Factory method: construct Cloid from BigInteger (cover scenarios exceeding 64 bits).
     *
     * @param cloid BigInteger value
     * @return Cloid instance
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
     * Factory method: construct Cloid from string (aligned with Python from_str).
     *
     * @param cloid string (must satisfy 0x + 32 hex rule)
     * @return Cloid instance
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
     * Factory method: construct Cloid from decimal string.
     * <p>
     * Usage scenario: when upstream system generates pure numeric ID (e.g., "123456789"), can use this method to directly convert to
     * satisfy backend requirement of 0x prefix + 32-bit hexadecimal format.
     * Rules:
     * - Only allows non-negative integers composed of digit characters (regex ^\d+$);
     * - Automatically left-pads to 32 hexadecimal characters;
     * - Truncates high bits when exceeding 32 hexadecimal range (consistent with fromBigInt/leftPad behavior).
     *
     * @param s decimal numeric string (e.g., "123456")
     * @return Cloid instance
     * @throws HypeError when input is empty, contains non-digit characters, or is negative
     */
    public static Cloid fromDecimalString(String s) {
        if (s == null) {
            throw new HypeError("cloid is not a hex string");
        }
        String trimmed = s.trim();
        if (!trimmed.matches("^\\d+$")) {
            throw new HypeError("cloid is not a hex string");
        }
        // BigInteger construction ensures non-negative decimal
        BigInteger bi = new BigInteger(trimmed);
        if (bi.signum() < 0) {
            throw new HypeError("cloid is not a hex string");
        }
        String hex = bi.toString(16);
        String padded = leftPad(hex);
        return new Cloid("0x" + padded);
    }

    /**
     * Return raw string (aligned with Python to_raw).
     *
     * @return raw string (0x + 32 hex)
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
     * Left-pad to specified length.
     */
    private static String leftPad(String s) {
        if (s == null)
            s = "";
        if (s.length() >= 32)
            return s.substring(s.length() - 32);
        return String.valueOf('0').repeat(32 - s.length()) + s;
    }
}
