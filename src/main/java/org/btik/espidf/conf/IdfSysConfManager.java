package org.btik.espidf.conf;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.jetbrains.cidr.cpp.cmake.CMakeSettings;
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace;
import com.jetbrains.cidr.cpp.toolchains.CPPToolSet;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import org.btik.espidf.run.config.model.DebugConfigModel;
import org.btik.espidf.run.config.model.Serial;
import org.btik.espidf.service.IdfSysConfService;
import com.intellij.openapi.diagnostic.Logger;
import org.btik.espidf.util.ClassMetaUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

import static org.btik.espidf.util.ClassMetaUtils.isMod;
import static org.btik.espidf.util.OsUtil.IS_WINDOWS;
import static org.btik.espidf.util.I18nMessage.*;

/**
 * @author lustre
 * @since 2024/2/14 23:17
 */
public class IdfSysConfManager implements IdfSysConfService {
    private static final Logger LOG = Logger.getInstance(IdfSysConfManager.class);
    private static final String IDF_FOLDER_NAME = "org.btik.espidf";

    private static final String IDF_JSON_NAME = "espidf.json";

    private static final String GDB_MAP_CONF = "/org-btik-esp-idf/conf/esp32_gdb.json";

    private final HashSet<IdfToolConf> idfToolConfs = new HashSet<>();

    private final HashMap<String, IdfToolConf> idfToolConfMap = new HashMap<>();

    private final HashMap<String, String> gdbMap = new HashMap<>();

    private final Type toolConfSetType = new TypeToken<HashSet<IdfToolConf>>() {
    }.getType();

    private final Type gdbMapType = new TypeToken<HashMap<String, String>>() {
    }.getType();


    private List<ClassMetaUtils.PropOptMeta> propOptMetas;

    public IdfSysConfManager() {

        Path configDir = PathManager.getConfigDir();
        Path idfFolder = configDir.resolve(IDF_FOLDER_NAME);
        if (!Files.exists(idfFolder)) {
            return;
        }
        Path idfJson = idfFolder.resolve(IDF_JSON_NAME);
        if (!Files.exists(idfJson)) {
            return;
        }
        parseToolConf(idfJson);

        parseGdbConf();

        parseDebugModelSerialMeta();

    }

    private void parseDebugModelSerialMeta() {
        propOptMetas = ClassMetaUtils.parseFieldsByAnnotation(DebugConfigModel.class, Serial.class);
        for (ClassMetaUtils.PropOptMeta propOptMeta : propOptMetas) {
            Method getter = propOptMeta.getter();
            Method setter = propOptMeta.setter();
            if (getter == null || !isMod(getter, Modifier.PUBLIC)
                    || setter == null || !isMod(setter, Modifier.PUBLIC)) {
                propOptMeta.field().setAccessible(true);
            }
        }
    }

    private void parseGdbConf() {
        InputStream gdpMapConfIn = IdfSysConfManager.class.getResourceAsStream(GDB_MAP_CONF);
        if (gdpMapConfIn == null) {
            return;
        }
        Gson gson = new Gson();
        JsonElement jsonElement = gson.fromJson(new JsonReader(new InputStreamReader(gdpMapConfIn)), JsonElement.class);
        JsonObject jsonObject = jsonElement.getAsJsonObject();
        JsonElement gdbMapElement = jsonObject.get(IS_WINDOWS ? WINDOWS : UNIX_LIKE);
        if (gdbMapElement != null) {
            HashMap<String, String> result = gson.fromJson(gdbMapElement, gdbMapType);
            gdbMap.putAll(result);
        }
    }

