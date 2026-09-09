# 其余发型共享纹理维修

2026-09-09：为前发 02–08、后发 02–06/08、尾巴 02–07 整理灰度纹理与六区遮罩，共 19 个部件。01 的已确认 UV、灰度纹理与遮罩保持不变；07 后发和 08 尾巴继续搁置。

## 资源与复用

- 仍为一张 256×256 鬃毛图集，模型逻辑 UV 为 128×128；不提高分辨率，不更改造型、骨骼、pivot 或动画。
- 1566 个面映射到 1138 个纹理块，428 次面映射复用已有块。只允许明暗近似且同款色区不冲突的面复用；不同款式可以共用灰度像素而使用自己的遮罩。
- 图集中的既有身体预览区域及 01 区域被保护。新块含一像素边缘延展，避免采样到透明空隙。大块发片和细小厚度面按实际尺寸分配 UV，保持粗像素质感。
- 灰度先安排连贯的整体明暗和表面朝向，再叠加顺着各发束长轴的纤维细节；降低原有重复色块在手调阴影/高光时产生的杂乱感。手动极端色阶会放大差异，不能只在默认白色上验收。
- 六区沿发束的局部横向坐标排列，随方块方向转弯，不再使用固定世界空间平面切片。连续主卷传递同一横截面色序，小发片只继承所属的部分区间；不是每个小方块都重新铺一遍六色。遮罩烘焙到静止 UV 后随原骨骼运动，不根据灰度猜颜色。

正式资源：

- `Resources/Textures/MareMane.png` 与运行时 `textures/entity/mane.png`。
- `Resources/Models/Mare.geo.json`、运行时 `geo/mare_geo.json`、`Resources/BlockbenchProjects/Mare.bbmodel` 的相应 UV 同步。
- `Resources/Textures/BaseTextureBackup.png` 和工程内嵌副本仍然只负责合并预览；游戏继续分别使用 base 与 mane。
- `Resources/ManeDyes/style02.json` 至 `style08.json` 与运行时 `mane_dyes` 中同名文件一致。

不要删除 `Style07FrontManeHighlight` 整支骨骼：其中还包含有效的发片。这些发片现在也由普通灰度图和分区遮罩着色，不依赖旧的单独挑染颜色。

## 沿发流分区修订

本次只改色区的安排，保留上一轮灰度笔触，不重新画质感。对照修订前的 1566 个面，123 个面调整了 UV，其中 44 个主发束面为容纳六条可见色带进行了横向最近邻扩采样；其他面读取的 RGBA 像素保持一致。11 个冲突块拆开，兼容部分继续复用；挪动少量已有块腾出连片空间，图集仍为 256×256。01 及身体预览区域均受保护。

`Resources/ManeDyes/flow-layout.json` 保存 261 个发块的源坐标：`u = u + (local[axis] - center) * slope`，再分为六区。03/06/07 共用冠部发流，侧卷逐段传递横向色序；07 额外薄片继承底下主片的子区间。06 尾尖的横向坐标随方块侧转，避免尾尖沿长度依次变色。02/05 碎发及 04 短卷沿各自局部方向分区，08 丸子头沿其倾斜发块方向铺带。

维护工具（只需 Node.js，无额外依赖）：

```text
node tools/generate-mane-dye-masks.mjs --check
node tools/generate-mane-dye-masks.mjs --write
```

默认只检查；显式 `--write` 才同时导出源目录与运行时的 02–08 遮罩。工具会拒绝空色区、过期发块记录、同款同部件共享 UV 冲突和边缘冲突，不修改 01、模型或贴图。若改了 UV 或发块，先同步 `flow-layout.json`；发生冲突时拆开冲突 UV 后再导出。发流参数来自局部几何，不要改回根据世界 X/Z 自动切片。

前发、后发、尾巴的运行时独立镜像见 `docs/mane-mirroring.md`：镜像整个部件及其色带，不新增一份贴图或发型编号。

## 检查与验收

已经检查模型导出一致性、保护区域逐像素不变、19 个部件各有六区、全部面内采样覆盖和混搭不串色。离线模型预览覆盖正面、侧面、背面及两个斜视角；这些不是实机截图。

沿发流修订后的正式资源严格检查通过 150,434 项：1566 个面、14882 个面内采样与分区坐标一致，53 次同款同部件 UV 复用无冲突，223 个非目标元素及 32234 个保护像素不变。遮罩生成工具逐字节复现 14 份源/运行时 JSON；六区路由回归与镜像/预设/骑乘回归通过，29 个修改或新增生产类联合 Java 17 类型检查通过。完成了 35 个离线视角检查；本次修订尚未完整构建或进行游戏内验收。

游戏内重点检查：六区全部设为异色、相邻区域同色、关闭挑染、前后尾混搭、手动阴影/高光、保存重载和联机。旧版客户端不了解这些遮罩，联机请使用相同新版。

## 纤维素材来源

本次使用内置 imagegen 生成灰度纤维源，随后降采样并按模型朝向、发流和现有三阶色调范围装配到图集；没有让生成工具重绘整个角色或覆盖 01。最终项目位图保存在上述 `MareMane.png` / `mane.png`，Blockbench 内亦有同步副本。

生成提示词：

> Use case: stylized-concept. Asset type: tileable grayscale pixel-art hair texture module for an existing Blockbench Minecraft cartoon pony model. The reference is the current shared grayscale hair atlas; it is a STYLE REFERENCE ONLY, do not redraw the atlas or draw a pony. Generate ONE completely filled square swatch of loosely grouped flowing vertical mane fibers, suitable for mapping root to tip on many differently shaped hair chunks. Actual use is a very low-resolution 32 by 32 pixel texture swatch, so make the art deliberately chunky pixel art, not high-detail fur. Monochrome neutral grays only, mostly very light gray 225–250/255 with a few controlled darker 200–220 grooves. Noticeable yet restrained long irregular strand strokes, occasional 1-pixel breaks, grouped soft wide highlight ribbons interrupted by narrow flow-aligned creases. The hair must retain hand-painted textured character, NOT a smooth gradient, NOT random checkerboard noise, NOT wood planks, NOT metallic or PBR. Across the full square, fibers run top to bottom with only slight bends, reach all borders, and vary gently in width; no individual hair silhouettes or empty background. Broad overall brightness is even so the model-specific lighting can be applied separately later. Seamless left/right and top/bottom as much as possible. No border, grid, labels, text, decorative stars, logos, color accents, or transparency. It will be nearest sampled at low resolution, so do not add microdetail.
