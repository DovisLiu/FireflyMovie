# 流萤影视 (FireflyMovie)

基于开源 TV 影音聚合生态（上游可追溯至 [CatVod](https://github.com/CatVodTVOfficial/CatVodTVJarLoader) 猫影视）二次开发的 Android 影音应用。保留原功能基础上进行多项工程改造，同时支持 **Android TV 大屏** 与 **手机** 两种使用场景。
本项目仅提供播放能力，不包含任何影音内容源，内容源由用户通过外部配置自行扩展。

> 本项目遵循生态上游的 **GPL v3** 许可证（见 [LICENSE.md](LICENSE.md)）。

## 项目信息

| 项目 | 值 |
|------|-----|
| 应用名称 | 流萤影视 |
| 包名 | `com.fireflymovie.tv` |
| 最低版本 | Android 7.0（API 24）|
| 原生架构 | `arm64-v8a`（64 位）、`armeabi-v7a`（32 位） |
| 界面变体 | `leanback`（电视版）、`mobile`（手机版）|

## 功能特性

- **点播**：多站点分类浏览与筛选；多站点并行搜索（关键字自动繁转简以提升兼容性）；播放失败自动换源（解析器 → 线路 → 搜索其他站 → 下一站点）；观看记录（保留 60 天）、收藏、无痕模式；电视版支持遥控器操作，手机版支持手势（亮度 / 音量 / 进度）、上下滑切集、屏幕旋转与锁定。
- **直播**：支持 M3U / TXT（`#genre#` 分组）/ JSON 三种直播源格式；XMLTV 的 EPG（支持 `.gz`，每 6 小时自动刷新）；追看 / 时移（`append`、`pltv` 等类型）；频道收藏、隐藏分组密码保护；TVBus、ForceTech 等特殊引擎。
- **播放器**：ExoPlayer（Media3）+ FFmpeg 软解，硬解 / 软解自动降级切换；SurfaceView / TextureView 渲染；Widevine、PlayReady、ClearKey DRM（支持 `#KODIPROP` 声明）；弹幕（DanmakuFlameMaster，与播放时间轴精确同步，支持远程推送）；SRT / SSA / ASS 外挂字幕、系统 CaptioningManager、远程实时注入；倍速、多缩放比例、画中画（PiP）、后台音频、片头 / 片尾自动跳过。
- **爬虫引擎**：支持三种语言编写爬虫 —— Java JAR（DexClassLoader）、JavaScript（QuickJS）、Python（Chaquopy）。
- **网络功能**：DoH（DNS over HTTPS，支持 Bootstrap IP）；HTTP / HTTPS / SOCKS4 / SOCKS5 代理（按 host 正则动态选择）；Hosts 解析覆盖（支持通配符 `*`）；CORS 注入（按 host 规则在响应中注入自定义头）；广告拦截黑名单；WebView 嗅探（regex 拦截媒体 URL，支持 UA 伪装）。
- **DLNA 投放**：DMC（投放端，手机版）扫描局域网 DLNA 设备并投放媒体；DMR（被投放端，电视版）作为 Renderer 接收其他设备投放。基于 JUPnP 3.0.4。
- **Android Auto**：电视版支持 Android Auto，`PlaybackService` 实现 `MediaLibraryService`，可在车机浏览播放记录与直播频道、进行播放控制。
- **远程控制**：应用启动后绑定本地 HTTP 服务器（NanoHTTPD），端口从 **9978** 自动探测至 **9998**，用于播放控制、推送字幕 / 弹幕、多设备同步等。完整端点说明见 [docs/LOCAL.md](docs/LOCAL.md)。

## 模块架构

```
├── app/            主应用（含两套 UI 变体：leanback 电视版 / mobile 手机版）
├── catvod/         爬虫抽象层（Spider 接口、OkHttp 网络栈）
├── quickjs/        QuickJS JavaScript 引擎
├── chaquo/         Chaquopy Python 引擎（爬虫用）
└── firefly6stub/   firefly版本定制的media3编译桩（DolbyVisionOutputPolicy 等）
```

`app/src/main/` 为两个版本共用的业务逻辑，`app/src/leanback/` 与 `app/src/mobile/` 各自实现对应 UI。


## 构建

### 环境要求

| 工具 | 版本 |
|------|------|
| Android Gradle Plugin | 9.3.1 |
| Gradle | 9.6.1 |
| JDK | 21（Android Studio 自带 JBR 21）|
| compileSdk / targetSdk | 37 |
| minSdk | 24 |


### 变体（Flavor）

构建变体由两个维度组合而成：

- **abi**：`arm64_v8a`（64 位）/ `armeabi_v7a`（32 位）
- **mode**：`leanback`（电视版）/ `mobile`（手机版）

因此共有 4 种 release 变体，例如：`leanbackArmeabi_v7a`、`leanbackArm64_v8a`、`mobileArmeabi_v7a`、`mobileArm64_v8a`。

### 签名

正式包使用 `firefly_release.jks` 签名（别名 `firefly`），配置由根目录 `local.properties` 驱动（该文件不入库，需自行创建）：

```properties
storeFile=firefly_release.jks
keyAlias=firefly
keyPassword=你的密码
storePassword=你的密码
```

请使用自己的签名密钥；切勿将 `.jks` 或密码提交到版本库。

### 构建命令

```bash
# 设置 JDK（AGP 9.3.1 需要 JDK 21，请指向你本机的 JDK 21 安装路径，
# 例如 Android Studio 自带的 JBR：C:/Program Files/Android/Android Studio/jbr）
export JAVA_HOME="/path/to/your/jdk-21"

# 构建全部 4 种 release 变体
./gradlew :app:assembleRelease

# 或单独构建某一变体，例如电视版 32 位
./gradlew :app:assembleLeanbackArmeabi_v7aRelease
```

产物位于 `app/build/outputs/apk/<变体>/release/`。

### Python 依赖（Chaquopy）

爬虫所需的 Python 包（lxml、pycryptodome、requests、beautifulsoup4、pyquery、ujson 等）由 Chaquopy 按 Android ABI 下载并打包进 APK 的 assets，运行时自解压，不依赖设备联网或预装 Python。

> **Windows 构建注意**：Chaquopy 在 Windows 上对 `pip_install.py` 的文件系统操作存在竞态（"Access is denied"）。本项目已在 `chaquo/build.gradle` 的 `afterEvaluate` 中注入针对性补丁，在正常构建流程中自动生效，无需额外处理。

## 配置说明

应用通过 Vod / Live 配置（JSON）加载内容源，详见以下文档：

| 文档 | 说明 |
|------|------|
| [docs/CONFIG.md](docs/CONFIG.md) | Vod / Live 配置完整字段说明 |
| [docs/SPIDER.md](docs/SPIDER.md) | 爬虫所有方法规格与返回格式 |
| [docs/LOCAL.md](docs/LOCAL.md) | 本地 HTTP API 所有端点完整说明 |
| [docs/LIVE.md](docs/LIVE.md) | 直播源格式完整说明 |

## 许可证

本项目基于开源 TV 影音聚合生态（上游可追溯至 [CatVod](https://github.com/CatVodTVOfficial/CatVodTVJarLoader)）二次开发，遵循 **GPL v3** 许可证。详见 [LICENSE.md](LICENSE.md)。
