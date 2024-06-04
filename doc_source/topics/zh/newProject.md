# 新建项目

## windows离线ESP-IDF

* ESP-IDF 工具安装器 安装

>可参考ESP-IDF教程[ESP-IDF 工具安装器](https://docs.espressif.com/projects/esp-idf/zh_CN/latest/esp32/get-started/windows-setup.html#esp-idf)

在[该地址](https://dl.espressif.com/dl/esp-idf/)下载对应版本的ESP-IDF

![idf_dl.png](idf_dl.png)

选择一个 `Offline Installer(离线安装器)`，无需使用加速器。

下载完成按向导安装，可以勾选附加的驱动。


* 新建项目
新建项目类型为ESP-IDF的项目，选择`Env Type` 为`ESP-IDF TOOL`
 ![win.png](win.png)
再选择上一步离线包安装的路径如:`D:\Espressif`。

选择正确会自己加载已安装的`ESP-IDF`框架。

然后创建项目即可。


## 源码安装
通过克隆 ESP-IDF项目，并在对应平台使用install脚本安装ESP-IDF。这样可以通过git更新IDF。

### windows下源码安装
克隆代码后。
参考[在 Windows 环境下更新 ESP-IDF 工具](https://docs.espressif.com/projects/esp-idf/zh_CN/latest/esp32/get-started/windows-setup-update.html#windows-esp-idf)
使用install脚本进行安装。

并将Env Type 选为`ESP-IDF`

![win_source.png](win_source.png)

再选择对应的源码路径。

### linux(和MACOS)
需要手动安装一些组件，再使用install脚本。
参考[Linux 和 macOS 平台工具链的标准设置](https://docs.espressif.com/projects/esp-idf/zh_CN/latest/esp32/get-started/linux-macos-setup.html#linux-macos)
>本人暂无测试条件，本插件未在macOS下测试。

安装完成之后创建`ESP-IDF`项目时选择源码路径即可。

## 注意事项
构建之前，使用`IDF Export Console`执行`idf.py set-target esp32xxx`来设置对应的esp32芯片类型。默认是esp32不用设置。

