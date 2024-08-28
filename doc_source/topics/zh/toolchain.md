# Toolchain

一般是默认生成的Toolchain，Toolchain对应的ESP-IDF环境正常则无需关心Toolchain。

部分版本可以多版本共存，而有些离线安装版本在安装完未手动配置任何环境变量也会导致其他ESP-IDF不可用。
手动卸载冲突的版本时，同时需要清理Toolchain。


![toolchain.png](toolchian.png)

可以查看ESP-IDF对应Toolchain的环境变量脚本路径。

例如:`C:\Users\admin\AppData\Roaming\JetBrains\CLion2024.2\org.btik.espidf\export_bfae8acc.bat`。

### Toolchain 拓展配置文件

因为Toolchain的持久化是clion内部功能，暂无法修改，创项目时需要选择ESP-IDF对应的ToolChain,则额外存储了一份配置。

在clion配置目录的下的`org.btik.espidf`目录下有一个配置文件:`espidf.json`，配置目录一般在用户目录下，由Clion决定。
格式如下:

```JSON
[
  {
    "envFileName": "D:\\Users\\admin\\CLionProjects\\esp-idf\\export.bat",
    "idfToolPath": "D:\\Users\\admin\\CLionProjects\\esp-idf",
    "activeTime": 1724824476196
  },
  {
    "envFileName": "C:\\Users\\admin\\AppData\\Roaming\\JetBrains\\CLion2024.2\\org.btik.espidf\\export_bfae8acc.bat",
    "idfToolPath": "D:\\Espressif",
    "idfId": "esp-idf-6a5fb824804c253366aea1d8fb612125",
    "activeTime": 1723448151705
  }
]
```

* `envFileName` 对应Toolchain里面选择的环境变量脚本。
* `idfToolPath` ESP-IDF本地源码目录或者ESP-IDF离线安装工具的工具目录。具体由有无`idfId`区分。
* `idfId` 对应ESP-IDF离线安装工具下多个framework中的一个id，值来自于离线工具安装目录的`esp_idf.json`。若无此项说明是基于源码安装。
* `activeTime` 最后一次活跃时间的时间戳，用于新建项目时默认填充最后一次选中的Toolchain配置。

>本插件会在没有对应脚本的Toolchain时自动创建，但这里没有存Toolchain名称，但如果重命名Toolchain其他已使用的的项目可能需要重新选择。

### 离线工具的导出脚本

一般名为:`expport_xxxxx.bat` 而且是在当前用户目录下一个clion的数据目录,由本插件自动生成。
内容示例如下:

```Bash
 D:\Espressif\idf_cmd_init.bat esp-idf-6a5fb824804c253366aea1d8fb612125
```
其中后续是idfId ,在idf_cmd_init.bat中同级目录下有`esp_idf.json` 中含具体framework对应的idfId。

而离线工具安装完成生成的cmd快捷方式大致上也是使用idf_cmd_init.bat 去初始化对应的framework的环境到当前命令行。

这一点和源码安装有区别并不是直接使用具体framework下的export脚本。


### esp-idf源码导出脚本
就是linux或者windows下选Env Type为ESP-IDF时使用源码目录的export脚本。
