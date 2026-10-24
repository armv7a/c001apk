# c001apk miuix

基于 [frisk1127/c001apk-compose](https://github.com/frisk1127/c001apk-compose) 的原生 miuix 界面改造，使用 Kotlin / Jetpack Compose 和 [miuix](https://github.com/compose-miuix-ui/miuix)。最低 Android 13，ARM64 设备。

包含 miuix 主题、导航栏、毛玻璃顶栏/底栏、信息流卡片、搜索、设置、弹窗、下拉刷新和发帖编辑器的界面适配。在上游账号和网络实现基础上，增加数盟 ID 自动获取、缓存和手动管理。

构建、设计依据与实现说明见 [docs/MIUIX_MIGRATION.md](docs/MIUIX_MIGRATION.md)。

安装包名称为 **c001apk miuix**，包名 `com.example.c001apk.compose.miuix`，可与原版共存。账号登录和数据不会从原版自动迁移。
