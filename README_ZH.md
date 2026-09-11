<div align="center">

<img src="Resources/Icon/full-缩放.png" alt="Magical Land 标志" width="45%">

# 魔法大陆 · 外观包

把自己的小马带进《我的世界》。

Minecraft Java 1.20.1 · Fabric · 开发中

[English](README.md) | 简体中文

</div>

**魔法大陆（Magical Land）** 是一款受《小马宝莉：友谊是魔法》启发的粉丝模组。你可以搭配小马造型、调整颜色、绘制可爱标志，并保存自己的角色预设。

外观包负责模型、捏脸、动画和外观同步，可以单独使用。三族能力、成就系统等玩法在独立的 [Gameplay Addon](https://github.com/Elysian-Herd-Studio/Magical-Land-Gameplay) 中开发。

[版本下载](https://github.com/Elysian-Herd-Studio/Magical-Land/releases) · [使用与开发文档](docs/README.md) · [问题反馈](https://github.com/Elysian-Herd-Studio/Magical-Land/issues)

## 角色自定义

- **造型搭配**：分别选择前发、后发、尾巴和眼型，鬃毛与尾巴可独立镜像。
- **身体与鬃毛配色**：选好主色后，会自动搭配高光色和阴影色，也可以解锁后自行调整。前发、后发、尾巴的颜色可联动或分别设置。
- **六区挑染**：前发、后发和尾巴各有六个可独立调色的区域，能自由搭配双色或多色挑染，同时保留原有的纹理和明暗层次。
- **眼部配色**：虹膜的两种颜色可以分别调整，同时保留原有渐变。展开高级选项，还能调整眼白、瞳孔和睫毛的颜色。
- **自绘可爱标志**：12×12 像素画布，提供画笔、橡皮、吸色、撤销和重做。左右两侧可共用图案，也可分别绘制。
- **角色预设**：切换、新建、复制、重命名和删除预设，方便在已有角色上尝试新的搭配。

小马会眨眼、偶尔抖动耳朵，并注视周围的实体；部分动作还配有对应表情。独角兽的角和悬浮物品周围有柔和流动的魔法光晕，伴有少量星点和轻柔的魔法音效。悬浮物品带有惯性，移动时还会留下魔法拖尾。

飞行时，有翼角色拍翼，无翼角色直立悬浮；无翼独角兽还有包身魔法光。飞行权限由原版或玩法模组提供，详见[飞行姿态说明](docs/flight-visuals.md)。

<p align="center">
  <img src="https://github.com/user-attachments/assets/9d5d3c8d-fb6e-4066-bb05-3bfdbf14ad2d" alt="暮光闪闪的小马模型展示" width="45%">
  <img src="https://github.com/user-attachments/assets/be96510c-c7bc-45e5-a75e-e2c09c7155fb" alt="暮光闪闪的另一视角展示" width="45%">
</p>

*早期游戏截图，展示模型与美术风格。*

## 安装与使用

当前支持 **Minecraft Java 1.20.1 / Fabric**，之后也可能支持 Forge / NeoForge。

当前开发版本：外观 **0.3.5**（API **1.5**），对应 Gameplay **0.3.3**。扩展可直接使用已有飞行表现，无需改变原版飞行权限。

安装外观包时需要以下依赖：

- Fabric Loader **0.19.1 或更高版本**；
- 适配 Minecraft 1.20.1 的 **Fabric API**；
- 适配 Minecraft 1.20.1 的 **GeckoLib 4.7 或更高版本**，当前开发使用 4.8.3。

版本文件与安装说明见 [Releases](https://github.com/Elysian-Herd-Studio/Magical-Land/releases)。将外观包和依赖的 JAR 放进游戏的 `mods` 文件夹。由旧一体包升级时，用新的外观包替换旧包。

在游戏主菜单右下角点击 **“捏马”**，进入独立的角色编辑器。默认按 **F9** 打开模组设置；安装 Mod Menu 后，也可从模组列表进入外观包设置。

如果隐藏过主菜单按钮，可在外观包的 **“设置 → 通用”** 中调整“主菜单捏马按钮”的显示方式。

左侧预览会实时显示编辑结果，可拖动旋转；选择部件时镜头会自动聚焦，也可手动关闭。预览使用固定照明，款式缩略图使用固定示例配色。具体操作见[捏脸界面说明](docs/customization-ui.md)。

编辑中的改动保存在草稿里。点击 **“保存并应用”** 后，角色外观和预设才会更新；退出时选择放弃，则恢复此前保存的状态。

## 多人与 Gameplay

| 安装方式 | 使用范围 |
| --- | --- |
| 客户端安装外观包及依赖 | 本地小马外观与捏脸。 |
| 客户端和服务器都安装外观包及依赖 | 多人外观、动画与注视同步；服务器使用同一个外观 JAR。 |
| 双端再安装兼容的 Gameplay Addon | 使用独立玩法，具体内容见 Gameplay 仓库。 |

多人互相看到自定义外观，需要服务器提供兼容的外观同步支持。Gameplay 使用外观包的公共 API，两包分别维护版本，安装组合以各版本说明为准。

Gameplay 的服务器规则可决定角色是否显示角和翅膀，不改写已保存的外观预设。种族选择和允许外观混搭的设置都由 Gameplay 管理；单独使用外观包时，仍保留玩家自己的选择。

## 开发进度与反馈

项目仍在开发中，欢迎试用和反馈。已知问题与后续计划见[项目清单](TODO.md)。

反馈问题时，请附上游戏和模组版本、是否安装 Gameplay、单人或多人环境、复现步骤，以及必要的截图或日志。公开日志前请检查其中的个人信息。

## 一起参与

我们希望继续打磨模型、手绘纹理、动画和捏脸体验。欢迎参与 Java 开发、模型与动画制作、美术、测试和翻译，也欢迎在 [Issues](https://github.com/Elysian-Herd-Studio/Magical-Land/issues) 分享想法。

开发资料：[双仓开发说明](docs/module-split.md) · [公共 API](docs/appearance-api.md)

- QQ：2026010008
- Discord：mayhooves
- 电子邮件：w2026010008@outlook.com

本项目为非官方粉丝创作。仓库许可见 [LICENSE.txt](LICENSE.txt)，素材信息见相应文档；[魔法音效来源记录](docs/magic-sounds.md)仍在补充。
