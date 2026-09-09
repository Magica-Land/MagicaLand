# 手持物柔边魔法光

## 实现

- 原物品只画一次，同时记录实际提交的四边形、法线和纹理 UV。模型覆写、方块、扁平道具及特殊 renderer 的有纹理四边形都沿用原形状；附魔层不重复画。
- 在物品局部空间做六层外扩，越向外越淡，不生成笼统的球形包围壳。2026-09-09 将离原表面的扩张距离统一乘 1.65，不是将物品本体或整个包围盒放大 65%；层数、透明度和原 UV 不变。shader 取原纹理 alpha，花、工具等透明背景与内部空洞不会成为不透明矩形。
- 和角部复用 `horn_aura` shader：`UV1.x` 是循环时间，`UV1.y=2` 表示物品剪影，`UV2.y` 保存归一化物品高度，因此流动不需要滚动图集 UV。角部与星星原分支保持不变。
- 每 3–4 秒出现一批 2–4 颗小星星，独立于帧率；数量、寿命沿用 `MagicSparkles`，尺寸随道具与预览比例缩放，本次不增加单颗大小。

## 绘制顺序

世界物品和角部一起排队到 `AFTER_TRANSLUCENT`。普通模式使用主目标、只写颜色；Fabulous 使用 item/entity 目标并写入供透明合成排序使用的深度。前景深度测试始终保留。

第一人称必须在两只手都画完后再提交光晕：外层 `HeldItemRenderer.renderItem(float, MatrixStack, Immediate, ClientPlayerEntity, int)` 开头调用 `GlowingItem.beginFirstPersonPass()`，结束调用 `GlowingItem.endFirstPersonPass(buffers)`。这次队列独立于世界渲染阶段，保留第一人称投影，不把光晕带入下一帧。

捏脸道具预览调用：

```java
GlowingItem.renderPreviewWithGlow(renderer, stack, mode, matrices,
        buffers, world, light, seed, glowColor);
```

接口自行画原物品、提交对应缓冲，然后画光晕。调用前可设置预览光照颜色；魔法光仍保持自身发光颜色。

## 边界与检查

每个物品最多记录 8192 个顶点，限制只影响附加光晕，绝不截断原物品。没有可解析纹理或不采用标准实体四边形格式的第三方渲染层保留原样，不猜测错误的遮罩。地图等不经过普通物品模型渲染的特殊路径不额外改动。

新增几何与 GPU 测试检查左右手、GUI 缩放、UV 保持、透明角与内部孔洞、局部流动和前景遮挡；原角部及 Fabulous 排序测试继续执行。仍需在游戏内验收方块、剑、花／工具、双手持物以及预览效果。