    private void parseToolConf(Path idfJson) {
        try {
            String json = Files.readString(idfJson);
            HashSet<IdfToolConf> idfToolConfSet = new Gson().fromJson(json, toolConfSetType);
            if (idfToolConfSet == null || idfToolConfSet.isEmpty()) {
                return;
            }
            for (IdfToolConf toolConf : idfToolConfSet) {
                List<CPPToolchains.Toolchain> toolchains = CPPToolchains.getInstance().getToolchains();
                String envFileName = toolConf.getEnvFileName();
                Predicate<CPPToolSet.Kind> kindPredicate = IS_WINDOWS ?
                        (kind -> kind == CPPToolSet.Kind.SYSTEM_WINDOWS_TOOLSET) :
                        (kind -> kind == CPPToolSet.Kind.SYSTEM_UNIX_TOOLSET);
                for (CPPToolchains.Toolchain toolchain : toolchains) {
                    if (!kindPredicate.test(toolchain.getToolSetKind())) {
                        continue;
                    }
                    String environment = toolchain.getEnvironment();
                    if (Objects.equals(environment, envFileName)) {
                        toolConf.setToolchain(toolchain);
                        break;
                    }
                }
                idfToolConfMap.put(toolConf.getKey(), toolConf);
                this.idfToolConfs.addAll(idfToolConfSet);
            }

        } catch (JsonSyntaxException jsonSyntaxException) {
            LOG.error(jsonSyntaxException);
        } catch (IOException e) {
            NOTIFICATION_GROUP.createNotification(getMsg("idf.cmd.init.failed"),
                    getMsgF("idf.cmd.init.failed.with", e.getMessage()), NotificationType.ERROR).notify(null);
        }
    }

    @Override
    public IdfToolConf getLastActivedIdfToolConf() {
        if (idfToolConfs.isEmpty()) {
            return null;
        }
        return idfToolConfs.stream().max(Comparator.comparing(IdfToolConf::getActiveTime)).get();
    }

    @Override
    public void store(IdfToolConf newIdfToolConf) {
        idfToolConfs.add(newIdfToolConf);
        idfToolConfMap.put(newIdfToolConf.getKey(), newIdfToolConf);
        newIdfToolConf.setActiveTime(System.currentTimeMillis());
        saveConfig();
    }

    @Override
    public Path getIdfConfFolder() {
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

    @Override
    public IdfToolConf getToolConfByKey(String key) {
        IdfToolConf idfToolConf = idfToolConfMap.get(key);
        if (idfToolConf != null) {
            idfToolConf.setActiveTime(System.currentTimeMillis());
            saveConfig();
        }
        return idfToolConf;
    }

    @Override
    public IdfToolConf getIdfConfByProject(Project project) {
        CMakeWorkspace instance = CMakeWorkspace.getInstance(project);
        List<CMakeSettings.Profile> activeProfiles = instance.getSettings().getActiveProfiles();
        if (activeProfiles.isEmpty()) {
            return null;
        }
        CMakeSettings.Profile currentProfile = activeProfiles.get(0);
        CPPToolchains.Toolchain toolchain = CPPToolchains.getInstance()
                .getToolchainByNameOrDefault(currentProfile.getToolchainName());
        if (toolchain == null) {
            return null;
        }
        IdfToolConf[] idfToolConfRef = {null};
        idfToolConfMap.forEach((key, value) -> {
            if (Objects.equals(value.getEnvFileName(), toolchain.getEnvironment())) {
                idfToolConfRef[0] = value;
            }
        });
        return idfToolConfRef[0];
    }

    @Override
    public String getGdbExecutable(String target) {
        return gdbMap.get(target);
    }

    @Override
    public List<ClassMetaUtils.PropOptMeta> getPropOptMetas() {
        return propOptMetas;
    }

    private void saveConfig() {
        ApplicationManager.getApplication()
                .executeOnPooledThread(() -> {
                    Path idfConfFolder = getIdfConfFolder();
                    Path idfJson = idfConfFolder.resolve(IDF_JSON_NAME);
                    try {
                        Files.writeString(idfJson, new Gson().toJson(idfToolConfs));
                    } catch (IOException e) {
                        LOG.error(e);
                    }
                });
    }
}

