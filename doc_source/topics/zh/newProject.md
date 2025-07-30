# 新建项目
> 关于环境变量设置，
> 本项目使用了clion从toolchain环境变量文件加载环境变量的能力，idf的export脚本会导出变量，一般不需要设置全局的环境变量。

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


## 源码安装(windows/linux/macos)

通过克隆 ESP-IDF项目，并在对应平台使用install脚本安装ESP-IDF。这样可以通过git更新IDF。


### Windows下源码安装
>windows下这里默认是在安装过离线版本后测试，不能确保未安装离线版的环境，源码安装时一些需要的依赖项已经安装。
>测试时python git等也有单独安装的版本

克隆代码后使用install脚本进行安装。

#### 克隆代码
>git需要自行安装

在一个没有空格路径下打开cmd执行以下命令
```bash
git clone --recursive https://github.com/espressif/esp-idf.git
```
#### 无法克隆时使用代理
需要替换代理服务器地址为实际代理程序真实监听地址
```Bash
:: 设置 HTTP 代理
set http_proxy=http://127.0.0.1:7890
:: 设置 HTTPS 代理
set https_proxy=http://127.0.0.1:7890

git clone --recursive https://github.com/espressif/esp-idf.git
```

#### 切换到一个具体的稳定版本(可选)

例如`v5.5`标签
```Bash
cd esp-idf
git checkout v5.5
git submodule update --init --recursive

```

#### 安装
安装会下载一些东西,根据需要选择后续是否使用镜像站

* 直接安装
```bash
install.bat
```

* 或者使用乐鑫下载站
```bash
set IDF_GITHUB_ASSETS="dl.espressif.com/github_assets"
install.bat
```
* 或者使用乐鑫中国下载站
>这一步不必过http代理，通过新建命令行则不会继承之前克隆源码时设置的临时环境变量。
```bash
set IDF_GITHUB_ASSETS="dl.espressif.cn/github_assets"
install.bat
```

#### 新建项目
并将Env Type 选为`ESP-IDF`

![win_source.png](win_source.png)

再选择对应的源码路径export.bat和export.ps1所在目录。

设置对应target,然后新建项目。

### Linux


#### 安装具体依赖的组件
需要手动安装一些组件，再使用install脚本。
参考[Linux安装具体的组件](https://docs.espressif.com/projects/esp-idf/zh_CN/latest/esp32/get-started/linux-macos-setup.html#linux)

#### 克隆代码

进入一个没有空格的目录，克隆代码。
```bash
git clone --recursive https://github.com/espressif/esp-idf.git
```
#### 无法克隆时使用代理

> 很多时候代理软件可能只能在windows和macos上安装，在将代理软件的局域网模式打开，防火墙要运行应用程序过防火墙。
> 然后在linux的终端上设置为其局域网具体地址
```Bash
# 设置 HTTP 代理
export http_proxy=http://192.168.137.1:7890
# 设置 HTTPS 代理
export https_proxy=http://192.168.137.1:7890

git clone --recursive https://github.com/espressif/esp-idf.git
```

#### 切换到一个具体的稳定版本(可选)

例如`v5.5`标签
```Bash
cd esp-idf
git checkout v5.5
git submodule update --init --recursive

```

#### 安装
安装会下载一些东西,根据需要选择后续是否使用镜像站

* 直接安装
```bash
./install.sh
```

* 或者使用乐鑫下载站
```bash
export IDF_GITHUB_ASSETS="dl.espressif.com/github_assets"
./install.sh
```
* 或者使用乐鑫中国下载站
>不必过http代理，可新开终端，或者ssh会话。
```bash
export IDF_GITHUB_ASSETS="dl.espressif.cn/github_assets"
./install.sh
```
#### 新建项目
安装完成之后创建`ESP-IDF`项目时选择源码路径下`export.sh`所在的目录。

设置对应target,然后新建项目。


### Macos
>这里使用m4测试成功

#### 安装具体依赖的组件

需要手动安装一些组件。
参考[macos安装具体的组件](https://docs.espressif.com/projects/esp-idf/zh_CN/latest/esp32/get-started/linux-macos-setup.html#macos)
>这里m4使用HomeBrew安装具体组件测试成功,`ccache`也进行了安装

#### 克隆代码

进入一个没有空格的目录，克隆代码。
```bash
git clone --recursive https://github.com/espressif/esp-idf.git
```

#### 无法克隆时使用代理

需要替换代理服务器地址为实际代理程序真实监听地址
```Bash
export http_proxy=http://127.0.0.1:7890
export https_proxy=http://127.0.0.1:7890
git clone --recursive https://github.com/espressif/esp-idf.git
```

#### 切换到一个具体的稳定版本(可选)

例如`v5.5`标签
```Bash
cd esp-idf
git checkout v5.5
git submodule update --init --recursive

```

#### 安装
安装会下载一些东西,根据需要选择后续是否使用镜像站

* 直接安装
```bash
./install.sh
```

* 或者使用乐鑫下载站
```bash
export IDF_GITHUB_ASSETS="dl.espressif.com/github_assets"
./install.sh
```

* 或者使用乐鑫中国下载站
>不必过http代理，可新开终端，或者ssh会话。
```bash
export IDF_GITHUB_ASSETS="dl.espressif.cn/github_assets"
./install.sh
```

安装完成之后创建`ESP-IDF`项目时选择源码路径下`export.sh`所在的目录。

设置对应target,然后新建项目。


