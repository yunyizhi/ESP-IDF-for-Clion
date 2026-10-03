package org.btik.espidf.util;


import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EnvironmentVarUtilTest {

    Map<String, String> env;

    @Before
    public void setUp() {
        String path;
        if (OsUtil.IS_WINDOWS) {
            path = Objects.requireNonNull(getClass().getResource("/idf_fake_path/windows")).getPath();
        } else {
            path = Objects.requireNonNull(getClass().getResource("/idf_fake_path")).getPath();
        }
        path = path.replace("%20", " ");
        // PathEnvironmentVariableUtil 在非windows下需要判断可执行
        if (!OsUtil.IS_WINDOWS) {
            Path.of(path).resolve(OsUtil.Const.IDF_EXE).toFile().setExecutable(true);
        }
        File file = new File(path);
        env = new HashMap<>();
        env.put("PATH", file.getAbsolutePath());
    }

    @Test
    public void getIdf() {
        String idfFullPath = EnvironmentVarUtil.findIdfFullPath(env);
        System.out.println(idfFullPath);
        assert idfFullPath != null && !idfFullPath.isEmpty();
    }

    /**
     * 复现子标签写法：pretty-print 后会带缩进且末尾有一行纯空格，
     * 旧实现会因该空行 indexOf('=')<0 抛异常导致整份 env 丢失。
     */
    @Test
    public void parseEnv_childBlockWithIndentAndTrailingBlankLine() {
        Map<String, String> env = EnvironmentVarUtil.parseEnv("\n            A=B\n        ");
        assertEquals(1, env.size());
        assertEquals("B", env.get("A"));
    }

    @Test
    public void parseEnv_multipleVarsOnePerLine() {
        Map<String, String> env = EnvironmentVarUtil.parseEnv("\n        FOO=1\n        BAR=hello world\n    ");
        assertEquals(2, env.size());
        assertEquals("1", env.get("FOO"));
        assertEquals("hello world", env.get("BAR"));
    }

    /** 行内属性多行写法（属性值中的 &#10; 换行） */
    @Test
    public void parseEnv_inlineAttributeMultiLine() {
        Map<String, String> env = EnvironmentVarUtil.parseEnv("A=1\nB=2");
        assertEquals("1", env.get("A"));
        assertEquals("2", env.get("B"));
    }

    @Test
    public void parseEnv_crlf() {
        Map<String, String> env = EnvironmentVarUtil.parseEnv("\r\n  X=9\r\n  Y=8\r\n");
        assertEquals("9", env.get("X"));
        assertEquals("8", env.get("Y"));
    }

    @Test
    public void parseEnv_blankLinesIgnored() {
        assertTrue(EnvironmentVarUtil.parseEnv("   \n\n\t\n").isEmpty());
    }

    @Test
    public void parseEnv_emptyText() {
        assertTrue(EnvironmentVarUtil.parseEnv("").isEmpty());
    }

    /** value 允许包含空格与 '='，以第一个 '=' 分割 */
    @Test
    public void parseEnv_valueMayContainSpacesAndEquals() {
        Map<String, String> env = EnvironmentVarUtil.parseEnv("PATH=/a b:/c=d");
        assertEquals("/a b:/c=d", env.get("PATH"));
    }

    @Test
    public void parseEnv_keyTrimmed() {
        Map<String, String> env = EnvironmentVarUtil.parseEnv("   A  =B   ");
        assertEquals("B", env.get("A"));
    }

    @Test(expected = RuntimeException.class)
    public void parseEnv_missingEqualsThrows() {
        EnvironmentVarUtil.parseEnv("A=B\nJUST_KEY");
    }

    @Test(expected = RuntimeException.class)
    public void parseEnv_emptyKeyThrows() {
        EnvironmentVarUtil.parseEnv("=B");
    }

    /** 固定行为：export 前缀不是特例，会被解析为名为 "export A" 的变量，避免用户误写 */
    @Test
    public void parseEnv_exportPrefixIsNotSpecial() {
        Map<String, String> env = EnvironmentVarUtil.parseEnv("export A=B");
        assertEquals("B", env.get("export A"));
    }

    @Test
    public void substitute_envAndMacro() {
        Map<String, String> env = Map.of("PORT", "/dev/ttyUSB0");
        Map<String, String> macros = Map.of("TOOL", "esptool");
        assertEquals("--port /dev/ttyUSB0 -m esptool",
                EnvironmentVarUtil.substitute("--port ${env:PORT} -m ${v:TOOL}", env, macros));
    }

    @Test
    public void substitute_unresolvedBecomesEmpty() {
        assertEquals("A=[]B=[]",
                EnvironmentVarUtil.substitute("A=[${env:MISSING}]B=[${v:MISSING}]", Map.of(), Map.of()));
    }

    @Test
    public void substitute_emptyKeyBecomesEmpty() {
        assertEquals("", EnvironmentVarUtil.substitute("${env:}", Map.of("A", "1"), Map.of()));
    }

    @Test
    public void substitute_nullReturnsNull() {
        assertNull(EnvironmentVarUtil.substitute(null, Map.of(), Map.of()));
    }

    @Test
    public void substitute_nullMapsTreatedAsEmpty() {
        assertEquals("[]", EnvironmentVarUtil.substitute("[${env:A}]", null, null));
    }

    /** 其它前缀或不闭合的占位符保持原样 */
    @Test
    public void substitute_otherPrefixOrUnclosedUnchanged() {
        assertEquals("plain ${x:Y} ${env:Z",
                EnvironmentVarUtil.substitute("plain ${x:Y} ${env:Z", Map.of(), Map.of()));
    }

    /** 替换值里的 $ 和 \ 必须按字面输出，不能被当成正则分组引用 */
    @Test
    public void substitute_replacementWithDollarAndBackslashIsLiteral() {
        Map<String, String> env = Map.of("VAL", "$1\\d");
        assertEquals("v=$1\\d end", EnvironmentVarUtil.substitute("v=${env:VAL} end", env, Map.of()));
    }

    /** 解析期：已知环境变量展开，未知 ${env:} 保留占位符，未命中宏置空 */
    @Test
    public void substitutePartial_keepsUnknownEnvButEmptiesUnknownMacro() {
        Map<String, String> env = Map.of("A", "1");
        assertEquals("1-${env:B}-[]",
                EnvironmentVarUtil.substitutePartial("${env:A}-${env:B}-[${v:M}]", env, Map.of()));
    }

    @Test
    public void substitutePartial_nullReturnsNull() {
        assertNull(EnvironmentVarUtil.substitutePartial(null, Map.of(), Map.of()));
    }

    /** 拓展环境变量的值支持追加写法：PATH=/x:${env:PATH} 用基础环境展开 */
    @Test
    public void resolveEnvValues_appendsUsingBaseEnv() {
        Map<String, String> resolved = EnvironmentVarUtil.resolveEnvValues(
                Map.of("PATH", "/opt/qemu/bin:${env:PATH}", "A", "1"),
                Map.of("PATH", "/usr/bin", "A", "old"));
        assertEquals("/opt/qemu/bin:/usr/bin", resolved.get("PATH"));
        assertEquals("1", resolved.get("A"));
    }

    @Test
    public void resolveEnvValues_unresolvedBaseBecomesEmpty() {
        assertEquals("a:", EnvironmentVarUtil.resolveEnvValues(Map.of("X", "a:${env:MISSING}"), Map.of()).get("X"));
    }

    /** env 值里不处理宏：${v:...} 原样保留，不做解析 */
    @Test
    public void resolveEnvValues_doesNotProcessMacros() {
        Map<String, String> resolved = EnvironmentVarUtil.resolveEnvValues(
                Map.of("X", "${v:MACRO}:${env:PATH}"), Map.of("PATH", "/usr/bin"));
        assertEquals("${v:MACRO}:/usr/bin", resolved.get("X"));
    }
}