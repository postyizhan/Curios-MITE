# Curios-MITE

Curios 的 Minecraft 1.6.4 MITE / FishModLoader 移植项目，为 BountifulBaubles 提供饰品槽位和兼容 API。

## 当前状态

`0.1.0` 提供面向旧版 BountifulBaubles 的首批 Java API：可按需合并槽位类型，查询和操作实体饰品栏，并定义饰品装备回调。槽位默认为空，由内容模组注册。玩家存档持久化、物品合法性校验、容器 GUI、网络同步和游戏生命周期接入仍在移植中。

当前目录中的现代 Curios 源码（NeoForge 26.x）仅作为行为参考；接口设计优先兼容 BountifulBaubles 使用的旧版 Curios 契约。

## 构建

```bash
./gradlew build --no-daemon
```

需要 Java 17，以及 FishModLoader 的 Gradle 依赖仓库。

## 提交规范

项目使用中文约定式提交，例如：`fix: 修复戒指槽位索引问题`、`feat: 增加饰品栏持久化`、`docs: 更新移植路线`。
