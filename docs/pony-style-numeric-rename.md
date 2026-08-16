# 小马发型/尾巴/眼睛风格标识符重构：角色缩写 → 数字编号

> 面向：负责程序的同事（本文档为 Claude Code 生成，可直接粘给另一个 Claude 会话读取上下文）
> 日期：2026-08-15
> 关联 plan 文件（本次会话的原始设计稿）：`C:\Users\20260\.claude\plans\abstract-doodling-allen.md`

## 1. 为什么改

模组原来用小马宝莉六位主角姓名缩写（`TS/RD/RR/PP/AJ/FS`）当"发型/尾巴/眼睛"风格的内部标识符，贯穿 Java 代码、模型骨骼名、动画绑定和玩家存档四层。因为以后会持续新增不再绑定这六位角色的原创/自定义发型，缩写命名法不可扩展（找不到不冲突的新缩写），所以改成数字编号。同时原来 UI 直接把裸代码（如 "RD"）糊给玩家看，这次也一并加了展示层，不管是内部代码还是角色名，都不再出现在玩家能看到的文字里。

## 2. 命名规则

- **内部 id**：两位零填充数字字符串 `"01"`~`"06"`，对应旧缩写 `TS=01 RD=02 RR=03 PP=04 AJ=05 FS=06`；未来新增原创发型从 `"07"` 起，编号永不回收复用。
- **骨骼名前缀**：`"Style" + id`，如 `Style01FrontMane`、`Style03Tail02`。用 `Style` 这个词把"风格编号"和骨骼名里已有的"分段序号"（`Tail01`/`Tail02` 表示第几节）区分开。
- **RD/AJ 共享前发**：这两个角色的前鬃毛本来就是同一份模型资源（旧骨骼名 `"RD/AJFrontMane"`），所以**前鬃毛（FRONT_MANE）这个部位的可选 id 只有 5 个：`01/02/03/04/06`，不含 `05`**——`02` 直接代表这份共享资源，骨骼改名为 `Style02FrontMane`，不需要复合命名。后鬃毛（BACK_MANE）、尾巴（TAIL）两个部位各角色资源互不相同，仍保留完整 6 个 id。
- **玩家看到的编号是"这个下拉里排第几个"，不是内部 id**：前鬃毛的 5 个候选项按顺位显示为"发型01~发型05"（连续、不跳号），第 5 项内部实际 id 是 `06`，只是在前鬃毛这个候选列表里排第 5 位——**这是预期设计，不是漏了一项**。后鬃毛/尾巴的顺位和内部 id 天然一致（都是完整 6 项）。眼睛风格只有 3 个候选项（对应旧 TS/RR/FS），显示为"眼睛01/02/03"。玩家全程看不到角色名，也看不到内部 id/骨骼名。

### 6 个风格的完整对照表

| 旧缩写 | 新内部 id | 前鬃毛可选？ | 前鬃毛显示编号 | 后鬃毛/尾巴显示编号 | 眼睛可选？ |
|---|---|---|---|---|---|
| TS | 01 | 是 | 发型01 | 01 | 是（眼睛01） |
| RD | 02 | 是（含共享资源） | 发型02 | 02 | 否 |
| RR | 03 | 是 | 发型03 | 03 | 是（眼睛02） |
| PP | 04 | 是 | 发型04 | 04 | 否 |
| AJ | 05 | **否**（并入02） | — | 05 | 否 |
| FS | 06 | 是 | 发型05（顺位第5，非06） | 06 | 是（眼睛03） |

## 3. 新增文件

`src/client/java/top/csituka/magicaland/client/config/style/`（新包，替代原来散落在 `ModelConfig`/`ManePage`/`FacePage`/`PonyRenderer` 里的 `Set`/数组/字符串判断）：

- `PonyStylePart.java`：枚举 `FRONT_MANE/BACK_MANE/TAIL/EYE`，携带骨骼名后缀 + 展示层通用名 lang key
- `PonyStyleDefinition.java`：单个风格的不可变数据（id、legacy 缩写、该风格在哪些部位可选、legacy 缩写在个别部位的 id 覆盖——AJ 在 FRONT_MANE 部位覆盖成 `02`）
- `PonyStyleRegistry.java`：唯一数据源，6 个 `register(...)` 调用；核心方法 `stylesFor(part)`（按部位返回候选集）、`byId(id)`、`legacyCodeToId(part, legacyCode)`、`isValidStyleId(part, id)`、`displayOrdinal(part, id)`、`DEFAULT_ID`

未来加一个原创发型，只需要在这里加一行 `register("07", null, ...)`，不需要再碰其余任何文件的候选列表逻辑。

