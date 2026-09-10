# 外观主模组与独立 Gameplay Addon

## 职责与安装

两个 Git 仓库、两个 Mod 安装包。外观同一个 JAR 保留客户端显示与服务端同步，不改为禁止服务端加载，不拆第三个同步安装包，也不引入云同步。

| 仓库 | 模组 ID | 内容 |
| --- | --- | --- |
| [Magical-Land](https://github.com/Magical-Land-Official/Magical-Land) | `magicaland` | 模型、捏脸、动画、预设、挑染、可爱标志、眼神、耳动、手持物视觉、光效音效、外观同步、公共 API |
| [Magical-Land-Gameplay](https://github.com/Magical-Land-Official/Magical-Land-Gameplay) | `magicaland_gameplay` | 能力轮盘、念力出窍、投影交互、携带槽、金胡萝卜彩蛋、后续三族玩法 |

外观不依赖 Gameplay。玩法只通过[公共 API](appearance-api.md)使用外观表现，不读配置管理器、网络缓存或渲染内部实现。权限、库存、伤害和交互仍由 Gameplay 服务端判断，捏脸显示角翼不是授权依据。

- 客户端装外观及 Fabric API、GeckoLib：使用本地外观与编辑器；没有服务端同步支持时不保证互见自定义外观。
- 客户端和服务器都装外观及依赖：保留多人外观、动画和辅助注视同步，不需要 Gameplay。
- 使用玩法：两端都装兼容的外观及 Gameplay。Addon 注册自定义投影实体，不承诺无 Addon 客户端加入玩法服务器。
- 玩家只安装普通 JAR，不安装 API 编译产物，也不把旧一体包与新外观包并装。

## 版本与源码

两个仓库各自使用根 `src/main`、`src/client`、`tests` 和 Gradle 配置。美术与 Blockbench 源文件只保留在外观仓库 `Resources/`。

新架构从外观 `0.3.0`、Gameplay `0.1.0` 开始，公共 API 主版本为 1；不再要求两包版本号相同。Addon 记录准确开发依赖版本，运行时声明兼容范围，API 破坏性变更必须同步调整范围与测试。

模组、资源、通道、预设及可爱标志格式不变。玩法保留 `magicaland:not_what_i_meant` 进度、`misunderstanding` 条件、`magicaland_remote_cargo` 存档及原授权标签。

## 独立构建与联合开发

两仓使用 Gradle 9.4.1 wrapper、Loom 1.16.3，Java 输出仍为 17。请使用能运行这些开发工具的现代 JDK。以下命令供开发者按需执行，不代表此次已构建或实机验收。

外观仓库：

```powershell
.\gradlew.bat build
.\gradlew.bat publishMavenJavaPublicationToLocalDevelopmentRepository
```

默认发布到 `build/repo`，当前开发坐标为 `top.csituka:magicaland-appearance:0.3.2`，同时提供普通安装 JAR、`api`、`sources`、`api-sources` 产物。API 运行实现只在主 Mod 中打包一次。

Gameplay 仓库：

```powershell
.\gradlew.bat build -PappearanceMavenRepo=C:/absolute/path/to/appearance/build/repo
.\gradlew.bat runClient -PappearanceMavenRepo=C:/absolute/path/to/appearance/build/repo
```

两边也支持 Maven Local：先在外观仓库执行 `publishToMavenLocal`。新克隆的 Addon 需要先取得声明版本的主 Mod/API 产物；本次未假定已经存在公开 Maven 托管。

两个仓库可分开或在同一个编辑器工作区打开。各自 `runClient`/`runServer` 使用独立 `run/client`/`run/server`；Gameplay 通过发布物加载外观，外观运行配置不加载 Gameplay。不要恢复跨仓 `sourceSets`、复制美术或共享正在使用的存档。

## 验证与后续

两仓分别运行 `node tests/architecture/RepositoryArchitectureTest.mjs`；Gameplay 另运行 `node tests/remote/RemoteStructureTest.mjs`。外观 `node tools/generate-mane-dye-masks.mjs` 默认仅检查。原有独立 Java 测试不是 JUnit，任务图通过不等于它们全部通过。

发布前检查仅外观客户端/同步服务器、两包联合客户端/服务器；首次入服与退出同步、旧预设、草稿保存/取消、第一人称合成收尾、出窍结束后注视恢复、满包/死亡/重连携带物品。

遵循 `AGENTS.md`，未经用户要求不自行构建或启动游戏。[迁移记录](repository-migration.md)区分已执行检查与待实机验收项。

[独立同步服务](appearance-sync-future.md)只保留备选方案。当前握手、通道和同步行为不改；首次快照与自身广播开关耦合、重复全量回传问题另列待办，不混入此次迁移。
