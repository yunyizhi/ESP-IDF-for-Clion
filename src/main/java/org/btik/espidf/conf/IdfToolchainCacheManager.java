package org.btik.espidf.conf;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.platform.ide.progress.ModalTaskOwner;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import org.apache.commons.lang3.StringUtils;
import org.btik.espidf.service.IdfEnvironmentService;
import org.btik.espidf.service.IdfToolchainCacheService;
import org.btik.espidf.util.EnvironmentVarUtil;
import org.btik.espidf.util.ToolChainTool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.List;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static org.btik.espidf.service.IdfEnvironmentService.ADF_PATH;
import static org.btik.espidf.service.IdfEnvironmentService.IDF_PATH;
import static org.btik.espidf.service.IdfEnvironmentService.IDF_TOOLS_PATH;
import static org.btik.espidf.util.I18nMessage.NOTIFICATION_GROUP;
import static org.btik.espidf.util.I18nMessage.$i18n;

/**
 * {@link IdfToolchainCacheService} 的实现：以 JSON 文件持久化工具链环境缓存，
 * 存放于 {@code <config>/org.btik.espidf/espidf_toolchain_cache.json}。
 *
 * @author lustre
 */
public class IdfToolchainCacheManager implements IdfToolchainCacheService {
    private static final Logger LOG = Logger.getInstance(IdfToolchainCacheManager.class);
    private static final String IDF_FOLDER_NAME = "org.btik.espidf";
    private static final String CACHE_FILE = "espidf_toolchain_cache.json";

    private static final Type MAP_TYPE = new TypeToken<HashMap<String, IdfToolchainCacheEntry>>() {
    }.getType();

    /** envFile -> 缓存条目。 */
    private final Map<String, IdfToolchainCacheEntry> entries = new ConcurrentHashMap<>();

    public IdfToolchainCacheManager() {
        load();
    }

    private void load() {
        Path jsonPath = cacheFilePath();
        if (!Files.exists(jsonPath)) {
            return;
        }
        try {
            String json = Files.readString(jsonPath);
            Map<String, IdfToolchainCacheEntry> loaded = new Gson().fromJson(json, MAP_TYPE);
            if (loaded != null) {
                loaded.forEach((k, v) -> {
                    if (k != null && v != null) {
                        entries.put(k, v);
                    }
                });
            }
        } catch (JsonSyntaxException jsonSyntaxException) {
            LOG.error(jsonSyntaxException);
        } catch (IOException e) {
            LOG.warn(e);
        }
    }

    @Override
    public @Nullable IdfToolchainCacheEntry get(@NotNull String envFile, @Nullable String stamp) {
        IdfToolchainCacheEntry entry = entries.get(envFile);
        if (entry == null) {
            return null;
        }
        // stamp 不一致（激活脚本已变化）视为失效
        if (!Objects.equals(entry.getStamp(), stamp)) {
            return null;
        }
        return entry;
    }

    @Override
    public void put(@NotNull IdfToolchainCacheEntry entry) {
        if (entry.getEnvFile() == null) {
            return;
        }
        entries.put(entry.getEnvFile(), entry);
        save();
    }

    @Override
    @Nullable
    public IdfToolchainCacheEntry buildAndCache(@NotNull CPPToolchains.Toolchain toolchain, @NotNull ModalTaskOwner owner) {
        String envFile = toolchain.getEnvironment();
        if (StringUtils.isEmpty(envFile)) {
            return null;
        }
        // 无法校验新鲜度（脚本缺失/异常）时不构建，交由调用方决定是否重试
        String stamp = computeStamp(envFile);
        if (stamp == null) {
            return null;
        }
        Map<String, String> rawEnv = ToolChainTool.toolChainEnvByOwner(toolchain, owner);
        String idfPath = rawEnv.get(IDF_PATH);
        IdfToolchainCacheEntry entry;
        if (StringUtils.isEmpty(idfPath)) {
            // 无 IDF_PATH：非 ESP-IDF 工具链，记录标记避免反复试探
            entry = new IdfToolchainCacheEntry(toolchain.getName(), null, null, null);
            entry.setIdf(false);
        } else {
            String idfToolsPath = rawEnv.get(IDF_TOOLS_PATH);
            String version = EnvironmentVarUtil.getIdfVersion(rawEnv, owner, toolchain.getName());
            entry = new IdfToolchainCacheEntry(toolchain.getName(), version, idfPath, idfToolsPath);
            entry.setIdf(true);
            entry.setAdfPath(rawEnv.get(ADF_PATH));
        }
        entry.setEnvFile(envFile);
        entry.setStamp(stamp);
        entry.setEnv(rawEnv);
        put(entry);
        return entry;
    }

    @Override
    public void rebuildAll(@NotNull ModalTaskOwner owner) {
        // 丢弃全部工具链环境变量缓存（不影响任何已打开项目的环境变量）
        entries.clear();
        List<CPPToolchains.Toolchain> toolchains = ToolChainTool.getFilteredToolchains(
                (toolchain) -> StringUtils.isNotEmpty(toolchain.getEnvironment())
        );
        for (CPPToolchains.Toolchain toolchain : toolchains) {
            try {
                buildAndCache(toolchain, owner);
            } catch (Exception e) {
                LOG.warn("rebuild toolchain cache failed: " + toolchain.getName(), e);
            }
        }
    }

    private Path idfConfFolder() throws IOException {
        Path idfFolder = PathManager.getConfigDir().resolve(IDF_FOLDER_NAME);
        if (!Files.exists(idfFolder)) {
            Files.createDirectories(idfFolder);
        }
        return idfFolder;
    }

    private Path cacheFilePath() {
        return PathManager.getConfigDir().resolve(IDF_FOLDER_NAME).resolve(CACHE_FILE);
    }

    /**
     * 计算激活脚本文件的失效标记（lastModified + size）；文件不存在或异常返回 null。
     * 供各消费方统一复用，避免重复实现失效判定逻辑。
     */
    @Nullable
    public static String computeStamp(@Nullable String envFile) {
        if (StringUtils.isEmpty(envFile)) {
            return null;
        }
        try {
            Path path = Path.of(envFile);
            if (!Files.exists(path)) {
                return null;
            }
            return Files.getLastModifiedTime(path).toMillis() + "_" + Files.size(path);
        } catch (Exception e) {
            LOG.warn("compute toolchain env stamp failed: " + envFile, e);
            return null;
        }
    }

    private void save() {
        // 拷贝一份快照，避免异步写盘时并发修改
        Map<String, IdfToolchainCacheEntry> snapshot = new HashMap<>(entries);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Path jsonPath = idfConfFolder().resolve(CACHE_FILE);
                Files.writeString(jsonPath, new Gson().toJson(snapshot, MAP_TYPE));
            } catch (IOException e) {
                LOG.warn(e);
                NOTIFICATION_GROUP.createNotification($i18n("notification.group.idf"),
                        e.getMessage(), NotificationType.WARNING).notify(null);
            }
        });
    }
}
