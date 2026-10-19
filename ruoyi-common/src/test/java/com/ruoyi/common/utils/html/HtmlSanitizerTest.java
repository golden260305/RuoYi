package com.ruoyi.common.utils.html;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * HtmlSanitizer 单元测试
 *
 * 公告内容以 th:utext 渲染，属于存储型 XSS 的高危路径。
 * 本测试锁定白名单清洗必须达到的安全底线：可执行标签、事件属性、
 * 危险协议一律剥离，同时不破坏正常富文本排版。
 *
 * @author ruoyi
 */
class HtmlSanitizerTest
{
    @Test
    @DisplayName("script 标签及其内容应被完全清除")
    void shouldStripScriptElement()
    {
        String cleaned = HtmlSanitizer.clean("<script>alert(1)</script>");
        assertFalse(cleaned.contains("script"), "不应残留 script 标签: " + cleaned);
        assertFalse(cleaned.contains("alert"), "不应残留 script 内容: " + cleaned);
    }

    @Test
    @DisplayName("外链 script 应被清除")
    void shouldStripExternalScriptElement()
    {
        String cleaned = HtmlSanitizer.clean("<script src=\"http://evil.example/x.js\"></script>");
        assertFalse(cleaned.contains("script"), "不应残留 script 标签: " + cleaned);
    }

    @Test
    @DisplayName("iframe 标签应被清除")
    void shouldStripIframeElement()
    {
        String cleaned = HtmlSanitizer.clean("<iframe src=\"http://evil.example\"></iframe>");
        assertFalse(cleaned.contains("iframe"), "不应残留 iframe 标签: " + cleaned);
    }

    @Test
    @DisplayName("事件处理属性应被剥离")
    void shouldStripEventAttributes()
    {
        String cleaned = HtmlSanitizer.clean("<div onclick=\"alert(1)\">hi</div>");
        assertFalse(cleaned.contains("onclick"), "不应残留 onclick: " + cleaned);
        assertFalse(cleaned.contains("alert"), "不应残留事件脚本: " + cleaned);
        assertTrue(cleaned.contains("hi"), "文本内容应保留: " + cleaned);
    }

    @Test
    @DisplayName("img 的 onerror 应被剥离")
    void shouldStripImgOnerror()
    {
        String cleaned = HtmlSanitizer.clean("<img src=x onerror=alert(1)>");
        assertFalse(cleaned.contains("onerror"), "不应残留 onerror: " + cleaned);
        assertFalse(cleaned.contains("alert"), "不应残留事件脚本: " + cleaned);
    }

    @Test
    @DisplayName("javascript 协议应被剥离，链接文字保留")
    void shouldStripJavascriptProtocol()
    {
        String cleaned = HtmlSanitizer.clean("<a href=\"javascript:alert(1)\">x</a>");
        assertFalse(cleaned.contains("javascript"), "不应残留 javascript 协议: " + cleaned);
        assertTrue(cleaned.contains("x"), "链接文字应保留: " + cleaned);
    }

    @Test
    @DisplayName("data 协议图片应被剥离，SVG 可携带脚本")
    void shouldStripDataProtocol()
    {
        String cleaned = HtmlSanitizer.clean("<img src=\"data:image/svg+xml;base64,AAA\">");
        assertFalse(cleaned.contains("data:"), "不应残留 data 协议: " + cleaned);
    }

    @Test
    @DisplayName("style 属性应被剥离，可能携带 url 注入")
    void shouldStripStyleAttribute()
    {
        String cleaned = HtmlSanitizer.clean("<p style=\"color:red\">正文</p>");
        assertFalse(cleaned.contains("style="), "不应残留 style 属性: " + cleaned);
        assertTrue(cleaned.contains("正文"), "文本内容应保留: " + cleaned);
    }

    @Test
    @DisplayName("正常富文本应原样保留")
    void shouldKeepSafeRichText()
    {
        String html = "<p>hello <b>world</b></p>";
        assertEquals(html, HtmlSanitizer.clean(html));
    }

    @Test
    @DisplayName("安全的 http 链接与图片应保留")
    void shouldKeepSafeUrls()
    {
        String link = HtmlSanitizer.clean("<a href=\"http://example.com\">ok</a>");
        assertTrue(link.contains("http://example.com"), "http 链接应保留: " + link);

        String image = HtmlSanitizer.clean("<img src=\"http://example.com/a.png\" alt=\"a\">");
        assertTrue(image.contains("http://example.com/a.png"), "http 图片应保留: " + image);
    }

    @Test
    @DisplayName("编辑器常用的结构性标签应保留")
    void shouldKeepEditorStructuralTags()
    {
        String cleaned = HtmlSanitizer.clean("<figure><figcaption>说明</figcaption></figure><hr>");
        assertTrue(cleaned.contains("figure"), "figure 标签应在白名单内: " + cleaned);
        assertTrue(cleaned.contains("figcaption"), "figcaption 标签应在白名单内: " + cleaned);
        assertTrue(cleaned.contains("hr"), "hr 标签应在白名单内: " + cleaned);
    }

    @Test
    @DisplayName("空值与空串应安全返回，不应抛异常")
    void shouldHandleEmptyInput()
    {
        assertNull(HtmlSanitizer.clean(null));
        assertEquals("", HtmlSanitizer.clean(""));
    }
}
