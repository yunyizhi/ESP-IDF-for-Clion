package org.btik.espidf.conf;

import com.google.gson.Gson;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.Computable;
import com.jetbrains.cidr.cpp.toolchains.CPPToolSet;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import com.jetbrains.cidr.toolchains.OSType;
import org.btik.espidf.service.IdfToolConfService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;


import static org.btik.espidf.util.OsUtil.IS_WINDOWS;
import static org.btik.espidf.util.I18nMessage.*;

/**
 * @author lustre
 * @since 2024/2/14 23:17
 */
public class IdfToolConfManager implements IdfToolConfService {
    private static final String IDF_FOLDER_NAME = "org.btik.espidf";

    private static final String IDF_JSON_NAME = "espidf.json";

    IdfToolConf idfToolConf;

    public IdfToolConfManager() {
        Path configDir = PathManager.getConfigDir();
        Path idfFolder = configDir.resolve(IDF_FOLDER_NAME);
        if (!Files.exists(idfFolder)) {
            return;
        }
        Path idfJson = idfFolder.resolve(IDF_JSON_NAME);
        if (!Files.exists(idfJson)) {
            return;
        }
        parseConf(idfJson);

    }

    private void parseConf(Path idfJson) {
        try {
            String json = Files.readString(idfJson);
            IdfToolConf idfToolConfFormFile = new Gson().fromJson(json, IdfToolConf.class);

            List<CPPToolchains.Toolchain> toolchains = CPPToolchains.getInstance().getToolchains();
            String envFileName = idfToolConfFormFile.getEnvFileName();
            Predicate<CPPToolSet.Kind> kindPredicate = IS_WINDOWS ?
                    (kind -> kind == CPPToolSet.Kind.SYSTEM_WINDOWS_TOOLSET) :
                    (kind -> kind == CPPToolSet.Kind.SYSTEM_UNIX_TOOLSET);
            for (CPPToolchains.Toolchain toolchain : toolchains) {
                if (!kindPredicate.test(toolchain.getToolSetKind())) {
                    continue;
                }
                String environment = toolchain.getEnvironment();
                if (Objects.equals(environment, envFileName)) {
                    idfToolConfFormFile.setToolchain(toolchain);
                    break;
                }
            }
            if (idfToolConfFormFile.getToolchain() != null) {
                this.idfToolConf = idfToolConfFormFile;
            }
        } catch (IOException e) {
            NOTIFICATION_GROUP.createNotification(getMsg("idf.cmd.init.failed"),
                    getMsgF("idf.cmd.init.failed.with", e.getMessage()), NotificationType.ERROR).notify(null);
        }
    }

    @Override
    public IdfToolConf getIdfToolConf() {
        return idfToolConf;
    }

    @Override
    public IdfToolConf createWinToolConf(String idfToolPath, String idfId) {
        Path idfConfFolder = getIdfConfFolder();
        String exportEnvCmd = idfToolPath + File.separatorChar + "idf_cmd_init.bat " + idfId;
        Path idfExportBat = idfConfFolder.resolve("export.bat");
        try {
            Files.writeString(idfExportBat, exportEnvCmd);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String exportBatPath = idfExportBat.toString();
        IdfToolConf newIdfToolConf = new IdfToolConf();
        newIdfToolConf.setIdfId(idfId);
        newIdfToolConf.setEnvFileName(exportBatPath);
        newIdfToolConf.setIdfToolPath(idfToolPath);

        CPPToolchains.Toolchain toolchain = getToolChain(exportBatPath);
        newIdfToolConf.setToolchain(toolchain);
        Path idfJson = idfConfFolder.resolve(IDF_JSON_NAME);
        try {
            Files.writeString(idfJson, new Gson().toJson(newIdfToolConf));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.idfToolConf = newIdfToolConf;
        return this.idfToolConf;
    }

    @Override
    public IdfToolConf createUnixToolConf(String idfFrameworkPath) {
        IdfToolConf newIdfToolConf = new IdfToolConf();
        String envFileName = idfFrameworkPath + File.separatorChar + "export.sh";
        newIdfToolConf.setEnvFileName(envFileName);
        newIdfToolConf.setIdfToolPath(idfFrameworkPath);
        CPPToolchains.Toolchain toolchain = getToolChain(envFileName);
        newIdfToolConf.setToolchain(toolchain);
        Path idfConfFolder = getIdfConfFolder();
        Path idfJson = idfConfFolder.resolve(IDF_JSON_NAME);
        try {
            Files.writeString(idfJson, new Gson().toJson(newIdfToolConf));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.idfToolConf = newIdfToolConf;
        return newIdfToolConf;
    }

    private Path getIdfConfFolder(){
        Path configDir = PathManager.getConfigDir();
        Path idfFolder = configDir.resolve(IDF_FOLDER_NAME);
        if (!Files.exists(idfFolder)) {
            try {
                Files.createDirectories(idfFolder);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return idfFolder;
    }

    private CPPToolchains.Toolchain getToolChain(String envFileName) {
        CPPToolchains.Toolchain existsToolChain = ApplicationManager.getApplication().runReadAction((Computable<CPPToolchains.Toolchain>) () -> {
            List<CPPToolchains.Toolchain> toolchains = CPPToolchains.getInstance().getToolchains();
            for (CPPToolchains.Toolchain toolchain : toolchains) {
                if (Objects.equals(toolchain.getEnvironment(), envFileName)) {
                    return toolchain;
                }
            }
            return null;
        });
        if (existsToolChain != null) {
            return existsToolChain;
        }

        CPPToolchains.Toolchain idfToolChain = new CPPToolchains.Toolchain(OSType.getCurrent());
        idfToolChain.setToolSetKind(IS_WINDOWS ? CPPToolSet.Kind.SYSTEM_WINDOWS_TOOLSET : CPPToolSet.Kind.SYSTEM_UNIX_TOOLSET);
        idfToolChain.setName("Idf" + Integer.toHexString(envFileName.hashCode()));
        ApplicationManager.getApplication().invokeLater(() ->
                ApplicationManager.getApplication().runWriteAction(() -> {
                            CPPToolchains.getInstance().beginUpdate();
                            CPPToolchains.getInstance().addToolchain(idfToolChain);
                            idfToolChain.setEnvironment(envFileName);
                            CPPToolchains.getInstance().endUpdate();
                        }
                ));
        return idfToolChain;
    }

}

