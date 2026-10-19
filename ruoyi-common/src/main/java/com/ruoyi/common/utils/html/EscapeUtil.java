package com.ruoyi.common.utils.html;

import com.ruoyi.common.utils.StringUtils;

/**
 * 转义和反转义工具类
 * 
 * @author ruoyi
 */
public class EscapeUtil
{
    public static final String RE_HTML_MARK = "(<[^<]*?>)|(<[\\s]*?/[^<]*?>)|(<[^<]*?/[\\s]*?>)";

    /**
     * 按 JavaScript escape 规则转义，非 HTML 实体转义
     *
     * <p><strong>注意：本方法不能用于防范 XSS。</strong>它输出的是 {@code %} 与 {@code %u} 形式的
     * URL / JavaScript 转义序列，而不是 {@code &lt;} 这类 HTML 实体。用于过滤 HTML 标签请调用
     * {@link #clean(String)}。</p>
     *
     * @param text 被转义的文本
     * @return 转义后的文本
     */
    public static String escape(String text)
    {
        return encode(text);
    }

    /**
     * {@link #escape(String)} 的逆操作
     *
     * @param content 包含转义符的内容
     * @return 还原后的字符串
     */
    public static String unescape(String content)
    {
        return decode(content);
    }

    /**
     * 清除所有HTML标签，但是不删除标签内的内容
     * 
     * @param content 文本
     * @return 清除标签后的文本
     */
    public static String clean(String content)
    {
        if (StringUtils.isEmpty(content))
        {
            return content;
        }
        return new HTMLFilter().filter(content);
    }

    /**
     * Escape编码
     * 
     * @param text 被编码的文本
     * @return 编码后的字符
     */
    private static String encode(String text)
    {
        if (StringUtils.isEmpty(text))
        {
            return StringUtils.EMPTY;
        }

        final StringBuilder tmp = new StringBuilder(text.length() * 6);
        char c;
        for (int i = 0; i < text.length(); i++)
        {
            c = text.charAt(i);
            if (c < 256)
            {
                tmp.append("%");
                if (c < 16)
                {
                    tmp.append("0");
                }
                tmp.append(Integer.toString(c, 16));
            }
            else
            {
                tmp.append("%u");
                if (c <= 0xfff)
                {
                    // issue#I49JU8@Gitee
                    tmp.append("0");
                }
                tmp.append(Integer.toString(c, 16));
            }
        }
        return tmp.toString();
    }

    /**
     * Escape解码
     * 
     * @param content 被转义的内容
     * @return 解码后的字符串
     */
    public static String decode(String content)
    {
        if (StringUtils.isEmpty(content))
        {
            return content;
        }

        StringBuilder tmp = new StringBuilder(content.length());
        int lastPos = 0, pos = 0;
        char ch;
        while (lastPos < content.length())
        {
            pos = content.indexOf("%", lastPos);
            if (pos == lastPos)
            {
                // 转义序列被截断或格式非法时原样保留，避免抛异常
                if (pos + 1 >= content.length())
                {
                    tmp.append(content.charAt(pos));
                    lastPos = pos + 1;
                    continue;
                }
                boolean unicode = (content.charAt(pos + 1) == 'u');
                int start = pos + (unicode ? 2 : 1);
                int end = pos + (unicode ? 6 : 3);
                if (end > content.length())
                {
                    tmp.append(content.substring(lastPos));
                    lastPos = content.length();
                    break;
                }
                try
                {
                    ch = (char) Integer.parseInt(content.substring(start, end), 16);
                }
                catch (NumberFormatException e)
                {
                    tmp.append(content.charAt(pos));
                    lastPos = pos + 1;
                    continue;
                }
                tmp.append(ch);
                lastPos = end;
            }
            else
            {
                if (pos == -1)
                {
                    tmp.append(content.substring(lastPos));
                    lastPos = content.length();
                }
                else
                {
                    tmp.append(content.substring(lastPos, pos));
                    lastPos = pos;
                }
            }
        }
        return tmp.toString();
    }
}
