package org.btik.espidf.toolwindow.tasks.mcp;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.btik.espidf.toolwindow.tasks.EspIdfTaskTreeFactory;
import org.btik.espidf.toolwindow.tasks.model.EspIdfTaskTreeNode;
import org.btik.espidf.util.DomUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.btik.espidf.toolwindow.tasks.TreeXmlMeta.ESP_CUSTOM_TASKS_XML;

/**
 * 项目级服务：解析并缓存该项目自己的 {@code esp_custom_tasks.xml} 自定义任务，
 * 与默认任务树（内置任务）隔离，避免跨项目互相污染。
 * 自定义任务的标识统一加 {@link #PREFIX} 前缀，避免与内置任务 id 重复。
 */

@Service(Service.Level.PROJECT)
public final class EspIdfCustomTasksService {

    /** 自定义任务标识前缀，用于与内置任务区分。 */
    public static final String PREFIX = "custom:";

    private final Project project;

    private final Map<String, EspIdfTaskTreeNode> customRegistry = new HashMap<>();
    private final List<EspIdfTasksMcpRegistry.Entry> customEntries = new ArrayList<>();

    public EspIdfCustomTasksService(@NotNull Project project) {
        this.project = project;
    }

    public static @NotNull EspIdfCustomTasksService getInstance(@NotNull Project project) {
        return project.getService(EspIdfCustomTasksService.class);
    }

    /** 解析项目级 esp_custom_tasks.xml，结果按项目隔离缓存（每次调用重新解析以反映最新文件）。 */
    public synchronized void load() {
        customRegistry.clear();
        customEntries.clear();
        String basePath = project.getBasePath();
        if (basePath == null) {
            return;
        }
        String filePath = Path.of(basePath).resolve(ESP_CUSTOM_TASKS_XML).toString();
        // 优先读取编辑器中的实时文档（含尚未保存的内容），使 agent 通过 MCP 调用时也能立即找到
        // 用户刚输入的自定义任务；若文件未在编辑器打开或实时内容暂不完整（用户正在输入），回退磁盘已保存文件。
        // 文档访问需在 read-action 内进行（MCP 工具运行于后台线程，无隐式读权限）。
        String liveText = ReadAction.computeBlocking(() -> {
            VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(filePath);
            if (vf == null) {
                return null;
            }
            com.intellij.openapi.editor.Document liveDoc = FileDocumentManager.getInstance().getDocument(vf);
            return liveDoc == null ? null : liveDoc.getText();
        });
        if (liveText != null) {
            try {
                Document liveDom = DomUtil.parseXml(liveText);
                for (DefaultMutableTreeNode root : EspIdfTaskTreeFactory.loadCustomTaskFromElement(liveDom.getDocumentElement())) {
                    EspIdfTasksMcpRegistry.collect(root, PREFIX, new StringBuilder(), customRegistry, customEntries);
                }
                return;
            } catch (Exception ignored) {
                // 实时文档暂不完整，回退到磁盘已保存版本
            }
        }
        File taskXml = new File(filePath);
        if (!taskXml.exists()) {
            return;
        }
        for (DefaultMutableTreeNode root : EspIdfTaskTreeFactory.loadCustomTask(taskXml)) {
            EspIdfTasksMcpRegistry.collect(root, PREFIX, new StringBuilder(), customRegistry, customEntries);
        }
    }

    /** 按标识查找自定义任务节点；接受带/不带 {@link #PREFIX} 前缀的写法。 */
    public synchronized @Nullable EspIdfTaskTreeNode lookup(@NotNull String key) {
        load();
        String lower = key.toLowerCase(Locale.ROOT);
        EspIdfTaskTreeNode node = customRegistry.get(lower);
        if (node != null) {
            return node;
        }
        return customRegistry.get(PREFIX + lower);
    }

    /** 返回该项目的自定义任务条目（标识已带 {@link #PREFIX} 前缀）。 */
    public synchronized @NotNull List<EspIdfTasksMcpRegistry.Entry> getEntries() {
        load();
        return customEntries;
    }
}
