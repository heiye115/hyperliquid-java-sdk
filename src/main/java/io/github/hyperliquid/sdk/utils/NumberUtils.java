package io.github.hyperliquid.sdk.utils;

public class NumberUtils {


    private NumberUtils() {
        throw new AssertionError("No instances for you!");
    }

 
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
