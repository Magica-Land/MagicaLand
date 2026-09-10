<div align="center">

<img src="Resources\Icon\full-缩放.png"
        width ="45%">

# 魔法大陆 (Magical Land)  
将玩家和生物变成小马

[English](README.md) | 简体中文

</div>

## 模块与安装

外观与玩法现在由两个独立仓库维护，本仓库只发布外观主模组：

- **外观包 `magicaland-appearance`**：模型、动画、捏脸、预设、挑染、可爱标志、眼神、魔法光效和外观同步。模组 ID 保持 `magicaland`，不依赖玩法包；仍需 Fabric API 和 GeckoLib。
- **[Gameplay Addon `magicaland-gameplay`](https://github.com/Magical-Land-Official/Magical-Land-Gameplay)**：独立版本，通过公共 API 对接外观包。包含「不是这个意思！」金胡萝卜彩蛋和实验性念力出窍，其余三族能力继续在玩法仓库设计。

升级时移除旧一体包，避免与外观包重复安装。只要外观就安装外观包，要使用玩法则安装两包及各自依赖。外观包支持仅在客户端安装；没有同步支持的服务器上只能保证本地外观显示。服务器安装同一个外观 JAR 即可提供现有多人外观、动画和注视同步，不必安装 Gameplay，也不额外发布同步端。

两个仓库可以分别打开，也可以在同一个编辑器工作区并排打开。构建不依赖固定目录或另一个仓库的源码。见[双仓开发说明](docs/module-split.md)与[公共 API](docs/appearance-api.md)。

## 项目介绍
**魔法大陆（Magical Land）** 是一款《我的世界》 Java 版模组，致力于将动画剧集《小马宝莉：友谊是魔法》中的小马角色及相关内容融入游戏。

<p align="center">
    <img src="https://github.com/user-attachments/assets/9d5d3c8d-fb6e-4066-bb05-3bfdbf14ad2d"
        width ="45%">
    <img src="https://github.com/user-attachments/assets/be96510c-c7bc-45e5-a75e-e2c09c7155fb" 
        width ="45%">
    <p align="center">
        <sub><em> *暮光闪闪在我的世界之中* </em></sub>
</p>

## 愿景
- 以精致、还原的方式把小马们带入到《我的世界》的世界中来
- 逐步扩展高质量模型、材质、动画及新机制
- 打造一个可供游玩、学习与贡献的开放粉丝项目 


## 来一起开发
美术与创意是本项目的核心，但仍需扎实的程序基础才能完整落地
我们寻找对《小马宝莉》和《我的世界》都充满热情、愿意长期协作的伙伴，尤其需要以下方面的协助：
- 模组开发中的 Java 代码编写与调试；
- 实体行为与动画控制器的实现；
- 实现类似 pony.town 的模型自定义系统。

这主要是一个出于热爱的项目。我们目前还不知道它是否能够产生收益，因此主要目标是共同创造一些有意义的事物。我也希望通过这个项目，我们也能够结下真挚的友谊，互相学习，共同成长。  


## 加入方式
若你有意参与或协作：  
1. 在 Issues / Discussions 中分享想法、反馈 Bug 或提出功能建议。  
2. 希望深度合作？可通过以下方式联系我们
- QQ ：2026010008
- 电子邮件：w2026010008@outlook.com
- Discord：Mayhooves

---

期待与你开启一段愉快而难忘的合作之旅！
