# 功能移植与协议来源

本工程保留根目录 AGPL-3.0 LICENSE 和原作者声明。Kotlin 功能移植参考 TG-Twilight/c001apk，冻结提交与逐项对应关系见 [TWILIGHT_PORT.md](TWILIGHT_PORT.md)。账号存储、本地关注、设备标识、会话和验证等移植文件沿用 AGPL-3.0。

Token V3 算法、协议查找表、公开分享页 JSON 与网易验证码配置，参考 TG-Twilight 的对应实现，原始协议兼容来源为 [daimiaopeng/coolapk-desktop](https://github.com/daimiaopeng/coolapk-desktop/tree/3ef2ad4da6d171e118d2b29b0f18d7ffa6da9086) 的 `auth.rs`、`auth_blob.b64`、`client.rs`、`neteaseCaptcha.ts`。MIT 完整版权和许可随应用收录在 [coolapk-desktop-MIT.txt](../app/src/main/assets/licenses/coolapk-desktop-MIT.txt)。查找表用于协议兼容，其第三方权利不由该 MIT 许可担保；酷安名称、商标及服务内容归相应权利人。

签名测试向量沿用参考工程的合成设备与固定时间数据，不包含真实账号或设备标识。人机验证由用户在网易官方 Web SDK 中完成，不分发验证码 SDK 源码。

32 张 `niuniu_emotion_*.png` 沿用 TG-Twilight 所收录的酷安官方 16.6.4 表情素材，名称映射用于显示兼容。版权归酷安及相应权利人，根 AGPL-3.0 不改变这些素材的权利。

## Markdown

CommonMark 0.30.0 与 GFM 表格、删除线、任务列表扩展遵循 BSD 3-Clause；完整许可证打包于 `app/src/main/assets/licenses/CommonMark-BSD-3-Clause.txt`。解析行为参考 TG-Twilight 98526cc，使用原生 Compose 渲染，不使用 WebView 执行正文。
