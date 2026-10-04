package org.btik.espidf.toolwindow.tasks.mcp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * {@link McpOutputView#limit} 的语义：{@code maxLines <= 0} 不限制；
 * 截断时保留末尾<b>恰好 maxLines 个完整行</b>，并如实给出被丢弃的行数。
 */
public class McpOutputViewTest {

    private static String lines(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            sb.append("line-").append(i).append('\n');
        }
        return sb.toString();
    }

    private static int lineCount(String text) {
        return text.split("\n", -1).length - 1;
    }

    /** 回归：22 行输出 + maxLines=5，必须回 5 行（而不是 4 行），并标记截断 */
    @Test
    public void limit_keepsExactlyMaxLinesCompleteLines() {
        McpOutputView view = McpOutputView.limit(lines(22), 5);

        assertTrue(view.truncated());
        assertEquals(17, view.omittedLines());
        assertEquals(5, lineCount(view.text()));
        assertTrue(view.text().startsWith("line-18\n"));
        assertTrue(view.text().endsWith("line-22\n"));
    }

    @Test
    public void limit_maxLinesOneKeepsLastCompleteLine() {
        McpOutputView view = McpOutputView.limit(lines(3), 1);

        assertTrue(view.truncated());
        assertEquals(2, view.omittedLines());
        assertEquals("line-3\n", view.text());
    }

    /** 结尾换行不算一行：行数正好等于上限时不应截断、原样返回 */
    @Test
    public void limit_trailingNewlineIsNotCountedAsLine() {
        McpOutputView view = McpOutputView.limit(lines(2), 2);

        assertFalse(view.truncated());
        assertEquals(0, view.omittedLines());
        assertEquals(lines(2), view.text());
    }

    /** 末行没有换行符时同样能正确截断，并补回换行 */
    @Test
    public void limit_lastLineWithoutTrailingNewline() {
        McpOutputView view = McpOutputView.limit("a\nb", 1);

        assertTrue(view.truncated());
        assertEquals(1, view.omittedLines());
        assertEquals("b\n", view.text());
    }

    @Test
    public void limit_nonPositiveMaxLinesMeansNoLimit() {
        for (int maxLines : new int[]{0, -1}) {
            McpOutputView view = McpOutputView.limit(lines(5), maxLines);
            assertFalse("maxLines=" + maxLines, view.truncated());
            assertEquals(lines(5), view.text());
        }
    }

    @Test
    public void limit_emptyOrNullText() {
        assertEquals(McpOutputView.EMPTY, McpOutputView.limit("", 5));
        assertEquals(McpOutputView.EMPTY, McpOutputView.limit(null, 5));
    }

    /** 只读 text 的调用方也要能看出结果被截断 */
    @Test
    public void note_reportsTruncationInText() {
        String template = "[kept %d, dropped %d]";
        String note = McpOutputView.limit(lines(22), 5).note(template, 5);

        assertEquals("[kept 5, dropped 17]\n", note);
        assertEquals("", McpOutputView.limit(lines(2), 5).note(template, 5));
    }
}
