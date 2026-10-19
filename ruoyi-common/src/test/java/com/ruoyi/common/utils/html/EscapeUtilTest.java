package com.ruoyi.common.utils.html;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * EscapeUtil 单元测试
 *
 * @author ruoyi
 */
class EscapeUtilTest
{
    @Test
    @DisplayName("clean 应清除 HTML 标签但保留标签内文本")
    void cleanShouldStripTags()
    {
        assertEquals("alert(1);", EscapeUtil.clean("<script>alert(1);</script>"));
        assertEquals("内容", EscapeUtil.clean("<div>内容</div>"));
    }

    /**
     * 澄清 escape 的真实语义：它做的是 JavaScript escape / URL 百分号编码，
     * 而不是 HTML 实体转义。此测试用于锁定该行为，避免有人误以为它能防 XSS。
     */
    @Test
    @DisplayName("escape 是百分号编码而非 HTML 实体转义")
    void escapeIsPercentEncodingNotHtmlEntity()
    {
        // ASCII 字符逐个编码为 %xx，非 ASCII 字符编码为 %uxxxx
        assertEquals("%3c%73%63%72%69%70%74%3e", EscapeUtil.escape("<script>"));
        assertEquals("%3c", EscapeUtil.escape("<"));
        assertEquals("%u4e2d", EscapeUtil.escape("中"));
        assertFalse(EscapeUtil.escape("<").contains("&lt;"), "escape 不应输出 HTML 实体");
    }

    @Test
    @DisplayName("escape 与 unescape 应可往返还原")
    void shouldRoundTrip()
    {
        String original = "<script>alert('xss');</script> 中文";
        assertEquals(original, EscapeUtil.unescape(EscapeUtil.escape(original)));
    }

    /**
     * 空值输入不应抛异常。注意三者对 null 的返回值并不一致：
     * escape 返回空串（encode 走 StringUtils.EMPTY 分支），
     * clean 与 unescape 透传 null。此处记录当前既有行为。
     */
    @Test
    @DisplayName("空值输入应安全返回，不应抛异常")
    void shouldHandleEmptyInput()
    {
        assertEquals("", EscapeUtil.escape(null));
        assertNull(EscapeUtil.clean(null));
        assertNull(EscapeUtil.unescape(null));
        assertEquals("", EscapeUtil.clean(""));
        assertEquals("", EscapeUtil.unescape(""));
    }

    @Test
    @DisplayName("unescape 应正确还原标准转义序列")
    void shouldDecodeValidSequences()
    {
        assertEquals("A", EscapeUtil.unescape("%41"));
        assertEquals("<", EscapeUtil.unescape("%3c"));
        assertEquals("中", EscapeUtil.unescape("%u4e2d"));
        assertEquals("AB", EscapeUtil.unescape("%41%42"));
    }

    /**
     * 回归测试：修复前 decode() 在遇到截断的转义序列时会抛
     * StringIndexOutOfBoundsException。以下输入均不应抛异常。
     */
    @ParameterizedTest(name = "畸形输入不应抛异常: [{0}]")
    @ValueSource(strings = { "%", "abc%", "%u", "%u1", "%u12", "%u123", "%u1234", "%1", "%g1" })
    @DisplayName("转义序列被截断或非法时应原样保留，不抛异常")
    void shouldNotThrowOnMalformedInput(String input)
    {
        assertDoesNotThrow(() -> EscapeUtil.unescape(input));
    }

    @Test
    @DisplayName("结尾的百分号应原样保留")
    void trailingPercentShouldBePreserved()
    {
        assertEquals("abc%", EscapeUtil.unescape("abc%"));
        assertEquals("%", EscapeUtil.unescape("%"));
    }

    @Test
    @DisplayName("非法十六进制字符应原样保留")
    void invalidHexShouldBePreserved()
    {
        assertTrue(EscapeUtil.unescape("%zz").contains("%zz"));
    }

    @Test
    @DisplayName("正常内容与转义序列混合时应正确解析")
    void shouldHandleMixedContent()
    {
        assertEquals("你好A世界", EscapeUtil.unescape("你好%41世界"));
        assertEquals("AAB", EscapeUtil.unescape("%41%41B"));
        assertEquals("A未编码B", EscapeUtil.unescape("A未编码B"));
    }
}
