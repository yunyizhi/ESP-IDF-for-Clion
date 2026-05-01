# MacOs下新建项目

## 选择通过EIM安装的ESP-IDF

### 通过EIM安装ESP-IDF {id="instal_eim-esp-idf"}

参考[在 Macos 上安装 ESP-IDF 及工具链](https://docs.espressif.com/projects/esp-idf/zh_CN/v6.0/esp32/get-started/linux-setup.html)

### 新建项目 {id="create_by_eim-esp-idf"}

### 首次新建项目通过EIM创建工具链
* 选择新建方式为EIM

![eim_mac.png](eim_mac.png)


* 选择eim_idf.json 文件 并选择具体esp-idf，至于工具链名称默认会生成，也可以自定义

>macos下可以关闭设置中使用原生文件浏览器选择文件，来选择隐藏文件。

![mac_sel_eim.png](mac_sel_eim.png)

确认后则可以新建一个工具链

## 源码安装

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

<tab title="从中国站直接下载">

以idf6.0为例，将idf6.0的release中附件esp-idf-v6.0.1.zip的url复制出<br>
然后替换`github.com` 到 `dl.espressif.cn/github_assets`
```Bash
mkdir -p ~/esp
cd ~/esp
curl -O  https://dl.espressif.cn/github_assets/espressif/esp-idf/releases/download/v6.0.1/esp-idf-v6.0.1.zip
unzip esp-idf-v6.0.1.zip
```

</tab>
</tabs>

### 切换到一个具体的稳定版本(可选)
> 使用中国站下载源码包已经是具体版本了无需进行这一步

例如`v6.0.1`标签
```Bash
cd esp-idf
git checkout v6.0.1
git submodule update --init --recursive

```

### 安装
安装会下载一些东西,根据需要选择后续是否使用镜像站

<tabs>
    <tab title="直接安装">

```bash
cd ~/esp/esp-idf-v6.0.1
./install.sh
```
</tab>
<tab title="使用中国站(乐鑫中国站和python阿里源)">

```bash
cd ~/esp/esp-idf-v6.0.1
export IDF_GITHUB_ASSETS="dl.espressif.cn/github_assets"
export PIP_INDEX_URL=https://mirrors.aliyun.com/simple
export PIP_TRUSTED_HOST=mirrors.aliyun.com
./install.sh
```
</tab>
</tabs>



### 新建项目 {id="create_by_src"}

有以下两种方法新建项目

#### 使用自定义脚本创建工具链

安装完成之后选择以自定义脚本方式选择export.sh创建工具链，可以在创建工具链时自定义工具链名称


![new_script.png](new_script.png)

#### 或者使用旧版创建方式 {id="#2"}

![src_old_new.png](src_old_new.png)

会自动生成一个工具链，需要后续自己重命名，以便区分不同版本
