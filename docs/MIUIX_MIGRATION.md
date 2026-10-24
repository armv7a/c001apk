# miuix 改造说明

## 基线与设计依据

- 上游：[frisk1127/c001apk-compose](https://github.com/frisk1127/c001apk-compose)，基线提交 `82ffea3c746955420c1d4f00380f5a6d7a6cad02`。
- 视觉参考：[flutter_miuix](https://github.com/ChuxinNeko/flutter_miuix)。实现采用 Kotlin 原生 Compose，不引入 Flutter 引擎。
- 设计规范参考：[Mishka UI guidelines](https://github.com/YuKongA/Mishka/blob/main/docs/ui-guidelines.md)。采用 squircle 圆角、12 dp 外边距、分段设置卡片、选中即生效的单选弹窗、窗口弹窗与毛玻璃背景采样。
- 组件：[miuix v0.9.4](https://github.com/compose-miuix-ui/miuix/tree/v0.9.4)，使用 `miuix-ui`、`miuix-preference`、`miuix-icons`、`miuix-blur`。

## 实现

- 主题完全使用 `MiuixTheme` / `ThemeController`，新安装默认使用 miuix 蓝色，保留系统动态色、自定义色、深色、纯黑、字体和内容缩放设置。原有 `materialYou` 持久化字段继续表示“系统主题色”，避免破坏数据兼容性。
- 主导航参考 TG-Twilight/c001apk，采用“主页 / 关注 / 我的”；宽屏切换为 NavigationRail。主页保留独立标题，标题与搜索按钮同高；下方为应用、动态、热门、话题、数码、酷图页签，默认动态；关注独立成页；我的保留个人资料与消息，并提供设置入口。首页板块编辑移到设置，保存完成后才退出编辑页；保留重选刷新。
- 页面骨架通过 `MiuixScaffold` 统一接入官方 `LayerBackdrop` / `textureBlur`，主页内外层独立采样。顶栏和底栏使用透明颜色呈现模糊内容。
- WebView 页面使用相同的 miuix 顶栏，但网页内容不进入额外的背景采样绘制，避免 Chromium 硬件合成闪烁。加载进度条覆盖在固定视口上方，离开页面时停止加载并销毁 WebView。
- 信息流、详情与各类消息卡片使用 miuix Card / squircle modifier。设置按独立 lazy item 分段绘制圆角卡片。
- 动态详情将作者资料并入正文的连续阅读卡片，评论独立分区；长文章继续逐块惰性绘制，宽屏阅读内容限制为 800 dp。评论分类使用与标题同行的 13 sp 文字选项和 miuix 按压反馈，选中项以主题色和字重标识；不使用 TabRow，不提供独立“查看评论”底栏。机型紧跟 IP，与时间放在同一行；话题按钮与子评论底色直接沿用首页样式。用户主页采用封面与叠放头像、资料操作和独立统计卡片，保留关注、私信、搜索及列表入口。
- 公共图片请求使用固定通用 User-Agent，避免 CDN 拒绝缺失 User-Agent 的请求，同时在每次重定向保留凭据隔离。头像、封面和九宫格提供失败状态与点击重试，重复重组不再取消并重新发起相同的 View 图片请求。
- 输入框、按钮、图标按钮、单选/多选/开关、进度条、分隔线、通知条、菜单、底部弹层和下拉刷新使用原生 miuix。窗口弹窗限制内容高度，按钮独立于滚动区。
- 动态、评论、历史与通知卡片的三点菜单统一使用 miuix IconButton / BasicComponent / WindowListPopup。按钮使用透明底色，展开后不保持灰色按压底；菜单以实际按钮为锚点右对齐，按压与开合动画由 miuix 实现。
- 发帖编辑器保留 Android Editable、表情 span、选区和输入法处理；输入区使用 squircle 背景，勾选框和加载指示器通过 ComposeView 使用原生 miuix。View 页面上的确认、验证码和下载弹窗使用 miuix 内容。
- “我的”页采用可滚动的 miuix 个人资料卡片、两行三列快捷入口和连续消息分组，统一 12 dp 外边距、20–24 dp 圆角及原生点击反馈；私信与通知分别保留入口。对应 14 个 Flutter 图标提取为独立矢量，许可见 [图标声明](THIRD_PARTY_ICONS.md)。
- 首页快捷入口卡片的图标顶部留白为 16 dp；三点图标统一缩小 12.5%，按钮点击区域保留。
- 主要图标使用 MiuixIcons。点赞、表情、退格等少量社区动作保留独立 AOSP 矢量源码，附原始 Apache 2.0 许可头，不依赖 Material 图标库；Swatch 保留上游署名。机型名称使用本地绘制的现代手机轮廓，动态与评论操作菜单使用三点 More 图标。
- 删除直接 Material 3、Material Components、Material 图标库及旧 MaterialKolor Compose 配色依赖。miuix 自身内部使用的颜色算法和窗口尺寸分类工具由 miuix 管理。折叠工具栏库未使用 Material API，已排除其发布时附带的 Material 和预览工具依赖。

- 启动页采用 Android 原生 AnimatedVectorDrawable，无穷符号展开后回到原图标。参考 TG-Twilight/c001apk 的系统启动画面形式，沿用本项目现有图标路径，自行实现展开、回弹和淡入动画；数盟初始化期间保留系统启动画面，删除中间的“正在准备连接”页面。缓存 ID 的后续启动直接进入首页。

## 构建

最低 Android **13 / API 33，ARM64**，使用官方 miuix-blur 的 AGSL 渲染；没有低版本模糊降级分支。目标 API 35，编译平台 `android-37.0`。

构建工具版本已固定：JDK 17、Gradle 9.7.1、AGP 9.4.1、Kotlin 2.4.20、KSP 2.3.12、Hilt 2.60.1、Activity / Activity Compose 1.13.0。Activity 版本提供 miuix 窗口菜单要求的 NavigationEventDispatcher。Android SDK 需安装 `platforms;android-37.0` 和 `build-tools;36.0.0`，子模块还使用 API 34/35。

```sh
export JAVA_HOME=/path/to/jdk17
export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME=/path/to/Android/Sdk
bash ./gradlew :app:assembleRelease
bash ./gradlew :app:assembleDebug :app:testDebugUnitTest :media-support:testDebugUnitTest :app:lintDebug
bash ./gradlew :app:assembleDebugAndroidTest
# 已连接 Android 13+ 设备时：
bash ./gradlew :app:connectedDebugAndroidTest
```

压缩 APK：`app/build/outputs/apk/release/c001apk-miuix_<版本名>(<版本号>).apk`；调试 APK：`app/build/outputs/apk/debug/app-debug.apk`。调试包使用本地调试签名；调试版和发布版统一使用独立包名 `com.example.c001apk.compose.miuix`；后续覆盖更新需保留同一签名密钥。发布包启用 R8 和资源压缩。未配置发布密钥时，本地构建使用本机调试密钥；CI Release 强制要求发布密钥。签名配置支持环境变量及 local.properties；公开分发需保管固定发布密钥。

版本名使用 `1.3.5-miuix+<Git 提交>`，支持 `-PappVersion=x.y.z`；建议从包含 Git 历史的仓库或 bundle 构建。网络环境需要代理时，Gradle/JVM 应配置对应代理属性。

## 验证范围

保留原有应用和媒体单元测试，新增设置开关只保存一次、单选弹窗即时应用，以及横竖屏发帖布局与 Editable 输入的仪器测试。测试范围见 [验证记录](VERIFICATION.md)，最终安装包的版本、签名与摘要见交付目录内的 APK 校验 JSON。未登录情况下不测试发帖、点赞、私信等会写入远程账号的操作。

## 数盟 ID

根据用户提供的 `smid.zip` 接入 `cn.shuzilm.core` Java 包装层、配置资源和 ARM64 `libdu.so`。SDK 使用资料中的官方身份 `com.coolapk.market` / `16.1.3` / `2604022`；应用安装包继续使用独立的 miuix 包名和 Git 版本号。身份适配仅作用于 SDK 的 Context / PackageManager，不改动系统包管理器或官方客户端。

首次进入时异步获取并等待 native 注册完成，最长等待 35 秒；已经保存的 ID 直接复用。设置中的“数字联盟 ID”可以重新获取或手动填写。保存 ID 时在同一 DataStore 事务内更新设备请求头和旧协议默认参数，保留账号、主题以及用户自定义参数。重新获取失败不会覆盖已保存的 ID。SDK 回调、超时和重复操作由仓库层统一处理；日志只记录成功与否，不输出 ID、Cookie 或 token。

保留参考实现的 `pkglist`、`operation`、`cdlmt` 禁用配置。SDK 初始化仅在需要获取 ID 时进行，未增加电话、位置等运行时权限。此集成使用用户提供的二进制，不能据此推断其全部内部行为。数盟获取和酷安接口是否可用，以交付验证记录中的实机结果为准。

`libdu.so` SHA-256：`3f37818d7aa8af50ad13956cfb3acfd72a447f721b77d3216043f710d57044ea`。

数盟网络仪器测试需显式启用，普通测试不会请求数盟服务：

```sh
adb shell am instrument -w -e smidNetwork true \
  -e class com.example.c001apk.compose.ui.DigitalUnionNetworkTest \
  com.example.c001apk.compose.miuix.test/androidx.test.runner.AndroidJUnitRunner
```

## 分发校验

`python3 tools/verify_apk.py <APK> --sdk <Android SDK> --certificate-sha256 <预期证书摘要>` 核对独立包名、版本、Android 13 下限、ARM64 原生数盟库、压缩、v2 签名和 16 KiB zipalign。证书解析兼容不同 Build Tools 输出前缀，不允许用 Source Stamp 代替实际签名者。

CI 手动构建，不自动发布。Release 要求 `KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD` 和 `SIGNING_CERT_SHA256`，缺失或签名不符即失败；Debug 使用调试密钥。Gradle 支持同名 `C001APK_` 环境变量及既有 `local.properties`，密钥不打包进源码或产物。

## 本轮上游功能移植

完整范围和 33 条 TG-Twilight 提交对应关系见 [移植跟踪](TWILIGHT_PORT.md)。默认分享使用 `www.coolapk1s.com`，官方链接仍能打开；数盟继续使用 native SDK。公告小助手和独立 announcements 分支按用户要求排除。

应用自更新只读取本版本的发布仓库。当前不预置他人的发布仓库；可在设置填写 `owner/repo`，或构建时传入 `-PupdateRepository=owner/repo`。CI 默认使用当前 GitHub 仓库。未配置时手动检查会说明原因，不会引导安装 Flutter 版 APK。
