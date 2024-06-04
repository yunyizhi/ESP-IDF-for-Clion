# 打开IDF项目
为了减少阁下的工作量，打开项目之前，至少成功创建过一次ESP—IDF项目，这样才能使用配好的ESP-IDF ToolChain。

## 选择需要打开的项目
![openProject.png](openProject.png)

## 选择之前生成的ToolChain

![ToolChain.png](ToolChain.png)

>如果有多个版本ToolChain,可以通过`IDF Export Console` 
>打印ESP_IDF_VERSION变量的值。

## 选择build目录
和idf.py保存一致，使用build目录。
![build_dir.png](build_dir.png)

## 设置target
在`IDF Export Console` 中使用idf.pt set-target 设置芯片类型。 