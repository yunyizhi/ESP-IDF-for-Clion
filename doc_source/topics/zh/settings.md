# 设置

单项目设置。切换到Settings这个tab页。

![settings_panel.png](settings_panel.png)

当前只包含串口相关设置。会使用clion自带持久化能力，保存设置参数。

>项目的`.idea`目录下会有一个`espidf_settings.xml`。

## 串口设置
* Port 端口号

保存后会在执行命令时候 导出环境变量 `ESPPORT`。
使用idf.py flash或者monitor时 传给esptool.py和idf_monitor.py的端口号会使用当前变量。

* Monitor Baud 监视器波特率

保存后会在执行命令时候 导出环境变量 `IDF_MONITOR_BAUD`。
idf.py会把会把波特率传给idf_monitor.py。

* Upload Baud 烧录的波特率

保存后会在执行命令时候 导出环境变量 `ESPBAUD`。
idf.py会把会把波特率传给esptool.py。

>通过环境变量设置，不会影响`IDF Export Console`生成的当前终端会话环境变量。