## 4. 改了哪些既有文件

| 文件 | 改动内容 |
|---|---|
| `ModelConfig.java` | 删除 `MANE_STYLES`/`EYE_STYLES` 两个 `Set`；4 个风格字段默认值从 `"TS"` 改为 `PonyStyleRegistry.DEFAULT_ID`；`sanitize()` 里的校验逻辑改用按部位查注册表，并内联了 legacy 缩写→新 id 的兼容翻译（见第 5 节） |
| `ModelManager.java` | `createModel()` 里 3 处 `"TS"` 字面量改用 `PonyStyleRegistry.DEFAULT_ID` |
| `PonyCustomPageHelper.java` | `createStyleButton` 从"字符串数组 + 拼接文字再反解析"改造成"按部位查注册表 + `CustomButton` 的 label/value 双段构造器"，按钮上显示"通用名+顺位编号"（如"发型01"），不再显示任何代码/角色名；删除了不再使用的 `getNextStyle` |
| `ManePage.java` / `FacePage.java` | 删除硬编码的 `FRONT_MANE_STYLES`/`BACK_MANE_STYLES`/`TAIL_STYLES`/`EYE_STYLES` 数组，改为在调用 `createStyleButton` 时传 `PonyStylePart` 常量，候选集完全由注册表按部位提供 |
| `PonyRenderer.java` | 所有 `boneName.startsWith(style + "FrontMane")` 之类的字符串前缀判断，改成统一的 `bonePrefix(styleId, part)` = `"Style" + id + part.boneSuffix`；删除了硬编码的 RD/AJ 特例分支（`frontStyle.equals("RD") || frontStyle.equals("AJ")`），因为前鬃毛候选集里已经没有 `05`，公式天然覆盖了共享骨骼的情况；`shouldRenderSelectedEye` 的 `switch` 从 `case "FS"/"RR"` 改成 `case "06"/"03"`；**顺带修复了一处遗漏**——`renderRecursively` 里睡觉/蹲下时隐藏眼睛骨骼的判断还硬编码着旧骨骼名 `"FSCommonFace"`/`"RRCommonFace"`，一并改成了 `"Style06CommonFace"`/`"Style03CommonFace"`（这处如果不改，改完骨骼名之后睡觉动画会失效） |
| `src/main/resources/assets/magicaland/lang/en_us.json` / `zh_cn.json` | 各新增 3 条 key：`text.magicaland.style.generic.mane`（发型/Hairstyle）、`.tail`（尾巴/Tail）、`.eye`（眼睛/Eyes）。没有逐角色的显示名 key，因为玩家看到的文字本来就不含角色信息 |
| `src/main/resources/assets/magicaland/geo/mare_geo.json` | 51 个骨骼名批量重命名（含 1 处合并：`"RD/AJFrontMane"` → `"Style02FrontMane"`；2 处保持原样只改名的例外：`AJHat`→`Style05Hat`、`FSSmile`→`Style06Smile`），共 86 处字符串替换（每个骨骼名的 `"name"` 字段 + 所有引用它的子骨骼 `"parent"` 字段都同步改了） |
| `src/main/resources/assets/magicaland/animations/mare_animation.json` **以及** `Resources/Animations/mare_animation.json`（两份内容一致的副本，都改了） | 替换 4 个漂浮动画绑定的骨骼名引用：`AJBackMane01`→`Style05BackMane01`（1处）、`RRTail01`→`Style03Tail01`（18处）、`PPTail01`→`Style04Tail01`（15处）、`PPTail02`→`Style04Tail02`（15处），每份文件 49 处替换 |

## 5. 存档兼容策略

`ModelConfig.sanitize()` 里的 `sanitizeStyle(value, part)` 会先查 `PonyStyleRegistry.legacyCodeToId(part, value)`：命中旧缩写（"TS"/"RD"/…）就翻译成新 id，翻译不出来就当作已经是新格式交给 `isValidStyleId` 校验，两者都不满足才回退默认值。

**关键细节**：同一个旧缩写 `"AJ"` 在不同字段要翻译成不同的新 id——`frontManeStyle` 字段里的 `"AJ"` 要翻译成 `"02"`（AJ 前发本来就和 RD 共用资源），而 `backManeStyle`/`tailStyle` 字段里的 `"AJ"` 要翻译成 `"05"`（后鬃毛/尾巴 AJ 有自己独立资源）。这靠 `PonyStyleDefinition` 里 AJ 定义的 `legacyIdOverrideByPart = {FRONT_MANE: "02"}` 实现，其余风格没有覆盖、直接用自己的 id。

