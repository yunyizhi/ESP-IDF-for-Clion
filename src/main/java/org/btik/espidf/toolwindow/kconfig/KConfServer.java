package org.btik.espidf.toolwindow.kconfig;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfProjectConfigService;
import org.btik.espidf.toolwindow.kconfig.model.KconfigMeta;
import org.btik.espidf.toolwindow.kconfig.model.KconfigStatus;
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
    private final Consumer<KconfigStatus> onMsg;


    public KConfServer(Project project, Consumer<KconfigStatus> onMsg) {
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
            String beforeContent = reader.readLine();
            if (beforeContent == null) {
                return;
            }
            LOG.info("exec confserver");
            while (!beforeContent.contains("Server running")) {
                System.out.println(beforeContent);
                beforeContent = reader.readLine();
                if (beforeContent == null) {
                    return;
                }
            }
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            JsonParser jsonParser = mapper.getFactory().createParser(reader);
            while (process.isAlive()) {
                try {
                    JsonToken token = jsonParser.nextToken();
                    if (token == null) break;

                    if (token == JsonToken.START_OBJECT) {
                        JsonNode node = jsonParser.readValueAsTree();
                        KconfigStatus status = mapper.treeToValue(node, KconfigStatus.class);
                        if (node.has(KconfigMeta.ERROR)) {
                            status.setError(true);
                        }
                        onMsg.accept(status);
                    }
                } catch (JsonParseException e) {
                    jsonParser.skipChildren();
                }
            }
            LOG.info("KConfServer exited");
        } catch (IOException e) {
            LOG.error("Error reading process output", e);
        }
    }
}
