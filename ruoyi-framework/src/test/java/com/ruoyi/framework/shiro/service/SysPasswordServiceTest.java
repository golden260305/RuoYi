package com.ruoyi.framework.shiro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.security.Md5Utils;
import com.ruoyi.common.utils.security.PasswordUtils;
import com.ruoyi.system.service.ISysUserService;

/**
 * SysPasswordService 单元测试
 *
 * 覆盖 BCrypt 迁移的两条关键契约：
 * 1. 存量用户（历史 MD5 摘要）仍可登录，含 salt 为空的历史导入账号；
 * 2. 登录成功后摘要被自动升级为 BCrypt 并落库。
 *
 * 说明：validate() 的失败分支会经由 AsyncManager 异步写登录日志，
 * 而 AsyncFactory 在构造任务时即读取当前请求（ServletUtils.getRequest()），
 * 脱离 Web 容器无法触发，因此失败分支不在本单元测试范围内。
 *
 * @author ruoyi
 */
class SysPasswordServiceTest
{
    private static final String LOGIN_NAME = "admin";
    private static final String RAW_PASSWORD = "admin123";

    private SysPasswordService passwordService;

    private ISysUserService userService;

    @BeforeEach
    void setUp()
    {
        passwordService = new SysPasswordService();
        userService = mock(ISysUserService.class);

        CacheManager cacheManager = mock(CacheManager.class);
        doReturn(new MapCache()).when(cacheManager).getCache(anyString());

        ReflectionTestUtils.setField(passwordService, "cacheManager", cacheManager);
        ReflectionTestUtils.setField(passwordService, "userService", userService);
        ReflectionTestUtils.setField(passwordService, "maxRetryCount", "5");
        passwordService.init();
    }

    @Test
    @DisplayName("BCrypt 摘要应校验通过")
    void matchesShouldAcceptBcryptPassword()
    {
        SysUser user = buildUser(PasswordUtils.encode(RAW_PASSWORD), "salt");

        assertTrue(passwordService.matches(user, RAW_PASSWORD));
    }

    @Test
    @DisplayName("BCrypt 摘要在明文错误时应校验失败")
    void matchesShouldRejectWrongPasswordForBcrypt()
    {
        SysUser user = buildUser(PasswordUtils.encode(RAW_PASSWORD), "salt");

        assertFalse(passwordService.matches(user, "wrong-password"));
    }

    @Test
    @DisplayName("历史 MD5 摘要（带盐）应仍可登录")
    void matchesShouldAcceptLegacyMd5Password()
    {
        String salt = "abc123";
        SysUser user = buildUser(Md5Utils.hash(LOGIN_NAME + RAW_PASSWORD + salt), salt);

        assertTrue(passwordService.matches(user, RAW_PASSWORD), "存量用户必须能继续登录");
    }

    @Test
    @DisplayName("历史 MD5 摘要（无盐，批量导入场景）应仍可登录")
    void matchesShouldAcceptLegacyMd5PasswordWithoutSalt()
    {
        // 批量导入用户历史上按 md5(loginName + password) 写入且 salt 为空
        SysUser user = buildUser(Md5Utils.hash(LOGIN_NAME + RAW_PASSWORD), "");

        assertTrue(passwordService.matches(user, RAW_PASSWORD), "历史导入账号不能因算法升级而无法登录");
    }

    @Test
    @DisplayName("历史 MD5 摘要在明文错误时应校验失败")
    void matchesShouldRejectWrongPasswordForLegacyMd5()
    {
        SysUser user = buildUser(Md5Utils.hash(LOGIN_NAME + RAW_PASSWORD + "abc123"), "abc123");

        assertFalse(passwordService.matches(user, "wrong-password"));
    }

    @Test
    @DisplayName("encryptPassword 应返回 BCrypt 摘要，且不受 loginName 与 salt 影响")
    void encryptPasswordShouldReturnBcrypt()
    {
        String encoded = passwordService.encryptPassword(LOGIN_NAME, RAW_PASSWORD, "abc123");

        assertTrue(PasswordUtils.isBcrypt(encoded), "重置密码必须写入 BCrypt 摘要");
        assertTrue(PasswordUtils.matches(RAW_PASSWORD, encoded));

        // 换用不同的 loginName 与 salt，摘要仍只由明文决定，可正常校验
        String other = passwordService.encryptPassword("other", RAW_PASSWORD, "other-salt");
        assertTrue(PasswordUtils.isBcrypt(other));
        assertTrue(PasswordUtils.matches(RAW_PASSWORD, other), "loginName 与 salt 不再参与运算");
    }

    @Test
    @DisplayName("登录成功后历史 MD5 摘要应被自动升级为 BCrypt 并落库")
    void validateShouldUpgradeLegacyDigestToBcrypt()
    {
        SysUser user = buildUser(Md5Utils.hash(LOGIN_NAME + RAW_PASSWORD + "abc123"), "abc123");

        passwordService.validate(user, RAW_PASSWORD);

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).updateUserInfo(captor.capture());
        SysUser updated = captor.getValue();

        assertNotNull(updated.getPassword());
        assertTrue(PasswordUtils.isBcrypt(updated.getPassword()), "落库摘要应为 BCrypt");
        assertTrue(PasswordUtils.matches(RAW_PASSWORD, updated.getPassword()));
        assertEquals(user.getUserId(), updated.getUserId());
        assertNotNull(updated.getPwdUpdateDate(), "升级时应刷新密码更新时间");
        assertTrue(PasswordUtils.isBcrypt(user.getPassword()), "内存中的用户对象应同步为 BCrypt");
    }

    @Test
    @DisplayName("已是 BCrypt 的用户登录后不应重复写库")
    void validateShouldNotUpgradeBcryptUser()
    {
        SysUser user = buildUser(PasswordUtils.encode(RAW_PASSWORD), "abc123");

        passwordService.validate(user, RAW_PASSWORD);

        verify(userService, never()).updateUserInfo(any(SysUser.class));
    }

    private SysUser buildUser(String password, String salt)
    {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setLoginName(LOGIN_NAME);
        user.setPassword(password);
        user.setSalt(salt);
        return user;
    }

    /**
     * 内存版登录失败计数缓存，避免依赖真实 Shiro 缓存容器
     */
    private static class MapCache implements Cache<String, AtomicInteger>
    {
        private final Map<String, AtomicInteger> store = new HashMap<>();

        @Override
        public AtomicInteger get(String key)
        {
            return store.get(key);
        }

        @Override
        public AtomicInteger put(String key, AtomicInteger value)
        {
            return store.put(key, value);
        }

        @Override
        public AtomicInteger remove(String key)
        {
            return store.remove(key);
        }

        @Override
        public void clear()
        {
            store.clear();
        }

        @Override
        public int size()
        {
            return store.size();
        }

        @Override
        public Set<String> keys()
        {
            return store.keySet();
        }

        @Override
        public Collection<AtomicInteger> values()
        {
            return store.values();
        }
    }
}