这层兼容是**永久保留**的，不是"迁移一次就删除"：项目里没有 schema version 字段，且本地加载（`ModelManager`）、周期性 `sanitize`、远端同步（`ClientNetworkHandler.parseRemoteModel`）三个入口都统一流经 `ModelConfig.sanitize()`，在这一处做值翻译比每个入口各自记得调用迁移逻辑更稳妥。旧值（两字母大写）和新值（两位数字）值域完全不相交，判断无歧义。老存档加载后只要玩家有任何操作触发保存，就会自然落盘为新格式；不操作也不会出错，下次加载依然会被正确翻译。

## 6. 还没做 / 需要你确认或配合的事

1. **`Resources/BlockbenchProjects/Mare.bbmodel` 工程文件没有改**，我这次只改了打包资源 `mare_geo.json` 和两份动画 json。如果后续有人用 Blockbench 重新打开这个工程文件并另存/导出，骨骼名会以工程里记录的旧名字（`TSFrontMane` 等）覆盖回 `mare_geo.json`，抵消这次重命名。**需要美术在 Blockbench 里把工程内骨骼（outliner 分组）逐一改名成新命名后保存，再重新导出**，而不是让这次改的 json 单方面生效。
2. **`AJHat`/`FSSmile`（→`Style05Hat`/`Style06Smile`）这两个骨骼改名后行为仍然未知**——它们当前没有被任何 Java 代码引用，不受风格切换/显隐/染色逻辑控制，这次只做了等价改名，没有新增任何逻辑。如果你们知道这两个骨骼原本该干什么（比如只有选中 Style05/06 时才显示），需要另外接入渲染逻辑，不在本次改动范围内。
3. **美术源贴图** `Resources/Textures/{TS,RD,RR,PP,AJ,FS}.png` 命名没动（不是打包资源，只是 Blockbench 工程参考文件），建议后续顺手改成 `Style01.png`~`Style06.png` 保持一致，非阻塞项。
4. **没有跑编译验证**：这个环境里没有 `gradlew` 脚本（仓库只带了 `gradle/wrapper/gradle-wrapper.properties`，没有 `gradlew`/`gradlew.bat`/wrapper jar），也没有全局 `gradle` 命令，所以没法在这里跑一次真正的 Fabric Loom 编译。我改完后做了这些替代验证：
   - 全项目 grep 确认 Java 源码里旧字面量（`"TS"`/`"RD"`/…）已清零，只剩注释和 legacy 映射表本身
   - 逐个改动文件通读了一遍确认类型/方法签名对得上（过程中发现并修复了一处遗漏：`PonyRenderer.renderRecursively` 里睡觉/蹲下隐藏眼睛骨骼的判断还写着旧骨骼名，已同步改成新名）
   - 用 Python 校验了所有改过的 JSON 文件（geo/animation/lang）语法合法
   - 用 Python 脚本做骨骼名重命名（精确按带引号的完整 token 替换，不是模糊正则），跑完后 grep 确认新旧骨骼名数量对得上、没有旧名字残留

   **请你在本地/IDE 里跑一次 `./gradlew build`（或对应的 Loom 任务）确认编译通过**，我这边无法替你跑这一步。

## 7. 建议的验证清单（游戏内手动测）

1. 新建存档，确认默认风格是 Style01 对应外观，落盘字段是 `"01"`。
2. Mane & Tail 页分别切换前鬃毛（应循环 5 次：发型01~05，不跳号）、后鬃毛、尾巴（各循环 6 次：01~06）。确认前鬃毛切到"发型02"时和老版本 RD/AJ 共用的那份前发视觉一致。
3. Face 页切换眼睛风格（3 项：眼睛01~03），确认换脸骨骼互斥显示正确。
4. 手动构造一份旧格式存档 JSON（`frontManeStyle: "AJ"`、`backManeStyle: "AJ"`、`tailStyle: "AJ"`）丢进 `%config%/magicaland/ponies/`，启动游戏确认能正确加载外观（不是被重置成默认值）；重点核对 `frontManeStyle` 从 `"AJ"` 迁移后变成 `"02"`，而 `backManeStyle`/`tailStyle` 从 `"AJ"` 迁移后变成 `"05"`——两者不同，是本次合并编号后最容易出错的一步。
5. Idle 状态下观察漂浮动画：AJ 对应部位的后鬃毛、PP 对应部位的尾巴、RR 对应部位的尾巴仍正常摆动，确认动画文件替换无遗漏。
6. 睡觉/蹲下时确认 Style06（原FS）、Style03（原RR）对应的换脸骨骼仍会正确隐藏（这是本次顺手修的那处遗漏，建议重点测一下）。
7. 若条件允许，联机测试远端玩家旧存档同步渲染是否正确。
