# 移植验证记录

日期：2026-10-24。参考冻结点：TG-Twilight/c001apk `fb3e06c`。源码分支：`feat/miuix`。最终 APK 的精确提交、版本、证书和 SHA-256 以交付目录 `apk-verification.json` 为准。

## 自动验证

- `:app:testDebugUnitTest`：52 项通过，无失败/跳过。
- `:media-support:testDebugUnitTest`：8 项通过，无失败/跳过。
- `:app:lintDebug`：通过，0 error；保留已有 warning，没有把 warning 当作 error 隐藏。
- `:app:assembleRelease`：R8 和资源压缩构建通过。
- `python3 -m unittest discover -s tools -p 'test_*.py'`：2 项通过，涵盖新旧证书前缀、Source Stamp 拒绝和多个签名者拒绝。
- `tools/verify_apk.py`：包名、Android 13 下限、ARM64、数盟库、必需资源、原生库压缩、预期证书、v2 签名、16 KiB ZIP 对齐检查通过。

应用测试覆盖账号状态与真实 DataStore IO、关注合并分页与取消、请求凭据域名边界、V3 签名向量、设备导入、API 会话与验证码解析、链接/分享、Markdown、导出内容、更新版本/资产筛选及自适应列数/阅读面板策略。测试只使用合成凭据。

## Android 16 真机

- 保留既有数据覆盖安装，native 数盟 ID 继续复用。
- `ProtocolReadTest` 两项通过：APK 包含 Cookie/Markdown 运行资源；已配置的数盟与 V3 请求可读取真实首页数据。不记录 ID、Cookie、token 或响应正文。
- 手动确认：首页数据加载、动态详情、首页三点菜单展开及返回关闭、保存格式弹窗、游客“我的”和本地关注入口。
- Markdown 通过系统 SAF 实际保存，核对文件正文及来源链接后删除本次测试文件。
- `MiuixPreferencesTest.clickingSwitchRowSavesExactlyOnce` 通过。
- 单选弹窗自动测试被设备切换到其他应用打断，未计为通过。此前数盟身份适配和横竖屏 Editable 编辑器测试已通过；未将旧测试结果当作整套最终仪器测试通过。
- 修复实机暴露的两个崩溃：打包排除 OkHttp publicsuffixes.gz，以及旧 Activity 不提供 miuix 窗口所需 NavigationEventDispatcher。

## 范围与限制

未发送真实私信、发帖、点赞或修改远程账号；多账号切换与失败重试以本地测试验证。当前手机为游客状态。宽屏/平板及完整验证码人工流程未实测；离线 HTML 已做本地内容/资源限制测试，尚未在真机完整保存。GitHub Actions 配置已更新但未提交到远端或运行。

公告小助手按用户要求排除。自更新实现完成，发布仓库尚未配置；不会检查或安装 TG-Twilight 的 Flutter APK。

本地交付 Release 使用既有本机调试密钥以保持覆盖安装兼容，APK 已启用 R8；这是可安装的本地测试分发版本。公开发布应配置并长期保管正式签名，切换签名后不能直接覆盖已安装版本。源码包不包含签名私钥、local.properties、用户数据或构建缓存。
