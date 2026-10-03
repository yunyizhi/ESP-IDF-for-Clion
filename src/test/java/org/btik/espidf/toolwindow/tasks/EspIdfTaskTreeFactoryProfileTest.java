package org.btik.espidf.toolwindow.tasks;

import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskCommandNode;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskTreeNode;
import org.btik.espidf.toolwindow.tasks.model.LocalExecNode;
import org.junit.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * 覆盖变量组 profile 的解析：匿名全局导入、命名按引用生效、多命名顺序覆盖、内联最高优先，
 * 以及 value / args / path 的 ${env:key} / ${v:key} 替换与未命中替换为空串。
 *
 * @author lustre
 */
public class EspIdfTaskTreeFactoryProfileTest {

    private static final String XML = """
            <?xml version="1.0" encoding="UTF-8" ?>
            <esp-tasks>
                <profile>
                    <envs>
                        GLOBAL=g
                        SHARED=global
                        PORT=/dev/ttyUSB0
                        TOOL_PATH=/usr/bin/python
                    </envs>
                    <macros>
                        TOOL=esptool
                        SHARED_M=global
                    </macros>
                </profile>

                <profile name="a">
                    <envs>
                        FROM_A=1
                        SHARED=a
                    </envs>
                    <macros>
                        TARGET=debug
                        SHARED_M=a
                    </macros>
                </profile>

                <profile name="b">
                    <envs>
                        FROM_B=2
                        SHARED=b
                    </envs>
                    <macros>
                        SHARED_M=b
                    </macros>
                </profile>

                <command name="plain" value="--port ${env:PORT} erase" env="INLINE=i"/>
                <exec name="tool" args="python -m ${v:TOOL} --target ${v:TARGET}" profile="a"/>
                <exec name="multi" args="x${env:SHARED}y${v:SHARED_M}z" profile="a b"/>
                <command name="inline-wins" value="run" env="SHARED=inline"/>
                <exec name="path-sub" path="${env:TOOL_PATH}" args="a" profile="a"/>
                <exec name="missing" args="A=[${v:NOPE}]B=[${env:NOPE}]"/>
            </esp-tasks>
            """;

    private static Map<String, EspIdfTaskTreeNode> loadNodes() throws Exception {
        File file = File.createTempFile("esp_custom_tasks", ".xml");
        file.deleteOnExit();
        Files.writeString(file.toPath(), XML, StandardCharsets.UTF_8);
        Map<String, EspIdfTaskTreeNode> nodes = new HashMap<>();
        for (DefaultMutableTreeNode node : EspIdfTaskTreeFactory.loadCustomTask(file)) {
            EspIdfTaskTreeNode taskNode = (EspIdfTaskTreeNode) node.getUserObject();
            nodes.put(taskNode.getDisplayName(), taskNode);
        }
        return nodes;
    }

    @Test
    public void anonymousProfileAppliedGlobally() throws Exception {
        EspIdfTaskTreeNode plain = loadNodes().get("plain");
        assertEquals("g", plain.getEnvVars().get("GLOBAL"));
        assertEquals("/dev/ttyUSB0", plain.getEnvVars().get("PORT"));
        // 未引用命名 profile，不应出现其变量
        assertFalse(plain.getEnvVars().containsKey("FROM_A"));
        assertFalse(plain.getEnvVars().containsKey("FROM_B"));
        // value 中的 ${env:...} 已展开
        assertEquals("--port /dev/ttyUSB0 erase", ((EspIdfTaskCommandNode) plain).getCommand());
    }

    @Test
    public void namedProfileAppliedOnlyWhenReferenced() throws Exception {
        Map<String, EspIdfTaskTreeNode> nodes = loadNodes();
        EspIdfTaskTreeNode tool = nodes.get("tool");
        assertEquals("1", tool.getEnvVars().get("FROM_A"));
        assertEquals("a", tool.getEnvVars().get("SHARED")); // 命名覆盖匿名
        assertFalse(tool.getEnvVars().containsKey("FROM_B"));
        // args 中的 ${v:...} 已展开
        assertEquals("python -m esptool --target debug", ((LocalExecNode) tool).getArgs());
    }

    @Test
    public void multipleNamedProfilesLastWins() throws Exception {
        LocalExecNode multi = (LocalExecNode) loadNodes().get("multi");
        assertEquals("b", multi.getEnvVars().get("SHARED"));
        assertEquals("1", multi.getEnvVars().get("FROM_A"));
        assertEquals("2", multi.getEnvVars().get("FROM_B"));
        // 宏同名同样后者覆盖：global -> a -> b
        assertEquals("xbybz", multi.getArgs());
    }

    @Test
    public void inlineEnvHasHighestPriority() throws Exception {
        EspIdfTaskTreeNode inline = loadNodes().get("inline-wins");
        assertEquals("inline", inline.getEnvVars().get("SHARED"));
    }

    @Test
    public void pathAndUnresolvedAreSubstituted() throws Exception {
        Map<String, EspIdfTaskTreeNode> nodes = loadNodes();
        LocalExecNode pathSub = (LocalExecNode) nodes.get("path-sub");
        assertEquals("/usr/bin/python", pathSub.getPath());
        assertEquals("a", pathSub.getArgs());

        LocalExecNode missing = (LocalExecNode) nodes.get("missing");
        // 解析期：未声明的宏直接置空；未声明的 ${env:} 保留占位符，待执行期用真实运行环境展开
        assertEquals("A=[]B=[${env:NOPE}]", missing.getArgs());
    }
}
