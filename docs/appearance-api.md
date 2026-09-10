# 外观 API v1

Magicaland Appearance 0.3.0 通过 `top.csituka.magicaland.api` 和 `top.csituka.magicaland.api.client` 提供公共接口。Gameplay 及其他扩展（Addon）依赖这些包；配置、渲染实现、动画状态和同步缓存均属于外观模组内部实现。

`ApiVersion` 位于主源码集，只依赖 Java 标准库，可在独立服务端安全查询。`.api.client` 下的接口仅供客户端使用：查询和注册操作必须在客户端线程执行，视觉接口必须在渲染线程执行。本 API 不授予玩法能力，也不提供可作为服务端判定依据的权威外观数据。

## 依赖与兼容性

API 版本与模组版本独立。`ApiVersion.requireCompatible(1, 0)` 要求已安装 API 的主版本为 1、次版本至少为 0，不满足时抛出明确异常；`isCompatible` 提供不抛异常的兼容性检查。API 次版本更新保持已有签名和语义，不兼容变更必须提升主版本。扩展的模组元数据也必须声明兼容的 Appearance 版本要求；运行时检查无法解决 API 本身未安装的问题。

发布坐标为 `top.csituka:magicaland-appearance:0.3.0`。编译时依赖带 `api` 分类标识（classifier）的产物，运行时依赖完整 Appearance 模组。扩展应与主模组使用相同的 Minecraft 1.20.1、Fabric 和映射版本。通过 Loom 引用重映射后的 API 产物，Loom 会将其中的 Minecraft 类型签名转换为扩展开发环境所用的命名空间。

```groovy
modCompileOnly "top.csituka:magicaland-appearance:0.3.0:api"
modRuntimeOnly "top.csituka:magicaland-appearance:0.3.0"
```

`api` JAR 仅用于编译。不要将它放入 `mods` 文件夹、通过 `include` 嵌套打包、合并打包（shade），或把其中的类复制进扩展。运行时由完整 Appearance JAR 提供唯一一份公共 API 及其实现。Appearance 现有的服务端同步功能也保留在这同一个完整 JAR 中。

API 产物包含 `top/csituka/magicaland/api/**` 下的全部类文件，包括嵌套枚举类。v1 具体包含 `ApiVersion`、`Registration`、`AppearanceSnapshot`、`Appearances`、`AppearanceOverrides`、`AppearanceOverrides$Visibility` 和 `AppearanceVisuals`。它不包含 `client/api` 内部桥接实现、`ModelConfig`、渲染内部类、网络类、Mixin 或资源。公共方法签名只使用 Java、Minecraft 或 API 自身的类型。`api-sources` 分类产物包含相应的公共源码；完整源码产物供主模组开发使用。

## 只读外观查询

`Appearances.find(UUID)` 返回 `Optional<AppearanceSnapshot>`。查询本地玩家时读取已应用的外观；即使编辑器中存在未提交草稿，也仍然读取已应用版本。查询远程玩家时读取当前客户端最后获知的外观。没有可用模型时返回 `Optional.empty()`，不接受 null UUID。

每次返回的结果都是不可变快照，包含以下字段：

| 字段 | 含义 |
| --- | --- |
| `modelReplacementEnabled` | 本地设置已启用模型替换；查询远程玩家时，还要求相关远程同步可用。此字段不检查实体是否可见、是否存活，也不代表玩法权限。 |
| `hasHorn` / `hasWings` | 已应用外观中关于角和翅膀的选择。 |
| `magicColor` | RGB 魔法颜色，不包含透明度通道。 |

`Appearances.magicColor(UUID)` 返回相同的魔法颜色；没有已知外观时沿用现有默认值 `0xAA00FF`。查询结果不会暴露可变配置对象、预设名称或底层映射表。已经保存的快照不会随外观更新而变化；需要最新的同步或已应用数据时，应重新查询。

## 持物可见性与注视覆盖

`AppearanceOverrides.registerMainHandVisibility(ownerId, priority, provider)` 和 `registerGaze(ownerId, priority, provider)` 返回 `Registration` 注册句柄。`ownerId` 是带命名空间的小写字符串，例如 `magicaland_gameplay:remote_tool`；命名空间应标识发起注册的扩展。同一 owner 可以在任一覆盖类别下拥有多条注册。`provider` 回调接收正在渲染的玩家 UUID。

优先级整数越大，越先采用；优先级相同时，先注册且仍有效的条目优先。回调按需求值，不缓存返回结果：

