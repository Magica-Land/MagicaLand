# 外观与玩法分包

## 结构与归属

一个仓库、一个 Gradle 根工程、两个独立 JAR。开发者直接打开根目录；不复制外观代码或美术资源到玩法模块。

| 模块 | 模组 ID | 内容 |
| --- | --- | --- |
| `appearance` | `magicaland` | 原有外观、捏脸、预设、眼神、耳动、动画、悬浮手持物视觉、魔法音效、光效、保存变身光尘及多人外观同步 |
| `gameplay` | `magicaland_gameplay` | 金胡萝卜马匹互动、轻微碰撞伤害、爱心演出与「不是这个意思！」进度；未来三族能力 |

服务端眼神目标选择仅服务外观，仍属于外观包。现有悬浮物没有获得远程操作、采矿能力；未来实际能力判定与魔法值归玩法包。两模块均保留公共/客户端代码分离，不把外观包改为仅客户端。

`Resources/` 的 Blockbench 工程和美术源文件、`docs/`、`tools/`、`tests/` 保持在根目录。正式外观运行资源改到 `appearance/src/main/resources`；生成挑染遮罩的脚本和独立测试路径已同步迁移。

## 依赖与数据兼容

- 外观不依赖玩法；玩法通过 Gradle `namedElements` 和客户端输出依赖外观，且 Fabric 元数据要求同版本外观。没有 jar-in-jar 嵌入，不在玩法包重复发布模型或贴图。
- 外观继续依赖 Fabric API、GeckoLib；Mod Menu 仍为开发依赖和可选集成。玩法联合开发配置显式提供外观所需的运行库。
- 根 `gradle.properties` 统一管理两包版本，首个分包版本为 `0.2.0`。旧 `0.1` 一体包不满足新玩法包依赖，不能与外观包并装。
- 外观模组 ID、Java 包、资源命名空间、网络通道及预设数据格式不变。没有重写模型、UV、贴图、动画或玩家文件。
- 彩蛋 Java 包迁到 `top.csituka.magicaland.gameplay` 下，但进度仍为 `magicaland:not_what_i_meant`，条件仍为 `misunderstanding`。旧进度记录可继续匹配。
- 彩蛋翻译也迁入玩法包，保留原翻译键及 `assets/magicaland/lang` 路径；两包语言文件的键互不重复，游戏按资源包合并语言。未来新增玩法资源使用 `magicaland_gameplay` 命名空间。
- 不新增空泛的能力接口；开始三族实现时由外观提供小范围、稳定的表现入口，玩法只传递状态，不直接耦合骨骼内部结构。外观选择不能作为服务端能力授权。

## 开发与产物

需要与原项目一致的 Gradle 9.4.1 和能运行当前 Loom 的 JDK；Java 输出目标仍为 17。仓库目前仅有 wrapper 配置文件，没有 wrapper 启动脚本和 JAR，以下 `gradle` 指本机安装的 Gradle 9.4.1 或 IDE 配置的对应发行版。

| 命令（从根目录运行） | 用途 |
| --- | --- |
| `gradle build` | 构建两个模块 |
| `gradle :appearance:build` | 构建外观包 |
| `gradle :gameplay:build` | 构建玩法及必要的外观依赖产物；不把外观嵌入玩法 |
| `gradle :appearance:runClient` | 仅外观客户端 |
| `gradle :appearance:runServer` | 仅外观专用服务端 |
| `gradle :gameplay:runClient` | 外观＋玩法客户端 |
| `gradle :gameplay:runServer` | 外观＋玩法专用服务端 |

产物分别位于 `appearance/build/libs/magicaland-appearance-0.2.0.jar` 和 `gameplay/build/libs/magicaland-gameplay-0.2.0.jar`。正式安装用 remap 后的普通 JAR，不用 `sources` 或 `dev` JAR。根工程不产出第三个模组。

两个模块的客户端/服务端分别使用各自的 `run/client`、`run/server`，避免共用存档锁。首次启动不会自动搬迁旧测试存档或设置；需有意复制，不能覆盖玩家数据。专用服务端的 EULA 由使用者确认。

## 验证与后续验收

- `node tests/modules/ModuleSplitTest.mjs`：无需构建，检查模块边界、入口、Mixin、翻译、依赖和迁移后路径。
- `node tests/modules/MigrationSnapshotTest.mjs a97115e`：与分包前提交对照，逐项检查原文件完整迁移，允许的改动仅为入口、包名、元数据及翻译归属。
- `node tools/generate-mane-dye-masks.mjs`：默认只检查，不重写模型和遮罩。
- `tests/mane/run-dye-tests.ps1`：保留独立 Java 回归入口，路径已适配新结构。
- 根 `check` 聚合各模块 Gradle 检查，但现有根 `tests/` 的独立测试不是 JUnit，不能将 Gradle `check` 成功当作它们全部通过。
- 遵循根 `AGENTS.md`，本次分包不自行构建或启动游戏；源码/资源与独立测试验证不等于完整 Loom 构建和游戏验收。

后续实际构建后应验收：仅外观客户端与专用服务端、两包联合客户端与专用服务端；旧预设加载、多玩家外观同步、只装外观不触发彩蛋、两包触发规则不变、旧成就进度延续；缺失外观或装旧版本时由 Loader 明确拒绝玩法包。

### 本次检查结果（2026-09-09）

- 模块边界检查通过；分包前 161 个源文件及资源全部保留或按明确规则迁移，模型、动画、纹理、声音内容未改。
- 挑染生成器只读检查通过；独立调色与分区路由回归分别通过 407843、1443804 项断言。
- 迁移后的彩蛋食物与触发路径通过 1323 项断言，状态、时限和原版 GoalSelector 相关检查通过 43968 项。测试借助 Fabric 类加载器运行，没有进入 Minecraft 主循环；这不是实际马匹互动或 Mixin 注入的游戏验收。
- Gradle 离线 `build --dry-run` 成功解析两个模块及独立 remap 产物任务；两个模块的 `runClient`、`runServer` 任务图也通过 dry-run。所有构建和启动任务均跳过，尚未生成新 JAR，也未进行客户端或专用服务端实机验收。
