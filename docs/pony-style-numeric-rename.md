# 小马发型/尾巴/眼睛风格标识符重构：角色缩写 → 数字编号

> 面向：负责程序的同事（本文档为 Claude Code 生成，可直接粘给另一个 Claude 会话读取上下文）
> 日期：2026-08-15；2026-09-07 更新 AJ 独立前发、08 丸子头后发；07 后发和 08 尾巴暂缓。
> 关联 plan 文件（本次会话的原始设计稿）：`C:\Users\20260\.claude\plans\abstract-doodling-allen.md`

## 1. 为什么改

模组原来用小马宝莉六位主角姓名缩写（`TS/RD/RR/PP/AJ/FS`）当"发型/尾巴/眼睛"风格的内部标识符，贯穿 Java 代码、模型骨骼名、动画绑定和玩家存档四层。因为以后会持续新增不再绑定这六位角色的原创/自定义发型，缩写命名法不可扩展（找不到不冲突的新缩写），所以改成数字编号。同时原来 UI 直接把裸代码（如 "RD"）糊给玩家看，这次也一并加了展示层，不管是内部代码还是角色名，都不再出现在玩家能看到的文字里。

## 2. 命名规则

- **内部 id 按部位独立计数**：每个部位都从 `"01"` 开始编号，暂缓款式可以保留空号，不能因此挤占下一套的编号。同一个 id 可以在不同部位代表不同款式，唯一键是 `(part, id)`，而不是单独的 `id`。
- **骨骼名前缀**：`"Style" + id`，如 `Style01FrontMane`、`Style03Tail02`。用 `Style` 这个词把"风格编号"和骨骼名里已有的"分段序号"（`Tail01`/`Tail02` 表示第几节）区分开。
- **RD/AJ 独立前发**：RD 保留 `Style02FrontMane`；AJ 新增 `Style05FrontMane`，保持原共用造型，仅轻微回收四处发束末端，沿用鬃毛贴图。FS 前发顺延为 `06`，使前六款的前发、后发、尾巴编号重新对应。
- **原创款式**：DeepSidePart=`Style07FrontMane*`、Undercut=`Style08FrontMane`、Bun=`Style08BackMane`、ShortSpikyTail=`Style07Tail*`。新后发 `07` 和尾巴 `08` 暂缓，不进入正式模型和选择列表。原创部件仍可独立混搭，不强制补齐套装。
- **玩家显示编号与部位内 id 一致**：显示真实 id，不用候选列表的顺位编号。当前后发为 `01–06、08`，丸子头必须显示"发型08"，不能显示为第七项"发型07"。

### 旧 6 个风格的完整对照表

| 旧缩写 | 前鬃毛 id | 后鬃毛 id | 尾巴 id | 眼睛 id |
|---|---|---|---|---|
| TS | 01 | 01 | 01 | 01 |
| RD | 02 | 02 | 02 | — |
| RR | 03 | 03 | 03 | 02 |
| PP | 04 | 04 | 04 | — |
| AJ | 05（独立轻变体） | 05 | 05 | — |
| FS | 06 | 06 | 06 | 03 |

## 3. 新增文件

`appearance/src/client/java/top/csituka/magicaland/client/config/style/`（新包，替代原来散落在 `ModelConfig`/`ManePage`/`FacePage`/`PonyRenderer` 里的 `Set`/数组/字符串判断）：

- `PonyStylePart.java`：枚举 `FRONT_MANE/BACK_MANE/TAIL/EYE`，携带骨骼名后缀 + 展示层通用名 lang key
- `PonyStyleDefinition.java`：单个部位款式的不可变数据（part、部位内 id）
- `PonyStyleRegistry.java`：按部位保存候选集和旧缩写映射；核心方法 `stylesFor(part)`、`byId(part, id)`、`legacyCodeToId(part, legacyCode)`、`isValidStyleId(part, id)`、`displayOrdinal(part, id)`、`DEFAULT_ID`

后发 `07`、尾巴 `08` 留待后续补全。新套装使用 `09` 起的对应编号；前发/后发不因暂缓空号而重新顺延。眼睛下一个是 `04`。

## 4. 改了哪些既有文件

