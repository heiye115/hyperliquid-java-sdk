package io.github.hyperliquid.sdk.utils;

/**
 * Small numeric parsing helpers used when normalizing order and asset inputs.
 */
public class NumberUtils {


    private NumberUtils() {
        throw new AssertionError("No instances for you!");
    }

    /**
     * Returns {@code true} if {@code s} parses as an integer strictly greater than zero.
     *
     * @param s string to test; {@code null} and empty strings yield {@code false}
     * @return {@code true} iff parsing succeeds and value {@code > 0}
     */
    public static boolean isPositiveInt(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        try {
            return Integer.parseInt(s) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
