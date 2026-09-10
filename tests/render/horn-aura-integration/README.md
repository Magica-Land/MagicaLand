# 连续魔法光纹理批次的混合回归

在隐藏 OpenGL 窗口中使用真实 Minecraft `RenderLayer.draw → ShaderProgram.bind → GlBlendState`，连续绘制两个不同纹理的光效层。普通与 Fabulous 路径都检查第二批命中同一 shader 混合缓存时，透明度仍与第一批相同。

测试包含旧 `LIGHTNING_TRANSPARENCY` 的负对照：第二批透明度约从 0.475 降至 0.224；生产 `MagicGlow` 的专用透明状态应使两批保持约 0.475。另检查 RGB、GPU 错误及实际缓存身份。

只替换客户端服务入口、纹理查找、世界 framebuffer 引用和物品渲染器的两个纹理常量，避免初始化完整游戏。RenderPhase、RenderLayer、ShaderProgram、GlBlendState、BufferBuilder、Framebuffer 和 GPU 绘制均为实际实现；shader 使用仓库文件。

运行 `run-tests.ps1` 时提供：

- `ClasspathFile`：分号分隔的真实 Minecraft/Fabric 依赖列表。
- `MinecraftJar`：包含原版 shader 资源的 Minecraft 客户端 jar。
- `WidenedMinecraftJar`：应用当前项目 access widener 后的客户端 jar，可由 `../body-aura-integration/PrepareTestMinecraft.java` 生成。
- `AppearanceClasses`：外观包当前已编译类目录，用于解析生产 `MagicGlow` 的其他依赖。测试会重新编译仓库中的 `MagicGlow.java`。
- `JavaBin` 与全新的 `OutputDirectory`：可选，分别指定 Java 路径和日志/测试产物目录。

此测试不启动 Minecraft，不操作现有游戏窗口或存档。
