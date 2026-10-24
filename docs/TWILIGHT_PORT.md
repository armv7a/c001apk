# TG-Twilight 功能移植跟踪

用户范围：只移植 TG-Twilight 在其提交中新增、补全和修复的 Android 相关功能。当前原生 Compose + miuix 实现、独立包名、数盟 SDK 和用户指定的 UI 调整继续保留。

上游冻结点：`fb3e06cac5c65e9f86c902f1b4304aeee055f3e2`（main，1.3.5）。审阅完整历史 81 条；TG-Twilight GitHub 身份有 33 条，其中 2 条使用 Simon Twilight 显示名且仅修改文档。不会因上游继续提交而悄悄扩大本轮范围。

## 提交约定

- 使用 Conventional Commits：`feat(scope): summary`、`fix(scope): summary`、`test(scope): summary`、`docs(scope): summary`、`build(scope): summary`。
- 一个功能或独立修复对应一个提交；测试与所验证功能一起提交，避免无法独立评审的大杂烩。
- 提交正文写明行为变化、关键验证，并以 `Upstream-commit: <完整 SHA>` 记录对应来源。
- Author / Committer 均为 `KiritoXDone <58523182+KiritoXDone@users.noreply.github.com>`，每次提交前后核验；不重写已有历史。
- 不直接 cherry-pick Dart/Flutter、桌面宿主或 CI Secret 配置；Android 功能用 Kotlin 实现，相关行为用原生测试验收。

## 功能清单

| 范围 | 当前状态 | 验收要点 |
| --- | --- | --- |
| 我的页面与对应图标 | 已实现；Debug 构建、现有单元测试与 lint 通过 | 固定资料区、快捷入口、紧凑消息分组、对应 Flutter 矢量图标、账号/设置入口 |
| 多账号 / 游客模式 | 待移植 | 原账号迁移、切换/删除、账号会话隔离、异步结果不串号 |
| 本地 / 悄悄关注与分类 | 待移植 | 游客可用、名单持久化、分类选择、合并分页去重、登录退出不清单 |
| 酷安 API / 登录 / 验证码 | 待逐项核对补齐 | 签名与头部、错误类型、单次验证重试、取消与账号切换、公开详情回退 |
| 设备标识与持久会话 | 部分已有 | 已有 native 数盟生成与原子保存；补官方文本识别/跳转、域路径有效期、验证状态绑定与过期 |
| c001apk 自身更新 | 待移植 | 独立网络客户端、语义版本、正式 Release、ABI 资产、冷启动/手动/跳过逻辑 |
| 私信 / 小秘书 / 事件链接 | 待逐项核对补齐 | 文本编码、图片上传、动态选择与发送、消息分类、通知计数与恢复 |
| 自适应 / 链接 / 返回 / 图片 | 部分已有 | 原生已带宽屏栏与详情分屏；逐项核对上游 Android 增量与边界测试 |
| Markdown | 待移植 | 保守识别、原文切换、代码/表格/列表/任务/锚点、安全链接与图片、限行预览 |
| 正文导出 | 待移植 | Markdown/离线 HTML、SAF 保存、置顶+热门最多三条评论、图片限制与失败确认 |
| 公告小助手 | 待移植 | 独立请求、索引校验、版本/撤回/过期、按需正文与摘要校验、缓存、已读/提醒状态、更新优先 |
| 构建与分发 | 部分已有 | Android 13+ ARM64、正确 SDK 名、压缩、签名与对齐；保留本版本包名和签名身份 |

## 上游提交索引

仅文档、桌面和 Flutter 构建机制变动记录为无需功能移植；不能将功能提交笼统标成完成。功能验收完成后回填本地提交与测试证据。

