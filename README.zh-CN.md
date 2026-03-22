# Archivex

[English](./README.md) | 简体中文

`archivex` 是一个基于 Kotlin Multiplatform + Compose Multiplatform 的离线笔记应用，当前目标平台包括 Android、iOS 和 Desktop (JVM)。它是 Archive 的 KMP 重构版本，原版 Archive 已在 Google Play 上架。

它的核心价值很直接：默认本地、用户可控。笔记不依赖在线服务即可使用，敏感内容尽量保留在设备内，同时在多端之间提供一致的记录、查看和迁移体验。

核心能力包括：

- 本地离线笔记与文件夹管理
- Markdown 编辑与预览，以及 Mermaid 预览
- 敏感笔记保护与设备认证
- 备份 / 恢复，以及明文导入 / 导出

## 模块结构

- `androidApp`
  Android 应用宿主模块，负责 APK 打包和 app manifest。
- `composeApp`
  共享 KMP 模块，承载共享 UI、业务逻辑以及大部分平台实现。
- `iosApp`
  iOS 宿主工程和 Xcode 入口。
- `scripts`
  用于启动 iOS 模拟器和运行应用的辅助脚本。
- `gradle/libs.versions.toml`
  统一管理依赖和插件版本的 version catalog。

`composeApp/src` 下的主要 source set：

- `commonMain`
  跨平台共享 UI、导航、数据层和领域逻辑。
- `androidMain`
  Android 平台实现、平台安全能力、文档选择器和系统集成。
- `iosMain`
  iOS 平台实现以及 `UIViewController` 入口。
- `jvmMain`
  Desktop 平台实现，包括 JavaFX 和 Mermaid 相关支持。
- `commonTest`
  共享测试。

## 开发环境

建议环境：

- JDK 17
- Android Studio 或 IntelliJ IDEA
- Android SDK
- Xcode（仅 iOS 开发需要）

项目当前启用了：

- Gradle configuration cache
- Gradle build cache
- Gradle version catalog

## 运行与构建

### Android

构建 Debug APK：

```bash
./gradlew :androidApp:assembleDebug
```

编译 Android Kotlin：

```bash
./gradlew :composeApp:compileAndroidMain
```

### Desktop

运行 Desktop 应用：

```bash
./gradlew :composeApp:run
```

编译 Desktop Kotlin：

```bash
./gradlew :composeApp:compileKotlinJvm
```

### iOS

运行 iOS 应用常见有两种方式：

1. 用 Xcode 打开 [`iosApp/iosApp.xcodeproj`](/Users/ppp/repos/archivex/iosApp/iosApp.xcodeproj) 并直接运行。
2. 使用辅助脚本启动模拟器，并安装 / 启动应用。

启动一个可用的 iPhone 模拟器：

```bash
./scripts/start_ios_simulator.sh
```

构建并运行 iOS 应用：

```bash
./scripts/run_ios_app.sh
```

也可以传入设备名，例如：

```bash
./scripts/run_ios_app.sh "iPhone 16"
```

## 常用任务

运行共享测试：

```bash
./gradlew :composeApp:allTests
```

构建 Desktop 安装包：

```bash
./gradlew :composeApp:packageDistributionForCurrentOS
```

## 依赖管理

依赖和插件版本统一通过 [`gradle/libs.versions.toml`](/Users/ppp/repos/archivex/gradle/libs.versions.toml) 管理。新增或升级依赖时，优先修改 version catalog，再在 [`composeApp/build.gradle.kts`](/Users/ppp/repos/archivex/composeApp/build.gradle.kts) 中通过 `libs.*` 引用。

## 说明

- 这是一个仍在演进中的 KMP 工程，部分目录和命名仍保留了从 `Archive` 迁移而来的历史痕迹。
- Android 应用入口现在位于 `androidApp`，`composeApp` 已切到 Android-KMP library 插件，以便和 AGP 9 迁移路径保持一致。