| 文件 | 改动内容 |
|---|---|
| `ModelConfig.java` | 删除 `MANE_STYLES`/`EYE_STYLES` 两个 `Set`；4 个风格字段默认值从 `"TS"` 改为 `PonyStyleRegistry.DEFAULT_ID`；`sanitize()` 里的校验逻辑改用按部位查注册表，并内联了 legacy 缩写→新 id 的兼容翻译（见第 5 节） |
| `ModelManager.java` | `createModel()` 里 3 处 `"TS"` 字面量改用 `PonyStyleRegistry.DEFAULT_ID` |
| `PonyCustomPageHelper.java` | `createStyleButton` 从"字符串数组 + 拼接文字再反解析"改造成"按部位查注册表 + `CustomButton` 的 label/value 双段构造器"，按钮上显示"通用名+实际 id"（如"发型08"），不再显示角色名；删除了不再使用的 `getNextStyle` |
| `ManePage.java` / `FacePage.java` | 删除硬编码的 `FRONT_MANE_STYLES`/`BACK_MANE_STYLES`/`TAIL_STYLES`/`EYE_STYLES` 数组，改为在调用 `createStyleButton` 时传 `PonyStylePart` 常量，候选集完全由注册表按部位提供 |
| `PonyRenderer.java` | 所有发型/尾巴选择统一使用 `"Style" + 部位内id + part.boneSuffix`；眼睛使用 TS=`01`、RR=`02`、FS=`03`；删除了 Bun 强制隐藏等硬编码特例 |
| `appearance/src/main/resources/assets/magicaland/lang/en_us.json` / `zh_cn.json` | 各新增 3 条 key：`text.magicaland.style.generic.mane`（发型/Hairstyle）、`.tail`（尾巴/Tail）、`.eye`（眼睛/Eyes）。没有逐角色的显示名 key，因为玩家看到的文字本来就不含角色信息 |
| `appearance/src/main/resources/assets/magicaland/geo/mare_geo.json` / `Resources/Models/Mare.geo.json` | AJ 新增 `Style05FrontMane`；FS、DeepSidePart、Undercut 的前发分别使用 `Style06FrontMane*`、`Style07FrontMane*`、`Style08FrontMane`；原 07 丸子头后发改为 08，新 07 草案撤出，尾巴不变 |
| `Resources/BlockbenchProjects/Mare.bbmodel` | 同步迁移所有 group 和动画 animator 名称，保留 UUID、元素、贴图及关键帧，避免重新打开或导出时恢复旧命名 |
| `appearance/src/main/resources/assets/magicaland/animations/mare_animation.json` **以及** `Resources/Animations/mare_animation.json` | 动画绑定同步使用部位内编号，移除旧 Bun 在 13 段动作中的 scale=0 隐藏轨道；Bun 改为 `Style08BackMane`，不改其他动作轨道 |

## 5. 存档兼容策略

`ModelConfig.sanitize()` 里的 `sanitizeStyle(value, part)` 会先查 `PonyStyleRegistry.legacyCodeToId(part, value)`：命中旧缩写（"TS"/"RD"/…）就翻译成新 id，翻译不出来就当作已经是新格式交给 `isValidStyleId` 校验，两者都不满足才回退默认值。

**关键细节**：旧缩写按部位分别映射。AJ 前发/后发/尾巴均映射为 `05`；FS 前发/后发/尾巴均映射为 `06`，FS 眼睛仍映射为 `03`。

**2026-09-07 开发期调整**：经项目作者确认尚无玩家，不对无版本号的旧数字配置做猜测式自动迁移。原丸子头后发的开发测试配置应手动从 `07` 改选 `08`；`07` 新草案已撤出，选择 `07` 的测试配置将按无效款式规则回退 `01`，不做无版本的自动递增。已有开发测试配置如需保持原外观，手动把前发 `05→06`、`06→07`、`07→08`；原 `02` 仍是 RD。若原来用 `02` 搭配 AJ，需主动改选新 `05`。不要在每次 `sanitize()` 中递增数字，否则会反复改号。正式发布前应冻结编号或增加明确的配置版本。

这层兼容是**永久保留**的，不是"迁移一次就删除"：项目里没有发型 schema version 字段（新增的 `maneColorLinkVersion` 仅用于调色联动），且本地加载（`ModelManager`）、周期性 `sanitize`、远端同步（`ClientNetworkHandler.parseRemoteModel`）三个入口都统一流经 `ModelConfig.sanitize()`，在这一处做值翻译比每个入口各自记得调用迁移逻辑更稳妥。旧值（两字母大写）和新值（两位数字）值域完全不相交，判断无歧义。老存档加载后只要玩家有任何操作触发保存，就会自然落盘为新格式；不操作也不会出错，下次加载依然会被正确翻译。

