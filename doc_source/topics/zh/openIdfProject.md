# 打开IDF项目
为了减少阁下的工作量，打开项目之前，至少成功创建过一次ESP—IDF项目，这样才能使用配好的ESP-IDF ToolChain。

## 选择需要打开的项目
![openProject.png](openProject.png)

## 选择之前生成的ToolChain

![ToolChain.png](ToolChain.png)

>如果有多个版本ToolChain,可以通过`IDF Export Console` 
>打印ESP_IDF_VERSION变量的值。

## 选择build目录
建议和idf.py默认保存一致，使用`build`目录。

![build_dir.png](build_dir.png)

> 如果未填写build目录 或者使用了其他目录，在使用命令行执行idf.py时候注意需要 加`-B` 参数 指定实际的build输出目录
> 例如`idf.py -B build_1 flash monitor` <br>
> 自本插件0.5版起 使用本插件的任务树上节点执行相关操作会主动设置-B参数。

## 设置target

### 通过profile设置cmake cache变量设置
当打开一个通用项目时候，而sdkconfig/sdkconfig.defaults没有指定`CONFIG_IDF_TARGET`，会默认使用`esp32`,
当然也可以可以打断cmake加载重新设置。

为了省一步操作，在打开项目过程中指定

![open_set_target.png](open_set_target.png)