| 上游提交 | 内容 | 处理 |
| --- | --- | --- |
| [`e9b9262`](https://github.com/TG-Twilight/c001apk/commit/e9b9262d6e4ef3d7e442507749799e7e24b8a3f0) | 网络协议、签名、错误分类、游客详情回退、登录与验证码、缓存清理；Revive c001apk and add reproducible multi-platform Actions builds | 待核对 / 关联上方功能项 |
| [`b0f87eb`](https://github.com/TG-Twilight/c001apk/commit/b0f87eb32442a65b6b53206a9df416baacdba66f) | Android SDK CI 安装；Install Android SDK command-line tools explicitly in CI | 本地已使用明确 SDK 包；原生 CI 待核对 |
| [`934a736`](https://github.com/TG-Twilight/c001apk/commit/934a73684600871820fc83172dd38f10ddb7760d) | Android SDK CI 安装；Avoid requesting the discontinued Android tools package | 本地已使用明确 SDK 包；原生 CI 待核对 |
| [`735ca56`](https://github.com/TG-Twilight/c001apk/commit/735ca56126a3c02c06a3075b38efc880586ada07) | iOS 构建；Pass the iOS binary before lipo architecture verification flags | 不适用于 Android 原生工程 |
| [`001038f`](https://github.com/TG-Twilight/c001apk/commit/001038f1aaf1c31a896017382aee175e9859e89a) | 文档 / 发布记录 / 仓库维护；Document verified Android split version codes and add build badge | 无新增应用功能，保留来源记录 |
| [`0c68f9d`](https://github.com/TG-Twilight/c001apk/commit/0c68f9d2e205736ccb56eb98ce654d6770d374fc) | 文档 / 发布记录 / 仓库维护；Revise browsing content and features in README | 无新增应用功能，保留来源记录 |
| [`65c698b`](https://github.com/TG-Twilight/c001apk/commit/65c698b706943a0b6c172da18c37d52fea26cb71) | 文档 / 发布记录 / 仓库维护；Record successful cloud builds and artifact verification | 无新增应用功能，保留来源记录 |
| [`38db0a9`](https://github.com/TG-Twilight/c001apk/commit/38db0a95e0cf75d8b7597ed9907bf9e0af0f8077) | 文档 / 发布记录 / 仓库维护；Merge remote README edits with cloud build verification docs | 无新增应用功能，保留来源记录 |
| [`4bf240a`](https://github.com/TG-Twilight/c001apk/commit/4bf240a714b1a7af4b8f3a5e3de0a0b56595dbaf) | 文档 / 发布记录 / 仓库维护；Update basic account repair progress and center README feature table | 无新增应用功能，保留来源记录 |
| [`1fe9eb6`](https://github.com/TG-Twilight/c001apk/commit/1fe9eb6991c1fd8b4c2c781be4689fc67373be29) | 文档 / 发布记录 / 仓库维护；Link the published 1.1.0 release from download and build guides | 无新增应用功能，保留来源记录 |
| [`5a0663f`](https://github.com/TG-Twilight/c001apk/commit/5a0663f1fddecb4cf14b71fddb7dc311013ce5e4) | 多账号与游客切换、本地/悄悄关注、自身更新、导航与分类设置；Release 1.2.0 features: local follows, accounts and update checks | 待核对 / 关联上方功能项 |
| [`53bcec0`](https://github.com/TG-Twilight/c001apk/commit/53bcec0bfc805edf2b04fe215e25849018e538c5) | 更新提示异步回归；Wait for update UI state across real and fake async clocks | 待核对 / 关联上方功能项 |
| [`7a15623`](https://github.com/TG-Twilight/c001apk/commit/7a15623ad8ce7fdce1956d66784ba5c054094e10) | iOS 构建；Align iOS native lockfile with upgraded WebView dependency | 不适用于 Android 原生工程 |
| [`c2bb245`](https://github.com/TG-Twilight/c001apk/commit/c2bb24587c761ac1659d467f86afaab270c58b06) | iOS 构建；Package unsigned iOS arm64 IPA with release checksums | 不适用于 Android 原生工程 |
| [`1b093c8`](https://github.com/TG-Twilight/c001apk/commit/1b093c8014e8d30f17be73d9b93c984cdf3ca845) | 账号切换真实 IO 回归；Wait for account actions to finish real IO in widget tests | 待核对 / 关联上方功能项 |
| [`14621eb`](https://github.com/TG-Twilight/c001apk/commit/14621eb98aa1e8c9dc257e1658bd59a96696ab69) | iOS 构建；Document 1.2.0 release and unsigned iOS download | 不适用于 Android 原生工程 |
| [`a2f5fbb`](https://github.com/TG-Twilight/c001apk/commit/a2f5fbbdfd05e82388a87587bff5670b6e67b2ba) | 关注分类、图片/动态私信、酷安小秘书、事件链接、表情处理；feat: release 1.2.5 with follow categories and richer messaging | 待核对 / 关联上方功能项 |
| [`148effb`](https://github.com/TG-Twilight/c001apk/commit/148effb6e83260af365801082f577e08634779fc) | 文档 / 发布记录 / 仓库维护；docs: use compact SVG banner in README | 无新增应用功能，保留来源记录 |
| [`aed2333`](https://github.com/TG-Twilight/c001apk/commit/aed23337b2c4dbbf5a4a4f72daa68579dfdf6213) | 文档 / 发布记录 / 仓库维护；chore: keep temporary release notes out of tracked files | 无新增应用功能，保留来源记录 |
| [`50d6f28`](https://github.com/TG-Twilight/c001apk/commit/50d6f28d1a93d7d9b53561c82b48cc645ca5635f) | 文档 / 发布记录 / 仓库维护；docs: version screenshot paths to avoid stale image caches | 无新增应用功能，保留来源记录 |
| [`94e9a6e`](https://github.com/TG-Twilight/c001apk/commit/94e9a6e0a3fcd9cb2468110054ee5255044297ab) | 文档 / 发布记录 / 仓库维护；docs: record verified 1.2.5 cloud distribution | 无新增应用功能，保留来源记录 |
| [`4084b81`](https://github.com/TG-Twilight/c001apk/commit/4084b814d4d56fd5f0b412a72ecc314881ce4fa5) | 空验证码字段 validate Cookie、验证后重试、每次冷启动更新检查；fix: restore verified comments and startup updates for 1.2.6 | 待核对 / 关联上方功能项 |
| [`1f23265`](https://github.com/TG-Twilight/c001apk/commit/1f2326567ff6314573b5a5a43fc2f865c173bde0) | 文档 / 发布记录 / 仓库维护；docs: record verified 1.2.6 cloud distribution | 无新增应用功能，保留来源记录 |
| [`875c542`](https://github.com/TG-Twilight/c001apk/commit/875c542209162b548ad8283b3a550abbeaeb9945) | 官方设备标识导入、设备参数一致性、API 会话与验证码状态持久化；feat: import official device identity and persist API sessions for 1.2.8 | 待核对 / 关联上方功能项 |
| [`f309b43`](https://github.com/TG-Twilight/c001apk/commit/f309b4382b5ef12675437d584487d54e5b890eee) | 文档 / 发布记录 / 仓库维护；Update DEVICE_ID_IMPORT.md | 无新增应用功能，保留来源记录 |
| [`99ceff3`](https://github.com/TG-Twilight/c001apk/commit/99ceff3fd211b5789b631dda201d9d011b0543df) | Android 签名配置与维护环境；chore: resume Flutter maintenance and unify Android signing | 待核对 / 关联上方功能项 |
| [`9a601e9`](https://github.com/TG-Twilight/c001apk/commit/9a601e928d06205c9035868407b7ebd1ef583ed1) | 自适应浏览与阅读面板、消息布局、返回动效、Android 链接、分享与下架提示；feat: prepare 1.3.2 with adaptive layouts and expressive UI | 待核对 / 关联上方功能项 |
| [`d8a095d`](https://github.com/TG-Twilight/c001apk/commit/d8a095db0396b5b3a6aa1a7f3761b8dcc9f31891) | Android API 37 发布包名；fix: install Android API 37 by its published SDK package name [skip ci] | 待核对 / 关联上方功能项 |
| [`e077e15`](https://github.com/TG-Twilight/c001apk/commit/e077e1506dbcd52f2c141a63fd5944a2ea85486d) | Flutter Android 插件注册；fix: regenerate Android plugin registration for release builds [skip ci] | 待核对 / 关联上方功能项 |
| [`f1fa1b4`](https://github.com/TG-Twilight/c001apk/commit/f1fa1b427425cda0a2e9ecb73652b6f8e7d3e3cf) | APK 证书核验工具兼容；fix: parse Build Tools 37 certificate reports without weakening verification [skip ci] | 待核对 / 关联上方功能项 |
| [`b31ae23`](https://github.com/TG-Twilight/c001apk/commit/b31ae23ea1fa022016fc21299571aa28b835bc3e) | APK 内原生库压缩；fix: compress Android native libraries in local and CI APKs [skip ci] | 待核对 / 关联上方功能项 |
| [`98526cc`](https://github.com/TG-Twilight/c001apk/commit/98526cc30cc6ed5d96fc4bed32ee03dfc92b7024) | Markdown、正文导出、公告小助手；feat: add Markdown reading, content export and announcement assistant [skip ci] | 待核对 / 关联上方功能项 |
| [`fb3e06c`](https://github.com/TG-Twilight/c001apk/commit/fb3e06cac5c65e9f86c902f1b4304aeee055f3e2) | 将测试功能纳入稳定应用身份与存储；release: integrate test features under stable identity for 1.3.5 [skip ci] | 待核对 / 关联上方功能项 |
