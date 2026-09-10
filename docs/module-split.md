# 外观主模组与独立 Gameplay Addon

## 职责与安装

两个 Git 仓库、两个 Mod 安装包。外观 JAR 同时提供客户端显示与服务端同步；Gameplay 负责三族能力、成就系统等玩法。

| 仓库 | 模组 ID | 内容 |
| --- | --- | --- |
| [Magical-Land](https://github.com/Magical-Land-Official/Magical-Land) | `magicaland` | 模型、捏脸、动画、预设、挑染、可爱标志、眼神、耳动、手持物视觉、光效音效、外观同步、公共 API |
| [Magical-Land-Gameplay](https://github.com/Magical-Land-Official/Magical-Land-Gameplay) | `magicaland_gameplay` | 三族能力、能力轮盘、成就系统及后续玩法 |

外观不依赖 Gameplay。玩法只通过[公共 API](appearance-api.md)使用外观表现，不读配置管理器、网络缓存或渲染内部实现。权限、库存、伤害和交互仍由 Gameplay 服务端判断，捏脸显示角翼不是授权依据。

- 客户端装外观及 Fabric API、GeckoLib：使用本地外观与编辑器；没有服务端同步支持时不保证互见自定义外观。
- 客户端和服务器都装外观及依赖：保留多人外观、动画和辅助注视同步，不需要 Gameplay。
- 使用玩法：客户端和服务器都安装兼容的外观、Gameplay 及依赖。
- 玩家只安装普通 JAR，不安装 API 编译产物，也不把旧一体包与新外观包并装。

## 版本与源码

两个仓库各自使用根 `src/main`、`src/client`、`tests` 和 Gradle 配置。美术与 Blockbench 源文件只保留在外观仓库 `Resources/`。

新架构从外观 `0.3.0`、Gameplay `0.1.0` 开始，公共 API 主版本为 1；不再要求两包版本号相同。Addon 记录准确开发依赖版本，运行时声明兼容范围，API 破坏性变更必须同步调整范围与测试。

外观预设与可爱标志沿用已有格式。玩法的协议和存档兼容规则由[玩法仓库](https://github.com/Magical-Land-Official/Magical-Land-Gameplay/blob/1.20.1-Fabric/docs/repository-boundary.md)记录。

## 独立构建与联合开发

两仓使用 Gradle 9.4.1 wrapper、Loom 1.16.3，Java 输出仍为 17。请使用能运行这些开发工具的现代 JDK。以下是两个仓库的构建与联合调试步骤。

外观仓库：

```powershell
.\gradlew.bat build
.\gradlew.bat publishMavenJavaPublicationToLocalDevelopmentRepository
```

默认发布到 `build/repo`，当前开发坐标为 `top.csituka:magicaland-appearance:0.3.4`，同时提供普通安装 JAR、`api`、`sources`、`api-sources` 产物。API 运行实现只在主 Mod 中打包一次。

Gameplay 仓库：

```powershell
.\gradlew.bat build -PappearanceMavenRepo=C:/absolute/path/to/appearance/build/repo
.\gradlew.bat runClient -PappearanceMavenRepo=C:/absolute/path/to/appearance/build/repo
```

首次构建 Gameplay 前，需要先取得其声明版本的外观模组和 API 文件。可按上面的步骤将外观包发布到本地开发仓库，也可使用 Maven Local：先在外观仓库执行 `publishToMavenLocal`。

两个仓库可分开或在同一个编辑器工作区打开。各自 `runClient`/`runServer` 使用独立 `run/client`/`run/server`；Gameplay 通过发布物加载外观，外观运行配置不加载 Gameplay。不要恢复跨仓 `sourceSets`、复制美术或共享正在使用的存档。

## 验证与后续

两仓分别运行 `node tests/architecture/RepositoryArchitectureTest.mjs`；Gameplay 另运行 `node tests/remote/RemoteStructureTest.mjs`。外观 `node tools/generate-mane-dye-masks.mjs` 默认仅检查。

仓库中的独立 Java 测试需要单独运行，Gradle 任务检查的结果不包含它们的测试结果。测试入口与运行要求见[文档目录](README.md#开发与公共-api)。

发布前检查仅外观客户端／同步服务器、两包联合客户端／服务器，以及预设、草稿保存、显示恢复和多人同步。三族能力与成就系统的功能用例按[玩法文档](https://github.com/Magical-Land-Official/Magical-Land-Gameplay/blob/1.20.1-Fabric/docs/README.md)分别执行。

遵循 `AGENTS.md`，未经用户要求不自行构建或启动游戏。[迁移记录](repository-migration.md)区分已执行检查与待实机验收项。

[独立同步服务](appearance-sync-future.md)作为备选方案保留。外观包沿用现有握手、通道与同步方式；首次外观同步与自身广播开关的处理，以及重复发送完整配置的问题，另见[项目待办](../TODO.md)。
