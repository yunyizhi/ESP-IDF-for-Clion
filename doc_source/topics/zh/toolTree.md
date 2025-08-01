# 快捷命令树

![task_tree.png](task_tree.png)

快捷命令树会在任意的项目中保留，正常情况是给ESP-IDF项目使用。
如果希望使用非项目级别的ESP-IDF命令，如在platformio项目中使用`espefuse.py`也是可以的。

![espefuse.png](espefuse.png)

依赖当前项目的Cmake profile选中的ToolChain。
>在新版clion中platformio项目的settings中没有cmake配置，可以将IDF_TOOL chain设为默认，也可以在
platformio项目中使用idf命令行。

## 注意事项

* `IDF Export Console`会使用原来的export脚本，将环境变量导入当前会话，比本插件直接对比环境变量差异追加的环境变量更加全面。例如
`espefuse.py`在`IDF Console`中无法使用，建议使用`IDF Export Console`.

* 在CLion的终端里面的使用MenuConfig 中使用ESC默认行为是上方编辑器获取焦点。

从而使得ESC不可操作Menuconfig。 建议移除终端的<kdb>ESC</kbd>按键将焦点切换到编辑器功能<br>
进入Settings ->keymap -> Plugins | Terminal | Switch Focus To Editor
中文版本是 设置->按键映射->插件 | Terminal | 将焦点切换到编辑器
>若不愿移除，可以使用左箭头代替<kbd>ESC</kbd>回到上一级菜单的功能，使用`Q`代替退出MenuConfig的功能。但编辑文本框退出功能依然无法代替，仅仅可以通过回车确定来关闭。

* MacOS下终端类型任务命令截断

>这个问题比较奇怪，可能来自clion本身的bug.我们在终端未打开情况下，通过任务创建终端并执行命令，在zsh下会截断，但再执行一次，已经打开zsh里面又能只能执行那个之前会截断的命令。
而mac下clion打开bash终端则没有这个问题。

用经典终端

![class_tml.png](class_tml.png)


![class_tml_check.png](class_tml_check.png)

同时将的终端Shell设置为bash

![mac_bash.png](mac_bash.png)

可以避免，任务新建终端命令截断问题

> 如果仅使用经典终端，但继续使用zsh也可以，当任务树打开一个终端后，再次点击具体任务可以生成，正确的执行命令。
