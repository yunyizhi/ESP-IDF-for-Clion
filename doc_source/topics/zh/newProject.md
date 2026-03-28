# 新建项目
> 关于环境变量设置，
> 本项目使用了clion从toolchain环境变量文件加载环境变量的能力，idf的export脚本会导出变量，一般不需要设置全局的环境变量。

>关于中国下载站:本篇涉及idf安装方式均可完全走乐鑫在中国下载站以及python组件使用中国国内镜像源。若出现下载问题，请注意 **完全使用中国站(乐鑫中国站和python阿里源)** 这个选项
> 当然python也可设置其他国内源如阿里源。

## 首次使用clion注意事项
如果首次使用clion,第一次新建当前项目的时候，会弹出一个选择Toolchain的窗口，这个时候不要选择，
本插件初始化项目的时候会自动新建属于IDF的Toolchain并创建对应cmake profile选中它。
如果出现该弹窗，待`Set Target`任务执行完成关掉即可。

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


## Windows下源码安装

>python git需要提前安装，python的版本可能随着idf版本支持度有所不同，比如在测试idf5.5.1的时候使用3.14存在问题使用3.12版本则可以。
> 
> 目前依赖的python仅仅支持3.10, 3.11, 3.12, 3.13.在其他系统可能3.10也不支持参考[idf-im-ui/python版本](https://docs.espressif.com/projects/idf-im-ui/en/latest/prerequisites.html#python-version)

### 克隆代码
在一个没有空格路径下打开`cmd`执行以下命令(powershell设置环境变量的方式不同)
>git需要自行安装
<tabs>
    <tab title="clone代码">

```bash
git clone --recursive https://github.com/espressif/esp-idf.git
```
</tab>
    <tab title="通过http代理 clone代码">

以本机运行代理软件http代理场景为例
```Bash
:: 设置 HTTP 代理
set http_proxy=http://127.0.0.1:7890
:: 设置 HTTPS 代理
set https_proxy=http://127.0.0.1:7890

git clone --recursive https://github.com/espressif/esp-idf.git
```
</tab>
    <tab title="乐鑫中国站下载全量源码(含子模块)">

以idf6.0为例，将idf6.0的release中附件esp-idf-v6.0.zip的url复制出，<br>
然后替换`github.com` 到 `dl.espressif.cn/github_assets`

得到其乐鑫中国站下载地址如下:

`https://dl.espressif.cn/github_assets/espressif/esp-idf/releases/download/v6.0/esp-idf-v6.0.zip`

可用浏览器下载，后解压到一个没有空格的目录
</tab>
</tabs>

### 切换到一个具体的稳定版本(可选)
> 使用中国站下载源码包已经是具体版本了无需进行这一步

例如`v6.0`标签
```Bash
cd esp-idf
git checkout v6.0
git submodule update --init --recursive

```

### 安装
安装会下载一些东西,根据需要选择后续是否使用镜像站

命令行进入源码目录，然后执行以下操作

<tabs>
    <tab title="直接安装">

```bash
install.bat
```
</tab>
    <tab title="安装设置使用乐鑫下载站">

```bash
set IDF_GITHUB_ASSETS=dl.espressif.com/github_assets
install.bat
```
</tab>
<tab title="完全使用中国站(乐鑫中国站和python阿里源)">

```bash
set IDF_GITHUB_ASSETS=dl.espressif.cn/github_assets
set PIP_INDEX_URL=https://mirrors.aliyun.com/pypi/simple/
set PIP_TRUSTED_HOST=mirrors.aliyun.com
install.bat
```
</tab>
</tabs>




### 新建项目
并将Env Type 选为`ESP-IDF`

![win_source.png](win_source.png)

再选择对应的源码路径export.bat和export.ps1所在目录。

设置对应target,然后新建项目。

## Linux源码安装


### 安装具体依赖的组件
需要手动安装一些组件，再使用install脚本。
参考[Linux安装具体的组件](https://docs.espressif.com/projects/esp-idf/zh_CN/latest/esp32/get-started/linux-macos-setup.html#linux)

以ubuntu为例
```bash
sudo apt update
sudo apt-get install git wget flex bison gperf python3 python3-pip python3-venv cmake ninja-build ccache libffi-dev libssl-dev dfu-util libusb-1.0-0
```
### 克隆代码

<tabs>
    <tab title="clone代码">
<code-block>
mkdir -p ~/esp
cd ~/esp
git clone --recursive https://github.com/espressif/esp-idf.git
</code-block>
</tab>
    <tab title="通过HTTP代理克隆">
<code-block>
# 很多时候代理软件可能只能在windows和macos上安装，在将代理软件的局域网模式打开，防火墙要运行应用程序过防火墙。
# 然后在linux的终端上设置为其局域网具体地址
mkdir -p ~/esp
cd ~/esp
# 设置 HTTP 代理
export http_proxy=http://192.168.137.1:7890
# 设置 HTTPS 代理
export https_proxy=http://192.168.137.1:7890
git clone --recursive https://github.com/espressif/esp-idf.git
</code-block>
</tab>
    <tab title="从中国站直接下载">
<code-block>
# 以idf6.0为例，将idf6.0的release中附件esp-idf-v6.0.zip的url复制出
# 然后替换`github.com` 到 `dl.espressif.cn/github_assets`
mkdir -p ~/esp
cd ~/esp
wget https://dl.espressif.cn/github_assets/espressif/esp-idf/releases/download/v6.0/esp-idf-v6.0.zip
unzip esp-idf-v6.0.zip
mv esp-idf-v6.0/ esp-idf
</code-block>
</tab>
</tabs>

#### 切换到一个具体的稳定版本(可选)
> 使用中国站下载源码包已经是具体版本了无需进行这一步

例如`v6.0`标签
```Bash
cd esp-idf
git checkout v6.0
git submodule update --init --recursive

```

### 安装
安装会下载一些东西,根据需要选择后续是否使用镜像站

<tabs>
    <tab title="直接安装">

```bash
cd ~/esp/esp-idf
./install.sh
```
</tab>
    <tab title="安装设置使用乐鑫下载站">

```bash
cd ~/esp/esp-idf
export IDF_GITHUB_ASSETS="dl.espressif.com/github_assets"
./install.sh
```
</tab>
<tab title="完全使用中国站(乐鑫中国站和python阿里源)">

```bash
cd ~/esp/esp-idf
export IDF_GITHUB_ASSETS="dl.espressif.cn/github_assets"
export PIP_INDEX_URL=https://mirrors.aliyun.com/pypi/simple/
export PIP_TRUSTED_HOST=mirrors.aliyun.com
./install.sh
```
</tab>
</tabs>

### 新建项目
安装完成之后创建`ESP-IDF`项目时选择源码路径下`export.sh`所在的目录。

设置对应target,然后新建项目。

![linux_new.png](linux_new.png)

## Macos源码安装
>这里使用m4测试成功

### 安装具体依赖的组件

需要手动安装一些组件。
参考[macos安装具体的组件](https://docs.espressif.com/projects/esp-idf/zh_CN/latest/esp32/get-started/linux-macos-setup.html#macos)
>这里m4使用HomeBrew安装具体组件测试成功,`ccache`也进行了安装

### 克隆代码

<tabs>
    <tab title="clone代码">
<code-block>
mkdir -p ~/esp
cd ~/esp
git clone --recursive https://github.com/espressif/esp-idf.git
</code-block>
</tab>
    <tab title="通过HTTP代理克隆">

```bash
mkdir -p ~/esp
cd ~/esp
export http_proxy=http://127.0.0.1:7890
export https_proxy=http://127.0.0.1:7890
git clone --recursive https://github.com/espressif/esp-idf.git
```

</tab>
    <tab title="从中国站直接下载">

 以idf6.0为例，将idf6.0的release中附件esp-idf-v6.0.zip的url复制出<br>
 然后替换`github.com` 到 `dl.espressif.cn/github_assets`
```Bash
mkdir -p ~/esp
cd ~/esp
curl -O  https://dl.espressif.cn/github_assets/espressif/esp-idf/releases/download/v6.0/esp-idf-v6.0.zip
unzip esp-idf-v6.0.zip
mv esp-idf-v6.0/ esp-idf
```

</tab>
</tabs>

### 切换到一个具体的稳定版本(可选)
> 使用中国站下载源码包已经是具体版本了无需进行这一步

例如`v6.0`标签
```Bash
cd esp-idf
git checkout v6.0
git submodule update --init --recursive

```

### 安装
安装会下载一些东西,根据需要选择后续是否使用镜像站

<tabs>
    <tab title="直接安装">

```bash
cd ~/esp/esp-idf
./install.sh
```
</tab>
    <tab title="安装设置使用乐鑫下载站">

```bash
cd ~/esp/esp-idf
export IDF_GITHUB_ASSETS="dl.espressif.com/github_assets"
./install.sh
```
</tab>
<tab title="完全使用中国站(乐鑫中国站和python阿里源)">

```bash
cd ~/esp/esp-idf
export IDF_GITHUB_ASSETS="dl.espressif.cn/github_assets"
export PIP_INDEX_URL=https://mirrors.aliyun.com/simple
export PIP_TRUSTED_HOST=mirrors.aliyun.com
./install.sh
```
</tab>
</tabs>
安装完成之后创建`ESP-IDF`项目时选择源码路径下`export.sh`所在的目录。

设置对应target,然后新建项目。

![macos_new.png](macos_new.png)


## 重命名Toolchain

生成的Toolchain名称前缀为`EspIdfAutoGen`，目前本插件不依赖Toolchain名称，可以重命名。
重命名后，已经配置过的项目需要在cmake profile处重新指定对应名称即可。

![toolchains](toolchains.png)