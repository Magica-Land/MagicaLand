# 外观包文档

[返回中文首页](../README_ZH.md) · [English README](../README.md)

这里整理 Magical Land 外观包的使用说明、开发接口和历次检查记录。当前开发版本为外观 0.3.4、公共 API 1.4，可搭配 Gameplay 0.3.2。各轮检查与游戏验收范围见下方记录。

## 玩家指南与功能说明

从主菜单右下角“捏马”进入编辑器，F9 与外观包的 Mod Menu 入口用于设置。初次使用可先阅读首页的安装说明，再从[捏脸界面](customization-ui.md)了解款式、预设与“保存并应用”。

| 想了解什么 | 阅读入口 |
| --- | --- |
| 捏脸、预设、预览镜头与保存 | [捏脸界面](customization-ui.md)、[预览待机](preview-idle.md) |
| 鬃毛和尾巴配色、联动与挑染 | [基础调色](mane-palette.md)、[颜色二级菜单](color-submenus.md)、[六区遮罩分色](mane-mask-dye.md)、[左右镜像](mane-mirroring.md) |
| 眼色与可爱标志 | [眼睛颜色](eye-colors.md)、[可爱标志](cutie-marks.md) |
| 眼神、转头与耳动 | [自动注视](automatic-gaze.md)、[头部朝向](head-look.md)、[待机耳动](idle-ears.md) |
| 拍翼、无翼悬浮与独角兽包身光 | [飞行姿态](flight-visuals.md) |
| 魔法外观和声音 | [魔法光效](magic-glow.md)、[手持物光晕](held-item-aura.md)、[收放物品](魔法收放物品.md)、[惯性与拖尾](魔法手持物惯性与拖尾.md)、[手持物音效](magic-sounds.md) |

外观包独立提供模型、编辑器、动画和外观同步。三族能力、成就系统等玩法由独立的 Gameplay Addon 维护；安装组合见[双仓说明](module-split.md)。

## 开发与公共 API

- [双仓开发说明](module-split.md)：职责、安装组合、版本关系、独立构建与联合开发。
- [外观公共 API](appearance-api.md)：Gameplay 等扩展与外观包对接的接口和边界。
- [双仓迁移记录](repository-migration.md)：来源、保护范围和迁移时执行的检查。
- [测试源码](../tests/)：按功能组织的回归测试；运行要求见各测试入口与[双仓开发说明的验证部分](module-split.md#验证与后续)。独立 Java 测试不是 JUnit，不能仅凭 Gradle 构建成功判断全部测试已运行。
- [音频解码检查说明](../tests/audio/README.md)与[音频资产清单](magic-sound-assets.json)：素材核对与无播放解码检查。
- [项目待办](../TODO.md)：当前已知问题与后续工作。

## 历史报告与维修记录

报告按当时的代码与测试范围记录。第一条的两包构建和用户体验验收对应外观 0.3.3 / Gameplay 0.2.2，不覆盖本轮种族接入；多人及第三方光影兼容按各报告所列范围另行验证。

| 日期 | 记录 | 范围与阅读提示 |
| --- | --- | --- |
| 2026-09-10 | [蹲行动画随步速调整](reports/sneak-motion-20260910.md) | 慢蹲的连续播放时钟，不改动画资源；独立检查与本轮体验验收通过。 |
| 2026-09-10 | [独角兽飞行屏幕边缘光罩](reports/unicorn-flight-rim-20260910.md) | 已随慢蹲测试版构建；验收后按要求限制为第一人称，该小调整另待游戏确认。 |
| 2026-09-10 | [飞行、统一光效与玩法设置验收](reports/unified-magic-20260910.md) | 飞行姿态、通用光效、玩法表现与独立设置；两包构建完成，用户已确认本次体验并同意合入。 |
| 2026-09-10 | [起飞黑屏修复](reports/flight-blackout-20260910.md) | 光效与原版暗角的混色状态交接，含实际渲染链路复现与回归。 |
| 2026-09-10 | [悬浮与配声调整](reports/flight-polish-20260910.md)、[角光纹路调整](reports/horn-pattern-20260910.md)、[飞行初次检查](reports/flight-visuals-20260910.md) | 中间阶段记录；前腿最终姿态和光效以最新验收汇总为准。 |
| 2026-09-10 | [独立持物视觉 API 检查](reports/2026-09-10-remote-visual-api.md) | 外观 0.3.1 / Gameplay 0.2.0 的源码、逻辑与无窗口类转换检查；尚未完整构建或实机验收。 |
| 2026-09-10 | [双仓构建与启动](reports/2026-09-10-split-build.md) | 独立构建与开发客户端启动通过；用户已确认本轮体验验收，多人和普通启动器安装另待验证。 |
| 2026-09-10 | [双仓迁移检查](repository-migration.md) | 资源保全、源码/API 边界、回归和任务配置检查；文中“尚未完整构建”描述迁移检查当时的状态，后续构建另见上条。 |
| 2026-09-09 | [共享鬃毛纹理维修](mane-shared-texture-repair.md) | 纹理、六区遮罩、沿发流修订与离线检查；该文记录的修订当时尚未游戏内验收。 |
| 2026-09-08 | [表情系统游戏验收](face-runtime-check.md) | 眼型、表情与指定动作的限定范围验收；不覆盖后续新增自动注视、双客户端同步或完整身体编舞。 |
| 2026-09-08 | [颜色二级菜单游戏验收](color-submenus-runtime-check.md) | 展开、调色、滚动保持、保存与重启；未遍历全部语言、缩放和联机组合。 |
| 2026-09-08 | [款式缩略图维修](style-thumbnail-repair.md) | 固定参考配色、等比显示与离屏状态恢复；记录的是独立检查，完整游戏交互另行验收。 |
| 2026-09-08 | [表情基础维修与解耦](face-basic-repair.md) | 资源分工、眼型适配、控制器解耦和 Blockbench 编辑注意事项。 |
| 2026-09-07 | [鬃毛与尾巴调色游戏验收](mane-palette-runtime-check.md) | 调色、联动与重启保存；其中“切换锁后回到顶部”是当时的历史问题，9 月 8 日已在[二级菜单验收](color-submenus-runtime-check.md)中记录修复。 |
| 2026-08-15；9 月 7 日更新 | [款式数字编号迁移](pony-style-numeric-rename.md) | 编号对照与开发期配置处理；保留最初待办，不代表当前全部状态。 |

功能说明也可能附有局部检查结果，阅读时可对照记录日期、测试对象与后续修订。

## 规划与历史归档

- [独立外观同步服务备选](appearance-sync-future.md)：后续方案，当前没有实施该服务。
- [v1 宣传片构思草案](v1-trailer-draft.md)：2026-09-09 的讨论记录，尚未定稿。
- [文档与证据归档方案](development/documentation.md)：文档分类、报告格式与附件维护规则。

三族能力、成就系统的指南和设计资料统一从 [Gameplay 文档目录](https://github.com/Magical-Land-Official/Magical-Land-Gameplay/blob/1.20.1-Fabric/docs/README.md)进入。外观仓库中的旧玩法文档仅保留跳转，方便已有链接继续使用。

仓库收录精简报告、精选截图和可复用测试。原始日志、批量录屏、实验备份及个人配置保存在本地；需要分享的附件单独整理。历史记录中的本地路径供原作者查证。
