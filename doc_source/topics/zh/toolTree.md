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

* 在CLion的终端里面的使用MenuConfig 中使用ESC会导致，当前鼠标回到上方编辑文件。
可以使用左箭头代替<kbd>ESC</kbd>回到上一级菜单的功能，使用`Q`代替退出MenuConfig的功能。

