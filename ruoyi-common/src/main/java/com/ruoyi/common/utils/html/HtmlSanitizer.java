package com.ruoyi.common.utils.html;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import com.ruoyi.common.utils.StringUtils;

/**
 * HTML 白名单清洗工具
 *
 * 用于富文本（如公告内容）入库前的安全处理：只保留白名单内的标签与属性，
 * 剥离 script、iframe、事件属性、javascript 协议等可执行内容，
 * 从源头阻断存储型 XSS。
 *
 * 与 {@link EscapeUtil#clean(String)} 的区别：后者把整段内容转义为纯文本，
 * 会破坏富文本排版；本工具保留安全的排版标签，适用于需要渲染 HTML 的场景。
 *
 * @author ruoyi
 */
public class HtmlSanitizer
{
    /**
     * 富文本白名单：在 jsoup relaxed 策略基础上补充编辑器常用标签
     */
    private static final Safelist SAFELIST = Safelist.relaxed()
            // 编辑器常用的结构性标签
            .addTags("hr", "figure", "figcaption", "s", "font", "video", "source")
            // 允许 class 以保留编辑器样式类，但不允许 style（可携带 expression/url 注入）
            .addAttributes(":all", "class")
            .addAttributes("img", "src", "alt", "title", "width", "height")
            .addAttributes("a", "href", "title", "target")
            .addAttributes("td", "colspan", "rowspan", "width")
            .addAttributes("th", "colspan", "rowspan", "width")
            .addAttributes("table", "border", "cellpadding", "cellspacing", "width")
            .addAttributes("video", "src", "controls", "width", "height")
            .addAttributes("source", "src", "type")
            // 仅允许安全协议，禁止 data:（SVG 可携带脚本）与 javascript:
            .addProtocols("img", "src", "http", "https")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addProtocols("video", "src", "http", "https")
            .addProtocols("source", "src", "http", "https");

    /**
     * 清洗富文本内容
     *
     * @param html 原始 HTML
     * @return 清洗后的安全 HTML，入参为空时原样返回
     */
    public static String clean(String html)
    {
        if (StringUtils.isEmpty(html))
        {
            return html;
        }
        return Jsoup.clean(html, SAFELIST);
    }
}
