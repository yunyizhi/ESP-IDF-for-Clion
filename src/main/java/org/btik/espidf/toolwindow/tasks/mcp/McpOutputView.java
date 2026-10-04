package org.btik.espidf.toolwindow.tasks.mcp;

import org.jetbrains.annotations.NotNull;

/**
 * MCP 工具的输出视图：按 {@code maxLines} 保留末尾若干「完整行」。
 * <p>
 * 语义约定：
 * <ul>
 *     <li>{@code maxLines <= 0} 表示不限制，原样返回；</li>
 *     <li>截断时返回末尾恰好 {@code maxLines} 个完整行（每行补回换行符），并记录被丢弃的行数。</li>
 * </ul>
 * 注意：进程输出通常以换行结尾，{@code split} 会多切出一个空元素，它不是一行内容，
 * 因此不参与计数与截断——否则会出现「要 5 行却只回 4 行」的偏差。
 */
record McpOutputView(String text, boolean truncated, int omittedLines) {

    static final McpOutputView EMPTY = new McpOutputView("", false, 0);

    static McpOutputView of(String text) {
        return new McpOutputView(text == null ? "" : text, false, 0);
    }

    static McpOutputView limit(String text, int maxLines) {
        if (text == null || text.isEmpty()) {
            return EMPTY;
        }
        if (maxLines <= 0) {
            return of(text);
        }
        String[] lines = text.split("\n", -1);
        int end = lines.length;
        while (end > 0 && lines[end - 1].isEmpty()) {
            end--;
        }
        if (end <= maxLines) {
            return of(text);
        }
        int omitted = end - maxLines;
        StringBuilder sb = new StringBuilder();
        for (int i = omitted; i < end; i++) {
            sb.append(lines[i]).append('\n');
        }
        return new McpOutputView(sb.toString(), true, omitted);
    }

    /**
     * 截断标记的文本形式，{@code template} 内需有两个 {@code %d}（保留行数、丢弃行数）。
     * <p>
     * 工具描述承诺「截断时会标记」，但结构化字段只在 JSON 里，只读 text 的调用方会误以为
     * 拿到的是「窗口内全部」，因此标记同样写进文本。模板由调用方传入以便单测（i18n 需要平台）。
     */
    String note(@NotNull String template, int maxLines) {
        return truncated ? String.format(template, maxLines, omittedLines) + "\n" : "";
    }
}
