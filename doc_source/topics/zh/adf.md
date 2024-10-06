# ADF

ADF 暂未集成，但可以手动配置。

可以参考ADF的向导从[步骤1](https://docs.espressif.com/projects/esp-adf/zh_CN/latest/get-started/index.html#step-1-set-up-esp-idf)
开始

本篇采用的adf 在递归clone内置的idf， 以避免 adf和 idf版本不兼容。

## 1.clone adf源码

因为idf是不兼容有空格路径，请在一个无空格路径下 操作。

这一步可以使用命令行HTTP/HTTPS代理环境变量。

```shell
git clone --recursive https://github.com/espressif/esp-adf.git
```

## 2.安装

这里暂时仅仅使用windows测试，

### 2.1 为ADF安装IDF

进入 esp-adf目录下的 esp-idf目录

* 运行cmd 执行 `install.bat`
* 继续执行 `export.bat`

执行完毕 保留cmd窗口。此时当前命令行会话的已经含有IDF的环境变量。

### 2.2 安装ADF

* 是在上一步操作中的cmd窗口 退回到 esp-adf目录或者说是 clone 下来ADF的根目录。
  `cd ..`
* 执行 `install.bat` 安装ADF
* 执行 `export.bat` 导出ADF的环境变量。

> 这一步操作完成之后，该窗口已经包含ADF和IDF的环境变量，可以使用当前命令行窗口，进入ADF相关例程目录，使用
> idf.py 执行 set-target之后 便可使用 idf.py通过`menuconfig` 配置板子类型，具体可以参考例程下的readme。
> 然后使用`idf.py flash` 烧录到具体音频开发板。

## 配置Clion Toolchain

在ADF例子中项目的根CMakeLists.txt里面一般会包含ADF和IDF所在目录的cmake文件

```CMake
include($ENV{ADF_PATH}/CMakeLists.txt)
include($ENV{IDF_PATH}/tools/cmake/project.cmake)
```

这里需要两者的环境变量。

### 新建导出ADF需要的环境变量的脚本

类似于Clion配置 IDF使用环境变量脚本，ADF需要两个环境变量脚本导出的 变量。

这里我们新建一个脚本比如命名为:export_adf.bat 把两个export脚本调用起来。

这里我将其放在esp-adf clone下的根目录，按相对路径调用两个export.bat内容如下:

```Bash
call export.bat
call esp-idf\export.bat
```

### 配置TOOlChain

新建一个System类型的Toolchain，选择上一步新建的脚本即可。

![esp_adf_tool_chain.png](esp_adf_tool_chain.png)

### 打开ADF项目 {id="open_adf_project"}

打开ADF项目时选择刚才新建的Toolchain 然后将cmake输出路径改成 build文件夹。










