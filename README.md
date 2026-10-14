# Curios-MITE

Curios 的 Minecraft 1.6.4 MITE / FishModLoader 移植项目，为 BountifulBaubles 提供饰品槽位和兼容 API。

## 当前状态

`0.1.0` 提供面向旧版 BountifulBaubles 的首批 Java API：可按需合并槽位类型，通过旧版槽位预设和注册消息创建槽位，通过 Curios helper 查询实体饰品栏和扁平装备栏，并定义饰品装备、卸下、tick 和属性修饰符回调。槽位注册保留优先级、图标、可见性、锁定和外观元数据；玩家 GUI 会忽略隐藏槽位。handler 支持排队锁定/解锁和扩缩槽位，移除的装备会返还并更新属性及回调。玩家 GUI 可给启用 cosmetic 的槽位分配或取回外观物品；其外观模型渲染仍未实现。服务端槽位写入会校验 Curio 类型和装备限制，触发回调、更新属性并同步玩家快照。实体饰品栏与外观栏会随实体 NBT 存档。MITE API 使用 `Optional` 和 `IDynamicStackHandler`，不包含 Forge 专属 capability/item-handler 类型；观察者同步和通用内容物品合法性策略仍在移植中。

当前目录中的现代 Curios 源码（NeoForge 26.x）仅作为行为参考；接口设计优先兼容 BountifulBaubles 使用的旧版 Curios 契约。

## 构建

```bash
./gradlew build --no-daemon
```

需要 Java 17，以及 FishModLoader 的 Gradle 依赖仓库。

## 提交规范

项目使用中文约定式提交，例如：`fix: 修复戒指槽位索引问题`、`feat: 增加饰品栏持久化`、`docs: 更新移植路线`。
