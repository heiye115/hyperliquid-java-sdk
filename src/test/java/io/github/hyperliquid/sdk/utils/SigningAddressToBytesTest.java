package io.github.hyperliquid.sdk.utils;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 边界测试：Signing.addressToBytes 地址长度校验与兼容行为。
 *
 * 覆盖场景：
 * - 严格模式（默认）：20 字节地址正常返回；短地址/长地址/非十六进制输入抛出异常；
 * - 兼容模式：短地址左侧补零、长地址截取末尾 20 字节。
 */
public class SigningAddressToBytesTest {

    @AfterEach
    public void restoreStrict() {
        // 保证每个测试结束后恢复严格模式，避免测试间相互影响
        Signing.setStrictAddressLength(true);
    }

    @Test
    public void testStrictValidAddress() {
        Signing.setStrictAddressLength(true);
        String addr = "0x0000000000000000000000000000000000000000"; // 20 bytes
        byte[] bytes = Signing.addressToBytes(addr);
        assertNotNull(bytes);
        assertEquals(20, bytes.length);
        for (byte b : bytes) {
            assertEquals(0, b);
        }
    }

    @Test
    public void testStrictShortAddressThrows() {
        Signing.setStrictAddressLength(true);
        String addr = "0x1234"; // 2 bytes
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Signing.addressToBytes(addr));
        assertTrue(ex.getMessage().contains("20 bytes") || ex.getMessage().contains("must be exactly"));
    }

    @Test
    public void testStrictLongAddressThrows() {
        Signing.setStrictAddressLength(true);
        // 22 bytes（44 hex chars）
        StringBuilder sb = new StringBuilder("0x");
        for (int i = 0; i < 44; i++) sb.append('a');
        String addr = sb.toString();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Signing.addressToBytes(addr));
        assertTrue(ex.getMessage().contains("20 bytes") || ex.getMessage().contains("must be exactly"));
    }

    @Test
    public void testStrictInvalidHexThrows() {
        Signing.setStrictAddressLength(true);
        String addr = "0xGHIJKL"; // 非十六进制
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Signing.addressToBytes(addr));
        assertTrue(ex.getMessage().toLowerCase().contains("non-hex"));
    }

    @Test
    public void testLenientShortPadsLeft() {
        Signing.setStrictAddressLength(false);
        String addr = "0x1234"; // 2 bytes
        byte[] bytes = Signing.addressToBytes(addr);
        assertEquals(20, bytes.length);
        // 末尾两字节应为 0x12, 0x34（左侧补零）
        assertEquals(0x12, bytes[18] & 0xff);
        assertEquals(0x34, bytes[19] & 0xff);
        // 前 18 字节均为 0
        for (int i = 0; i < 18; i++) assertEquals(0, bytes[i]);
    }

    @Test
    public void testLenientLongTruncatesTail() {
        Signing.setStrictAddressLength(false);
        // 22 字节：前 2 字节 0x11, 0x22，然后 20 字节从 0x01 到 0x14
        StringBuilder sb = new StringBuilder("0x");
        sb.append("1122");
        for (int i = 1; i <= 20; i++) sb.append(String.format("%02x", i));
        String addr = sb.toString();

        byte[] bytes = Signing.addressToBytes(addr);
        assertEquals(20, bytes.length);
        // 应截取末尾 20 字节，即 0x01...0x14
        for (int i = 0; i < 20; i++) {
            int expected = i + 1; // 0x01 到 0x14
            assertEquals(expected, bytes[i] & 0xff);
        }
    }
}

