# ESP-IDF for CLion

这是一个非官方的ESP-IDF CLion插件。

当前文档对应插件版本:0.4。

在clion官方文档中[clion可无插件配置ESP-IDF](https://www.jetbrains.com/help/clion/esp-idf.html)。

> 依赖Toolchain添加export环境变量脚本的的能力。在执行cmake时，从Toolchain环境变量脚本里面读出的环境变量可以被esp-idf项目使用
> 从而正常加载cmake。同时可以完全基于cmake，运行对应的cmake target来开发esp32。

> 本插件也是基于clion的Toolchain环境变量脚本配置来实现自动配置esp-idf项目。

## 插件安装

* 可在CLion插件市场搜索:`ESP-IDF`

  ![market_place.png](market_place.png)

* 可访问[ESP-IDF](https://plugins.jetbrains.com/plugin/23886-esp-idf/)查看全部版本

* 也可以在本项目[release](https://github.com/yunyizhi/ESP-IDF-for-Clion/releases)仓库下载最新版本后，使用离线安装：

![install_from_disk.png](install_from_disk.png)

release仓会出现预览版，插件市场一般需要两个工作日审核，正式版也会比插件市场更早。

## 项目创建向导

本插件在clion新建项目选项中添加ESP-IDF选项。
可以自动配置clion 的toolchain ,cmake profile,不用做额外配置。

![newProject.png](newProject.png)

允许多个esp-idf共存(但手动切换多个idf toolchain情况，可能导致环境变量冲突，可以关闭clion重新打开。)

> 多个ESP-IDF共存的前提是多个ESP-IDF本身不会冲突，请保证每个ESP-IDF的命令行可用，才被能选择用于新建项目。
> （当前发现使用windows下IDF5.3离线版本会与同期的其他版本冲突。）

### 调试
0.4 新增调试功能 可以断点调试 查看线程、变量，集成gdb控制台，内存视图，和外设寄存器查看。

![debug.png](debug.png)
## 快捷命令树

本插件封装了一些常用ESP-IDF命令。

![task_tree.png](task_tree.png)

## 项目设置

支持单项目配置

![settings.png](settings.png)