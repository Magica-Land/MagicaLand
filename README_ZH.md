<div align="center">

<img src="Resources\Icon\full-缩放.png"
        width ="45%">

# 魔法大陆 (Magical Land)  
将玩家和生物变成小马

[English](README.md) | 简体中文

</div>

## 模块与安装

从 0.2.0 起，同一仓库分别产出两个模组：

- **外观包 `magicaland-appearance`**：模型、动画、捏脸、预设、挑染、可爱标志、眼神、魔法光效和外观同步。模组 ID 保持 `magicaland`，不依赖玩法包；仍需 Fabric API 和 GeckoLib。
- **玩法包 `magicaland-gameplay`**：依赖相同版本的外观包。包含「不是这个意思！」金胡萝卜彩蛋；0.2.1 开始接入实验性[念力出窍](docs/remote-presence.md)，其余三族能力仍在设计中。

升级时移除旧的一体包；只要外观就安装外观包，要保留彩蛋则安装两包。不要将旧一体包与新外观包同时安装。多人外观同步仍需要服务端支持，分包不等于已实现纯客户端兼容。

开发者打开仓库根目录即可，不需要两个工作区。目录、构建命令与测试组合见[分包开发说明](docs/module-split.md)。

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
