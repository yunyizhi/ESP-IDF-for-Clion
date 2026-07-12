package org.btik.espidf.conf;

import java.util.Map;

/**
 * 单个工具链的环境读取结果缓存条目（插件级、跨会话持久化）。
 * <p>
 * 以激活脚本路径 {@link #envFile} 为键，{@link #stamp}（脚本文件 mtime+size）用于失效判定：
 * stamp 未变即认为环境未变，可直接复用缓存，避免重复执行激活脚本与读取版本号。
 *
 * @author lustre
 */
public class IdfToolchainCacheEntry {
    /** 缓存键：激活脚本路径（toolchain.getEnvironment()）。 */
    private String envFile;

    /** 失效标记：激活脚本文件的 lastModified + size，形如 "mtime_size"。 */
    private String stamp;

    /** 该工具链是否为有效的 ESP-IDF 工具链（false 表示 env 中无 IDF_PATH，用于避免反复试探）。 */
    private boolean idf;

    private String name;
    private String idfVersion;
    private String idfPath;
    private String idfToolsPath;
    private String adfPath;
    private boolean hasSubVersion;

    /** 完整环境变量表，供创建项目时直接使用，避免再次执行激活脚本。 */
    private Map<String, String> env;

    public IdfToolchainCacheEntry() {
    }

    public IdfToolchainCacheEntry(String name, String idfVersion, String idfPath, String idfToolsPath) {
        this.name = name;
        this.idfVersion = idfVersion;
        this.idfPath = idfPath;
        this.idfToolsPath = idfToolsPath;
    }

    public String getEnvFile() {
        return envFile;
    }

    public void setEnvFile(String envFile) {
        this.envFile = envFile;
    }

    public String getStamp() {
        return stamp;
    }

    public void setStamp(String stamp) {
        this.stamp = stamp;
    }

    public boolean isIdf() {
        return idf;
    }

    public void setIdf(boolean idf) {
        this.idf = idf;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isHasSubVersion() {
        return hasSubVersion;
    }

    public String getIdfVersion() {
        return idfVersion;
    }

    public void setIdfVersion(String idfVersion, boolean hasSubVersion) {
        this.idfVersion = idfVersion;
        this.hasSubVersion = hasSubVersion;
    }

    public void setIdfVersion(String idfVersion) {
        this.idfVersion = idfVersion;
    }

    public String getIdfPath() {
        return idfPath;
    }

    public void setIdfPath(String idfPath) {
        this.idfPath = idfPath;
    }

    public String getIdfToolsPath() {
        return idfToolsPath;
    }

    public void setIdfToolsPath(String idfToolsPath) {
        this.idfToolsPath = idfToolsPath;
    }

    public String getAdfPath() {
        return adfPath;
    }

    public void setAdfPath(String adfPath) {
        this.adfPath = adfPath;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }
}
