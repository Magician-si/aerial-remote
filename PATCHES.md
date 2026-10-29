# 修改点清单（smali 层）

- `B2/t0`：桌面顶排首格（原信号源）改为遥控格；新增 `LC2/a` 手机枚举 `W` 与 `C2/e` 中的手机图标路径。
- `B2/w.a`：应用图标点击改走新类 `LaunchApp`，真正启动应用（原逻辑仅标记选中）。
- `B2/t0.k`：壁纸格 onClick 已还原为原选壁纸功能；`D2/c` 壁纸屏二维码改经 `RemoteQrHelper` 输出遥控配对地址（8898）。
- `l3/a`：音量标签改百分比（现读数值＋分支修正）；亮度标签加 `%`。
- `p0/c.f`：新增可选 focusProperties 参数（既有调用方全传 null）；新类 `SettingsFocus`／`SettingsFocusProps` 给设置顶排三格（信号源／网络／设置）定左右指向，物理遥控左右一按一跳。
- `AndroidManifest`：新增无障碍服务、`WakeActivity`、`RemoteInfoActivity`、`RemoteApp`。
