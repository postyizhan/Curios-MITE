# Curios-MITE

Curios 的 Minecraft 1.6.4 MITE / FishModLoader 移植项目，为 BountifulBaubles 提供饰品槽位和兼容 API。

## 当前状态

`ICurio` 还提供 `getFortuneBonus` 和 `getLootingBonus` 回调，并通过 MITE 的 `EnchantmentHelper` 汇总到原版附魔结果。逐格渲染状态列表会在读取和存档时恢复到槽位长度，缺失状态按显示处理。

`0.1.0` 提供按 MITE/FishModLoader 设计的 Java API：可按需合并槽位类型，通过槽位预设和注册消息创建槽位，通过 Curios helper 查询实体饰品栏、扁平装备栏、物品槽位标签和匹配指定谓词或物品的已装备饰品，并定义饰品装备、卸下、tick 和属性修饰符回调。`data/curios/tags/items/*.json` 会从 FishModLoader mod 根目录惰性加载，支持多文件合并、`replace` 和嵌套标签引用；FishModLoader 注册的物品 ID 用于关联标签，Java 注册标签 API 与资源标签合并。服务端装备写入和右键自动装备都会校验标签，未声明标签的物品视为通用饰品。饰品可选择启用右键自动装备；玩家交互流程会在调用物品自身右键方法前检查饰品，所以覆盖该方法的子类也能使用自动装备；服务端会选择首个可用槽位、扣除手持物品并同步背包。槽位注册保留优先级、图标、可见性、锁定和外观元数据；玩家 GUI 会忽略隐藏槽位。`CuriosApi.registerCurioProvider` 可按 ItemStack 创建 Curio 行为，替代 Forge item capability。handler 支持排队锁定/解锁和扩缩槽位，功能栈与外观栈的 `grow/shrink` 都会同步调整整个槽位；缩容时移除的物品会返还并触发对应属性和回调处理。饰品损坏后服务端会清除槽位并更新属性，持有者和观察客户端会收到损坏前的栈并触发 `ICurio.curioBreak`，随后同步更新后的库存。`ICurio.canSync/writeSyncData/readSyncData` 支持按饰品自定频率同步功能栈和外观栈的行为数据，未生成的客户端实体会暂存并在生成后应用；客户端会为两类栈调用 `curioAnimate`。玩家 GUI 可给启用 cosmetic 的槽位分配或取回外观物品；`CuriosClientApi.registerRenderer` 可为物品注册 `ICurioRenderer`，渲染回调会附着到头部、身体、背部、手臂或腿部模型。自动模型锚点目前适用于使用 `ModelBiped` 的实体，其他模型可通过 Curio 自定义渲染回调实现。槽位逐格渲染状态可通过 `ICurioStacksHandler.getRenders()` 读取和修改，并随实体 NBT 保存。服务端槽位写入会校验 Curio 类型和装备限制，触发回调、更新属性并同步玩家及观察者快照。实体饰品栏与外观栏会随实体 NBT 存档。MITE API 使用 `Optional` 和 `IDynamicStackHandler`，不包含 Forge 专属 capability/item-handler 类型。

旧版 BountifulBaubles Curios API 和当前目录中的现代 Curios 源码（NeoForge 26.x）仅作为功能与行为参考；接口和实现以 MITE/FishModLoader 的最佳适配为准，不承诺旧 API 或既有附属 mod 的兼容性。

## 构建

```bash
./gradlew build --no-daemon
```

需要 Java 17，以及 FishModLoader 的 Gradle 依赖仓库。

## 提交规范

项目使用中文约定式提交，例如：`fix: 修复戒指槽位索引问题`、`feat: 增加饰品栏持久化`、`docs: 更新移植路线`。
