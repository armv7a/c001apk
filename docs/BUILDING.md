# 构建与发布

## 环境

- JDK 21。
- Android SDK：`platforms;android-34`、`platforms;android-35`、`platforms;android-37.0` 和 `build-tools;36.0.0`。
- 仓库自带的 Gradle Wrapper，依赖版本见 [版本目录](../gradle/libs.versions.toml)。

配置 `JAVA_HOME`，并通过 `ANDROID_HOME` 或根目录 `local.properties` 中的 `sdk.dir` 指定 SDK 路径。例如：

```properties
sdk.dir=/path/to/Android/sdk
```

安装 SDK 组件：

```sh
sdkmanager 'platforms;android-34' 'platforms;android-35' \
  'platforms;android-37.0' 'build-tools;36.0.0'
```

请使用包含完整历史的 Git 克隆。`versionCode` 由 `git rev-list HEAD --count` 生成；源码 ZIP 缺少 `.git`，无法直接按当前配置构建，浅克隆则会产生不正确的版本号。

## 本地构建

在仓库根目录执行，Windows 使用 `gradlew.bat` 替代 `bash ./gradlew`：

```sh
bash ./gradlew :app:assembleDebug
bash ./gradlew :app:assembleRelease
```

产物路径：

| 类型 | APK 路径 |
| --- | --- |
| Debug | `app/build/outputs/apk/debug/app-debug.apk` |
| Release | `app/build/outputs/apk/release/c001apk_<版本名>(<版本号>).apk` |

Debug 不启用 R8 和资源裁剪；Release 默认开启。排查优化相关问题时，可对 Release 传入 `-PdisableR8=true`。

版本名默认取自 [app/build.gradle.kts](../app/build.gradle.kts)，可通过 `-PappVersion=x.y.z` 指定。应用包名为 `com.example.c001apk.compose`，最低 Android 版本为 7.0（API 24），仅包含 ARM64 原生库。

## 签名

未配置发布密钥时，本地构建使用调试密钥签名，不能覆盖安装本项目发布的正式 APK。自行分发时应使用固定的发布密钥，后续更新沿用相同签名。

通过以下环境变量配置签名，也可在 `local.properties` 中使用去掉 `C001APK_` 前缀后的同名键：

```text
C001APK_KEYSTORE_PATH
C001APK_KEYSTORE_PASSWORD
C001APK_KEY_ALIAS
C001APK_KEY_PASSWORD
```

配置后执行：

```sh
bash ./gradlew :app:assembleRelease -PrequireReleaseSigning=true
```

`requireReleaseSigning` 会在未配置发布密钥时终止构建。密钥文件和签名凭据不要提交到仓库。

## 测试与校验

```sh
bash ./gradlew :app:testDebugUnitTest :media-support:testDebugUnitTest \
  :mojito:testDebugUnitTest :app:lintDebug
python3 -m unittest discover -s tools -p 'test_*.py'
```

校验正式 APK，替换为实际文件路径及预期签名证书的 SHA-256 摘要：

```sh
python3 tools/verify_apk.py /path/to/app.apk \
  --sdk "$ANDROID_HOME" \
  --certificate-sha256 'EXPECTED_CERTIFICATE_SHA256'
```

工具检查包名、最低 Android 版本、ABI、必需资源、原生库压缩、签名和 ZIP 对齐，并输出 JSON 报告。

设备测试及需要显式启用的网络测试见 [miuix 改造说明](MIUIX_MIGRATION.md)。已有测试结果保存在[验证记录](VERIFICATION.md)中。

## GitHub Actions

[Android 工作流](../.github/workflows/android.yml) 支持手动构建。Release 构建需要配置以下仓库 Secrets：

| Secret | 内容 |
| --- | --- |
| `KEYSTORE_BASE64` | 发布密钥库文件的 Base64 编码 |
| `KEYSTORE_PASSWORD` | 密钥库密码 |
| `KEY_ALIAS` | 签名密钥别名 |
| `KEY_PASSWORD` | 签名密钥密码 |
| `SIGNING_CERT_SHA256` | 预期签名证书的 SHA-256 摘要 |

构建完成后可从该次运行的 Artifacts 下载产物。Fork 需要配置自己的签名，自签名 APK 无法覆盖安装本项目正式版。

## 发布版本

1. 更新 `app/build.gradle.kts` 中的默认版本、`.github/workflows/android.yml` 中的版本默认值，以及 `CHANGELOG.md` 对应版本的更新记录。
2. 提交并推送代码，为该提交创建 `vMAJOR.MINOR.PATCH` 格式的标签并推送。
3. 等待 [Publish Release 工作流](../.github/workflows/release.yml) 完成。

工作流从标签读取版本号，检出该标签的源码，执行单元测试、Lint、Release 构建和 APK 签名校验。通过后使用对应更新记录创建 GitHub Release，一并发布 APK、JSON 校验报告和 `SHA256SUMS`。

公开 APK 文件名为 `c001apk_<版本名>.<版本号>.apk`，与本地构建的命名格式不同。

若因 CI 环境问题失败，可手动运行 `Publish Release` 并填写原标签。源码仍从该标签检出，无需移动标签；如果 Release 已存在，重试会更新其说明和附件。

## Fork 的更新来源

应用内更新入口位于“设置 → 关于 → 应用更新”。更新仓库在构建时指定，应用内不提供自定义来源：

```sh
bash ./gradlew :app:assembleRelease -PupdateRepository=owner/repo
```

本地构建默认读取 `KiritoXDone/c001apk`，CI 默认使用当前 GitHub 仓库。应用检查正式 Release，支持自动检查、手动检查和跳过版本，APK 下载由浏览器处理。