错误的全局数字编号版本没有 schema version，部分数字与新局部编号含义重叠，无法自动无歧义迁移。如果该错误版本曾生成过存档，需要重新选择受影响的前发和眼睛款式。

## 6. 最初重构时的待办记录

以下保留最初重构时的记录，不代表 2026-09-07 的当前构建状态；本次 AJ 插入未新增游戏构建或启动。

1. **`AJHat`/`FSSmile`（→`Style05Hat`/`Style03Smile`）这两个骨骼改名后行为仍然未知**——它们当前没有被任何 Java 代码引用，不受风格切换/显隐/染色逻辑控制，这次只做了等价改名，没有新增任何逻辑。如果你们知道这两个骨骼原本该干什么，需要另外接入渲染逻辑。
2. **美术源贴图** `Resources/Textures/{TS,RD,RR,PP,AJ,FS}.png` 命名没动（不是打包资源，只是 Blockbench 工程参考文件），建议后续顺手改成 `Style01.png`~`Style06.png` 保持一致，非阻塞项。
3. **没有跑编译验证**：这个环境里没有 `gradlew` 脚本（仓库只带了 `gradle/wrapper/gradle-wrapper.properties`，没有 `gradlew`/`gradlew.bat`/wrapper jar），也没有全局 `gradle` 命令，所以没法在这里跑一次真正的 Fabric Loom 编译。我改完后做了这些替代验证：
   - 全项目 grep 确认 Java 源码里旧字面量（`"TS"`/`"RD"`/…）已清零，只剩注释和 legacy 映射表本身
   - 逐个改动文件通读了一遍确认类型/方法签名对得上（过程中发现并修复了一处遗漏：`PonyRenderer.renderRecursively` 里睡觉/蹲下隐藏眼睛骨骼的判断还写着旧骨骼名，已同步改成新名）
   - 用 Python 校验了所有改过的 JSON 文件（geo/animation/lang）语法合法
   - 用 Python 脚本做骨骼名重命名（精确按带引号的完整 token 替换，不是模糊正则），跑完后 grep 确认新旧骨骼名数量对得上、没有旧名字残留

   **请你在本地/IDE 里跑一次 `./gradlew build`（或对应的 Loom 任务）确认编译通过**，我这边无法替你跑这一步。

## 7. 建议的验证清单（游戏内手动测）

1. 新建存档，确认默认风格是 Style01 对应外观，落盘字段是 `"01"`。
2. Mane & Tail 页分别切换前鬃毛（8 项，01~08）、后鬃毛（7 项，01~06、08）和尾巴（7 项，01~07）。确认新 Style05 前发及顺延的 Style06/07/08 前发能够独立选择、显隐和染色，RD 原款保持不变。后发第七项必须显示真实编号 08。
3. Face 页切换眼睛风格（3 项：眼睛01~03），确认换脸骨骼互斥显示正确。
4. 用旧缩写配置验证 AJ 的前发/后发/尾巴均变为 `05`，FS 均变为 `06`，RD 前发仍为 `02`；重复 sanitize 不再改变已规范化的数字编号。旧数字开发配置按第 5 节手动重选。
5. Idle 状态下观察漂浮动画：AJ 对应部位的后鬃毛、PP 对应部位的尾巴、RR 对应部位的尾巴仍正常摆动，确认动画文件替换无遗漏。
6. 睡觉/蹲下时确认 Style03（原 FS）、Style02（原 RR）对应的换脸骨骼仍会正确隐藏。
7. 若条件允许，联机测试远端玩家旧存档同步渲染是否正确。

## 8. 07 / 08 后发调整与尾巴实验

07 分层短后发草案因大块方块和厚板感被否决，已从主 Blockbench 工程、两份几何和注册表撤出；编号保留，后续另行设计。08 保留原丸子头的形状、UV、pivot 与 UUID，仅调整编号并解除旧隐藏轨道。

圆团形 08 尾巴因过于球形、缺少盘团发束质感而搁置；仅保存在 `Resources/PolishLab_20260907/15_hair_sets_07_08/02_trial_tail08/`，没有加入正式选择列表。两项均已写入根目录 `TODO.md`。无本次游戏构建或 Git 推送。

前发/后发/尾巴的自动色阶、手动色阶锁及部位颜色联动见 [鬃毛与尾巴调色](mane-palette.md)。
