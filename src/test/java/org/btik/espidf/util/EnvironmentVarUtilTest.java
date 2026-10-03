package org.btik.espidf.util;


import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
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
}