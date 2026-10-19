package com.ruoyi.framework.shiro.service;

import java.util.concurrent.atomic.AtomicInteger;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.ShiroConstants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.user.UserPasswordNotMatchException;
import com.ruoyi.common.exception.user.UserPasswordRetryLimitExceedException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.security.Md5Utils;
import com.ruoyi.common.utils.security.PasswordUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.system.service.ISysUserService;
import jakarta.annotation.PostConstruct;

/**
 * 登录密码方法
 * 
 * @author ruoyi
 */
@Component
public class SysPasswordService
{
    private static final Logger log = LoggerFactory.getLogger(SysPasswordService.class);

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ISysUserService userService;

    private Cache<String, AtomicInteger> loginRecordCache;

    @Value(value = "${user.password.maxRetryCount}")
    private String maxRetryCount;

    @PostConstruct
    public void init()
    {
        loginRecordCache = cacheManager.getCache(ShiroConstants.LOGIN_RECORD_CACHE);
    }

    public void validate(SysUser user, String password)
    {
        String loginName = user.getLoginName();

        AtomicInteger retryCount = loginRecordCache.get(loginName);

        if (retryCount == null)
        {
            retryCount = new AtomicInteger(0);
            loginRecordCache.put(loginName, retryCount);
        }
        if (retryCount.incrementAndGet() > Integer.valueOf(maxRetryCount).intValue())
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(loginName, Constants.LOGIN_FAIL, MessageUtils.message("user.password.retry.limit.exceed", maxRetryCount)));
            throw new UserPasswordRetryLimitExceedException(Integer.valueOf(maxRetryCount).intValue());
        }

        if (!matches(user, password))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(loginName, Constants.LOGIN_FAIL, MessageUtils.message("user.password.retry.limit.count", retryCount)));
            loginRecordCache.put(loginName, retryCount);
            throw new UserPasswordNotMatchException();
        }
        else
        {
            clearLoginRecordCache(loginName);
            // 历史 MD5 摘要登录成功后自动升级为 BCrypt，用户无感知
            upgradePasswordIfNecessary(user, password);
        }
    }

    public boolean matches(SysUser user, String newPassword)
    {
        String storedPassword = user.getPassword();
        if (PasswordUtils.isBcrypt(storedPassword))
        {
            return PasswordUtils.matches(newPassword, storedPassword);
        }
        // 兼容历史摘要：MD5(loginName + password + salt)
        String legacy = Md5Utils.hash(user.getLoginName() + newPassword + StringUtils.defaultString(user.getSalt()));
        return legacy.equals(storedPassword);
    }

    /**
     * 将历史 MD5 摘要升级为 BCrypt，失败不影响本次登录
     *
     * @param user 用户信息
     * @param rawPassword 本次登录的明文密码
     */
    private void upgradePasswordIfNecessary(SysUser user, String rawPassword)
    {
        if (PasswordUtils.isBcrypt(user.getPassword()))
        {
            return;
        }
        try
        {
            String encoded = PasswordUtils.encode(rawPassword);
            SysUser update = new SysUser();
            update.setUserId(user.getUserId());
            update.setPassword(encoded);
            update.setPwdUpdateDate(DateUtils.getNowDate());
            userService.updateUserInfo(update);
            user.setPassword(encoded);
            log.info("用户 {} 的密码摘要已由 MD5 升级为 BCrypt", user.getLoginName());
        }
        catch (Exception e)
        {
            log.warn("用户 {} 的密码摘要升级失败，本次登录不受影响", user.getLoginName(), e);
        }
    }

    public void clearLoginRecordCache(String loginName)
    {
        loginRecordCache.remove(loginName);
    }

    /**
     * 加密密码
     *
     * 保留原方法签名以兼容既有调用方，loginName 与 salt 已不再参与运算，
     * BCrypt 会在内部生成独立随机盐。
     *
     * @param loginName 登录名（保留参数，不再使用）
     * @param password 明文密码
     * @param salt 历史盐值（保留参数，不再使用）
     * @return BCrypt 摘要
     */
    public String encryptPassword(String loginName, String password, String salt)
    {
        return PasswordUtils.encode(password);
    }
}
