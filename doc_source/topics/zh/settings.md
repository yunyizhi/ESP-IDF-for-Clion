# 设置

单项目设置。切换到Settings这个tab页。

![settings_panel.png](settings_panel.png)

会使用clion自带持久化能力，保存设置参数。

>项目的`.idea`目录下会有一个`espidf_settings.xml`。

## 串口设置
* Port 端口号

保存后会在执行命令时候 导出环境变量 `ESPPORT`。
使用idf.py flash或者monitor时 传给esptool.py和idf_monitor.py的端口号会使用当前变量。
>带监视器的任务，会被新建的会请求串口的任务停掉，但仅仅停掉设置的同一串口，若清空端口号，新建的需要使用串口的任务，
> 则会替换原来存活也未设置端口号的任务。

* Monitor Baud 监视器波特率

保存后会在执行命令时候 导出环境变量 `IDF_MONITOR_BAUD`。
idf.py会把会把波特率传给idf_monitor.py。

* Upload Baud 烧录的波特率

保存后会在执行命令时候 导出环境变量 `ESPBAUD`。
idf.py会把会把波特率传给esptool.py。

>通过环境变量设置，不会影响`IDF Export Console`生成的当前终端会话环境变量。

## CMake Profile
可以指定任务树使用的CMake Profile用于执行任务时指定cmake build目录。
可以参考 [Profiles](profiles.md)


## 设置Target(芯片类型)

如果是esp-idf项目，build目录生成后，含对应描述json,

默认情况下在**右下角状态栏**会有本插件的icon和芯片类型

![set-target_statusBar](set-target_statusBar.png)

* 不勾选前面Preview复选框时列出芯片类型

    ![no_preview_targets.png](no_preview_targets.png)


* 勾选前面Preview复选框时列出芯片类型

    ![targets_with_preview.png](targets_with_preview.png)

* 点击具体选项，也会按`Preview复选框`勾选状态 决定是否添加`--preview`参数

### 目前的问题

* 该命令不属于cmake target，使用无法使用ninja make等执行，设置具体的build输出目录并不能更改具体的目录的target，而只能修改默认的profile对应的target

    建议多profile项目通过Profile里面cmake cache变量设置

    ![profile_set_target.png](profile_set_target.png)

* 多profile项目在使用当前功能设置target之后也会触发重新加载cmake,可能会报错target不匹配，可手动删除其build输出目录,重新加载cmake