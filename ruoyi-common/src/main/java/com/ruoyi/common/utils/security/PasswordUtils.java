package com.ruoyi.common.utils.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密工具类
 *
 * 使用 BCrypt 自适应哈希替代原有的单次 MD5，BCrypt 自带随机盐并支持计算强度调节，
 * 同样的明文每次加密结果不同，可有效抵御彩虹表与 GPU 暴力破解。
 *
 * 为兼容存量数据，{@link #isBcrypt(String)} 用于识别历史 MD5 摘要，
 * 登录成功后由业务层自动升级为 BCrypt。
 *
 * @author ruoyi
 */
public class PasswordUtils
{
    /**
     * BCrypt 计算强度，默认 10（2^10 次迭代）
     */
    private static final int BCRYPT_STRENGTH = 10;

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(BCRYPT_STRENGTH);

    /**
     * BCrypt 摘要固定以 $2a$ / $2b$ / $2y$ 开头，总长 60
     */
    private static final String BCRYPT_PREFIX_REGEX = "^\\$2[aby]?\\$\\d{2}\\$[./A-Za-z0-9]{53}$";

    /**
     * 加密明文密码
     *
     * @param rawPassword 明文密码
     * @return BCrypt 摘要
     */
    public static String encode(String rawPassword)
    {
        return ENCODER.encode(rawPassword);
    }

    /**
     * 校验明文密码与摘要是否匹配
     *
     * @param rawPassword 明文密码
     * @param encodedPassword 已加密的摘要
     * @return 是否匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword)
    {
        if (rawPassword == null || encodedPassword == null || encodedPassword.isEmpty())
        {
            return false;
        }
        if (!isBcrypt(encodedPassword))
        {
            return false;
        }
        return ENCODER.matches(rawPassword, encodedPassword);
    }

    /**
     * 判断是否为 BCrypt 摘要
     *
     * @param encodedPassword 密码摘要
     * @return true 表示已经是 BCrypt 格式
     */
    public static boolean isBcrypt(String encodedPassword)
    {
        return encodedPassword != null && encodedPassword.matches(BCRYPT_PREFIX_REGEX);
    }
}
