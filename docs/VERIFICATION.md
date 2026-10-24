# 移植验证记录

日期：2026-10-24。参考冻结点：TG-Twilight/c001apk `fb3e06c`。源码分支：`feat/miuix`。最终 APK 的精确提交、版本、证书和 SHA-256 以交付目录 `apk-verification.json` 为准。

## 自动验证

- `:app:testDebugUnitTest`：53 项通过，无失败/跳过。
- `:media-support:testDebugUnitTest`：8 项通过，无失败/跳过。
- `:app:lintDebug`：通过，0 error；保留已有 warning，没有把 warning 当作 error 隐藏。
- `:app:assembleRelease`：R8 和资源压缩构建通过。
- `python3 -m unittest discover -s tools -p 'test_*.py'`：2 项通过，涵盖新旧证书前缀、Source Stamp 拒绝和多个签名者拒绝。
- `tools/verify_apk.py`：包名、Android 13 下限、ARM64、数盟库、必需资源、原生库压缩、预期证书、v2 签名、16 KiB ZIP 对齐检查通过。

应用测试覆盖账号状态与真实 DataStore IO、关注合并分页与取消、请求凭据域名边界、V3 签名向量、设备导入、API 会话与验证码解析、链接/分享、Markdown、导出内容、更新版本/资产筛选及自适应列数/阅读面板策略。测试只使用合成凭据。

## Android 16 真机

- 保留既有数据覆盖安装，native 数盟 ID 继续复用。
- `ProtocolReadTest` 两项通过：APK 包含 Cookie/Markdown 运行资源；已配置的数盟与 V3 请求可读取真实首页数据。不记录 ID、Cookie、token 或响应正文。
- `ProtocolReadTest.nativeImagesDecodeAvatarThumbnailAndFullSize` 显式启用真实图片网络后通过：关闭内存/磁盘缓存，头像解码为 800×800，原图和缩略图分别解码为 800×601。未记录图片 URL 或用户请求凭据。
- 图片回归根因已实测：清除 API 设备 User-Agent 后未补充公共 User-Agent，CDN 对头像、原图、缩略图均返回 HTTP 567；固定通用浏览器 User-Agent 的对照请求返回 HTTP 200 / image/jpeg，无需 Referer。修复仍在每次重定向清除外域 Cookie、token 和设备身份，并新增边界测试。
- 手动确认新版用户主页的封面/头像、动态详情缩略图和全屏原图均显示。图片失败时提供点击重试；未将失败后手动重试计为已完成的真机用例。
- 动态详情作者资料并入正文卡片，话题按钮与子评论使用首页相同样式，机型跟在 IP 后方，保留正文后的评论和登录后的回复浮钮，移除独立“查看评论”底栏；评论分类改为 13 sp 文字选项，无 TabRow 容器或滑块。用户主页采用封面、头像、资料操作和统计卡片。
- 手动确认新版“我的”页资料卡、快捷入口与消息分组；首页快捷图标顶部留白及三点图标缩放。
- 网页登录闪烁：修复前连续 12 秒观测中网页已加载完成、无重复导航且视口尺寸稳定；将 WebView 排除出外层毛玻璃采样并固定进度条布局后，用户在真机确认不再闪烁。诊断只读取加载/布局状态，不读取表单内容；不等同于完整登录流程验收。
- 手动确认：首页数据加载、动态详情、首页三点菜单展开及返回关闭、保存格式弹窗、游客“我的”和本地关注入口。
- Markdown 通过系统 SAF 实际保存，核对文件正文及来源链接后删除本次测试文件。
- `MiuixPreferencesTest.clickingSwitchRowSavesExactlyOnce` 通过。
- 单选弹窗自动测试被设备切换到其他应用打断，未计为通过；Release 手动选择、立即生效与自动关闭已确认，并恢复原设置。此前数盟身份适配和横竖屏 Editable 编辑器测试已通过；未将旧测试结果当作整套最终仪器测试通过。
- 修复实机暴露的两个崩溃：打包排除 OkHttp publicsuffixes.gz，以及旧 Activity 不提供 miuix 窗口所需 NavigationEventDispatcher。

## 范围与限制

未发送真实私信、发帖、点赞或修改远程账号；多账号切换与失败重试以本地测试验证。当前手机为游客状态。宽屏/平板及完整验证码人工流程未实测；离线 HTML 已做本地内容/资源限制测试，尚未在真机完整保存。GitHub Actions 配置已更新但未提交到远端或运行。

公告小助手按用户要求排除。自更新实现完成，发布仓库尚未配置；不会检查或安装 TG-Twilight 的 Flutter APK。

本地交付 Release 使用既有本机调试密钥以保持覆盖安装兼容，APK 已启用 R8；这是可安装的本地测试分发版本。公开发布应配置并长期保管正式签名，切换签名后不能直接覆盖已安装版本。源码包不包含签名私钥、local.properties、用户数据或构建缓存。
