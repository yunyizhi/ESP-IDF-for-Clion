# Windows下新建项目

## 选择通过EIM安装的ESP-IDF

### 通过EIM安装ESP-IDF {id="instal_eim-esp-idf"}

参考[在 Linux 上安装 ESP-IDF 及工具链](https://docs.espressif.com/projects/esp-idf/zh_CN/v6.0/esp32/get-started/windows-setup.html)

### 新建项目 {id="create_by_eim-esp-idf"}

### 首次新建项目通过EIM创建工具链
* 选择新建方式为EIM

  ![toolchain_by_eim.png](toolchain_by_eim.png)

* 选择eim_idf.json 文件 并选择具体esp-idf，至于工具链名称默认会生成，也可以自定义

![select_eim.png](select_eim.png)
确认后则可以新建一个工具链

## 源码安装

>对于旧版本的esp-idf，可以使用源码安装，当然新版本依然可以通过源码安装

### 前置python git
>python git需要提前安装，python的版本可能随着idf版本支持度有所不同，比如在测试idf5.5.1的时候使用3.14存在问题使用3.12版本则可以。
>
> 目前依赖的python仅仅支持3.10, 3.11, 3.12, 3.13 参考[idf-im-ui/python版本](https://docs.espressif.com/projects/idf-im-ui/en/latest/prerequisites.html#python-version)

### 克隆代码
在一个没有空格路径下打开`cmd`执行以下命令(powershell设置环境变量的方式不同)

>git需要自行安装
<tabs>
    <tab title="clone代码">

```bash
git clone --recursive https://github.com/espressif/esp-idf.git
```
</tab>
<tab title="乐鑫中国站下载全量源码(含子模块)">

以idf6.0为例，将idf6.0的release中附件esp-idf-v6.0.zip的url复制出，<br>
然后替换`github.com` 到 `dl.espressif.cn/github_assets`

得到其乐鑫中国站下载地址如下:

`https://dl.espressif.cn/github_assets/espressif/esp-idf/releases/download/v6.0/esp-idf-v6.0.zip`

国内[点此下载6.0](https://dl.espressif.cn/github_assets/espressif/esp-idf/releases/download/v6.0/esp-idf-v6.0.zip)
下载后解压到一个没有空格的目录
</tab>
</tabs>


### 安装
安装会下载一些东西,根据需要选择后续是否使用镜像站

命令行进入源码目录，然后执行以下操作

<tabs>
<tab title="直接安装">

```bash
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


### install脚本python的编译报错

![pip_i_py_c_err.png](pip_i_py_c_err.png)

安装过程中会创建一个虚拟环境，然后pip安装一些依赖，直接安装可能遇到编译失败，
可以新开一个终端，加载虚拟环境的active 文件,再设置python镜像环境变量，仅仅安装二进制文件

![pip_i_bin.png](pip_i_bin.png)

虚拟环境一般在用户目录的 `.espressif`目录下`python_env`下,在cmd执行其active便可加载该虚拟环境，然后手动安装bin

```bash
pip install xxx  --only-binary=:all:
```

### 新建项目 {id="create_by_src"}

![src_old_new.png](src_old_new.png)


## 旧版离线安装ESP-IDF

* ESP-IDF 工具安装器 安装

>可参考ESP-IDF教程[ESP-IDF 工具安装器](https://docs.espressif.com/projects/esp-idf/zh_CN/release-v5.5/esp32/get-started/windows-setup.html#esp-idf)

在[该地址](https://dl.espressif.com/dl/esp-idf/)下载对应版本的ESP-IDF

![idf_dl.png](idf_dl.png)

选择一个 `Offline Installer(离线安装器)`，无需使用加速器。

下载完成按向导安装，可以勾选附加的驱动。

### 制作自定义脚本

1.在桌面找到打开对应idf命令行的脚本
 ![legacy_offline_active.png](legacy_offline_active.png)

2.右键其中cmd脚本

![legacy_offline_active_prop.png](legacy_offline_active_prop.png)

3.复制其目标属性栏的值

例如内容如下

```plain text
C:\WINDOWS\system32\cmd.exe /k ""D:\Espressif\idf_cmd_init.bat" esp-idf-542f7c536b076a89457cb6e0b96d5d74"
```

4.新建一个bat脚本比如:按idf版本叫做: `export_idf54.bat`.
增加上述目标栏复制出传给cmd的最后命令(根据本机查看属性操作的实际结果)`"D:\Espressif\idf_cmd_init.bat" esp-idf-542f7c536b076a89457cb6e0b96d5d74`

可以去除一层引号

5.选择该脚本创建工具链

![custom-sh.png](custom-sh.png)

![custom_sh-name.png](custom_sh-name.png)

给工具栏取名，即可新建toolchain,便可以此新建idf项目