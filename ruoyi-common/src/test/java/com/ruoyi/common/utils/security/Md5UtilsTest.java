package com.ruoyi.common.utils.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Md5Utils 单元测试
 *
 * @author ruoyi
 */
class Md5UtilsTest
{
    @Test
    @DisplayName("标准测试向量应得到已知摘要")
    void shouldMatchKnownVectors()
    {
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", Md5Utils.hash(""));
        assertEquals("900150983cd24fb0d6963f7d28e17f72", Md5Utils.hash("abc"));
        assertEquals("5d41402abc4b2a76b9719d911017c592", Md5Utils.hash("hello"));
    }

    @Test
    @DisplayName("摘要应为 32 位小写十六进制")
    void shouldReturn32LowercaseHexChars()
    {
        String hash = Md5Utils.hash("ruoyi");
        assertEquals(32, hash.length());
        assertTrue(hash.matches("[0-9a-f]{32}"), "摘要应只包含小写十六进制字符: " + hash);
    }

    /**
     * 回归测试：摘要中若存在小于 0x10 的字节，必须补零到两位，
     * 否则密文长度会不足 32 位，且无法与历史密文比对。
     */
    @Test
    @DisplayName("摘要字节小于 0x10 时应补前导零，保持 32 位长度")
    void shouldPadLeadingZeroBytes()
    {
        // md5("abc") = 900150983cd24fb0d6963f7d28e17f72
        // 字节序列含 0x01，必须输出为 "01" 而非 "1"
        String hash = Md5Utils.hash("abc");
        assertEquals("900150983cd24fb0d6963f7d28e17f72", hash);
        assertTrue(hash.contains("01"), "小于 0x10 的字节应补前导零: " + hash);
        assertEquals(32, hash.length());
    }

    /**
     * 核心回归测试：修复前 hash() 在摘要计算失败时会兜底返回原始输入，
     * 导致密码以明文形式落库。此处确认结果始终不等于原文。
     */
    @Test
    @DisplayName("任何输入的摘要都不得等于原始输入")
    void shouldNeverReturnPlaintext()
    {
        String[] inputs = { "", "a", "admin", "123456", "测试中文密码", "!@#$%^&*()", "a".repeat(1000) };
        for (String input : inputs)
        {
            assertNotEquals(input, Md5Utils.hash(input), "摘要不得回退为明文: " + input);
        }
    }

    @Test
    @DisplayName("相同输入应得到相同摘要，不同输入应得到不同摘要")
    void shouldBeDeterministicAndDistinct()
    {
        assertEquals(Md5Utils.hash("ruoyi"), Md5Utils.hash("ruoyi"));
        assertNotEquals(Md5Utils.hash("ruoyi"), Md5Utils.hash("ruoyj"));
    }

    @Test
    @DisplayName("中文等多字节字符不应抛异常")
    void shouldHandleNonAsciiInput()
    {
        assertEquals(32, Md5Utils.hash("若依管理系统").length());
    }

    @Test
    @DisplayName("项目密码哈希场景（loginName + password + salt）应稳定")
    void shouldMatchShiroPasswordComposition()
    {
        // 对应 SysPasswordService.encryptPassword 的组合格式
        String loginName = "admin";
        String password = "admin123";
        String salt = "abcdef";
        String expected = Md5Utils.hash(loginName + password + salt);
        assertEquals(expected, Md5Utils.hash(loginName + password + salt));
        assertNotEquals(Md5Utils.hash(loginName + password), expected);
    }
}
