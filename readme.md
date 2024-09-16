# ESP-IDF for CLion
为CLion添加完善的<a href="https://docs.espressif.com/projects/esp-idf/">ESP-IDF(Espressif IoT Development Framework)</a>支持。

### 插件市场

[ESP-IDF](https://plugins.jetbrains.com/plugin/23886-esp-idf/)

## 文档

[ESP-IDF-for-Clion](https://yunyizhi.github.io/ESP-IDF-for-Clion/home.html)

### 功能

### 自动配置

安装好IDF之后，无需配置环境变量，ToolChain, Cmake Profile.

在CLion上直接创建ESP-IDF类型项目。即可自动生成。省去了复杂的配置过程。



![newProject.png](https://yunyizhi.github.io/ESP-IDF-for-Clion/images/newProject.png)

#### 支持调试

集成openocd esp32-gdb 调试,可使用clion图形化断点调试，查看线程和变量，查看内存视图和外设寄存器。

![debug.png](https://yunyizhi.github.io/ESP-IDF-for-Clion/images/debug.png)

#### 支持快捷工具，将常用IDF命令封装成命令树，并提供内部IDF控制台。

![task_tree.png](https://yunyizhi.github.io/ESP-IDF-for-Clion/images/task_tree.png)

