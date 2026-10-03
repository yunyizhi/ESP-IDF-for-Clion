package org.btik.espidf.toolwindow.tasks;

import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskTreeNode;
import org.junit.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 覆盖自定义任务 XML 中拓展环境变量（env）的解析：
 * 行内属性、子标签，以及两者同名时子标签覆盖属性。
 *
 * @author lustre
 */
public class EspIdfTaskTreeFactoryEnvTest {

    private static final String XML = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <esp-tasks>
                <command name="cmd child" value="build">
                    <env>
                        CMD_CHILD=1
                    </env>
                </command>
                <command name="cmd inline" value="build" env="CMD_INLINE=2"/>
                <console-command name="console child" value="monitor">
                    <env>CONSOLE_CHILD=3</env>
                </console-command>
                <exec name="exec both" args="echo hi" env="A=attr&#10;SAME=attr">
                    <env>
                        B=child
                        SAME=child
                    </env>
                </exec>
            </esp-tasks>
            """;

    private static Map<String, EspIdfTaskTreeNode> loadNodes(String xml) throws Exception {
        File file = File.createTempFile("esp_custom_tasks", ".xml");
        file.deleteOnExit();
        Files.writeString(file.toPath(), xml, StandardCharsets.UTF_8);
        Map<String, EspIdfTaskTreeNode> nodes = new HashMap<>();
        for (DefaultMutableTreeNode node : EspIdfTaskTreeFactory.loadCustomTask(file)) {
            EspIdfTaskTreeNode taskNode = (EspIdfTaskTreeNode) node.getUserObject();
            nodes.put(taskNode.getDisplayName(), taskNode);
        }
        return nodes;
    }

    @Test
    public void parsesEnvFromAttributeAndChild() throws Exception {
        Map<String, EspIdfTaskTreeNode> nodes = loadNodes(XML);

        // command 子标签
        assertEquals(Map.of("CMD_CHILD", "1"), nodes.get("cmd child").getEnvVars());
        // command 行内属性
        assertEquals(Map.of("CMD_INLINE", "2"), nodes.get("cmd inline").getEnvVars());
        // console-command 子标签
        assertEquals(Map.of("CONSOLE_CHILD", "3"), nodes.get("console child").getEnvVars());

        // exec 同时存在属性与子标签：都生效，且同名时子标签覆盖属性
        Map<String, String> execEnv = nodes.get("exec both").getEnvVars();
        assertEquals(3, execEnv.size());
        assertEquals("attr", execEnv.get("A"));
        assertEquals("child", execEnv.get("B"));
        assertEquals("child", execEnv.get("SAME"));
    }

    @Test
    public void noEnvYieldsEmptyMap() throws Exception {
        Map<String, EspIdfTaskTreeNode> nodes = loadNodes("""
                <?xml version="1.0" encoding="UTF-8" ?>
                <esp-tasks>
                    <command name="plain" value="build"/>
                </esp-tasks>
                """);
        assertTrue(nodes.get("plain").getEnvVars().isEmpty());
    }

    /** 旧实现遇到子标签里带缩进的空行会抛异常并吞掉整份 env，这里回归验证不再发生 */
    @Test
    public void childBlockWithIndentDoesNotDropEnv() throws Exception {
        Map<String, EspIdfTaskTreeNode> nodes = loadNodes("""
                <?xml version="1.0" encoding="UTF-8" ?>
                <esp-tasks>
                    <exec name="indented">
                        <env>

                            ONLY=kept

                        </env>
                        <args>echo hi</args>
                    </exec>
                </esp-tasks>
                """);
        assertEquals(Map.of("ONLY", "kept"), nodes.get("indented").getEnvVars());
    }
}
