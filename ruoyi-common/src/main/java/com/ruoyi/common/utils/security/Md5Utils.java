package com.ruoyi.common.utils.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Md5加密方法
 * 
 * @author ruoyi
 */
public class Md5Utils
{
    private static final Logger log = LoggerFactory.getLogger(Md5Utils.class);

    private static byte[] md5(String s)
    {
        try
        {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.update(s.getBytes(StandardCharsets.UTF_8));
            return algorithm.digest();
        }
        catch (NoSuchAlgorithmException e)
        {
            // 摘要计算失败必须中断，绝不能降级返回原始输入，否则密码将以明文形式落库
            log.error("MD5 algorithm is not available", e);
            throw new IllegalStateException("MD5 algorithm is not available", e);
        }
    }

    private static String toHex(byte hash[])
    {
        if (hash == null)
        {
            return null;
        }
        StringBuilder buf = new StringBuilder(hash.length * 2);
        int i;

        for (i = 0; i < hash.length; i++)
        {
            if ((hash[i] & 0xff) < 0x10)
            {
                buf.append("0");
            }
            buf.append(Integer.toHexString(hash[i] & 0xff));
        }
        return buf.toString();
    }

    /**
     * 计算字符串的 MD5 摘要
     *
     * @param s 原文
     * @return 32 位小写十六进制摘要，算法不可用时抛出 {@link IllegalStateException}
     */
    public static String hash(String s)
    {
        return toHex(md5(s));
    }
}
