# debug
集成了openocd调试。可以断点调试 查看线程、变量，集成gdb控制台，内存视图，和外设寄存器查看。

默认的openocd未设置参数，如esp32c3 esp32c6 esp32s3 esp32h2 esp32p4 等含 内置jtag对于这些 idf.py openocd会自动选择内置jtag进行调试，
而如esp32 和esp32s2等 官方idf文档里面也是使用了核心板上外置的jtag进行调试，如果采用外置jtag调试器连接方式，需要openocd 参数项。

详情见[配置其他jtag](https://docs.espressif.com/projects/esp-idf/zh_CN/v5.3.1/esp32/api-guides/jtag-debugging/configure-other-jtag.html#jtag)

>如果需要进入openocd目录查看对应board配置，可以在idf命令行查看该环境变量`OPENOCD_SCRIPTS`
 
>即使配置了OpenOcd Arguments 在运行OpenOcd Server任务时不会打印参数是正常现象，该参数通过设置任务环境变量实现，目前测试的5.3版本直接传参
> 放在后面会被idf.py 视为一个任务，所以OpenOcd Server任务的命令行无参。

### 默认的运行配置
使用当前插件新建项目时，会基于选择的target创建一份运行配置。如果使用esp32c3 esp32c6 esp32s3 (esp32h2暂未测试，esp32p4目前的usb口可能未正常连接jtag)等无需其他配置，
连接esp32的jtag到pc即可一键debug。

### 手动新建配置
新建运行配置选择ESP-IDF,会默认基于build目录下的`project_description.json`填充一些默认参数。

elf文件可以加载调试需要符号。rom一般是固定的，部分芯片rom存在不同版本，默认配置可能不是硬件对应的版本，需要手动选择。

gdb一般会自动选择,可根据需要重新选择。


### 调试方法
![debug_run.png](debug_run.png)

在调试之前需要将程序编译完成并写入esp32的flash，可以使用任务树上的flash按钮或者点击 debug配置的运行图标。

点击debug图标开始调试,会启动openocd和gdb

![openocd.png](openocd.png)

会有一个默认断点，并非当前插件设置，建议在app_main手动打断点.然后跳过去。

![default_break.png](default_break.png)

### 查看线程

> 查看线程请勿打开clion的freertos集成开关，否则会与查看线程的功能冲突。

可以下拉线程框切换线程，每个线程栈帧可以点击跳转对应文件行。

![thread.png](thread.png)

### 变量

可以查看变量，进行一些表达式计算，设置变量值，复制地址到内存视图查看，等操作。
![vars.png](vars.png)

### gdb控制台

可执行gdb命令，比如在这里写入寄存器。

比如输入以下命令,将io2设置为输出模式并写入高低，用来控制led灯。超过32的寄存器需要另外一组，具体可以查看技术参考手册。

```gdb
set {uint32_t } 0x60004024 |= (1 << 2)
set {uint32_t } 0x60004008 |= (1 << 2)
set {uint32_t } 0x6000400c |= (1 << 2)
```
![gdb_console.png](gdb_console.png)

### 内存视图

需要复制一个地址查看内存数据分布

![mem_view.png](mem_view.png)

### 外设

可以查看芯片外设的寄存器地址和值

#### 加载对应芯片的svg文件

![peripherals.png](peripherals.png)

点此跳转[乐鑫svd](https://github.com/espressif/svd/releases)项目进行下载。

加载之后如图，如果选错，需要根节点 hide,然后重选。该文件地址本身会被记忆，如果切换芯片，需要手动重选。

![periphreals_show.png](periphreals_show.png)

需要搜索可以使用以csv编辑器打开的功能。写入寄存器可以复制地址到gdb控制台进行操作。


