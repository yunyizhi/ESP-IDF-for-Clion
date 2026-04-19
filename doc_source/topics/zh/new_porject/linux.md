# Linux下新建项目
## 选择通过EIM安装的ESP-IDF

### 通过EIM安装ESP-IDF {id="instal_eim-esp-idf"}

参考[在 Linux 上安装 ESP-IDF 及工具链](https://docs.espressif.com/projects/esp-idf/zh_CN/v6.0/esp32/get-started/linux-setup.html)

### 新建项目 {id="create_by_eim-esp-idf"}

### 首次新建项目通过EIM创建工具链
* 选择新建方式为EIM
    ![toolchain_by_eim.png](toolchain_by_eim.png)

* 选择eim_idf.json 文件 并选择具体esp-idf，至于工具链名称默认会生成，也可以自定义
![select_eim.png](select_eim.png)
确认后则可以新建一个工具链

## 源码安装

>对于旧版本的esp-idf，可以使用源码安装，当然新版本依然可以通过源码安装

### 安装前置组件
需要手动安装一些组件，再使用install脚本。
参考[Linux安装具体的组件](https://docs.espressif.com/projects/esp-idf/zh_CN/release-v5.5/esp32/get-started/linux-macos-setup.html#linux)

以ubuntu为例
```bash
sudo apt update
sudo apt-get install git wget flex bison gperf python3 python3-pip python3-venv cmake ninja-build ccache libffi-dev libssl-dev dfu-util libusb-1.0-0
```

### python的版本
目前依赖的python仅仅支持3.10, 3.11, 3.12, 3.13. 3.14 参考[idf-im-ui/python版本](https://docs.espressif.com/projects/idf-im-ui/en/latest/prerequisites.html#python-version)
不是所有版本支持3.14如果使用ubuntu则推荐ubuntu22 和ubuntu24 

### 安装esp-idf

### 克隆代码

<tabs>

<tab title="从github clone代码">
<code-block>
mkdir -p ~/esp-idf-v6.0
cd ~/esp-idf-v6.0
git clone -b release/v6.0 --recursive https://github.com/espressif/esp-idf.git
</code-block>
</tab>
<tab title="从中国站直接下载">
<code-block>
# 以idf6.0为例，将idf6.0的release中附件esp-idf-v6.0.zip的url复制出
# 然后替换`github.com` 到 `dl.espressif.cn/github_assets`
cd ~/
wget https://dl.espressif.cn/github_assets/espressif/esp-idf/releases/download/v6.0/esp-idf-v6.0.zip
unzip esp-idf-v6.0.zip
</code-block>
</tab>
</tabs>

### 安装
安装会下载一些东西,根据需要选择后续是否使用镜像站

<tabs>
    <tab title="直接安装">
```bash
cd ~/esp-idf-v6.0
./install.sh
```
</tab>
<tab title="使用中国站(乐鑫中国站和python阿里源)">

```bash
cd ~/esp-idf-v6.0
export IDF_GITHUB_ASSETS="dl.espressif.cn/github_assets"
export PIP_INDEX_URL=https://mirrors.aliyun.com/pypi/simple/
export PIP_TRUSTED_HOST=mirrors.aliyun.com
./install.sh
```
</tab>
</tabs>

### install脚本python的编译报错

安装过程中会创建一个虚拟环境，然后pip安装一些依赖，直接安装可能遇到编译失败，
可以新开一个终端，加载虚拟环境的active 文件,再设置python镜像环境变量，仅仅安装二进制文件

以ubuntu24为例 

<tabs>
    <tab title="直接手动安装二进制">
```bash
source ~/.espressif/python_env/idf6.0_py3.12_env/bin/activate
pip install xxx --only-binary=:all:
```
</tab>
<tab title="使用阿里源镜像">
```bash
export PIP_INDEX_URL=https://mirrors.aliyun.com/pypi/simple/
export PIP_TRUSTED_HOST=mirrors.aliyun.com
source ~/.espressif/python_env/idf6.0_py3.12_env/bin/activate
pip install xxx  --only-binary=:all:
```
</tab>
</tabs>

### 新建项目 {id="create_by_src"}

![src_old_new.png](src_old_new.png)

