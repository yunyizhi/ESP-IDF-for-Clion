package org.btik.espidf.service;

import com.intellij.platform.ide.progress.ModalTaskOwner;
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains;
import org.btik.espidf.conf.IdfToolchainCacheEntry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 应用（插件）级工具链环境缓存服务：将工具链激活脚本读取到的环境变量与展示信息
 * 持久化到磁盘，跨 IDE 会话复用，避免工具链较多时每次都执行激活脚本、读取版本号的耗时操作。
 *
 * @author lustre
 */
public interface IdfToolchainCacheService {

    /**
     * 按激活脚本路径与当前 stamp 获取缓存条目。
     *
     * @param envFile 激活脚本路径（toolchain.getEnvironment()）
     * @param stamp   激活脚本文件当前的 stamp（mtime+size）；与缓存不一致视为失效返回 null
     * @return 命中且未失效的缓存条目，否则 null
     */
    @Nullable
    IdfToolchainCacheEntry get(@NotNull String envFile, @Nullable String stamp);

    /** 写入/更新缓存条目并异步持久化。 */
    void put(@NotNull IdfToolchainCacheEntry entry);

    /**
     * 读取工具链激活脚本并构建缓存条目（含版本号），写入持久化缓存。
     * env 中无 IDF_PATH 时标记为非 idf 工具链（idf=false），避免反复试探。
     *
     * @param toolchain 待构建的工具链（须含环境变量文件）
     * @param owner     模态进度宿主（组件或项目），用于展示读取进度
     * @return 构建好的缓存条目；脚本不可读或无 environment 时返回 null
     */
    @Nullable
    IdfToolchainCacheEntry buildAndCache(@NotNull CPPToolchains.Toolchain toolchain, @NotNull ModalTaskOwner owner);

    /**
     * 丢弃所有工具链环境变量缓存并逐一重新构建。仅重建插件级缓存，不修改任何已打开项目的环境变量。
     *
     * @param owner 模态进度宿主（组件或项目），用于展示读取进度
     */
    void rebuildAll(@NotNull ModalTaskOwner owner);
}
