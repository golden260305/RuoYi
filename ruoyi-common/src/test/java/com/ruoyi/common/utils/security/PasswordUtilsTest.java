package com.ruoyi.common.utils.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PasswordUtils 单元测试
 *
 * 锁定 BCrypt 迁移的核心契约：摘要格式、随机盐特性、格式识别，
 * 以及对历史 MD5 摘要的拒绝，避免后续改动破坏存量用户登录。
 *
 * @author ruoyi
 */
class PasswordUtilsTest
{
    @Test
    @DisplayName("BCrypt 摘要应为 60 字符且可被 isBcrypt 识别")
    void encodeShouldProduceBcryptDigest()
    {
        String encoded = PasswordUtils.encode("admin123");
        assertEquals(60, encoded.length(), "BCrypt 摘要固定 60 字符");
        assertTrue(PasswordUtils.isBcrypt(encoded), "自身生成的摘要必须能被识别为 BCrypt");
    }

    @Test
    @DisplayName("相同明文每次加密结果不同（BCrypt 自带随机盐）")
    void encodeShouldUseRandomSalt()
    {
        assertNotEquals(PasswordUtils.encode("admin123"), PasswordUtils.encode("admin123"),
                "随机盐保证相同明文产生不同摘要");
    }

    @Test
    @DisplayName("正确明文校验通过，错误明文校验失败")
    void matchesShouldVerifyPlainPassword()
    {
        String encoded = PasswordUtils.encode("admin123");
        assertTrue(PasswordUtils.matches("admin123", encoded));
        assertFalse(PasswordUtils.matches("admin124", encoded));
        assertFalse(PasswordUtils.matches("", encoded));
    }

    @Test
    @DisplayName("空值与非法摘要不应抛异常，应返回 false")
    void matchesShouldHandleInvalidInput()
    {
        String encoded = PasswordUtils.encode("admin123");
        assertFalse(PasswordUtils.matches(null, encoded));
        assertFalse(PasswordUtils.matches("admin123", null));
        assertFalse(PasswordUtils.matches("admin123", ""));
        assertFalse(PasswordUtils.matches("admin123", "not-a-bcrypt-digest"));
    }

    @Test
    @DisplayName("历史 MD5 摘要不应被识别为 BCrypt")
    void isBcryptShouldRejectLegacyDigest()
    {
        assertFalse(PasswordUtils.isBcrypt(Md5Utils.hash("adminadmin123")), "32 位 MD5 摘要不是 BCrypt");
        assertFalse(PasswordUtils.isBcrypt(null));
        assertFalse(PasswordUtils.isBcrypt(""));
    }

    @Test
    @DisplayName("摘要中不应出现明文密码")
    void encodedShouldNotContainPlainPassword()
    {
        assertFalse(PasswordUtils.encode("admin123").contains("admin123"));
    }
}
