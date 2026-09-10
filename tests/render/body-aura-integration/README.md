# 包身魔法光渲染回归

此测试在隐藏 OpenGL 窗口中运行当前生产 `BodyFlightAura`，使用真实 Minecraft `ShaderProgram`、`Framebuffer` 和 `BufferRenderer`，接着绘制原版暗角、最终画面及 HUD 四边形。Fabulous 分支还运行真实 `PostEffectPass`、`JsonEffectShaderProgram` 和原版 `transparency.fsh`。客户端容器及窗口尺寸以小桩替代，不启动游戏或运行 Gradle。

```powershell
./tests/render/body-aura-integration/run-tests.ps1 -ClasspathFile <依赖类路径文件>
```

需要 JDK 17+、OpenGL 3.3，以及与当前源码匹配的 named Minecraft/Fabric/LWJGL 依赖和本机 LWJGL natives。类路径文件为一行，使用系统类路径分隔符；可通过 `-JavaBin` 指定 JDK、`-MinecraftJar` 指定其中的 Minecraft client jar。脚本将当前 access widener 应用于临时副本，原依赖保持不变；`-OutputDirectory` 可指定一个尚不存在的结果目录。

覆盖首次分配、连续绘制、停止光效后的帧、分辨率变化及降采样、普通/Fabulous 目标切换。逐帧检查混合缓存的精确引用（包括 null）、活动纹理/程序/缓冲区恢复、暗角混合参数，以及世界和后续 HUD 像素。

Fabulous 实体目标沿用真实 `SimpleFramebuffer` 的 8-bit alpha：检查低透明度的包身填充仍有非零 alpha，且经原版透明合成后确实改变世界中心像素。只检查目标 RGB 会漏掉 alpha 平方后量化为零、被 `try_insert` 丢弃的缺陷；关闭光效后的合成像素必须恢复原色。

2026-09 的黑屏缺陷来自光效结束后遗留的 `GlBlendState.activeBlendState`，它会让原版暗角改用普通 alpha 混合，覆盖世界画面。测试先通过真实着色器建立正常的 enabled 缓存；删去生产代码的缓存恢复会触发失败。

边界：测试不加载 Fabric 事件回调，资源工厂仅移除着色器 JSON 名称中的模组命名空间，以替代命名空间加载 mixin；着色器源码和混合定义保持原样。Fabulous 使用真实实体目标和原版透明合成，其余透明层为空；云层前后排序另由 `MagicCloudOrderingTest` 覆盖。
