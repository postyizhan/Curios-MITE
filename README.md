# Curios-MITE

Curios 的 Minecraft 1.6.4 MITE / FishModLoader 移植项目，为 BountifulBaubles 提供饰品槽位和兼容 API。

## 当前状态

`0.1.0` 提供面向旧版 BountifulBaubles 的首批 Java API：可按需合并槽位类型，通过旧版槽位预设和注册消息创建槽位，通过 Curios helper 查询实体饰品栏、扁平装备栏、物品槽位标签和匹配指定谓词或物品的已装备饰品，并定义饰品装备、卸下、tick 和属性修饰符回调。内容 mod 可用 `CuriosApi.registerCurioTag` 声明物品适用槽位；服务端装备写入和右键自动装备都会校验标签，未声明标签的物品视为通用饰品。饰品可选择启用右键自动装备；玩家交互流程会在调用物品自身右键方法前检查饰品，所以覆盖该方法的子类也能使用自动装备；服务端会选择首个可用槽位、扣除手持物品并同步背包。槽位注册保留优先级、图标、可见性、锁定和外观元数据；玩家 GUI 会忽略隐藏槽位。`CuriosApi.registerCurioProvider` 可按 ItemStack 创建 Curio 行为，替代 Forge item capability。handler 支持排队锁定/解锁和扩缩槽位，功能栈与外观栈的 `grow/shrink` 都会同步调整整个槽位；缩容时移除的物品会返还并触发对应属性和回调处理。耐久损坏通知会清除对应槽位、属性并触发卸下回调。玩家 GUI 可给启用 cosmetic 的槽位分配或取回外观物品；其外观模型渲染仍未实现。槽位逐格渲染状态可通过 `ICurioStacksHandler.getRenders()` 读取和修改，并随实体 NBT 保存。服务端槽位写入会校验 Curio 类型和装备限制，触发回调、更新属性并同步玩家及观察者快照。实体饰品栏与外观栏会随实体 NBT 存档。MITE API 使用 `Optional` 和 `IDynamicStackHandler`，不包含 Forge 专属 capability/item-handler 类型；标签 JSON 数据包加载仍未实现。

当前目录中的现代 Curios 源码（NeoForge 26.x）仅作为行为参考；接口设计优先兼容 BountifulBaubles 使用的旧版 Curios 契约。

## 构建

```bash
./gradlew build --no-daemon
```

需要 Java 17，以及 FishModLoader 的 Gradle 依赖仓库。

## 提交规范

项目使用中文约定式提交，例如：`fix: 修复戒指槽位索引问题`、`feat: 增加饰品栏持久化`、`docs: 更新移植路线`。
