package org.btik.espidf.toolwindow.kconfig;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.util.EnvironmentVarUtil;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author lustre
 * @since 2025/6/17 23:38
 */
public class KConfServer {
    private static final Logger LOG = Logger.getInstance(KConfServer.class);
    private final Project project;

    private OutputStream processStdIn;
    private InputStream processStdOut;
    private Process process;
    private final Consumer<String> onMsg;


    public KConfServer(Project project, Consumer<String> onMsg) {
        this.project = project;
        this.onMsg = onMsg;
    }

    private void stopLast() {
        if (process == null) {
            return;
        }
        try {
            processStdOut.close();
            for (int i = 0; i < 10 && process.isAlive(); i++) {
                Thread.sleep(200);
            }
            if (process.isAlive()) {
                process.destroy();
            }
        } catch (IOException | InterruptedException e) {
            LOG.error(e);
        }
    }

    public void start() {
        stopLast();
        IdfProjectConfigService projectConfigService = project.getService(IdfProjectConfigService.class);
        String cmakeBuildDir = projectConfigService.getCmakeBuildDir();
        IdfEnvironmentService environmentService = project.getService(IdfEnvironmentService.class);
        Map<String, String> environments = environmentService.getEnvironments();
        List<String> args = new ArrayList<>();
        args.add(EnvironmentVarUtil.findIdfFullPath(environments));
        args.add("-B");
        args.add(cmakeBuildDir);
        args.add("confserver");
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.environment().putAll(environments);
        if (project.getBasePath() == null) {
            LOG.error("Project base path is null");
            return;
        }
        processBuilder.directory(Path.of(project.getBasePath()).toFile());
        processBuilder.redirectErrorStream(true);
        try {
            process = processBuilder.start();
            startStdOut(process);
        } catch (IOException e) {
            LOG.error("start KConfServer failed", e);
        }
    }

    public void startStdOut(Process process) {
        processStdIn = process.getOutputStream();
        processStdOut = process.getInputStream();
        ApplicationManager.getApplication().executeOnPooledThread(this::stdOutReadTask);
    }

    private void stdOutReadTask() {
        try (var reader = new BufferedReader(new InputStreamReader(processStdOut))) {
            String firstLine = reader.readLine();
            if (firstLine == null) {
                return;
            }
            LOG.info("exec confserver");
            while (!firstLine.contains("Server running")) {
                LOG.info(firstLine);
                firstLine = reader.readLine();
                if (firstLine == null) {
                    return;
                }
            }

            StringBuilder jsonBuffer = new StringBuilder();
            int braceCount = 0;
            boolean inString = false;
            boolean escape = false;

            while (process.isAlive()) {
                int ch = reader.read();
                if (ch == -1) break; // 流结束

                char c = (char) ch;
                jsonBuffer.append(c);

                if (!inString) {
                    if (c == '{') braceCount++;
                    else if (c == '}') braceCount--;
                    else if (c == '"') inString = true;
                } else {
                    if (escape) escape = false;
                    else if (c == '\\') escape = true;
                    else if (c == '"') inString = false;
                }

                // 5. 检测完整JSON对象
                if (braceCount == 0 && !jsonBuffer.isEmpty()) {
                    String jsonStr = jsonBuffer.toString();
                    jsonBuffer.setLength(0); // 重置缓冲区
                    onMsg.accept(jsonStr);
                }
            }
        } catch (IOException e) {
            LOG.error("Error reading process output", e);
        }
    }
}
