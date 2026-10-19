package com.ruoyi.common.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * IpUtils 单元测试
 *
 * @author ruoyi
 */
class IpUtilsTest
{
    @ParameterizedTest(name = "{0} 是内网地址")
    @ValueSource(strings = { "10.0.0.1", "10.255.255.255", "172.16.0.1", "172.31.255.255", "192.168.1.1", "127.0.0.1" })
    @DisplayName("应正确识别内网地址")
    void shouldDetectInternalIp(String ip)
    {
        assertTrue(IpUtils.internalIp(ip));
    }

    @ParameterizedTest(name = "{0} 不是内网地址")
    @ValueSource(strings = { "8.8.8.8", "1.1.1.1", "172.32.0.1", "192.169.1.1", "203.0.113.5" })
    @DisplayName("应正确识别外网地址")
    void shouldDetectExternalIp(String ip)
    {
        assertFalse(IpUtils.internalIp(ip));
    }

    /**
     * 回归测试：修复前 switch 缺少 break 导致 case 172 落穿到 case 192，
     * 使 172.168.x.x（公网地址）被误判为内网。此用例锁定修复后的正确行为。
     */
    @Test
    @DisplayName("172.168.x.x 是公网地址，不应因 switch 落穿被误判为内网")
    void switchFallthroughShouldNotMisclassify172()
    {
        assertFalse(IpUtils.internalIp("172.168.1.1"));
        assertFalse(IpUtils.internalIp("172.168.0.255"));
    }

    @ParameterizedTest(name = "null/空/非法输入 [{0}] 按内网处理且不抛异常")
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "abc", "not.an.ip" })
    @DisplayName("空值与非法输入应按内网处理且不抛异常")
    void shouldHandleInvalidInput(String ip)
    {
        assertTrue(IpUtils.internalIp(ip));
    }

    @ParameterizedTest(name = "isIP({0}) = {1}")
    @CsvSource({ "192.168.1.1,true", "0.0.0.0,true", "255.255.255.255,true", "256.1.1.1,false", "1.1.1,false", "abc,false" })
    @DisplayName("isIP 应校验 IPv4 格式")
    void shouldValidateIpFormat(String ip, boolean expected)
    {
        assertEquals(expected, IpUtils.isIP(ip));
    }

    @Test
    @DisplayName("isIpWildCard 应识别通配符地址")
    void shouldDetectWildCard()
    {
        assertTrue(IpUtils.isIpWildCard("192.168.*.*"));
        assertTrue(IpUtils.isIpWildCard("*.*.*.*"));
        assertFalse(IpUtils.isIpWildCard("192.168.1.1"));
    }

    @Test
    @DisplayName("isIPSegment 应识别网段格式")
    void shouldDetectIpSegment()
    {
        assertTrue(IpUtils.isIPSegment("10.10.10.1-10.10.10.99"));
        assertFalse(IpUtils.isIPSegment("10.10.10.1"));
    }

    @Test
    @DisplayName("ipIsInNetNoCheck 应判断 IP 是否落在网段内")
    void shouldCheckIpInSegment()
    {
        assertTrue(IpUtils.ipIsInNetNoCheck("10.10.10.1-10.10.10.99", "10.10.10.50"));
        assertTrue(IpUtils.ipIsInNetNoCheck("10.10.10.1-10.10.10.99", "10.10.10.1"));
        assertTrue(IpUtils.ipIsInNetNoCheck("10.10.10.1-10.10.10.99", "10.10.10.99"));
        assertFalse(IpUtils.ipIsInNetNoCheck("10.10.10.1-10.10.10.99", "10.10.10.100"));
    }

    /**
     * 回归测试：修复前缺少分隔符时会调用 substring(0, -1) 抛异常。
     */
    @ParameterizedTest(name = "非法网段配置不应抛异常: [{0}]")
    @ValueSource(strings = { "10.10.10.1", "abc", "10.10.10.1-", "-10.10.10.1", "1-2", "10.10.10.1-a.b.c.d" })
    @DisplayName("网段配置非法时应返回 false 而非抛异常")
    void shouldNotThrowOnInvalidSegment(String iparea)
    {
        assertDoesNotThrow(() -> IpUtils.ipIsInNetNoCheck(iparea, "10.10.10.50"));
        assertFalse(IpUtils.ipIsInNetNoCheck(iparea, "10.10.10.50"));
    }

    @Test
    @DisplayName("ipIsInWildCardNoCheck 应支持通配符匹配")
    void shouldMatchWildCard()
    {
        assertTrue(IpUtils.ipIsInWildCardNoCheck("192.168.*.*", "192.168.1.10"));
        assertFalse(IpUtils.ipIsInWildCardNoCheck("192.168.*.*", "192.169.1.10"));
    }

    /**
     * 回归测试：修复前通配符与 IP 段数不一致时会数组越界。
     */
    @Test
    @DisplayName("通配符与 IP 段数不一致时应返回 false 而非抛异常")
    void shouldNotThrowOnSegmentLengthMismatch()
    {
        assertDoesNotThrow(() -> IpUtils.ipIsInWildCardNoCheck("192.168.*", "192.168.1.1"));
        assertFalse(IpUtils.ipIsInWildCardNoCheck("192.168.*", "192.168.1.1"));
    }

    @Test
    @DisplayName("textToNumericFormatV4 应正确转换 IPv4")
    void shouldConvertToBytes()
    {
        assertEquals(4, IpUtils.textToNumericFormatV4("192.168.1.1").length);
        assertNull(IpUtils.textToNumericFormatV4(null));
        assertNull(IpUtils.textToNumericFormatV4(""));
    }

    @Test
    @DisplayName("多级反向代理应取第一个非 unknown 的 IP 并去除空白")
    void shouldParseReverseProxyChain()
    {
        assertEquals("1.1.1.1", IpUtils.getMultistageReverseProxyIp("1.1.1.1, 2.2.2.2"));
        assertEquals("1.1.1.1", IpUtils.getMultistageReverseProxyIp("unknown, 1.1.1.1"));
        assertEquals("1.1.1.1", IpUtils.getMultistageReverseProxyIp("1.1.1.1"));
    }

    @Test
    @DisplayName("getIpAddr 对空请求应返回 unknown")
    void shouldHandleNullRequest()
    {
        assertEquals("unknown", IpUtils.getIpAddr(null));
    }

    @Test
    @DisplayName("isMatchedIp 应支持精确、通配符与网段三种写法")
    void shouldMatchFilterRules()
    {
        assertTrue(IpUtils.isMatchedIp("192.168.1.1", "192.168.1.1"));
        assertTrue(IpUtils.isMatchedIp("192.168.*.*", "192.168.1.10"));
        assertTrue(IpUtils.isMatchedIp("10.10.10.1-10.10.10.99", "10.10.10.50"));
        assertFalse(IpUtils.isMatchedIp("192.168.1.1", "10.0.0.1"));
        assertFalse(IpUtils.isMatchedIp(null, "10.0.0.1"));
    }
}