* 可见性回调返回 `DEFAULT` 或 null 时，交给下一条注册决定；`HIDDEN` 隐藏正常主手持物的视觉显示；`VISIBLE` 阻止较低优先级的隐藏覆盖。没有回调作出决定时，执行原有持物逻辑。本 API 不修改背包、手部动画或副手状态。
* 注视目标覆盖原有头部和眼睛的目标。null、已移除实体或其他世界中的实体均交给下一条回调处理。没有有效目标时，执行原有注视逻辑。
* 回调抛出 `RuntimeException` 时，该条注册会被撤销并记录一次日志，然后继续尝试较低优先级条目。对应句柄将报告注册已失效。

`registration.close()` 只撤销该句柄对应的注册，重复调用不会产生额外影响。`unregisterOwner(ownerId)` 撤销该 owner 在两类覆盖中的全部注册，保留其他 owner 的注册。撤销后立即恢复到下一条适用回调或原有行为，并释放已撤销的回调引用。扩展可以通过 `ownerId()`、`priority()` 和 `isRegistered()` 检查自己的注册句柄。

每次断线都会清空两类覆盖，并使尚未关闭的句柄失效。需要跨会话使用的扩展必须在 `ClientPlayConnectionEvents.JOIN` 中重新注册，并在 `DISCONNECT` 或功能关闭时关闭自己的句柄。无论断线回调先后顺序如何，关闭已失效句柄都是安全的。回调应根据 UUID 获取当前状态，并在世界切换时释放扩展自己持有的世界和实体引用。能力结束时返回 `DEFAULT` 或 null 即可恢复正常视觉表现，无需反复安装回调。

Gameplay 扩展在 JOIN 时注册当前远程工具查询与原持物隐藏判断，断线时关闭两个句柄，同时保留已有能力输入和同步流程。

## 视觉入口

`AppearanceVisuals.renderOrb(matrices, magicColor, ticks, seed)` 在调用方提供的变换下，调用现有魔法光团渲染器。

`renderGlowingItem(stack, mode, matrices, buffers, world, light, seed, magicColor)` 调用现有物品渲染及发光捕获逻辑。需要传入当前渲染上下文；底层预览渲染器支持无世界的场景时，`world` 可以为 null。调用方负责管理自己的变换栈。此入口不会重新定位物品或选择其他动画。

`renderFirstPerson(owner, camera, stack, buffers, renderCallback)` 临时向现有第一人称动画与持物渲染代码提供指定的 owner、camera 和 stack。调用顺序保持如下：

1. 打开临时持物视图。
2. 开始现有第一人称发光渲染阶段。
3. 同步执行回调。
4. 结束该阶段，完成原有缓冲刷新与光晕清理。
5. 恢复此前的持物视图。

即使回调或缓冲刷新抛出异常，也会恢复此前的视图。null 参数会在状态改变之前被拒绝。嵌套调用这个入口也会在修改外层渲染阶段之前被拒绝。不要在回调中开始或结束另一轮手部渲染，不要保留回调供稍后执行，也不要在手部渲染上下文之外使用这个入口。内部 `FirstPersonItemView` 仍属于实现细节，公共方法签名不暴露其作用域实现类型。

Gameplay 保留原有矩阵、插值、渲染种子、第一人称调用和渲染阶段顺序。其渲染器通过 `Appearances` 获取魔法色，手部 Mixin 则将已有渲染调用作为视觉入口的回调传入。

## 验证方式与范围

在 Appearance 仓库根目录执行 `node tests/api/run-api-tests.mjs`，环境需要可用的 Node 和 JDK 17 或更高版本。这是一组针对源码契约的独立测试，不会构建整个项目。测试使用轻量 Minecraft/Fabric 测试替身编译实际 API 与内部桥接源码，验证同优先级顺序、逐级回退、owner 与句柄清理、断线会话、无效注视目标、快照隔离、回调异常、刷新异常和第一人称状态恢复。还会仅使用公共 API JAR 与 Minecraft 测试替身编译外部调用示例，并检查公共字节码签名是否泄漏内部类型。

如果同级目录存在 Gameplay 仓库，测试还会扫描其 Java 源码，检查是否越过 API 边界引用 Appearance 内部实现。Gameplay 位于其他位置时，可将 `MAGICALAND_GAMEPLAY_REPO` 设置为该仓库路径以启用扫描。这些检查不能替代 Loom 构建、打包后 JAR 的启动检查或游戏内视觉验证。
