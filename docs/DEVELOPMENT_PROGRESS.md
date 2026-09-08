# 当前状态

当前发行版本：`1.7`；既有第 15 批功能完成，当前仅进行明确的新内容、平衡与维护更新。

## 维护：肩部背包快捷操作、原版槽位与翅膀调值（2026-09-08）

实现：行情书“炒币”页删除“价格范围”和“商人买卖手续费”显示，仅保留绿宝石今日/昨日现货价、涨跌和独立 30 日曲线；商人页的 4% 服务端结算不变。肩部槽修正为原版 `inventory.png` 盔甲格 `(7,7)` 的完整 18×18 裁取，取代错误的合成格素材；背包 GUI 显式调用原版 Tooltip 渲染。三种背包图标替换为简约、清晰、16×16 RGBA 且全部像素不透明的版本。

新增客户端可改键的默认 B：仅在无界面或原版玩家背包界面、且客户端已同步肩部背包时才发送既有 Packet 10。服务端仍要求真实 `inventoryContainer` 和肩部 `ItemBackpack`，故不存在客户端打开任意库存的路径。稳定 ID `feather_wings`、PlayerData、背包 NBT、WorldSavedData schema、GUI ID、Packet ID/编码均不改；显示名改为“翅膀”/“Wings”，飞行耐久改为每 20 tick 尝试 1 点，`MarketCatalog` 与 `TradeCatalog` 的珍宝基础价统一为 2,888,888，既有行情/Offer 不重写。

验证：Temurin Java 8 `1.8.0_504` 下 `check_toolchain.py` 为 0 error/0 warning；常规 Forge 1.12.2 audit 为 0 ERROR、7 条既有 S2C/Proxy `packet-thread` 保守 WARNING。`compileJava`、`compileTestJava`、`processResources`、`backpackSelfTest`、`featherWingsSelfTest`、`merchantCatalogSelfTest`、新增 `shoulderBackpackKeySelfTest`、`shoulderEquipmentSelfTest` 和 `marketPacketSelfTest` 均 PASS。最终 `./gradlew build --no-daemon --console=plain` PASS，包含 `reobfJar`、`exportReleaseJar` 和 `verifyReleaseJar`，实际核验 271 个生产 class 均为 Java 8；新 release JAR 为 563,556 bytes，SHA-256 `af1b1691ef89324b9b07b13e58f5e08952c641eb087ca0e0f3a6f9c531728008`，`unzip -t` PASS。覆盖前 561,839-byte JAR 已备份为 `release/backup/backup_20260908-145735.jar`。`runServer` 已以 JDK 8 进入 Forge 1.12.2/FML/Coremod 映射加载，但开发进程在模组生命周期前被环境断开；实际游戏内 Tooltip、B 键、三种贴图和肩部槽视觉，以及完整 Dedicated Server 世界验收仍为 NOT RUN。

## 维护：背包同步、行情书炒币与锄头铁砧修复（2026-09-08）

根因与修复：背包服务端 NBT 写入本身正确，但 `BackpackInventory` 把肩部 `ItemStack` 的对象引用当作身份。Capability 和所有者 Packet 9 同步均会复制该栈，首次放入/取出后的 S2C 副本替换客户端对象后，仍打开的 Container 因 `==` 失败显示全空，重开才从已写入 NBT 重新读到物品。现改为验证同一背包 Item tier；服务端 Container、物品私有 NBT、背包套娃拒绝和不同 tier/移除后的关闭边界不变。

行情书新增“炒币”第三页。`PastoralWorldData` 升至 v11，在既有绿宝石当前/昨日价旁保存至多 30 个实际世界日 `emeraldHistory` 点；新世界从首次 1,000 价开始记录，跨日补算逐日追加。v10 旧世界第一次读取只由保存的当前价补一个当前日点，不重抽也不改变普通行情。显示复用既有 Packet 1/2 的受限只读快照 key，客户端只画绿宝石曲线、今日/昨日和涨跌；价格范围与商人费率不在行情书中显示，交易仍仅在服务器权威的商人“炒币”页进行。

锄头铁砧问题经 Forge 1.12.2 映射源码/字节码核对，确认 `ItemHoe` 未覆写 `getIsRepairable`，继承的 `Item` 方法恒返回 false；近期作物耐久改动没有接管铁砧。新增受限 `AnvilUpdateEvent` 规则，只接受木板、圆石、铁锭、金锭、钻石分别修理木/石/铁/金/钻石锄，沿用每材料 25% 上限、消耗数、经验和后续 RepairCost；带 Reforged 的锄头继续按既有首次费用结果修理。

验证：Temurin JDK 8 `1.8.0_504` 下 `compileJava`、`compileTestJava`、`processResources`、`backpackSelfTest`、`emeraldMarketSelfTest`、`marketPacketSelfTest`、`enchantmentSelfTest` 与 `pastoralWorldDataSelfTest` 均 PASS。Forge 1.12.2 常规 audit 为 0 ERROR、7 条既有 S2C/Proxy `packet-thread` 保守 WARNING；`--strict-warnings` 因相同 7 条告警返回 2，未标记 PASS；`git diff --check` PASS。受限 60 秒 `runServer` 已实际进入 Forge/FML Java 8 启动与类加载，超时前未到模组生命周期/世界，完整 Dedicated Server 验收仍为 NOT RUN。最终 `./gradlew build --no-daemon --console=plain` PASS，含 `test`、`reobfJar`、`exportReleaseJar`、`verifyReleaseJar`；门禁核验 270 个 Java 8 生产 class。覆盖前 1.7 JAR 已备份为 `release/backup/backup_20260908-003216.jar`（556,164 bytes）；新 `release/LisBam_PastoralEconomy-1.7.jar` 为 561,839 bytes，SHA-256 `52042dee6111049944b27d03878391651fe9878c849eac85c8b9d5ea8c80f3d1`，`unzip -t` PASS。游戏内背包、行情书和铁砧回归为 NOT RUN：当前无可操作 Forge 客户端世界。

## 维护：炒币界面、出货箱晨间结算与区块加载器（2026-09-07）

实现：商人“炒币”页改为买入/卖出左右两栏，各栏均有独立原版数量滑条、输入框、服务器快照上限、预计金额和确认按钮；移除同一行“可买/可卖”之间原版字体不支持的全角空格，避免乱码。绿宝石交易的 Packet 11、服务端重验、费率和所有冻结数值不变。

根因与修复：出货箱此前在任意时段的首次 Overworld END tick 立即写入当天 `lastDispatchDay`，夜间加载或重启会在清晨前耗尽当天批次。现在只有世界时间 `% 24000` 为 `0～1000` 的晨间窗口才尝试领取当天标记，再以当天刷新后的市场快照出售已加载箱内商品。区块加载器此前只依赖方块邻居通知，已经强制加载的区块在红石邻居位于其他已卸载区块等情况下不会收到断电通知，因此票据可永久遗留；现在每个逻辑服务端世界 END tick 都重新核验持票加载器的方块、TileEntity 和红石状态，不符合条件立即释放。充能贴图改为与未充能一致的深色石质/紫色框架，仅添加克制的青色核心和紫蓝发光；`ItemChunkLoader` 改用显式基础显示键，去除物品名末尾 `.name`。

兼容性：没有改变 registry ID、TileEntity NBT、WorldSavedData schema、Capability、Packet discriminator 或经济规则；旧存档和已有 `forcedchunks.dat` 票据继续可读，重载后按当前红石状态决定保留或释放。

验证：Forge 1.12.2 常规和 `--strict-warnings` 静态审计均为 0 ERROR、7 条既有 S2C/Proxy `packet-thread` 保守 WARNING。`git diff --check` 通过，激活贴图已核验为 16×16、8-bit RGBA PNG。Temurin Java 8 `1.8.0_504` 恢复后，`./gradlew build --no-daemon --console=plain` 已 PASS，含 `test`、`reobfJar`、`exportReleaseJar` 与 `verifyReleaseJar`，门禁核验 267 个生产 class 均为 Java 8；覆盖前 JAR 已备份为 `release/backup/backup_20260907-194025.jar`，新 1.7 JAR 为 556,164 bytes、SHA-256 `dd12cb778c37f314aa9b056a1ed5c2865af75f0e2c4723e846f23a325e458337`，`unzip -t` PASS。用户要求直接导出，故 `chunkLoaderSelfTest`、`shippingBoxSelfTest` 未运行；游戏内与 Dedicated Server 验收仍为 NOT RUN。

## 新内容：出货箱（2026-09-07）

实现：新增 `shipping_box` 方块、ItemBlock、TileEntity、27 格原版三行箱子 Container/GUI、语言、模型、16×16 AI 像素贴图和 JSON 有序配方。方块各面接受漏斗输入、拒绝漏斗输出；玩家像使用普通箱子一样管理内容。逻辑服务端每天在市场价格更新后扫描已加载出货箱，只要箱内存在已绑定交易凭证，就按当天全局商人收购价卖出所有目录商品，整箱收入下调至 `floor(70%)`。每箱按唯一凭证 UUID 均分，余数稳定分配；牛奶桶返回空桶，凭证和不可售物不消费。

持久化：`PastoralWorldData` v10 追加有界 `shippingBoxPayouts` 段，保存每日仅执行一次的标记及每位离线/暂不可入账玩家的待发收益。所有出货箱完成核算后才统一发放，在线玩家每次领取合并金额只收到一条绿色 `【出货箱】今日收益+xxx金币！`；不新增网络包，金币仍经既有 `CoinService` 仅在服务端修改。旧 v1--v9 存档缺少该段时创建空状态，无需迁移。

验证：`shippingBoxSelfTest`、`pastoralWorldDataSelfTest` 已在 Temurin Java 8 `1.8.0_504` 通过；正式 JDK 8 build、release JAR 和游戏内/专服验证见本次测试清单。

## 新内容：绿宝石现货市场与经济调整（2026-09-07）

实现：新增独立的 `EmeraldMarketPriceGenerator` 与 `PastoralWorldData` v9 绿宝石市场字段。首次价格固定 1,000 金币，之后按冻结离散分布逐世界日变化并限制在 300～3,000；它不回归、不会在同日或时间回拨时重抽，跨日跳跃逐日补算。v8 或更旧世界缺少该字段时只建立今日/昨日各 1,000 的初始状态，普通市场快照和历史完全保留。商人第三个“炒币”页以原版绿宝石实物按 4% 手续费交易：买入为 `ceil(price * amount * 1.04)`，卖出为 `floor(price * amount * 0.96)`；界面仅显示服务端快照，Packet 11 请求不带价格、金币或库存，服务端在主线程复核真实会话、商人、距离、世界日、requestId、金币、主物品栏容量与持仓，并以库存/余额快照原子回滚异常。

经济调整：胡萝卜与马铃薯基础收购价由 40 调为 60，已保存普通行情不重写；羊毛通用配方由任意颜色羊毛 ×1 → 线 ×4 调为 ×1。普通商人购买池和未来每日 Offer 删除原版绿宝石，历史持久绿宝石 Offer 读入时失效并重建；绿宝石矿石继续保留在珍宝池。Packet 0～10 discriminator 未变，Packet 4 追加绿宝石展示字段，Packet 11 追加为新 C2S 请求，故商人交易客户端与服务端必须同升 1.6。

验证：Temurin Java 8 `1.8.0_504` 下 `compileJava`、`compileTestJava`、`processResources`、`emeraldTradeRulesSelfTest`、`emeraldMarketSelfTest`、`merchantCatalogSelfTest`、`marketCoreSelfTest`、`marketPacketSelfTest`、`pastoralWorldDataSelfTest` 与 `modGuiInputSelfTest` 均 PASS。`emeraldMarketSelfTest` 覆盖日变动全部区间端点、范围、确定性、首次初始化、跨日补算与 v8 缺字段迁移；`merchantCatalogSelfTest` 覆盖新价格、普通绿宝石删除、旧 Offer 重建及扩展快照编码。严格 Forge 1.12.2 audit 为 0 ERROR、7 条既有 S2C/Proxy 保守 `packet-thread` WARNING；新增 C2S handler 已显式调度服务端主线程。最终 JDK 8 `./gradlew build --no-daemon --console=plain` PASS，包含 `reobfJar`、`exportReleaseJar` 和 `verifyReleaseJar`，并核验 256 个生产 class 全部存在且为 Java 8。新 `release/LisBam_PastoralEconomy-1.6.jar` 为 531,118 bytes，SHA-256 `0ae27647b03a706a2d16218f7d13f64f6e2ff501bcac26f46b531dcc0ffadfab`，`unzip -t` PASS；目标版本 JAR 在构建前不存在，因此没有覆盖或备份。游戏内 GUI 与 Dedicated Server 端到端验证为 NOT RUN：当前环境没有可操作 Forge 客户端世界，且现有开发服务器 EULA 未接受。

## 紧急修复：多 Coremod 环境延迟类加载崩溃（2026-09-07）

根因：崩溃并非蟹笼库存、捕捞或农业规则自身抛错。用户日志中的 `LaunchClassLoader.findClass:182` 对空 `transformedClass` 执行 `defineClass`，同一进程先后无法解析 `AgricultureRules$Crop`、`ContainerCrabTrap` 和 `CrabTrapRules`；逐个检查发生崩溃时对应的 497,424-byte release 备份与当前 release，三个 class 均真实存在、非空且 JAR 可完整解压。项目作为 Coremod 与普通模组共包，却只将 `lisbam.pastoraleconomy.core` 排除于全局转换链，导致其余业务类的首次加载仍经过同环境中全部第三方 Transformer 及 LaunchWrapper 的负资源缓存；任一步返回空字节就会表现为这些互不相关类的 `NoClassDefFoundError`。

修复：`EnchantingTableCorePlugin` 将 `TransformerExclusions` 扩大为稳定根包 `lisbam.pastoraleconomy`。本模组自己的 class 从此直接由 LaunchClassLoader 的父实现定义，不再交给无关 Coremod 改写；两个本模组 Transformer 仍只处理明确的原版目标。构建增加 `verifyReleaseJar`：`build` 导出重混淆成品后，逐项对照全部编译生产 class，确认 JAR 条目存在、非空、可读取且均为 Java 8 major 52，任何漏包或损坏都会令构建失败。

兼容性与影响：不修改 registry ID、NBT、Capability、WorldSavedData、Packet discriminator、GUI ID 或存档 schema，旧世界无需迁移。修改只改变本模组 class 的装载边界，不改变蟹笼、农业、肩部装备或经济玩法。

验证：Temurin Java 8 `1.8.0_504` 下 `compileJava`、`compileTestJava`、`shoulderEquipmentSelfTest`、`crabTrapSelfTest` 均 PASS；Coremod 自检新增独立 LaunchClassLoader 对上述三个延迟类的实际装载，并保持肩部原版类注入通过。Forge 1.12.2 常规 audit 为 0 ERROR、7 条既有 `packet-thread` 保守 WARNING。带显式 Coremod 的 `runServer` 在 120 秒内确认根包隔离注解可装载且 Coremod 成功入队，超时前仍停在开发环境映射载入、未进入模组生命周期/世界；真实 Windows 多 Coremod 客户端及蟹笼点击为 NOT RUN。最终 `./gradlew build --no-daemon --console=plain` PASS，包含 `test`、`reobfJar`、`exportReleaseJar` 和 `verifyReleaseJar`；门禁成功核验 243 个生产 class。新 release JAR 为 503,927 bytes，SHA-256 `cb76eafbe1d45d4ef561b74cefb2bdef7a60705e9cf6488fcdf5830d0e742527`，`unzip -t` PASS；覆盖前 503,932-byte JAR 已备份为 `release/backup/backup_20260907-090138.jar`，SHA-256 `860ae85ab4c3099b246d687b8bde7d597383d1a3c39f145994d92c1dce38ffea`。

## 紧急修复：锄头耐久、作物掉落与多人方块同步（2026-09-06）

根因：上一轮把所有锄头作物破坏都塞入原本仅承载附魔成熟收获的玩家级 `HarvestAction`，并在 `HarvestDropsEvent` 回调仍执行原版掉落代码时直接损伤主手栈；精耕也在同一回调内立即把 AIR 改成新苗。Forge 1.12.2 的实际顺序会在 `BreakEvent` 前先给破坏者发送临时 AIR 包，再执行方块移除、掉落事件和区块批量同步。两项中途写入使工具补扣依赖附魔动作匹配，并让成熟作物、AIR、新苗的客户端更新发生在同一原版收获调用中，实机出现未扣耐久、无掉落假象、成熟幽灵作物及其他玩家看不到新苗。此前纯规则自检只覆盖“哪些作物需要补扣”，没有覆盖该事件时序。

修复：`AgricultureEnchantmentEventHandler` 恢复为只登记真正具有农业附魔效果的成熟收获；精耕在掉落表内消耗种植物后只登记补种，服务器 END tick 才确认当前位置仍为空气、下方仍为耕地并写入 age 0；真实玩家成功种植的最终状态也在 END tick 重新广播。新的 `HoeCropDurabilityEventHandler` 仅在服务端 `HarvestDropsEvent` 已证明原版成功移除/收获后登记零硬度作物，等完整收获调用结束再对同一快捷栏锄头调用一次 `damageItem`，补发物品损坏事件、强制容器同步，并把该坐标的最终服务端方块状态重新广播给所有区块追踪玩家。硬度非零方块仍只走原版耐久，创造模式和自动化仍不补扣。

兼容性与影响：不新增或修改 registry ID、NBT、Capability、WorldSavedData、Packet discriminator 或存档 schema；旧世界无需迁移。临时动作只存活到当前服务器 tick 结束。

验证：Temurin Java 8 `1.8.0_504` 下 `compileJava`、`compileTestJava`、`processResources`、`toolDurabilitySelfTest`、`enchantmentSelfTest`、`goldenBoneMealSelfTest` 均 PASS；耐久自测新增无附魔锄头恰好损失 1 点、创造/非锄/非零硬度排除项与四种精耕作物 age 0。Forge 1.12.2 常规 audit 为 0 ERROR、7 条既有 `packet-thread` 保守 WARNING；`--strict-warnings` 因这 7 条历史 warning 返回 2，不标记 PASS。`./gradlew build --no-daemon --console=plain` PASS，包含 `reobfJar` 与 `exportReleaseJar`；release JAR 为 503,932 bytes，SHA-256 `860ae85ab4c3099b246d687b8bde7d597383d1a3c39f145994d92c1dce38ffea`，`unzip -t` PASS，新处理器 class 为 Java 8 major version 52。覆盖前 497,424-byte JAR 已备份为 `release/backup/backup_20260906-225134.jar`。受限 120 秒的 `runServer` 已以 Java 8 发现并入队本模组 Coremod，但超时前未进入模组生命周期/世界，且 `eula=false`；完整 Dedicated Server 及单人/双客户端到端验收为 NOT RUN。

## 维护：伐木耐久下限与锄头作物耐久（2026-09-06）

根因与修复：伐木的二级树叶此前与原木一样走 `tryHarvestBlock`，因此 1.12.2 原版 `ItemTool#onBlockDestroyed` 也扣除了斧头耐久；同时伐木在触发原木执行原版耐久前就处理二级方块，无法保证本次结束仍留下 1 点。现在树叶继续走原版掉落/保护路径，但成功后恢复该次工具损耗；伐木只在剩余耐久大于 1 时启动，每个二级原木前预留触发原木的一次扣耐久与最后 1 点。原版 `ItemHoe` 仅在方块硬度非零时消耗耐久，故零硬度既有作物用锄头成功破坏时过去不会受损；收获成功后由逻辑服务端补调用一次 `damageItem`，保留耐久附魔并避免对非零硬度作物重复扣除。

兼容性与影响：不新增注册项、NBT、Capability、WorldSavedData 或 Packet；旧存档无需迁移。创造模式和自动化不进入锄头补扣路径。

验证：Temurin Java 8 `1.8.0_504` 下 `check_toolchain.py` 为 0 error/0 warning；`compileJava`、`compileTestJava`、`toolDurabilitySelfTest`、`enchantmentSelfTest`、`goldenBoneMealSelfTest` 均 PASS。常规 Forge 1.12.2 audit 为 0 ERROR、7 条既有 `packet-thread` 保守 WARNING；`--strict-warnings` 因这 7 条历史 warning 返回非零，故不标记 PASS。最终 `./gradlew build --console=plain` PASS，包含 `reobfJar` 和 `exportReleaseJar`；新 release JAR 为 497,424 bytes，SHA-256 `7a772a6d1d1db9446f65cf4be6a5e1d9d52ee85ea21cd4fe35e07a64e37af554`，`unzip -t` PASS，`FellingDurabilityRules.class` 为 Java 8 major version 52。覆盖前 495,524-byte JAR 已备份为 `release/backup/backup_20260906-222214.jar`。游戏内及 Dedicated Server 世界验收为 NOT RUN：当前环境没有可操作 Forge 客户端或可进入的服务器世界。

## 新内容：肩部羽毛翅膀与田园眷顾调值（2026-09-06）

实现：田园眷顾 I--IV 的单次经验概率固定为 `20%/40%/60%/80%`。新增稳定物品 `feather_wings`，仅可装备于既有肩部槽；非创造且非旁观模式穿戴后可使用原版自由飞行能力，飞行每连续 40 tick 尝试耗 1 耐久，最大耐久 500，伤害事件不会消耗耐久。飞行移动的饥饿消耗按距离为原版步行的两倍。它可附魔耐久、经验修补、绑定诅咒和消失诅咒；经验修补因肩部栈不属于原版装备扫描而在服务端经验球事件中补齐，绑定/消失诅咒分别在肩部取下/死亡 Clone 路径生效。铁砧每根羽毛修复 5 耐久，两个羽毛翅膀的合并仍用原版耐久装备逻辑。

经济与资源：不添加配方；羽毛翅膀以 1200000 基础价加入既有商人珍宝池，继续使用 8% 日波动和每日库存 1。图标由 AI 像素源图去绿幕后固定采样为 16×16 RGBA；物理客户端追加原版 `ModelElytra` 几何层并使用羽毛贴图。PlayerData、WorldSavedData、既有 NBT key 和 Packet discriminator 均不变，Packet 9 仅扩展既有追踪者渲染快照的合法物品范围。

验证：Temurin Java 8 `1.8.0_504` 下 `check_toolchain.py` 为 0 error/0 warning；`compileJava`、`compileTestJava`、`processResources`、`enchantmentSelfTest`、`merchantCatalogSelfTest`、`shoulderEquipmentSelfTest`、`featherWingsSelfTest` 均 PASS。常规 Forge 1.12.2 audit 为 0 ERROR、7 条既有 `packet-thread` 保守 WARNING；`--strict-warnings` 因这 7 条历史 warning 返回非零，故不标记 PASS。最终 `./gradlew build --console=plain` PASS，包含 `reobfJar` 和 `exportReleaseJar`；新 release JAR 为 495,524 bytes，SHA-256 `4f394f45aeffa05fc90759ea7a5ca410af17750d1d5882f33caf40c03969241f`，`unzip -t` PASS，`ItemFeatherWings.class` 为 Java 8 major version 52。覆盖前的 482,145-byte JAR 已备份为 `release/backup/backup_20260906-210434.jar`。游戏内及 Dedicated Server 世界验收为 NOT RUN：当前环境没有可操作 Forge 客户端或可进入的服务器世界。

## 维护：输入框、丰收斧头、田园眷顾与凭证箱加载（2026-09-06）

实现：行情书搜索、商人数量和交通节点取名框获得焦点时，打开背包键（默认 E，含改键）不再关闭界面，Esc 仍走既有服务端 Container 关闭路径。Harvest 现在允许五种原版斧头通过附魔书/铁砧获得并在成熟作物收获时使用原有增产公式，但斧头在附魔台不生成 Harvest 候选；田园眷顾经验概率随后维护为 I--IV `20%/40%/60%/80%`。

交易凭证箱不再依赖交易瞬间玩家附近的已加载 TileEntity：玩家关闭含绑定凭证的原版单箱/大箱，或交易扫描到它时，会记录每个物理箱子半块的位置。`PastoralWorldData` schema 升为 v8 后持久保存该有界位置索引，Forge 1.12.2 的单独 NORMAL ticket 持续加载每个登记半块，重启回调按 ticket 坐标恢复。每秒只校验登记箱；凭证移除、箱子失效、上锁或未展开战利品状态会释放票据和索引。旧存档中先前未登记的远方箱子需重新打开/关闭一次才能获知位置。

兼容性与影响：不改变 Harvest、Pastoral Favor、交易凭证的 registry ID、现有物品 NBT、Packet 编码、交易原子回滚或 UUID 授权。WorldSavedData 从 v7 安全读取为空凭证箱索引再写为 v8；Forge 票据上限由服主配置约束，无法立即获票的位置会保持索引并后续重试。

验证：Temurin Java 8 `1.8.0_504` 下 `compileJava`、`compileTestJava`、`processResources`、`modGuiInputSelfTest`、`enchantmentSelfTest`、`tradeVoucherSelfTest`、`pastoralWorldDataSelfTest` 与最终 `./gradlew build --no-daemon --console=plain` 均 PASS。严格 Forge 1.12.2 audit 为 0 ERROR、7 条既有 `packet-thread` 保守 WARNING（严格模式因 warning 返回非零，已保留并审查）；60 秒 `runServer` 实测进入 Forge/FML/coremod 引导，但未到本模组生命周期/世界且没有接受 EULA，完整 Dedicated Server 验收为 NOT RUN。重混淆 `release/LisBam_PastoralEconomy-1.5.jar` 为 482,145 bytes，SHA-256 `17249a71b23a4ee63064273221756b99922bea8fe87273a9265b2bdefff21841`，`unzip -t` PASS，新增生产 class major version 为 52；覆盖前的 472,033-byte 成品已备份为 `release/backup/backup_20260906-195947.jar`。

## 维护：凭证配方、死亡金币、价格回归与旧物品移除（2026-09-06）

实现：空交易凭证新增金粒环绕中央纸张的有序配方。额外动物骨头判定的各档成功概率改为原来的 2/3：大体型为 40/300 出 2、100/300 出 1，小体型为 50/300 出 1。真实玩家死亡在逻辑服务端按随机 10%--30%、且不低于随机 1000--2000 金币扣款（最多当前余额），并收到红色“本次死亡失去xxx金币！”通知；能力数据在 Clone 前已扣款，重生与 HUD 同步继续复用现有路径。

调价表本次把青金石、荧石粉、下界石英购买基础价改为 800、200、1600。现存 NBT 快照不重写；下一市场日从旧价按既有回归波动走向新价格带，进入后再按正常上/下限约束。旧交通定位物品的注册、行为、配方、模型、贴图和语言全部删除，旧存档通过 `MissingMappings<Item>` 忽略该 Item ID，故不会因已删除物品中断加载。

兼容性与影响：不增加 Packet 或持久化字段、不改 PlayerData/WorldSavedData 的名称和 schema。仅旧存档中的已删除物品会消失；已保存市场价在调价当天保持不变，后续逐日自然回归。

验证：Temurin Java 8 `1.8.0_504` 下 `compileJava`、`compileTestJava`、`processResources`、`deathCoinLossSelfTest`、`animalBoneDropSelfTest`、`marketCoreSelfTest`、`tradeVoucherSelfTest`、`transportCoreSelfTest`、`shoulderEquipmentSelfTest` 及最终 `./gradlew build --no-daemon --console=plain` 均 PASS。严格 Forge 1.12.2 audit 为 0 ERROR、7 条既有保守 `packet-thread` WARNING；`unzip -t` PASS，生产 class major version 52。重混淆 release JAR 为 472,033 bytes，SHA-256 `03eda8154b6c84e014271ed6f529cb2875def0b23e34ada022cea0238d4e260e`；覆盖前 502,564-byte 成品已备份为 `release/backup/backup_20260906-192806.jar`。游戏内和 Dedicated Server 世界验收仍为 NOT RUN：当前环境没有可操作 Forge 客户端或可进入测试世界。

## 新内容：三档肩部背包（2026-09-06）

实现：新增普通/高级/超级背包三个稳定物品，分别提供 27、54、108 格并装备在既有肩部槽；手持右键或 Shift-click 可穿戴，三者与鞘翅互斥且暂不绘制玩家模型。在原版背包肩部槽对已装备背包右键后打开三行仓储页，高级/超级使用原版按钮切换 2/4 页；容器支持普通拖放与 Shift-click，并拒绝任意背包套娃。内容写入该背包物品自身的有界 NBT，随换装、保存和玩家 Clone 保留。普通配方严格使用皮革/箱子/线；高级配方使用皮革/两个普通背包/拴绳，且非空普通背包不会匹配，避免吞物；超级背包无配方，以 200000 基础价、8% 珍宝波动和库存 1 加入商人珍宝池。三张物品图标均由独立 AI 像素源图处理为 16×16 RGBA，未添加穿戴模型。

架构与兼容性：复用 `PlayerData.shoulder`、现有 Coremod 槽和唯一网络通道；追加 GUI ID 4 与 C2S Packet 10，不重排 Packet 0～9。打开请求和分页在服务端主线程/实际 `ContainerBackpack` 验证，库存修改只经服务端 Container；完整背包 NBT 只同步给所有者，追踪者继续只得到鞘翅渲染所需快照。三个 Item ID、超级背包 market/catalog key 为追加式；Player Capability v3、WorldSavedData v7、旧肩部鞘翅与旧存档均不迁移。

验证：Temurin Java 8 `1.8.0_504` 下 `compileJava`、`compileTestJava`、`processResources`、`backpackSelfTest`、`shoulderEquipmentSelfTest`、`playerDataSelfTest`、`merchantCatalogSelfTest` 与最终 `./gradlew build` 均 PASS。背包自检覆盖 27/54/108 容量、四页首末槽 NBT 往返、禁止嵌套、非空配方材料保护和三张 16×16 RGBA 透明边角贴图；商人自检覆盖 38 项珍宝池、超级背包 200000 基础价/8% 波动/库存 1。Forge 1.12.2 audit 为 0 ERROR、7 条既有保守 `packet-thread` WARNING；新增 C2S Handler 明确调度服务端主线程。`timeout 180s ./gradlew runServer` 已在 Java 8 下到达 `eula=false` 的 EULA 提示，未接受协议（服务器仅刷新了该文件时间戳），因此模组生命周期、世界与多人端到端仍为 NOT RUN。最终构建包含 `reobfJar` 与 `exportReleaseJar`；新 `release/LisBam_PastoralEconomy-1.5.jar` 为 502,564 bytes，SHA-256 `7be4f580e2088e6f60078550b31832edddcfed5d8ff3a767d9886c516a2acf7e`，`unzip -t` PASS、生产 class major version 52。覆盖前的 454,587-byte 成品已备份为 `release/backup/backup_20260906-185437.jar`。

## 新内容：交易凭证与箱子出售（2026-09-06）

实现：新增最大堆叠为 1 的“空交易凭证”；右键后由逻辑服务端把使用者 UUID 与当时玩家名写入物品 NBT，物品显示为“交易凭证-玩家名”且不能再次绑定。空白/绑定状态共用稳定 registry ID，通过模型属性切换两张 AI 生成后处理为 16×16 RGBA 的原版风卷纸贴图。空白凭证的有序配方为八个金粒环绕中央纸张。

把已绑定凭证放入当前已加载的原版单箱或大箱后，凭证对应玩家在既有商人界面出售时可同时使用自己主物品栏和这些授权箱子的货物。服务端扫描所有已加载维度但绝不加载新区块；跳过上锁箱与未展开的战利品箱，授权按 UUID 而非可能重名/改名的显示文字判断。多库存事务优先扣随身物品，再按维度/坐标稳定扣箱中物品；牛奶桶的空桶也可返回这些来源，扣货、返还或加金币任一步失败都会恢复全部库存。GUI 将服务端箱子数与客户端本地主背包数相加显示“可出售”，交易请求格式不变。

兼容性与影响：只追加 Item ID `lisbam_pastoral_economy:trade_voucher` 及物品栈私有 NBT `VoucherOwner`/`VoucherOwnerName`，没有修改 Capability、WorldSavedData、TileEntity NBT、既有商品 key 或 Packet discriminator。玩家改名不会破坏授权，因为 UUID 始终是权限依据；物品名称保留绑定时的名字快照。旧存档无需迁移。

验证：Temurin Java 8 `1.8.0_504` 下 `compileJava`、`compileTestJava`、`processResources`、`tradeVoucherSelfTest`、`merchantCatalogSelfTest`、`marketPacketSelfTest` 和最终 `build` 全部 PASS；新自检覆盖一次性 UUID 绑定、NBT 拷贝、他人拒绝、玩家/箱子合并计数、玩家优先扣货、全来源回滚、空桶返还及两张 16×16 RGBA 贴图。严格 Forge 1.12.2 audit 为 0 ERROR、7 条既有 `packet-thread` 保守 WARNING，本次未增加网络处理器；`check_toolchain.py` 为 0 ERROR，并因默认环境无 Java 报 1 条 warning，真实 Gradle 命令已显式使用上述 JDK 8。Dedicated Server 在 180 秒内成功发现、注入并运行本模组 Coremod，随后到达 Minecraft 校验，但未进入模组生命周期/世界，完整 Server 验收仍为 NOT RUN，`run/eula.txt` 未修改。正式重混淆 `release/LisBam_PastoralEconomy-1.5.jar` 为 479,868 bytes，SHA-256 `4405eeb39dc73afe9306d9e60eaf577f72903663ad53e9ce61f345cbd87879bd`，`unzip -t` PASS，生产 class major version 为 52；覆盖前 463,258-byte 成品已备份为 `release/backup/backup_20260906-141853.jar`。

## 维护：肩部槽 UI、右键穿戴与模型显示（2026-09-06）

实现：生存背包肩部槽改到原版副手槽正上方 `77,44`，创造模式“生存物品栏”页按其副手槽位置改到 `35,2`；两处统一裁取原版 `inventory.png` 的同一 18×18 盔甲槽画面，因此会跟随资源包，并保留原版物品渲染、悬停遮罩与 Tooltip。创造页会重建一层 `CreativeSlot`，客户端因此在每帧绘制前按其共享的肩部 `IInventory` 重新识别和定位包装槽，避免新增槽被原版索引公式叠到快捷栏。

手持鞘翅右键现由 `RightClickItem` 双端拦截：客户端只返回成功，逻辑服务端独立确认肩部为空并修改既有玩家 Capability；生存模式消耗手持鞘翅，创造模式保持原版护甲右键的复制语义。原版 `LayerElytra` 的胸甲读取也由既有 Coremod 改读肩部，胸甲与鞘翅模型能够同时渲染。新增的 Packet 9 只负责 S2C 展示同步，在玩家本人、追踪者以及飞行耐久/损坏变化时更新客户端肩部副本，不改变服务端权威库存。

兼容性与影响：PlayerData 的 `shoulder` NBT、dataVersion、registry ID、WorldSavedData 名称和既有 Packet 0～8 均不变，仅在末尾追加 Packet 9；旧存档不需要迁移。新旧 1.5 客户端/服务端应使用同一构建，避免缺少新 S2C discriminator。根因是原实现只为 `GuiInventory` 固定绘制一个坐标，创造页按新增索引把肩部包装槽叠到快捷栏；原版 `ItemElytra#onItemRightClick` 与 `LayerElytra#doRenderLayer` 仍直接读写胸甲槽，且远端玩家没有肩部 Capability 同步。

验证：Temurin Java 8 `1.8.0_504` 下 `compileJava`、`compileTestJava`、`processResources`、`shoulderEquipmentSelfTest`、`playerDataSelfTest`、`enchantmentSelfTest` 与最终 `build` 全部 PASS；最终构建包含 `test`、`reobfJar` 和 `exportReleaseJar`，生产 class major version 为 52。Forge 1.12.2 toolchain 检查确认 Forge `14.23.5.2859`、Gradle 4.9 和 snapshot `20171003-1.12`；严格静态审计为 0 ERROR、7 条 `packet-thread` 保守 WARNING，其中新增肩部 S2C Handler 仅委托 Proxy，实际实体更新由客户端执行器调度到主线程。`release/LisBam_PastoralEconomy-1.5.jar` 已重新导出为 463,258 bytes，SHA-256 `5796b3dc4a9b6f47a1f09f6008824ada318ff30e3d14249039a630f519e557fe`，`unzip -t` PASS；覆盖前的 454,587-byte 旧成品备份为 `release/backup/backup_20260906-125014.jar`，最终坐标修正前的中间成品备份为 `release/backup/backup_20260906-130234.jar`。Dedicated Server 两次实际进入 Forge/FML 引导；第二次显式加载并运行本模组 Coremod 后到达 Minecraft 校验阶段，但 180 秒内未完成模组生命周期，故完整 Server 验收仍为 NOT RUN，`run/eula.txt` 保持 `eula=false`。生存/创造 GUI、右键穿戴和本人/远端模型仍需可操作客户端做游戏内验收。

## 紧急修复：客户端窗口号污染与界面暂停（2026-09-06）

真正根因位于 Forge 1.12.2 的双端 GUI 打开协议。服务端创建 `Container` 并发送 `OpenGui` 后，客户端处理器会先显示本地 GUI，再把服务端 `windowId` 写入客户端玩家当前的 `openContainer`。行情书、商人交易和交通站此前继承普通 `GuiScreen`；它们没有执行 `GuiContainer#initGui`，因此客户端 `openContainer` 仍是永久的 `inventoryContainer`。Forge 随即把模组窗口号直接写进窗口 0 的玩家背包容器。即使后来正确发送关窗包，`EntityPlayer#closeScreen` 也只把引用切回同一个 `inventoryContainer`，不会把被污染的 `windowId` 恢复为 0。此后原版背包点击和四格合成包携带错误窗口号，被服务端窗口 0 校验拒绝；物品拾取、骨粉与背包同步也随之表现异常。上一轮只补 Esc 关窗包只能解决服务端孤儿 Container，不能修复这个更早发生的客户端窗口号污染。

修复后 `GuiMarketBook`、`GuiMerchantTrade`、`GuiTransportStation` 都继承 `GuiContainer`，并分别持有与服务端同类型、但不拥有权威数据的客户端无槽生命周期 Container。三个 `initGui` 都先调用 `super.initGui()`，保证 Forge 分配窗口号时写入这个专用容器而不是玩家永久背包。商人客户端容器只携带实体 ID，交通客户端容器只携带坐标；服务端仍只使用绑定真实实体/Tile UUID 的权威构造器和既有校验。交通 GUI 的客户端创建不再依赖 TileEntity 已先同步到本地，避免合法服务端开窗因客户端短暂缺 Tile 而返回 `null`、再次污染背包容器。Esc 与改键背包键继续统一调用 `EntityPlayerSP#closeScreen`。

行情书、商人交易、交通站现都明确返回 `doesGuiPauseGame=false`；交通的原版 `GuiYesNo` 确认层也覆写为不暂停。单人世界打开这些界面时，集成服务端会继续 tick，行情/交易/交通请求可以即时处理。

兼容性与影响：没有修改 Packet discriminator、数据编码、registry ID、NBT、Capability、WorldSavedData、金币、库存或交通费用公式；客户端生命周期容器不保存数据，旧存档无需迁移。实际游戏内打开/关闭后合成、拾取、骨粉以及单人不暂停行为仍需在可进入世界的环境中确认。

验证：Temurin Java 8 `1.8.0_504` 下 `compileJava`、`modGuiInputSelfTest`、`marketPacketSelfTest`、`merchantCatalogSelfTest`、`transportCoreSelfTest`、`transportTravelSelfTest` 和 `goldenBoneMealSelfTest` 均 PASS；严格 Forge audit 为 0 ERROR、6 条既有 `packet-thread` 保守 WARNING。最终 `./gradlew compileJava processResources build --no-daemon --console=plain` PASS，包含 `test`、`reobfJar` 与 `exportReleaseJar`。`release/LisBam_PastoralEconomy-1.5.jar` 已由本次构建覆盖导出，406,463 bytes，SHA-256 `fc2f462ea631113b77272b3edfa706550cb165d921026892402f60e96db4ad44`；`unzip -t` PASS，并确认包含三类 GUI、对应 Container 与共用关窗类。游戏内与 Dedicated Server 为 NOT RUN：当前环境无法创建可操作客户端或进入服务器世界。

最近一次成功构建记录（本次客户端容器生命周期修复后）：

- `env JAVA_HOME=/tmp/lbpe-jdk8 PATH="/tmp/lbpe-jdk8/bin:$PATH" ./gradlew compileJava processResources build --no-daemon --console=plain`
- 日期：2026-09-06
- 结果：PASS（Forge 14.23.5.2859 / Temurin Java 8 `1.8.0_504`；release JAR 已实际覆盖导出且非空。）

## 维护：行情展示、附魔节奏与旅行费（2026-09-05）

实现：行情书在今日价下新增基础价行，并把昨日价、趋势和曲线纵向错开，避免最小 320×240 缩放下重叠。原版库存 Tooltip 中悬停 29 类商人收购品会显示格式为“今日价：xxx金币”的服务器冻结价格；Packet 7/8 仅传输有界市场 key 和当前世界日/价格，服务端主线程校验合法出售 key 后回复，客户端按市场日缓存且最多每秒重试一次。Tooltip 文案已移除“收购价”和“金币/单位”后缀。

疾步改为护腿附魔，只由服务端固定 UUID 的移动属性直接改变移速，并在穿戴、跳跃和落地期间保持修饰符稳定；客户端 FOV 监听器按去除该固定速度项后的属性重算，因此加速不改变 POV。夜视由客户端可恢复临时 gamma 实现：戴上时立即进入 10 秒视觉窗口，之后每 5 秒续 10 秒，不创建夜视药水效果。范围每级由 +1 调整为 +1.5 格；Forge 1.12.2 原生 `REACH_DISTANCE` 同时覆盖客户端选取和服务端实体攻击、方块及实体交互校验。前往费用提高到旧旅行曲线的 400%，即 `round20(160 + 0.48D)`；接入费不变。

兼容性与影响：新增 Packet discriminator 7/8，不重排 0～6；未变更任何 registry ID、既有 NBT key、WorldSavedData、Capability 或商人商品 key。旧疾步靴子保留其原有 NBT，但不再属于合法装备/不触发效果；护腿上的合法疾步立即生效。旅行费用不持久化，已解锁节点保持，下一次服务端重算旅行时使用新数值。

验证：Temurin Java 8 下 `compileJava`、`processResources`、`build`（含 `test`、`reobfJar`、`exportReleaseJar`）均 PASS；严格 Forge 1.12.2 audit 为 0 ERROR、6 条已审查的 `packet-thread` 保守 WARNING。游戏内背包 Tooltip、夜视亮度/恢复、护腿疾步 POV、范围实体攻击距离与 Dedicated Server 仍待可操作 Forge 世界。

## 2026-09-05 Tooltip 与纯客户端夜视修复

根因与修复：背包价格 Tooltip 原先同时限制 `GuiInventory` 实例、客户端实体对象身份以及服务端主背包槽位；中间尝试的 `DrawScreenEvent.Post` 还发生在 `GuiContainer` 选中悬停槽位之前，导致请求/显示链无法命中。现在客户端按玩家 UUID 在最终原版 `ItemTooltipEvent` 列表中直接追加“今日价：xxx金币”，服务端仅校验稳定出售目录 key 后直接在主线程返回冻结的今日价格，客户端仍按世界日丢弃过期缓存。疾步继续由服务端直接改变移速，并保持修饰符跨装备和跳跃同步边界，由客户端 FOV 监听器按去除固定疾步项后的属性重算，避免原版速度属性带来的 POV 拉伸。夜视完全移到 `ClientNightVisionRenderHandler`，只在客户端临时提高 gamma 并在摘下头盔、断开连接时恢复原值，不写入任何药水状态或世界光照。

## 维护：商人范围、命名牌与交通费用（2026-09-05）

实现：村庄识别、站点和交通仍使用既有的 128/64/160 格参考距离，但商人实体的出生与活动圆形半径独立收紧为 32 格；超出 32 格每 20 tick 尝试寻路返航，超出 64 格立即传送到已持久的村庄站旁。村庄维护保持为 200 tick（10 秒），初次实体载入等待也随之为一个 200 tick 周期。`EntityMerchant#processInteract` 对原版命名牌返回给 `EntityLivingBase` 处理，因此只有已命名的命名牌会写入持久 `CustomName` 并消耗物品，商人 UUID、交易和库存不受影响。交通旅行与接入费都降至原曲线的 1/10：`round5(40 + 0.12D)` 与 `round10(400 + 1.20D)`。

兼容性与影响：没有改动 registry ID、Packet discriminator、NBT key、`PastoralWorldData` schema 或既有商人/交通身份。已存在商人会在下一次服务端 tick 使用新的 32/64 格规则；交通费用按每次服务端计算即时生效，已解锁节点不会被重置。内容书删除了未由代码实现的“村民数量下降时维护移除超额商人”承诺，并将交易界面的购买目录同步为实际 8 条。

验证：此前 60 tick 版本的 `check_toolchain.py`、严格 Forge 审计、`compileJava`、`processResources`、`merchantCatalogSelfTest`、`transportTravelSelfTest` 与最终 `build` 均 PASS；正式 JAR 为上方 381,312 bytes 的 1.5 成品。本次将维护间隔回退为 200 tick 后，按请求未运行审计、构建或测试，因此该既有 JAR 不包含本次回退。

## 维护：1.5 附魔修复与扩展（2026-09-05）

实现：捷足保留服务器移动加成，并由物理客户端 FOV 钩子精确抵消自身速度倍率，故不再出现速度药水式视野拉伸。夜视改为客户端可恢复临时 gamma 的 10 秒初始/5 秒续期视觉，不创建药水效果。耕地行者中文名改为“耕地行者”，两侧取消 `FarmlandTrampleEvent`，玩家跳跃、落地和移动均不会把耕地变回泥土或破坏其上的作物。

新增攻速 I--V、范围 I--V、百炼如新 I、束锋诅咒 I；前两者分别按攻击间隔 80/60/40/20/0% 与交互距离 +1～+5 格实现，百炼如新以临时铁砧输出忽略先前工作惩罚，束锋诅咒为不进附魔台且与横扫之刃互斥的剑用宝藏诅咒。范围和丰收扩展到剪刀；丰收剪羊/剪哞菇仅在原版成功后补发 `Binomial(2L, 4/7)` 的同类掉落。商人目录加入四种书与冻结等级权重/价格，双语和拼音搜索同步更新。

兼容性与影响：旧八种附魔的 registry ID、已有物品 NBT、Packet、Capability、`PastoralWorldData` 和既有商品 key 不变；新增四个附魔 ID 与 merchant market key 为追加式变化，旧世界自然在后续商人商品生成时纳入新书。百炼如新不改写铁砧输入 NBT；极端纯改名若原版已因“过于昂贵”不产生输出，仍保持原版拒绝。

验证：JDK 8 下 `compileJava`、`processResources`、`compileTestJava`、`enchantmentSelfTest`、`merchantCatalogSelfTest`、`pinyinSearchSelfTest` 与最终 `build` 全部 PASS。严格 Forge audit 为 0 ERROR、5 条既有 packet-thread 保守 WARNING；release JAR 如上。游戏内附魔台、FOV、夜视、踩踏、铁砧、剪毛和 Dedicated Server 为 NOT RUN，需可进入实际世界的环境。

## 维护：商人交易快照与行情书拼音检索（2026-09-05）

根因与修复：上次将商人出售目录从 10 项缩减为 8 项时，服务端 `SyncMerchantTradeMessage` 已按当前快照写出 6+8 条，但客户端读取端仍硬编码 6+10 条。读取越过包尾后整份快照被拒绝，交易 GUI 因而把全部卡片显示为“后续内容”。现改为在读写两端均使用 `MerchantTradeSnapshot` 的槽位常量，并加入完整 6+8 快照往返自测。

行情书购买页新增本地拼音筛选：显示名除原始中文和稳定商品 key 外，接受全拼、首字母和混合拼音输入（例如 `xiaomai`、`xm`、`lvbs`）；`ü` 会规范化为 `v`。客户端只缓存本次实际显示名的分词结果；紧凑字典覆盖原版物品/方块和本模组中文本地化所用汉字，不引入外部运行时库、不发送搜索网络包，也不参与价格或交易判定。搜索提示同步标明名称/拼音/key。

兼容性与影响：未改 registry ID、Packet discriminator、字段顺序、NBT key、`PastoralWorldData` 名称或数据版本。新客户端必须与当前 8 槽协议服务端配套；没有任何存档迁移。Temurin Java 8 `1.8.0_504` 下 `merchantCatalogSelfTest`、`pinyinSearchSelfTest`、干净重建后的 `processResources build` 均 PASS；Forge 静态审计为 0 ERROR、5 条既有 `packet-thread` WARNING。`exportReleaseJar` 已导出 364,651-byte release JAR，SHA-256 `b88b3d48a83cdace34d0e4a2ff3ebebbd24d8de45c04ea64de1045821f26dd76`，`unzip -t` PASS。游戏内商人/行情书界面与 Dedicated Server 为 NOT RUN，需可操作运行环境。

## 维护：商人出售栏独立品质抽取（2026-09-05）

实现：商人每天出售的商品由 10 项缩减为 8 项，取消原先普通 4、罕见 3、稀有 2、珍宝 1 的固定品质配额。服务端为每个购买槽独立按普通 40%、罕见 30%、稀有 20%、珍宝 10% 抽取品质，再复用各池的轮换、跨池 Item/meta/NBT 变体去重和附魔等级解析，最后按普通→罕见→稀有→珍宝稳定排序。购买交易卡仍使用既有双列、每列四行的自适应布局，数量和同步快照均直接使用同一 8 槽常量。

兼容性与影响：未改动 registry ID、Packet discriminator、NBT key、`PastoralWorldData` 名称或数据版本。旧存档中的 10 槽 `DailyOfferState` 在读取时会因当前数量校验失败而丢弃；下一次商人交互会在逻辑服务端生成当天的新 8 槽列表，旧当天库存不会迁移，其余商人身份、轮换状态和金币保留。

验证：Temurin Java 8 `1.8.0_504` 下 `compileJava`、`compileTestJava`、`merchantCatalogSelfTest`、`processResources build` 均 PASS。自测覆盖 8 槽常量、40/30/20/10 的全部边界、品质排序与旧 10 槽 NBT 拒绝路径。Forge 工具链检查为 0 问题；静态审计为 0 ERROR、5 条既有 packet-thread WARNING。`exportReleaseJar` 已导出 357,843-byte release JAR，SHA-256 `f990838217fbb6f9a5b616237b39ecda24881ea2f288aa8099888d09442fd29f`，`unzip -t` PASS。游戏内跨日概率、交易卡视觉和 Dedicated Server 为 NOT RUN，需可操作运行环境。

## 维护：行情书可用性与村庄商人范围（2026-09-05）

实现：创造模式标签补齐 `itemGroup.lisbam_pastoral_economy=聆竹の休闲田园经济`。行情书购买页增加本地化名称/key 搜索、受限三列图标网格与鼠标滚轮逐行浏览；不再为整个购买目录创建越界按钮。选中商品仍优先向服务端请求，其他条目每 4 tick 渐进预取；若服务端 2 tick 合并节流导致请求未回包，40 client tick 后重新请求，消除永久“正在读取行情”。市场图书背景改为完整原版 `demo_background.png`，可见文本仍使用 `FontRenderer`。

审查并修正商人村庄范围：原先实体以站点的 64 格家区游荡、随机出生使用 128×128 方形，和 128 格村庄参考范围不一致。现在 `EntityMerchant` 将村庄中心作为持久 NBT 家点，以同一 128 格半径限制和寻路返航；`VillageService` 只在圆形范围内随机安全出生。旧实体缺少村庄中心 NBT 时先兼容站点坐标，下一次维护根据既有 `VillageRecord` 写入准确中心；没有改动 merchantId、villageId、stationId、WorldSavedData 名称、Packet discriminator 或 Offer/库存语义。

验证：Temurin Java 8 `1.8.0_504` 下 `compileJava`、`processResources`、`marketCoreSelfTest`、`marketPacketSelfTest`、`merchantCatalogSelfTest` 和最终 `build` 均 PASS。严格 Forge audit 为 0 ERROR、5 条既有 S2C/Proxy 主线程桥接 WARNING；`exportReleaseJar` 已覆盖导出 356,074-byte release JAR，SHA-256 `0a660a9d465461ce9ed0240b2ec2af31bef2b853a8789f3a16b65318851ebb33`，`unzip -t` PASS。游戏内 GUI、刷怪蛋收编、返航/解绑和 Dedicated Server 为 NOT RUN，需可操作运行环境。

## 维护：强制覆盖导出发布 JAR（2026-09-05）

更新 `AGENTS.md` 的发布规范：即使同名 release JAR 已存在，每次代码、资源、Gradle 或配置更新后仍必须实际运行最终 `build`，由 `exportReleaseJar` 直接覆盖导出，不能沿用上一次成品。本次按该规则使用 JDK 8 完成 `compileJava`、`processResources`、`build`，并确认 `release/LisBam_PastoralEconomy-1.0.jar` 非空且压缩完整。

## 维护：经济市场与商品价格统一修复（2026-09-05）

实现：按任务书将全部经济数值统一扩大 10 倍，商人购买改为单个物品单价与按件结算；有限库存按 16 组→4 组、8 组→2 组、4 组→1 组、1 组→1 个并换算为实际物品数。每日购买列表在服务端跨普通/罕见/稀有/珍宝池去重，仍保持 4+3+2+1 条。市场默认链式行情使用类别上下限、动态回归概率、边界强制反弹和整数方向保护；删除旧的 1/2 金币专用保护。交通旅行/接入费更新为 `round50(400 + 1.20D)` / `round100(4000 + 12D)`，GUI 删除“一组数量”并在原位置显示实际剩余数量。

根因与影响：旧实现把价格、购买数量和库存绑定到整组，且各购买池独立抽取，可能产生重复商品；市场无明确边界和固定回归概率，低价取整还会冻结。现在统一使用单件价格和 `remainingItems` NBT，缺少该字段或含非法池/等级/重复商品的旧 Offer 直接视为无效并按当前规则重生成；任务书明确不迁移旧价格、余额、库存或交通费用。

验证：此前 Temurin Java 8 的完整 `build` 已通过并导出本文顶部记录的发布 JAR；本次最终修订另以 JDK 8 `javac -source 8 -target 8` 编译全部主/测试源码，并运行市场、网络、商人和交通自测通过。尝试再次调用 Wrapper 时，Gradle 4.9 daemon 因当前沙箱禁止枚举本地网络接口而无法启动（`java.net.SocketException: Operation not permitted`），故该次 Wrapper `build` 记为 NOT RUN；已有发布 JAR 已执行 `unzip -t`。游戏内跨日、旧存档和 Dedicated Server 完整流程仍需可用运行环境。

## 维护：链式市场波动、设置与界面输入（2026-09-04）

实现：默认市场首次初始化使用冻结基础价，之后 `PastoralWorldData` 按每一遗漏世界日以昨日持久化快照推进；`MarketPriceGenerator` 以类别波动率的 25%～100% 生成单日步长，偏离基础价越远时回归概率按 50%/55%/65%/75%/85% 提高，并将结果限制在类别最低/最高倍率内，边界下一日强制反弹，整数取整后保留可见方向变化。新增 common `ModSettings` 和物理客户端 Mod List 配置界面：`market.moreStableMarketVolatility=false` 为新默认，`true` 使用独立稳定公式但同样采用新基础价和价格上下限。配置切换只在未来跨日读取，绝不重新生成同日或已保存的历史价格；v7 `PastoralWorldData` 的现有当前/昨日快照和 30 日收购历史直接保留。

商人、行情书、交通站和交通确认界面均把原版“打开背包”按键（默认 E、遵从改键）映射到 `EntityPlayerSP#closeScreen`，使服务端 Container 同步关闭；蟹笼仍使用原版 `GuiContainer` 的已有行为。商人数量输入改为白字；金币 HUD 页边距从 4 增至 12 个 scaled pixels。交通“接入最近村庄”的确认按钮不再根据客户端余额禁用，点击总会发送现有请求；`TransportService` 继续在服务端重新查找候选、重算费用并原子检查余额，失败只同步状态且不扣款。

验证：Temurin Java 8 `1.8.0_504` 下 `check_toolchain.py` 为 0 issue；`compileJava`、`compileTestJava`、`marketCoreSelfTest`、`marketPacketSelfTest`、`transportCoreSelfTest`、`transportTravelSelfTest` 与最终 `compileJava processResources build` 均 PASS。严格 Forge 1.12.2 audit 为 0 ERROR、5 条既有 `packet-thread` WARNING；正式重混淆 JAR 已导出为 `release/LisBam_PastoralEconomy-1.0.jar`（344,635 bytes，SHA-256 `34ddca6a5f64e5e71eadecb36ffd8293d949874c60d9d6a8c99fb321462ff775`，`unzip -t` PASS）。游戏内 E 关闭、HUD 位置、配置 UI、默认/稳定模式跨日及余额不足确认仍需可操作客户端或 Dedicated Server，当前为 NOT RUN。

## 维护：原版确认、蟹笼 Tooltip 与金闪闪骨粉兼容（2026-09-04）

实现：交通方块的“接入最近村庄”和“移出节点”不再缩放自定义 `demo_background` 确认层，改为直接使用 Java 1.12.2 原版 `GuiYesNo`：原版泥土背景、白色无重影文字及原版是/否按钮；接入费不足时仍禁用“是”，最终操作仍只在确认后发送既有 Packet 5 action。交通改名输入框改为白字，选中节点的名称、坐标和旅行费统一为绿色。蟹笼 GUI 明确走原版 `GuiChest` 的 `drawScreen` 悬停链，因而蟹笼内部和玩家背包的物品都显示原版 Tooltip。

商人交易与行情书只为任意颜色羊毛建立显示用 `ItemStack`，将原版“白色羊毛”名称显示为“羊毛”，不更改商品 key、metadata、交易匹配或共享行情。金闪闪的骨粉 16×16 图标扩大了高对比金色、橙色和腐肉色颗粒覆盖；物品在 common preInit 注册为 `dyeWhite`，对白色羊毛羊转调原版白色 `ItemDye` 行为，并注册自己的发射器增强催熟行为，发射失败时仍按原版默认行为抛出物品。

根因与兼容性：`GuiContainer#drawScreen` 本身不调用 `renderHoveredToolTip`，而原版 `GuiChest` 会显式补上该调用，这正是蟹笼没有提示框的根因。原先自绘确认层的缩放背景与标题色对比不足；改用实际原版确认 GUI 后不再复刻背景、按钮或字体。新兼容仅为注册表行为，不添加 NBT、Capability、WorldSavedData、registry ID 或 Packet；现有存档不需要迁移。

验证：Temurin Java 8 `1.8.0_504` 下 `check_toolchain.py` 为 0 问题，严格 Forge audit 为 0 ERROR、5 条既有 `packet-thread` WARNING；`compileJava`、`processResources`、`goldenBoneMealSelfTest`、`crabTrapSelfTest`、`merchantCatalogSelfTest`、`marketCoreSelfTest`、`marketPacketSelfTest`、`transportCoreSelfTest`、`transportTravelSelfTest` 与最终 `build` 均 PASS。最终 release JAR 尺寸、SHA-256 与解压验证见本文件顶部最近成功构建。游戏内确认层、Tooltip、羊染色和发射器为 NOT RUN：当前环境没有可操作 Forge 客户端，服务器 EULA 未接受。

## 维护：原版标题直绘、交通确认与金闪闪的骨粉（2026-09-04）

实现：商人与交通的每个静态非按钮字符串现直接调用原版 `FontRenderer.drawString(..., 4210752)`；居中文字手动按字符串宽度定位，因此不再经过带阴影的 `drawCenteredString`。蟹笼状态文字也统一该颜色。交通面板把节点列表和“我的节点”标题下移到“接入最近村庄”按钮之后；最近村庄确认层改为留白更均衡的完整原版背景比例，并增加“服务端最终结算”说明。移出节点现在先显示相同风格的本地确认层，确认后才发送既有 `REMOVE` action。

新增 `lisbam_pastoral_economy:golden_bone_meal`：骨粉/腐肉/萤石粉无序合成 1 个，或骨头/三腐肉/三萤石粉无序合成 3 个。其 16×16 图标以原版骨粉轮廓为底，加入金色、橙色和少量腐肉色颗粒。逻辑服务端对中心可耕作物最多重复 8 次原版骨粉至成熟、对同层 5×5 其余作物各执行一次；草方块/花则对对应草层 5×5 的每个草方块各执行一次。普通目标仍保留一次原版骨粉路径，只有实际生效才消耗一份。

根因与兼容性：`GuiScreen#drawCenteredString` 的阴影通道是重影根因，颜色常量相同并不能等同工作台/熔炉标题。确认层只是客户端输入节流，Packet 5 action ordinal、服务端重新验证、金币/交通数据和存档结构均未改。新物品没有 NBT、Capability 或 WorldSavedData 状态；旧存档不需迁移。

验证：Temurin Java 8 `1.8.0_504` 下 `check_toolchain.py` 为 0 问题，严格 Forge audit 为 0 ERROR、5 条既有 `packet-thread` WARNING；`compileJava`、`compileTestJava`、`processResources`、`goldenBoneMealSelfTest`、`crabTrapSelfTest`、`transportCoreSelfTest`、`transportTravelSelfTest` 和最终 `build` 均 PASS。最终重混淆 `release/LisBam_PastoralEconomy-1.0.jar` 为 332,322 bytes，SHA-256 `efaa297f28b3d0f1a639a7275d22ead758b3c66026726da55ac97e6a2770d6d1`，`unzip -t` PASS；已确认包含新增物品类、模型、两份配方、16×16 PNG、GUI 类与双语资源，生产 class major version 为 52。游戏内与 Dedicated Server 为 NOT RUN：当前环境没有可操作 Forge 客户端，服务器 EULA 未接受。

## 维护：蟹笼槽位闭环与交通界面滚动（2026-09-04）

实现：蟹笼 0 号钓竿槽、1 号生肉槽在 `ContainerCrabTrap` 使用显式受限 Slot，与 TileEntity/Hopper 的同一物品规则一致；`TileCrabTrap#setInventorySlotContents` 也拒绝新放入的错误功能物品，2～19 号槽仍接受任意物品。读旧 NBT 使用内部兼容写入路径，既有错误功能槽物品保留并可取出。蟹笼 GUI 继续沿 `GuiContainer` 原版 Slot 悬停路径显示槽内和玩家背包物品 Tooltip。交通站改为紧凑的原版 `demo_background` 面板比例，取消绿色非按钮节点文字，选中节点以深灰 `>` 前缀表示；所有非按钮文字与商人交易界面同样使用原版工作台/熔炉标题深灰 `0x404040`，按钮恢复原版 `GuiButton` 绘制。列表区域加入鼠标滚轮逐行滚动，同时保留翻页箭头。

根因与影响：功能槽此前依赖默认 Slot 转发，而直接库存写入未执行功能物品校验，形成可绕过的路径；现在 GUI、Hopper 和直接写入共享 TileEntity 规则。旧存档不迁移、不删物：历史错误物品只会继续留在原槽，供玩家取出。交通变更仅为客户端本地布局和 `scrollOffset`，没有修改 Packet、金币、旅行验证、TileEntity NBT、Capability 或 WorldSavedData。

验证：Temurin Java 8 `1.8.0_504` 下 `check_toolchain.py` 为 0 问题，严格 Forge audit 为 0 ERROR、5 条既有 packet-thread WARNING；`compileJava`、`compileTestJava`、`crabTrapSelfTest`、`transportCoreSelfTest`、`transportTravelSelfTest`、`processResources` 和最终 `build` 均 PASS。`release/LisBam_PastoralEconomy-1.0.jar` 已重混淆导出，324,547 bytes，SHA-256 `1954d080fe12e8723e68a440f2e43c806cc4cda9deac5c6deed781d57f69b71a`，`unzip -t` PASS；包含更新后的 GUI、Container、TileEntity、双语资源、`mcmod.info` 与 `pack.mcmeta`。游戏内 GUI/鼠标滚轮/Tooltip 和 Dedicated Server 仍为 NOT RUN：当前环境没有可操作 Forge 客户端，服务器 EULA 未接受。

## 维护：蟹笼专用槽与交通交互（2026-09-04）

实现：蟹笼 0 号槽恢复仅接收钓竿、1 号槽恢复仅接收合法生肉，2～19 保持通用；Container 和所有方向 Hopper 共用 `TileCrabTrap` 校验，任意槽仍可取出。蟹笼 GUI 继续继承 `GuiContainer` 原版物品悬停路径。交通方块配方改为四角红石块、上下左右铁块、中央指南针。商人和交通界面的非按钮文字改为原版工作台/熔炉标题深灰色；交通界面去除查找按钮与主面板村庄详情，自动获取候选并以单按钮打开距离/接入费确认层，余额不足时确认禁用。

根因与影响：蟹笼先前将 `isItemValidForSlot` 放宽为任意有效槽，导致功能槽失去限制；交通“移出”只保留 inactive tombstone，方块拆除只删除世界登记，因此列表会显示“已移出”或“节点无效”。现在移出直接删除玩家节点，拆除同步删除世界节点、Tile UUID 和在线玩家引用，读取旧 inactive 或构造交通快照时清理残项。保留 Packet 5 action ordinal，服务端继续重算村庄候选、费用和余额。旧蟹笼 0/1 槽中已有的非功能物品不会删除，仍可取出；旧 inactive 节点在下次读取时清除。

验证：Temurin Java 8 `1.8.0_504` 下 `compileJava`、`processResources`、`crabTrapSelfTest`、`transportCoreSelfTest`、`transportTravelSelfTest` 和最终 `build` 均 PASS。严格 Forge 1.12.2 audit 为 0 ERROR、5 条既有 packet-thread WARNING 已复核为 Proxy/主线程桥接。正式重混淆 JAR 已导出为 `release/LisBam_PastoralEconomy-1.0.jar`，324,313 bytes，SHA-256 `5c5dfa7073cec1251675e1f626b9672a9aa0ef388af11bbc505b6e19815ed2c3`，`unzip -t` PASS；资源清单确认包含更新后的交通配方、双语资源和实现类。游戏内 GUI、方块破坏与 Dedicated Server 未运行：当前环境不能创建可操作 Forge 客户端，服务器 EULA 未接受。

## 维护：商人整组、行情书与蟹笼规则（2026-09-04）

实现：商人购买单位统一为目标物品的 Java 1.12.2 原版最大堆叠数；删除 `TradeCatalog` 中会再次引入自定义小包的固定数量字段，购买 GUI 与同步视图均使用“组”。所有 22 种实际可出售给商人的逻辑商品均进入行情书的最近 30 日历史，16 色羊毛保留为一条共享曲线。羊毛转线 JSON 配方接受全部羊毛 metadata。蟹笼成为可任意存取的 20 槽普通容器，全部方向 Hopper 都可访问全部槽位；0/1 仍仅作为钓竿/饵料读取位，2～19 仍为捕捞输出位。每轮等待调整为 100～600 秒，饵钓每级减少 100 秒。

兼容性与影响：商人 Offer 改为 `remainingItems` 实际物品数；缺少该字段的旧组库存 Offer 视为无效并在服务端按当天规则重新生成，不执行旧库存或旧价格迁移。旧市场数据首次访问会重建完整的 22 商品历史窗口。蟹笼既有库存、pending loot 与倒计时均可读取；下一次抽取使用新等待范围。

验证：严格 Forge 1.12.2 审计为 0 ERROR、5 条既有 `packet-thread` WARNING；`compileJava`、`processResources`、`merchantCatalogSelfTest`、`marketCoreSelfTest`、`marketPacketSelfTest`、`crabTrapSelfTest` 和最终 `build` 均 PASS。release JAR 为 322,772 bytes、SHA-256 `296b440379f22a0f15990eaf49af31fc5b1272397427d7b0f222e05428290bb2`，`unzip -t` 通过并确认含任意羊毛配方、行情书/蟹笼/商人相关类与双语资源。游戏内和 Dedicated Server 未运行：当前环境没有可操作 Forge 客户端，且未接受服务器 EULA。

## 维护：主世界敌对生成、动物骨头、配方与粘液球（2026-09-04）

实现：删除针对敌对生物的玩家索敌/反击限制、Creeper 爆炸方块保护及农田骚扰 AI。新增服务端 `OverworldMonsterSpawnEventHandler`，只拒绝维度 0 的自然 `MONSTER` 生成，刷怪笼、刷怪蛋、命令与模组主动生成不受影响。新增动物骨头死亡掉落：牛、哞菇、猪、羊、马、驴、骡、羊驼按 20% 双骨/50% 单骨结算；鸡、兔、狼、豹猫、鹦鹉按 25% 单骨结算。抢夺仅增强已成功的基础掉落，屠宰在骨头加入掉落列表后按既有倍率处理。新增任意羊毛转 4 线、8 金块环绕苹果转附魔金苹果的 JSON 配方。粘液球迁入稀有购买池，保持 4 个/480，日库存为 8 组；旧当天罕见槽的无限库存粘液球在首次读取时仅替换该槽位。

兼容性与影响：不变更 registry ID、Packet、Capability、玩家数据或根 WorldSavedData 版本。已存在的敌对实体不删除，恢复原版行为；历史 `farmHarassment` 数据读取时忽略、下次保存时删除，不影响市场、商人和交通。旧商人当天除上述已迁移的粘液球槽外的商品、库存和价格均保持。

验证：Temurin Java 8 `1.8.0_504` 下 `compileJava`、`processResources`、`animalBoneDropSelfTest`、`merchantCatalogSelfTest` 与 `pastoralWorldDataSelfTest` 均 PASS。最终 `build` PASS，包含 `test`、`reobfJar` 与 `exportReleaseJar`；严格 Forge 1.12.2 审计为 0 ERROR，5 条既有 `packet-thread` WARNING 已复核为网络同步处理器，未由本次引入。`release/LisBam_PastoralEconomy-1.0.jar` 已导出，为 325,829 bytes、SHA-256 `ece4347f519d2adab909099d8772f720e3225c95df1d5d11584300b6aa95e961`，`unzip -t` PASS，确认包含新事件类和两份配方、不含已删除的敌对行为类。`timeout 60s ./gradlew runServer` 实际进入 Forge/FML 引导与 coremod 阶段，但在模组发现前到达时限；`run/eula.txt` 保持 `false`，未接受 EULA。本次运行时行为尚未进行游戏内手测。

## 维护：内容书与实际实现核对（2026-09-04）

实现：以当前 Java、资源和配方为事实来源，修正《聆竹の休闲田园经济》内容书中的过期或不准确说明。行情书历史改为准确描述实际的最近 30 个世界日保留窗口与按需读取；商人改为中文姓名、四种农作服装、无上限的 `max(ceil(村民数/5),3)` 规模和现有受击/游泳/无声行为；蟹笼配方改为铁锭、铁栅栏与陷阱箱，工作条件改为直接接触相邻水方块。同时补充普通蜘蛛与洞穴蜘蛛的边界、农田骚扰的 `mobGriefing`/无掉落规则，以及无图标金币 HUD。`AGENTS.md` 现要求每次更新后同步更新内容书。

兼容性与影响：本次只更新维护规则、内容书、构建日志和重新导出的发布 JAR；没有变更游戏代码、资源、注册 ID、Packet、NBT、Capability、WorldSavedData 或玩家可体验行为，现有存档无需迁移。

验证：Temurin Java 8 `1.8.0_504` 下 `check_toolchain.py` 为 0 issue；Forge 1.12.2 static audit 为 0 ERROR、5 条既有 `packet-thread` WARNING。`compileJava`、`processResources`、`marketCoreSelfTest`、`marketPacketSelfTest`、`merchantCatalogSelfTest`、`merchantNameSelfTest`、`crabTrapSelfTest` 与最终 `build` 均 PASS。正式 JAR 已导出为 `release/LisBam_PastoralEconomy-1.0.jar`，`unzip -t` PASS，且已确认包含语言、蟹笼配方、商人服装和元数据。游戏内与 Dedicated Server 未运行：本次未改动运行时行为，且当前环境没有可操作 Forge 客户端，服务器 EULA 也未接受。

## 发行：版本 1.0（2026-09-04）

`build.gradle` 的项目版本更新为 `1.0`；既有 `processResources` 展开使 `mcmod.info`、JAR Manifest 的 Specification/Implementation Version 与发布文件名保持一致。该变更不涉及注册 ID、网络协议、存档、经济或玩法数据。

验证：Temurin Java 8 `1.8.0_504` 下 `check_toolchain.py`、`compileJava`、`processResources` 与 `build` PASS。Forge 1.12.2 strict audit 为 0 ERROR、5 条既有 `packet-thread` WARNING。正式重混淆 JAR 已导出为 `release/LisBam_PastoralEconomy-1.0.jar`，`mcmod.info` 和 Manifest 均确认显示 `1.0`，压缩包完整性检查通过。

## 维护：商人属性、姓名权重、皮肤与密度（2026-09-04）

实现：商人姓名的前十常见百家姓按人口比例加权，余下传统姓氏构成低频池；姓名仍只在逻辑服务端产生。商人的最大生命、移动速度与攻击伤害改为原版玩家基础值 20、0.1、1；新增原版 `EntityAISwimming` 与可游泳导航以在水中上浮，并明确移除环境、受伤和死亡声音。Steve 原皮退出运行时皮肤池，四种农作服装保持 1--4 的稳定索引，旧索引 0 读取后随机迁移到其中一种。商人目标数量从 `max(ceil(2n/5),3)` 回调为 `max(ceil(n/5),3)`，仍无上限。

兼容性与影响：没有变更 registry ID、Packet、WorldSavedData、MerchantRecord 或交易规则；旧存档只会对 `merchantSkin=0` 执行一次展示迁移，已有 1--4 服装和自定义姓名保留。下一轮商人维护会按新的半数目标停止多余商人。验证：Temurin Java 8 `1.8.0_504` 下 `check_toolchain.py` 为 0 issue，严格 Forge audit 为 0 ERROR、5 条既有 `packet-thread` WARNING；`compileJava`、`processResources`、`merchantCatalogSelfTest`、`merchantNameSelfTest` 与最终 `build` 均 PASS。正式 JAR 为 335,340 bytes，SHA-256 `57abbb324cd260231a29e9ae92b5a4338433d5ca42dad14a3a7ae3c5988edfa0`，`unzip -t` PASS；确认包含实体、姓名/皮肤类和四张服装 PNG，且不含原版 Steve 贴图引用。游戏内水面上浮、音效、旧皮肤迁移、实体密度收敛与 Dedicated Server 仍为 NOT RUN：当前环境无法创建可操作 Forge 客户端，且没有接受 EULA。

## 维护：商人中文姓名与作物商人皮肤（2026-09-04）

实现：新增服务端 `MerchantNameGenerator`，从常用百家姓和常用中文名字符池生成“1 姓 + 1--2 名”显示姓名。`EntityMerchant` 在新绑定和旧实体 NBT 读取时只在逻辑服务端补齐缺失/旧通用“商人”名称，直接复用原版 `CustomName` 实体 NBT；UUID、`MerchantRecord`、交易、价格和库存均未改动。实体 NBT 新增可选 `merchantSkin`，其后续兼容索引与抽样规则由上方维护记录取代；交易 GUI 标题显示实体已同步姓名，客户端不会生成名称或皮肤随机数。

兼容性与影响：旧存档中没有姓名/皮肤字段的商人在首次服务端加载时补写一次，已有非通用自定义姓名原样保留；`merchantSkin` 缺失或非法也只补写一次。没有新 Packet、registry ID、WorldSavedData schema 或 MerchantRecord 字段；重名允许，姓名不参与任何主键或交易验证。贴图基于原版 Steve 宽臂 64×64 UV 改绘，未修改模型、像素尺寸或贴图布局。

验证：源码/PNG 结构检查 PASS：四张服装均为 64×64、8-bit RGBA PNG，且均实际改绘原版 Steve 像素。此记录的初次构建环境问题已被后续可用 Temurin Java 8 环境取代；当前源码、资源、姓名自检与发行 JAR 验证状态以本文件上方最新维护记录为准。游戏内的旧商人首次加载、Chunk reload、重启、姓名显示、四种服装和 Dedicated Server 均待可运行的 Forge 环境验证。

## 维护：交通名称、交易禁用态、资源与商人规模（2026-09-04）

实现：交通方块的 Block 与新增专用 ItemBlock 直接采用同一无后缀显示键，中文固定显示“交通方块”，不再走会附加 `.name` 的默认显示路径。商人界面的当前页签、金币不足/库存不足的购买确认以及背包没有对应物品的出售确认均改为真正禁用，因此使用原版深色禁用字体；其他交易文字仍保持浅色。交通站和蟹笼替换为更简洁的原版风 32×32 像素材质，行情书使用独立绿皮书图标；蟹笼合成改为铁锭/铁栅栏交错外圈、中央陷阱箱。村庄商人目标数由 `clamp(ceil(n/5),3,8)` 改为不设上限的 `max(ceil(2n/5),3)`，并用 long 中间值计算。

根因与影响：交通方块依赖默认 Block/ItemBlock 本地化链时会额外添加 `.name`，导致面向玩家的名称异常；现在显示键完全明确。先前为统一文字颜色而保留了不可成交按钮的白色标签，缺乏不可操作反馈；现在仅这些按钮恢复原版禁用外观。没有更改 registry ID、Packet、WorldSavedData、Capability 或 TileEntity NBT；旧存档无需迁移，下一次村庄维护会自动按新公式补足商人。

验证：Temurin Java 8 `1.8.0_504` 下 `check_toolchain.py` 为 0 issue，严格 Forge audit 为 0 ERROR、5 条既有 `packet-thread` WARNING；`compileJava`、`processResources`、`merchantCatalogSelfTest` 与最终 `build` 均 PASS。`merchantCatalogSelfTest` 覆盖新的人口公式边界和 100/1000 村民的无上限增长。`release/LisBam_PastoralEconomy-1.0.jar` 已由 `exportReleaseJar` 导出，324,370 bytes，SHA-256 `c6ac683b7a9c7569d275ca0ab0c519cd2bb1c64803215c930c1a1032d1352b6c`，`unzip -t` PASS；JAR 已确认包含新 ItemBlock、三张 32×32 PNG、绿皮书模型和新蟹笼配方。游戏内 UI/贴图/合成、旧存档实际加载与 Dedicated Server 仍为 NOT RUN：当前环境不能创建可操作 Forge 客户端，且没有接受 EULA。

## 维护：交通界面/资源与村庄商人重复补生（2026-09-04）

实现：交通界面所有文字统一为商人界面使用的原版按钮正常浅色；不可用按钮仍不可点击，但通过客户端重绘保持文字不加深。交通站输入框同样覆盖普通/禁用两种颜色。村庄交通站从原版橡木木板改为石质交通站模型与 Material，并与玩家交通方块共享新 32×32 罗盘贴图；蟹笼换为同尺寸的木框铁栅贴图。当时补齐了标准 `.name` 本地化；该显示方案现已由上方维护记录的无后缀显式键取代。

根因与修复：`Block#getLocalizedName` 查找 `getUnlocalizedName() + ".name"`，旧语言资源只有无后缀键。村庄站模型又硬编码 `minecraft:blocks/planks_oak`。商人则在世界 Load 期间立即扫描短暂为空的 `loadedEntityList` 并补生；持久化的原商人稍后随 Chunk NBT 加入时才去重死亡。初始化路径现不补生，首次周期多等待一轮；MerchantRecord 记录最后实体 Chunk，补生前必须确认其已加载，且索引严格验证 active/roster/village/station 绑定。

验证：Temurin Java 8 `1.8.0_504` 下 `check_toolchain.py`、`compileJava`、`compileTestJava`、`merchantCatalogSelfTest`、`transportCoreSelfTest`、`transportTravelSelfTest`、`processResources` 与 `build` 均 PASS。Forge 1.12.2 strict audit 为 0 ERROR、5 条既有 `packet-thread` WARNING。release JAR 非空、可解压，内部两张贴图均确认是 32×32 PNG，村庄模型和中文 `.name` 键均已确认。游戏内/旧存档/Dedicated Server 为 NOT RUN：无可操作 Forge 客户端，且没有接受 EULA。

## 维护：商人交易实时同步、实体行为与字体一致性（2026-09-04）

根因与修复：`GuiMerchantTrade` 继承普通 `GuiScreen` 而未覆写暂停语义，故单人集成服务端在交易窗口打开时暂停；交易 C2S 包及其库存/金币/快照同步都只能在关窗后才运行。并且商人专用 `ContainerMerchantTrade` 不含玩家背包 Slot，交易服务直接变更 `InventoryPlayer` 不会被该 Container 的常规 slot 差量同步捕获。窗口现不暂停游戏；服务端成功结算同一 tick 立即同步窗口 0 玩家背包，CoinService 同步金币，并向所有当前打开该商人的窗口推送新快照。打开窗口时目标商人停止移动，关闭时解除；商人平时注视 8 格内最近玩家，受到玩家实际伤害后避开该攻击者 200 tick。交易界面的标签、卡片、数量和按钮统一为原版 `GuiButton` 正常浅色，不再因不可交易状态加深字体。

验证：Temurin Java 8 `1.8.0_504` 下 `compileJava`、`compileTestJava`、`merchantCatalogSelfTest`、`processResources`、`build` 均 PASS；后者包含 `test`、`reobfJar` 和 `exportReleaseJar`。Forge 1.12.2 strict audit 为 0 ERROR、5 条既有 `packet-thread` WARNING（通用注册器/Proxy 到客户端主线程桥接，未由本次引入）。发布 JAR 非空，`unzip -t` PASS，SHA-256 如上。游戏内单人、多人与 Dedicated Server 交互为 NOT RUN：当前环境无法创建可操作 Forge 客户端窗口，且没有接受 EULA。

## 维护：市场、经济与商人性能（2026-09-04）

实现：

- `PastoralWorldData` schema 升至 v7：删除无限增长的市场 processed-day 集合，14 种作物只持久化当前日前 30 天；超大 `/time` 前跳按 marketSeed 直接重建窗口，不再逐日补齐。v3--v6 旧市场可读，首次访问后写回 v7；第 31 天及更早的作物曲线会被回收。
- 删除主世界每 tick 的 `MarketService.tick`。行情书打开请求和商人价格读取才在服务端惰性刷新市场；行情书请求必须绑定实际 `ContainerMarketBook`，每会话 2 tick 限速。客户端在打开/关闭建立和销毁会话缓存，并使用连接内递增请求号拒绝迟到包。
- 商人交易快照一次取得 `MarketPriceSnapshot`，村庄维护只扫描一次 `loadedEntityList`，商人离站返航每 20 tick 最多重新寻路一次；商人 GUI 缓存出售数量，行情书缓存折线节点。
- 蟹笼倒计时保持逐 tick 逻辑，但仅每 20 tick/状态转换标脏，减少大量加载蟹笼造成的区块保存压力。

验证：

- Forge 1.12.2 strict audit：PASS（0 ERROR；5 条既有 packet-thread WARNING 已人工复核为 Proxy/S2C 主线程桥接或通用注册器）。
- 临时 Temurin Java 8 `1.8.0_504`：`compileJava`、`compileTestJava`、`marketCoreSelfTest`、`marketPacketSelfTest`、`merchantCatalogSelfTest`、`crabTrapSelfTest`、`pastoralWorldDataSelfTest`、`processResources`、`build`：PASS。`merchantCatalogSelfTest` 的八条 FML alternative-prefix 提示为既有模组附魔注册警告，任务仍 PASS。
- 游戏内/ Dedicated Server：NOT RUN。本环境没有可用图形显示；`run/eula.txt` 保持 `eula=false`，未代为接受 EULA。仍需手测书本关闭后无迟到显示、跨日重开、v6 存档首次迁移、商人返航与非正常退出后的蟹笼倒计时最多 19 tick 回退。

## 维护：行情书快速切换读取卡住（2026-09-04）

根因与修复：上一轮的 2 tick 服务端限流会静默丢弃冷却内的合法作物切换；客户端已先进入读取状态，因此没有回包即可永久卡住。`ContainerMarketBook` 现保留冷却内最新请求，并在其自身下一次服务端 Container tick 处理；首个请求仍立即处理。由此维持每会话最多每 2 tick 一次快照的性能边界，同时保证每次界面选择最终都有回应。

验证：`marketPacketSelfTest`、`compileJava`、`compileTestJava`、`processResources` 与 `build` 均 PASS（Temurin Java 8 `1.8.0_504`）；发布 JAR 非空且 `unzip -t` PASS。Forge 1.12.2 strict audit 实际运行结果为 0 ERROR、5 条既有保守 packet-thread WARNING；本次 C2S Handler 仍先调度到服务端主线程，S2C 警告均为既有 Proxy/客户端主线程桥接。

## 维护：行情书作物切换仍卡住（2026-09-04）

根因与修复：实机反馈证明“先显示小麦、再为所选作物单独发 C2S 请求”的路径仍不可靠。现在书本打开时的首个最新窗口请求会一次准备并用既有 Packet 2 下发 14 种作物的 30 天快照，全部共享首个 requestId；作物按钮只从会话缓存读取，不再触发网络/限流路径。若批量包异常未到，才保留既有合并后备请求。

验证：`compileJava`、`compileTestJava`、`marketPacketSelfTest`、`processResources` 与 `build` PASS；发布 JAR 非空且 `unzip -t` PASS。Forge 1.12.2 strict audit 实际运行结果为 0 ERROR、5 条既有保守 packet-thread WARNING；C2S 行情请求仍先切到逻辑服务端主线程，S2C 缓存写入仍经 ClientProxy 调度。

# 批次记录

## 第 01 批

状态：完成

实现：

- 使用官方 Forge 1.12.2-14.23.5.2859 MDK 建立工程与 Java 8/ForgeGradle 3 基线。
- 冻结工程名、artifact、modid、Java 根包、中文说明和正式 Logo 资源路径。
- 建立主入口、Common/Client Proxy、统一 Registry Event 入口、唯一 Network Channel 与 GUI Handler/ID 入口。
- 建立资源目录、中英文语言文件、开发说明和长期项目文档；未注册任何玩法对象或 Packet。

验证：

- Toolchain check：PASS。
- Forge 1.12.2 audit：PASS（0 ERROR；1 条已人工审查的无 Packet 通用网络签名 WARNING）。
- `./gradlew compileJava`：PASS。
- `./gradlew processResources`：PASS。
- `./gradlew build`：PASS，生成 `LisBam_PastoralEconomy-0.1.0.jar`。
- `./gradlew runClient`：NOT RUN（任务实际进入客户端初始化，但 WSL 无可用 LWJGL 显示模式，在游戏窗口创建前失败）。
- `./gradlew runServer`：NOT RUN（任务进入 `MinecraftServer` 启动路径，但在模组加载前要求用户确认 Mojang EULA；未代为接受）。

遗留：

- 客户端主菜单、Mods 列表、Logo 可视确认、新世界创建与进入尚待具备可用图形显示的环境。
- Dedicated Server 完整模组加载尚待用户在测试环境自行接受 EULA 后执行。
- 本轮 WSL 验证中，Java 8 的 `libzip` 在 `/mnt/e` 挂载盘的 ForgeGradle 重映射输出上触发 SIGBUS；验证时临时将 `build/` 输出转至 Linux `/tmp`，完成后已将输出移回工程。下次在该 WSL 挂载盘执行完整构建时可能需要相同临时规避；这不是模组代码或 Forge API 问题。

## 第 02 批

状态：完成。

实现：

- 新增版本化 Player Capability（`lisbam_pastoral_economy:player_data`），保存非负 `long` 金币以及交通稳定字段预留，并在 Clone 时深复制。
- 新增跨维度唯一 `PastoralWorldData` 根（服务端主世界 MapStorage），保存数据版本、首次初始化状态和未来 market/merchant/transport 空 Section。
- 首次加载世界时仅一次设置 `keepInventory=true` 并持久化标记，后续不再触碰该规则。
- 新增服务端权威 `CoinService`：拒绝负数、超额扣款和 long 溢出；成功变更才同步。
- 冻结 Packet 0 为 S2C 金币同步，客户端主线程缓存、连接生命周期清理和右上角本地化 HUD。

主要文件：

- `data/player/*`、`data/world/PastoralWorldData.java`
- `event/PersistenceEventHandler.java`
- `network/message/SyncCoinsMessage.java`、`network/handler/SyncCoinsMessageHandler.java`
- `client/*`

验证：

- Toolchain check：PASS（2026-09-03，Java 8 / Forge 14.23.5.2859）。
- Forge 1.12.2 audit：PASS（0 ERROR；2 条已人工审查的 `packet-thread` WARNING。它们分别来自通用注册方法和无客户端引用的 S2C Proxy 桥；真实客户端写入在 `ClientCoinSyncExecutor` 的 `Minecraft#addScheduledTask` 主线程任务中，服务端没有 Packet Handler 状态写入）。
- `./gradlew compileJava`：PASS。
- `./gradlew processResources`：PASS。
- `./gradlew build`：PASS（含 `compileTestJava`、`test`、`reobfJar`）。
- `PlayerDataSelfTest`：PASS（初始/负数/超额/精确扣款/0 操作/Long.MAX_VALUE 溢出、NBT 往返与负值修复、Clone 复制、缓存清理、`0`/`999`/`1,000`/`12,580` 格式）。
- 最终 Jar：PASS（包含本批生产类、语言资源和已展开 `mcmod.info`；不包含测试自检类）。
- `./gradlew runClient`：NOT RUN（实际运行至 Minecraft 初始化，但 WSL 的 LWJGL `LinuxDisplay.getAvailableDisplayModes` 抛出 `ArrayIndexOutOfBoundsException`，窗口创建前结束）。
- `./gradlew runServer`：NOT RUN（两次实际启动均进入 Forge coremod 引导；第二次在加载 Srg→Mcp 映射后超过 60 秒没有前进，已安全中断。`run/eula.txt` 仍为 `false`，未代为接受 Mojang EULA）。
- 新玩家、死亡、维度、重登、保存重载/重启、keepInventory 一次性初始化、真实 HUD/F1：NOT RUN（需要可进入世界的客户端或 Dedicated Server 环境）。

遗留：

- 市场、商人、交通业务、蟹笼、附魔与其他后续批次内容均未实现。
- 上述真实游戏生命周期验收待具备可用图形显示或可完成 Forge 引导、且由用户自行接受 EULA 的测试环境执行；这不是已知代码失败。

## 第 03 批

状态：完成。

实现：

- 对精确的原版 Zombie、Zombie Villager、Husk、Skeleton、Stray、Creeper、Enderman 与普通 Spider 移除自然玩家寻敌；Cave Spider、Pig Zombie 和其他类型未改动。
- 玩家直接造成实际伤害后，仅直接攻击者可在短期内被原版反击；仇恨不会扩散至其他玩家。Enderman 注视不再触发攻击，但直接攻击仍会反击。
- 精确 Creeper 不再自然追逐/引爆玩家；直接攻击反击和手动点燃保留。其爆炸只清空方块影响列表，实体影响和其他爆炸源保持原样。
- 新增农田骚扰 AI：满足无战斗、48 格活玩家、`mobGriefing=true` 等条件时，六类非 Creeper 指定怪物按规定时序只破坏一格指定作物或空耕地，不掉落物。
- `PastoralWorldData` 升至 v2，迁移 v1，持久化同维度半径 32、6,000 Tick 窗口、最多 6 次成功的农田骚扰记录；实体 ForgeData 持久化候选和成功冷却。

主要文件：

- `entity/PastoralMobRules.java`、`entity/RetaliationTracker.java`
- `entity/ai/EntityAIFarmHarassment.java`
- `event/MobBehaviorEventHandler.java`
- `data/world/PastoralWorldData.java`
- `src/test/java/lisbam/pastoraleconomy/data/world/PastoralWorldDataSelfTest.java`

验证：

- Toolchain check：PASS（Java 8 / Forge 14.23.5.2859）。
- Forge 1.12.2 audit：PASS（0 ERROR；2 条继承自第 02 批网络桥的已审查 WARNING）。
- `./gradlew compileJava`：PASS。
- `./gradlew processResources`：PASS。
- `./gradlew build`：PASS（含 `compileTestJava`、`test`、`reobfJar`）。
- `PlayerDataSelfTest`、`PastoralWorldDataSelfTest`：PASS；后者覆盖配额上限、半径、维度隔离、持久化、过期清理和 v1→v2 迁移。
- 成品 Jar：PASS（包含第 03 批生产类，不包含测试自检类）。
- `./gradlew runClient`：NOT RUN（任务实际到 Minecraft 客户端启动，但 WSL 的 LWJGL `LinuxDisplay.getAvailableDisplayModes` 在窗口创建前失败）。
- `./gradlew runServer`：NOT RUN（实际进入 `MinecraftServer`，随后因 `run/eula.txt` 仍为 `false` 要求接受 Mojang EULA；未代为修改）。
- 任务单的真实世界怪物、爆炸、农田及存档重载验收：NOT RUN（需要可进入世界的客户端或已由用户接受 EULA 的 Dedicated Server）。

遗留：

- 没有已知的编译、资源、Java 8 或 Forge 1.12.2 API 问题。
- 上述真实游戏验收仍待具备可用图形显示或可完成 Dedicated Server 启动的环境执行；没有将此环境限制伪记为功能 PASS。

## 第 04 批

状态：完成。

实现：

- 建立唯一不可变 `MarketCatalog`、稳定经济 key、Item/meta 映射、完整冻结基础价格和 8 类固定波动率；14 种指定作物为唯一历史追踪项，苹果与畜牧品被排除。
- 建立确定性 `MarketPriceGenerator`：marketSeed 从 world seed 与固定盐稳定派生；u1/u2 只由 marketSeed、世界日、commodity key 与不同 salt 决定；唯一价格公式为 `max(1, Math.round(base * (1 + (u1 + u2 - 1) * volatility)))`。
- `PastoralWorldData` 升至 v3：持久保存 marketSeed、首次市场日、当前/昨日冻结快照、processed days 和 14 条无限历史；v2→v3 保留已有 keepInventory 与 Farm Harassment 数据。
- `PersistenceEventHandler` 仅在逻辑服务端主世界 END Tick 推进市场；睡眠/自然跨日与普通 `/time add` 顺序补齐，回拨复用已处理日并在查询时隐藏当前日之后的历史。
- 新增只读、服务端权威 `MarketService`，提供目录、当前/昨日价格、趋势、最近 30 日、旧页及单日历史；没有市场 Packet、GUI、商人、库存或交易。

主要文件：

- `market/*`、`data/world/PastoralWorldData.java`、`event/PersistenceEventHandler.java`
- `src/test/java/lisbam/pastoraleconomy/market/MarketCoreSelfTest.java`

验证：

- Toolchain check：PASS（ForgeGradle 3 / Forge 14.23.5.2859 / Gradle 4.9；本会话用 Temurin 8 实际构建）。
- Forge 1.12.2 audit：PASS（0 ERROR；2 条继承自第 02 批网络桥的已审查 WARNING）。
- `./gradlew compileJava`、`./gradlew processResources`、`./gradlew build`：PASS。
- `marketCoreSelfTest`：PASS（100 次确定性、8 类波动、最低价、小麦=5、14 历史、30 日/旧页、跨日、重载、回拨、v2→v3）。
- `./gradlew runClient`：NOT RUN。实际进入 Forge 客户端引导并写入 FML 日志，但在模组发现/窗口创建之前停止，无法进入主菜单或世界。
- `./gradlew runServer`：NOT RUN。实际进入 Forge Dedicated Server 引导并加载 Srg→Mcp 映射，但未继续到模组加载、EULA 提示或可创建世界的阶段；未改动 `run/eula.txt`。
- 真实单人、多维度、多人、睡眠和 Dedicated Server 重启仍依赖可用运行环境与用户接受 EULA。

遗留：

- 行情书/图表、商人、库存、买卖和市场网络同步属于后续批次，尚未实现。
- 没有已知的市场公式、存档迁移或 Forge 1.12.2 API 问题。

## 第 05 批

状态：完成。

实现：

- 注册可无限使用的 `lisbam_pastoral_economy:market_book`；主/副手右键仅由服务端打开 GUI，不消耗物品，也不触发市场刷新。
- 加入书 + 小麦 → 1 本行情书的无序 1.12.2 recipe、中英文名称及 item model；当前模型使用本模组绿皮书贴图。
- 分配稳定 GUI ID 0，以无槽位 common Container 配合 ClientProxy 的 `GuiMarketBook` 完成 Forge 1.12.2 GUI 协议；GUI 默认小麦，严格复用目录中的 14 条历史作物，显示今日/昨日、趋势、最多 30 点单作物折线、翻页与真实前一点悬停差值。
- 追加同一 `lb_pastoral` channel 的 Packet 1（有界 C2S 请求）和 Packet 2（最多 30 点 S2C 快照）。服务端在主线程验证玩家、商品、游标和未来日期；客户端缓存按商品/游标/请求号隔离迟到响应，连接与断开均清空。
- `MarketService` 增加显示专用的只读快照路径；其不调用市场推进/初始化逻辑，所以打开、切换或翻页不会重抽价格或新增历史点。

主要文件：

- `item/ItemMarketBook.java`、`item/ModItems.java`、`registry/RegistrationHandler.java`
- `gui/ContainerMarketBook.java`、`gui/GuiIds.java`、`gui/ModGuiHandler.java`、`client/gui/GuiMarketBook.java`
- `market/MarketHistorySnapshot.java`、`market/MarketService.java`
- `network/message/RequestMarketHistoryMessage.java`、`network/message/SyncMarketHistoryMessage.java`、对应 handlers 与 `ModNetwork.java`
- `client/ClientMarketState.java`、`ClientMarketSyncExecutor.java`、`ClientModelRegistry.java`
- `assets/.../models/item/market_book.json`、`recipes/market_book.json`、两份语言文件

验证：

- Toolchain check：PASS（结构确认 ForgeGradle 3 / Forge 14.23.5.2859 / Gradle 4.9；默认 PATH 无 Java，实际 Wrapper 验证使用 Temurin 8）。
- Forge 1.12.2 audit：PASS（0 ERROR；3 条 `packet-thread` WARNING 已审查：C2S 行情 handler 明确服务端调度，两个 S2C Proxy bridge 的真实客户端写入均调度至 `Minecraft#addScheduledTask`）。
- `./gradlew compileJava`、`./gradlew processResources`、`./gradlew build`：PASS。
- `marketCoreSelfTest`：PASS，第 04 批市场确定性、历史分页、重载与回拨回归通过。
- `marketPacketSelfTest`：PASS，验证 key/点数边界、快照往返、首点真实前日状态和同窗口迟到回包不会覆盖新快照。
- 成品 Jar：PASS，包含本批生产类、行情书模型、无序配方、语言资源、`mcmod.info` 与 `pack.mcmeta`。
- `./gradlew runClient`：NOT RUN。实际进入 Forge/FML 引导、模组目录搜索与 coremod 阶段，但本 WSL 运行环境在本模组加载和窗口创建前终止，无法进行视觉/GUI 手测。
- `./gradlew runServer`：NOT RUN。实际进入 Forge/FML Server 引导和 coremod 阶段，但在完成模组加载前终止；`run/eula.txt` 保持 `false`，未代为接受 EULA。

遗留：

- 游戏内物品模型、无序配方、GUI 缩放、跨日、重载、多玩家、三维度和恶意包的端到端操作测试，等待可进入世界的图形客户端或由用户接受 EULA 的 Dedicated Server 环境。
- 商人收购、玩家交易、库存、市场预测和其他第 06 批之后内容未实现。

## 第 06、07 批（合并）

状态：完成。

实现：

- 注册 Harvest、Farmland Walker、Pastoral Favor、Fine Cultivation、Felling、Slaughter、Fleetfoot、Night Vision 八个 RARE、非宝藏附魔；中英文名称、精确装备限制、Harvest/Fortune 和 Slaughter/四个战斗附魔互斥均已完成。
- 建立单一服务端农业事件流水线：成熟动作预捕获、原版掉落、Harvest 额外作物、Fine Cultivation 概率/真实种子消耗/age 0 补种、Pastoral Favor 单次 XP 判定；自动化和 Fine Cultivation 补种不产生额外田园眷顾。
- 新增保守原版树结构识别与逐块 `tryHarvestBlock` 伐木、严格动物白名单的直接近战 Slaughter 伤害/既有掉落倍增、服务端 Fleetfoot/Farmland Walker 属性修饰符、物理客户端 Night Vision 逐帧本地显示。
- 附魔台、附魔书、旧版 Librarian 和铁砧均使用 Forge 1.12.2 原版 Enchantment Registry 行为；没有重复候选或自定义交易/铁砧实现。

主要文件：

- `enchantment/EnchantmentPastoral.java`、`enchantment/ModEnchantments.java`、`registry/RegistrationHandler.java`
- `agriculture/AgricultureRules.java`、`event/AgricultureEnchantmentEventHandler.java`
- `event/MovementEnchantmentEventHandler.java`、`event/TreeFellingEventHandler.java`、`event/SlaughterEnchantmentEventHandler.java`
- `client/ClientNightVisionRenderHandler.java`、语言文件、`EnchantmentSelfTest.java`

验证：

- Toolchain check：PASS（Java 8 / ForgeGradle 3 / Forge 14.23.5.2859 / Gradle 4.9）。
- Forge static audit：PASS（0 ERROR；3 条既有 `packet-thread` WARNING 已审查，和本批无关）。
- `./gradlew compileJava`、`./gradlew processResources`、`./gradlew build`：PASS（含 `reobfJar`）。
- `enchantmentSelfTest`：PASS，覆盖八个定义、等级/兼容性/装备限制及 Harvest/Fine Cultivation/Pastoral Favor 的边界随机公式。
- 成品 Jar：PASS，包含八个附魔生产类和语言资源，测试类不在 Jar。
- `runClient`、`runServer`、游戏内手测：NOT RUN。两项任务均实际进入 Forge/FML/coremod 引导，但在本模组加载前终止；客户端未创建窗口，Server 未到达 EULA 检查，`run/eula.txt` 保持 `false` 且未代为接受。

遗留：

- 没有已知的编译、资源、Java 8 或 Forge 1.12.2 API 问题。
- 附魔台、图书管理员、铁砧、农业、相邻树、动物、移动、夜视、多人和 Dedicated Server 的端到端操作测试，需在可运行客户端和用户已接受 EULA 的 Dedicated Server 环境执行；这些不是后续批次内容。

## 第 08～10 批（合并）

状态：实现完成，构建与静态检查通过；真实世界端到端验收待运行环境。

实现：

- 仅在主世界低频维护 1.12.2 `VillageCollection`，识别至少 2 名村民的聚落，按 128/64/160 格参考去重并持久化稳定 villageId、中心、人口和 active 状态。
- 新增受保护 `village_station` 方块与 TileEntity；站点拥有稳定 stationId、villageId、role=VILLAGE，安全放置限制在已加载中心 12 格内，不覆盖箱子、门、农田、TileEntity 或非空气空间；异常消失后保留身份并可恢复。
- 注册独立 `EntityMerchant`（Steve 默认皮肤占位），全天、无主动攻击、可受伤死亡、`canDespawn=false`，绑定 MerchantRecord 并在距站点超过 32 格时归位；当前村民人口目标为 `max(ceil(2n/5),3)`，维护幂等补足且不设置人数上限。
- `PastoralWorldData` 升至 v5，在 merchant section 持久化 Village/Station/Merchant、每日 6 收购 + 10 购买 Offer、共享有限库存、可持久化循环轮换状态和特殊附魔书已解析等级；实体死亡不会重置逻辑身份或库存。
- 新增数据驱动 `TradeCatalog`：完整收购目录（含 16 色羊毛逻辑匹配和牛奶桶返桶规则）、普通 56 条和罕见 43 条购买目录；当时普通 4、罕见 3 启用，稀有 2、珍宝 1 保留禁用槽位；金锭、熔岩桶、岩浆膏、兔子脚分别为 16 bundle/次库存（稀有/珍宝栏已在第 11 批启用）。
- 新增服务端 `MerchantTradeService`、slotless `ContainerMerchantTrade`、Packet 3/4 和客户端 `GuiMerchantTrade`。最终交易在服务端主线程重新检查 Merchant、Session、世界日、Offer、MarketService 价格、背包、余额、库存和溢出；出售先移除物品，牛奶桶原子返空桶，购买先容量模拟并维护共享 bundle 库存；requestId/单会话 guard 防止重复包。GUI 提供逐栏数量加减、数量与预计总价显示。

主要文件：

- `merchant/*`、`entity/EntityMerchant.java`、`entity/ModEntities.java`
- `block/*`、`tile/TileVillageStation.java`、`event/VillageStationProtectionEventHandler.java`
- `data/world/PastoralWorldData.java`、`market/MarketCatalog.java`
- `merchant/MerchantTradeService.java`、`gui/ContainerMerchantTrade.java`
- `network/message/*MerchantTrade*`、对应 handlers、`network/ModNetwork.java`
- `client/GuiMerchantTrade.java`、`client/RenderMerchant.java`、Client merchant cache/executor
- `assets/lisbam_pastoral_economy/blockstates/village_station.json`、模型和语言键

验证：

- `check_toolchain.py`：PASS（ForgeGradle 3、Forge 1.12.2-14.23.5.2859、Gradle 4.9；默认 PATH 无 Java，使用 Temurin 8 Wrapper 实测）。
- `audit_forge1122.py`：PASS（0 ERROR；严格模式报告的 4 条 packet-thread WARNING 均为已审查的 Proxy/主线程桥接，不涉及未调度的权威状态写入）。
- `./gradlew compileJava`：PASS。
- `./gradlew processResources`：PASS。
- `./gradlew compileTestJava`：PASS。
- `./gradlew merchantCatalogSelfTest`：PASS。
- `playerDataSelfTest`、`pastoralWorldDataSelfTest`、`marketCoreSelfTest`、`marketPacketSelfTest`、`enchantmentSelfTest`、`merchantCatalogSelfTest`：均以退出码 0 通过。
- `./gradlew build`：PASS（含 `test`、`reobfJar`）；成品 Jar 已复制至 `release/LisBam_PastoralEconomy-0.1.0.jar`。
- `runClient`、`runServer`、真实村庄/多人/保存重启/跨日交易：NOT RUN；客户端受 WSL LWJGL 显示环境限制，Dedicated Server 受 `run/eula.txt` 未接受 EULA 限制。

取舍与遗留：

- Merchant 基础生命 20、移动速度 0.5、活动半径 12、交互距离 8 格，作为任务单未冻结参数的 1.12.2 原版类人基线。
- 下界疣与紫颂果在项目既有目录中属于 secondary sell pool，已按该正式映射录入 6 条收购 Offer 轮换。
- 当时后续稀有/珍宝完整商品池未实现，已由第 11 批补齐；玩家自建交通网络、线路解锁与旅行系统仍未实现。

## 第 11 批

状态：实现完成，构建与静态检查通过；真实世界/多人重启验收受运行环境限制。

实现：

- `TradeCatalog` 新增完整 Rare 27 项、Treasure 普通 21 项和 12 种特殊附魔候选；每日购买结构固定为普通 4 + 罕见 3 + 稀有 2 + 珍宝 1。
- 特殊附魔先选类型再按冻结权重解析等级，`DailyOffer` 持久化已解析等级；购买生成真实 1.12.2 `Enchanted Book`，价格使用对应等级 MarketCommodity。
- 旧 v4 当日 10-slot 占位 Offer 迁移时仅补齐末尾 Rare/Treasure 三栏，保留已有普通/罕见库存；`PastoralWorldData` schema 升至 v5。
- Rare/Treasure Item/meta、库存、波动率、唱片 12 项及目录唯一性加入自动自检。

验证：

- `check_toolchain.py`：PASS（Forge 1.12.2-14.23.5.2859 / Gradle 4.9；构建使用 Temurin 8）。
- `audit_forge1122.py`：0 ERROR；严格模式 4 条既有 packet-thread WARNING 已审查。
- `compileJava`、`processResources`、`compileTestJava`、六项 self-test（含 `merchantCatalogSelfTest`）：PASS。
- `./gradlew build`：PASS（含 `test`、`reobfJar`）；Jar 已复制至 `release/LisBam_PastoralEconomy-0.1.0.jar`。
- `build/libs` 与 `release` Jar SHA-256：`f56e1531a217c60160697e97655863cc6c74fc78e500d1b31851516f849934af`，大小 213,878 bytes；不含 `mods.toml` 或测试类。

遗留：

- `runClient`：NOT RUN。客户端在 WSL 无显示模式时于 LWJGL `LinuxDisplay.getAvailableDisplayModes` 初始化阶段崩溃，未创建窗口或进入世界。
- `runServer`：NOT RUN。Temurin 8 下实际进入 Forge/FML coremod 与 Srg→Mcp 映射阶段，90 秒超时（exit 124）后安全结束，尚未完成模组加载或 EULA 检查；`run/eula.txt` 保持 `eula=false`。
- 真实多人/重启/跨日交易仍为 NOT RUN，需要可完成 Forge 引导的 Dedicated Server/游戏环境。
- 玩家自建交通网络、线路解锁和正式旅行系统属于后续批次，未实现。

## 第 12～13 批（合并）

状态：实现完成；静态、编译、资源和自检通过；真实客户端/服务器交互测试记录见测试清单。

实现：

- 注册可回收 `crab_trap` Block、ItemBlock 和 TileEntity；当前 1.12.2 JSON 合成配方为铁锭/铁栅栏交错外圈和中央陷阱箱，BlockState、模型、双语名和简约 32×32 蟹笼贴图均已入库。
- 实现 `TileCrabTrap` 的固定 20 槽、服务端状态机、相邻原版水/流动水判定、100～600 ticks、Lure 非正等待重抽、每轮 Lure/Luck/饵料快照、原版 `GAMEPLAY_FISHING` 一次战利品、18 格完整容量预检与持久 pending loot。
- 钓竿不耗耐久；有饵在 Lure 后按 `(ticks + 1) / 2` 整数向上取整，且仅在战利品完整入栏后按 50% 判定消费一块当前仍存在的合法肉。
- 使用 1.12.2 `ISidedInventory` 完成自动化：上方/四侧只能向饵料槽输入生牛肉、生猪排、生鸡肉、生羊肉或生兔肉；下方只能从收获栏抽取，钓竿和饵料不会被输出。
- 新增 Container/Gui、普通点击及按规则的 shift-click，并同步 6 个只读捕捞状态字段；Block 破坏只掉落一次本体、20 槽库存和 pending loot。

主要文件：

- `block/BlockCrabTrap.java`、`tile/TileCrabTrap.java`、`crabtrap/CrabTrapRules.java`
- `gui/ContainerCrabTrap.java`、`client/gui/GuiCrabTrap.java`、现有注册/GUI/Proxy/Model 入口
- `assets/.../crab_trap.*`、语言文件、`CrabTrapSelfTest.java`、`build.gradle`

验证：

- Toolchain：PASS（ForgeGradle 3 / Forge 14.23.5.2859 / Gradle 4.9；临时 Temurin 8 `1.8.0_504` 实际构建）。
- 严格 Forge audit：PASS（0 ERROR；4 条既有 packet-thread WARNING 已审查）。
- `./gradlew compileJava`、`./gradlew processResources`、`crabTrapSelfTest`：PASS。
- `playerDataSelfTest`、`pastoralWorldDataSelfTest`、`marketCoreSelfTest`、`marketPacketSelfTest`、`enchantmentSelfTest`、`merchantCatalogSelfTest`：PASS。

遗留：

- `runClient`、`runServer`：NOT RUN（两项均实际进入 Forge/FML/coremod 引导并扫描 `run/mods`，但在本模组加载前停止；客户端未创建窗口，Server 未到 EULA/世界阶段，`run/eula.txt` 保持 `false`）。
- 真实世界水环境、Hopper、多人和保存重启端到端测试：NOT RUN；不将静态或自检结果替代真实游戏内验收。

## 第 14 批

状态：实现完成；静态、编译、资源、持久化与核心自检通过；真实客户端/服务器交互测试受当前运行环境限制。

实现：

- 注册 `transport_station` Block、ItemBlock 和 `TileTransportStation`，完成罗盘 + 铁块 + 红石的 1.12.2 无序配方、BlockState、模型、双语键和贴图；方块掉落不携带站点 UUID。
- `PlayerData` 升至 v2，保存一次性启动节点礼物、首次自建节点历史、个人别名与激活状态；`PastoralWorldData` 升至 v6，保存稳定的全局节点 UUID/Overworld 坐标/类型，旧存档安全迁移为空交通段。
- 实现服务端权威 `TransportService`：启动礼物背包满时安全掉落，Tile NBT 复制时重发 UUID，方块破坏取消登记；连接、改名、移除均验证打开的实际节点、归属、维度和服务器状态。
- 实现首个有效自建启动节点免费、首次普通节点永久失去免费机会、后续重新激活付费；费用只按 X/Z 距离，使用冻结的整数开方与十位四舍五入公式，经 `CoinService` 结算。
- 新增 Packet 5/6、无槽 `ContainerTransportStation` 与客户端快照 GUI，显示当前位置、节点列表、金币、连接费用、刷新/连接/改名/移除请求；不实现传送、线路或村庄节点功能。

主要文件：

- `transport/*`、`tile/TileTransportStation.java`、`block/BlockTransportStation.java`
- `data/player/PlayerData.java`、`data/world/PastoralWorldData.java`、`network/message/*Transport*`
- `gui/ContainerTransportStation.java`、`client/gui/GuiTransportStation.java`
- `assets/lisbam_pastoral_economy/{blockstates,models,recipes,textures}/transport_station*`、语言资源、`TransportCoreSelfTest.java`

验证：

- Toolchain check：PASS（Forge 1.12.2-14.23.5.2859 / Gradle 4.9 / Temurin Java 8 `1.8.0_504`）。
- 严格 Forge audit：0 ERROR；5 条 `packet-thread` WARNING 均为既有或经 Proxy 再调度的 S2C bridge，Packet 5 C2S handler 显式切回服务端主线程。
- `compileJava`、`processResources`、`build`（含 `reobfJar`）：PASS。
- `transportCoreSelfTest`、`playerDataSelfTest`、`pastoralWorldDataSelfTest` 与既有 market/enchantment/merchant/crab trap 自检：PASS。
- 成品 Jar：PASS，输出 `build/libs/LisBam_PastoralEconomy-0.1.0.jar`，SHA-256 `a77aea117b7382682fc9bb83579d02f15d454d7effd210970e0d98e8ad4bc9c0`。

遗留：

- `runClient`、`runServer`：NOT RUN。二者均进入 Forge/FML coremod 引导但在本 WSL 环境完成模组加载前停止；未创建游戏窗口，`run/eula.txt` 未改动且仍为 `false`。
- 真实单人/多人节点放置、满背包礼物、跨维度/重启、远程改名/移除和 Dedicated Server 验收待可运行环境；没有把静态检查或自检标为游戏内通过。
- 第 15 批及之后的旅行、村庄节点发现、线路解锁和跨节点传送尚未实现。

## 第 15 批

状态：实现完成；村庄解锁、旅行事务、费用、安全落点和 GUI 已接入，静态/编译/自检通过；真实游戏内端到端验证受当前 WSL 环境限制。

实现：

- 复用第 14 批 Transport Registry、PlayerData 和 CoinService；正式节点类型为 `SELF_BUILT`/`VILLAGE`，旧 `PLAYER` NBT 名称安全迁移。
- 使用已验证的 1.12.2 `WorldServer.findNearestStructure("Village", …)` 做有限结构探测，并与持久化 VillageRecord 去重；村庄站与 Station UUID 分离，一个村庄共享一个世界站点。
- 村庄站在真实接入时按需创建/恢复并镜像到交通注册表；玩家移出只改变个人 Active，不删除世界站；村庄站保持物理保护。
- 新增村庄接入、最近未接入村庄查询、远程解锁、全网旅行、主世界限制、半径 5 / Y -2..+4 安全落点、危险方块与碰撞检查、扣款后异常全额退款。
- 扩展 Packet 5/6、交通快照和最终 GUI：已解锁 SELF_BUILT/VILLAGE 列表、旅行、村庄查询/接入、改名和移除。
- 收尾审查修正已加载村庄站的物理可用性判断，按 `VILLAGE_STATION`/`TileVillageStation` 与 `TRANSPORT_STATION`/`TileTransportStation` 分支验证，确保村庄节点可参与最近节点、费用和旅行检查。

主要文件：

- `transport/TransportCost.java`、`transport/VillageTransportService.java`、`transport/VillageTransportCandidate.java`
- `transport/TransportService.java`、`transport/TransportStationType.java`
- `merchant/VillageService.java`、`merchant/MerchantWorldState.java`、`block/BlockVillageStation.java`
- `gui/ContainerTransportStation.java`、`client/gui/GuiTransportStation.java`
- `network/message/SyncTransportStateMessage.java`、交通 Action Handler、`TransportTravelSelfTest.java`

验证：

- Toolchain Checker：PASS（Forge 14.23.5.2859 / Gradle 4.9 / Temurin Java 8）。
- Forge 1.12.2 strict audit：0 ERROR；5 条 packet-thread WARNING 均为已审查的 Proxy/S2C 主线程桥接。
- `compileJava`、`processResources`、`compileTestJava`、`build`（含 `reobfJar`）：PASS。
- `transportCoreSelfTest`、`transportTravelSelfTest`、玩家/世界、market、enchantment、merchant、crab trap 自检：PASS。
- 正式 JAR 已生成并检查：`build/libs/LisBam_PastoralEconomy-0.1.0.jar`，2,538,070 bytes，SHA-256 `0968c04fb8c2b922ae1b8140f907fabff68490aa403b7570533f011b190bfeef`；未包含测试类、`mods.toml` 或现代 API。

遗留：

- `runClient`：NOT RUN；本环境在 Forge/FML/coremod 引导及显示初始化前无法创建可操作窗口。
- `runServer`：NOT RUN；本次实际进入 Forge/FML Server 与 coremod/Srg→Mcp 阶段，90 秒超时后安全终止，尚未完成模组加载；`run/eula.txt` 保持 `eula=false`，未代为接受 EULA，也未进入模组世界/玩家阶段。
- 真实村庄生成、多人并发、保存重启、危险地形、退款和 Dedicated Server 端到端场景待可运行环境。

## 第 15 批后界面维护

## 维护：行情书快照与附魔书提示（2026-09-05）

根因与修复：行情书曾把冷却内的选择请求放入无槽 `ContainerMarketBook`，再等待 `detectAndSendChanges` 的后续 tick；该延迟路径没有可依赖的槽位变化，导致客户端已进入“正在读取行情”却没有回包。现在 Packet 1 在既有的服务端主线程任务中直接验证打开的书本、商品和游标并返回单个有界快照。市场服务还保证最新窗口在旧存档缺失某商品历史列表时至少显示该商品已经冻结的当天价格点；`ensureMarketDay` 同时会持久补齐当天所有历史目录。

购买页的附魔书不再由裸 `Items.ENCHANTED_BOOK` 图标代替。`TradeCatalog` 根据稳定 market key 找回附魔定义与等级，构造和商人实际出售完全相同的 `ItemEnchantedBook` NBT；行情书原版 Tooltip 因而显示具体附魔和等级。

存档/协议影响：未更改 registry ID、Packet discriminator、Packet 字段、`PastoralWorldData` schema 或 NBT key。旧世界不需迁移；首次读取缺失的当天历史时按已有当天冻结价格补回。游戏内端到端验证仍需要可运行的 Forge 客户端。

验证：`check_toolchain.py`、`compileJava`、`compileTestJava`、`marketCoreSelfTest`、`marketPacketSelfTest`、`merchantCatalogSelfTest`、`processResources` 和 `build` 均 PASS（Temurin Java 8 `1.8.0_504`）。严格 Forge audit 为 0 ERROR、5 条既有 S2C Proxy/通用注册器 `packet-thread` WARNING，均已复核。`release/LisBam_PastoralEconomy-1.0.jar` 已由 `exportReleaseJar` 实际覆盖导出、非空，`unzip -t` PASS。

## 2026-09-05 任务维护

状态：市场双分页/渐进缓存、肉类收购、商人活动与刷怪蛋、创造标签和两项配置已实现。

- 肉类出售基础价：牛肉/猪排/羊肉 120，鸡肉/鳕鱼 80，兔肉 140，鲑鱼 100 金币/单位。
- 购买行情服务端每次仅返回一个窗口，客户端按 4 tick 节流预取；选中商品优先。行情书移除历史前后翻页。
- 商人随机村庄安全位置出生，远离村庄解除绑定并删除记录，不再瞬移；新增 `merchant_spawn_egg` 和模组创造标签。
- 配置新增 `restoreVanillaMonsterSpawns=false`、`disableAnimalBoneDrops=false`。

状态：实现完成；静态审计、编译与资源构建验证见本次记录，真实游戏内视觉测试待可运行客户端。

实现：

- 商人界面改为缩放自适应的双列交易卡，修复五行购买列表压住底部页签的问题；购买和出售均有可拖动数量滑条、数字直接输入框、实时预计总价和确认按钮。
- `ClientMerchantTradeViewState` 在客户端本次游戏会话中保留最后一次选择的购买/出售页，因此与任意商人再次交互时默认进入相同页；它不保存到 NBT，也不会发送权威状态。
- 商人和行情书的物品图标均使用原版物品悬停提示；蟹笼继续继承 `GuiContainer` 的原版槽位悬停提示。
- 蟹笼 GUI/Container 对齐为原版四行箱子贴图公式：专用钓竿/饵料槽、两行收获栏、玩家背包和快捷栏均与可点击槽位一致，状态/背包文字不再与槽位背景重叠。
- 交通站 GUI 改为居中自适应面板；所有操作按钮在 320x240 GUI Scale 下仍在面板内且无重叠，节点长名称截断显示并保留悬停完整信息。
- 后续修复：蟹笼不再直接渲染含四行虚假槽位的顶部箱子贴图，只保留真实的钓竿、饵料和 18 格收获栏，状态文字不会被格子遮住；商人与交通面板改为中性灰色的原版容器式背景。
- 商人出售页固定为 3 行 × 2 列的对称卡片布局；购买页依据客户端同步余额限制最大数量，余额不足一个整包时卡片、数量输入、滑条和确认按钮都会暗置，服务端交易复核不变。
- 交通快照为每个可用目的地由服务端计算并同步 `travelFee`；列表、悬停提示和“前往（费用）”按钮均显示该费用，余额不足、无有效路径或选中当前站时按钮暗置。
- `build.gradle` 新增 `exportReleaseJar`：作为 `build` 的 finalizer，在 `reobfJar` 后自动把可安装 JAR 复制到稳定的 `release/` 目录；`AGENTS.md` 规定每次更新后都必须构建并核验该产物。
- 视觉回归修复：移除商人、交通和蟹笼中手绘的仿原版槽框/凸起边框；所有文字继续直接使用 Minecraft `FontRenderer`，商人/交通改为裁取原版 `demo_background.png`，商人和蟹笼的可见物品槽均裁取原版 `generic_54.png`。蟹笼仍只显示真实 20 槽。
- 视觉细修：商人和交通面板改用原版 `demo_background.png` 的九宫格绘制，固定原始 1 像素边缘、只延展中心，避免整体缩放造成边框和字体观感失真；文字统一回归原版容器常用深灰色。蟹笼中间背景也改为直接取 `generic_54.png` 的原始背景像素，不再手工填色；有限购买库存由“剩余 X 包”改为直接显示“剩余数量：X”（实际剩余物品数）。
- 紧急视觉修复：实际游戏内验证发现 `demo_background.png` 九宫格分片出现错位图块，因此删除分片路径，商人与交通均改回单次完整原版贴图绘制。字体、原版槽位、交易/交通逻辑和蟹笼绘制均未改动。

验证：

- `cmd.exe /C "set JAVA_HOME=C:\\Users\\23107\\.jdks\\corretto-11.0.26&& gradlew.bat compileJava processResources build"`：PASS（含 `compileTestJava`、`test`、`reobfJar`；生产 class major 为 52）。
- Forge 1.12.2 strict audit：0 ERROR；5 条既有 `packet-thread` 保守 WARNING 已审查，均为 Proxy/S2C 主线程桥接或通用注册器，和本次 GUI 修改无关。
- `build.gradle` 显式设定 JavaCompile UTF-8 源文件编码，消除 Windows 默认 GBK 对既有中文/趋势符号源文本的编译诊断；没有改变 source/target Java 8。
- 本轮重新执行 `cmd.exe /C "set JAVA_HOME=C:\\Users\\23107\\.jdks\\corretto-11.0.26&& gradlew.bat compileJava processResources build"` 与 `transportCoreSelfTest`：PASS（前者含 `test`）；后者覆盖 Packet 6 的新增 `travelFee` 往返。生产 class 仍为 major 52。Forge 1.12.2 audit：0 ERROR、5 条既有 packet-thread WARNING。
- `gradlew.bat build`：PASS，实际执行 `exportReleaseJar`。`release/LisBam_PastoralEconomy-0.1.0.jar` 已导出且 `unzip -t` 完整性检查通过；SHA-256 为 `9a149aae2be3686f28341a64dcc25d314d82b4522cdbcaf9590f659a291d27e4`。
- 本轮 `gradlew.bat compileJava processResources build`：PASS（含 `reobfJar` 与 `exportReleaseJar`）；Forge 1.12.2 audit 为 0 ERROR、5 条既有 packet-thread WARNING。最新 release JAR 的 `unzip -t` 通过，SHA-256 为 `c80c805a6fa05c412c41170269a1566384da97eb078aa0d9ad303b45148904c3`。
- 本轮 UI 细修后再次执行 `cmd.exe /C "set JAVA_HOME=C:\\Users\\23107\\.jdks\\corretto-11.0.26&& gradlew.bat compileJava processResources build"`：PASS（含 `test`、`reobfJar`、`exportReleaseJar`）。Forge 1.12.2 audit：0 ERROR、5 条既有 packet-thread WARNING；release JAR `unzip -t`：PASS，SHA-256 为 `18098867f456eff8b5156f9c39aadd139b210893a936e81278c4a46c0099ac70`。
- 本轮移除不稳定的背景分片后执行 `cmd.exe /C "set JAVA_HOME=C:\\Users\\23107\\.jdks\\corretto-11.0.26&& gradlew.bat compileJava processResources build"`：PASS（含 `test`、`reobfJar`、`exportReleaseJar`）。Forge 1.12.2 audit：0 ERROR、5 条既有 packet-thread WARNING；release JAR `unzip -t`：PASS，SHA-256 为 `b1bbc38d695c89796dc6e61a30722597dff948ce89ab6f2636ded473550545d3`。

遗留：

- `runClient`、实际 320x240/常规分辨率、商人连续交互、滑条拖动/直接输入、物品悬停和蟹笼 Shift-click 手测仍待可创建 Minecraft 窗口的环境；没有将静态/编译验证记为游戏内 PASS。

## 经济市场与商品价格修复维护（2026-09-05）

- `MarketCatalog`、`TradeCatalog` 与附魔书价格统一采用任务书最终金币值；基础建材使用 500/160/240/320/480/640/400/400/1600/320/2000 等冻结单件价格，铁锭为 900/个，交通旅行与接入费分别为 `round50(400 + 1.20D)` 与 `round100(4000 + 12D)`。
- 商人购买从整组改为按件：有限库存按 16→4、8→2、4→1 组并以原版堆叠上限换算实际件数，原 1 组改为 1 件；`DailyOffer` 持久字段为 `remainingItems`。服务端按件验证库存、价格、金币和背包容量，GUI 仅显示单价/总价/实际剩余件数并移除“一组数量”。
- `MerchantOfferService` 在服务端跨购买池按 Item/meta/NBT 变体身份去重，继续生成普通 4、罕见 3、稀有 2、珍宝 1；附魔书身份包含附魔注册名和等级，重抽保持轮换确定性。缺少 `remainingItems` 或池/等级非法的旧 Offer 不迁移，读取为无效并在当天重新生成。
- 默认链式行情增加类别最低/最高价、50%/55%/65%/75%/85% 动态回归概率、边界强制反弹和整数方向保留；同一 Item/meta 买卖渠道共享随机身份。更新了市场、商人、交通自检以及内容书和维护文档。
# 2026-09-05 维护：挤奶冷却、全商品调价与剪刀附魔

- 新增成年牛/哞菇每头 6000 tick 挤奶冷却及默认关闭的 `disableMilkingCooldown` 配置；服务端仅在桶交互的冷却命中时拒绝交互，其他成功路径仍由 `EntityCow` 原版创建牛奶桶、播放声音并同步库存，同时记录实体 NBT。
- 按《聆竹の休闲田园经济_全商品手动调价表_调整后.xlsx》更新出售、购买普通/罕见/稀有/珍宝和附魔书基础价，保持经济 key 与历史兼容。
- 锄头和剪刀的 Harvest/Range 适用范围保持；剪刀上的 Harvest 成功剪毛后按既有二项式规则追加羊毛或红色蘑菇，原版 BREAKABLE 类附魔可正常用于剪刀。

## 2026-09-05 修复：原版效率附魔台兼容与原版挤奶路径

根因与修复：Forge 1.12.2 的剪刀和各材质锄头原始附魔力为 0，附魔台会在生成候选前退出；剪刀还会被原版效率的附魔台筛选排除，尽管原版效率书本可以通过铁砧应用。发行 JAR 因而带最小 Coremod，只精确补丁这两个 `Item` 钩子：剪刀按铁工具附魔力、锄头按自身材质附魔力进入原版候选，剪刀效率直接使用原版 `Efficiency`。删除 `shears_efficiency` 注册和专属书本；旧世界加载该旧 ID 时通过 `MissingMappings` 重映射为原版效率，已有物品不会失去附魔。

挤奶问题来自事件处理器自行扣桶、造桶并取消原版 `EntityCow` 交互，因而干扰了原版库存/饮用状态同步。现在处理器只在逻辑服务端、冷却命中时取消；不在冷却的成年牛/哞菇只写入该牛的持久最近成功 tick，随后完全交回原版桶替换路径。计时器降为 `LOWEST` 优先级，避免其他交互处理器已取消时错误记录冷却。

验证：Temurin Java 8 `1.8.0_504` 下 `enchantmentSelfTest`、`milkCooldownSelfTest`、`merchantCatalogSelfTest`、`marketCoreSelfTest`、`compileJava` 与 `compileTestJava` PASS；附魔自测会对补丁后的 `Item` 字节码确认剪刀/锄头附魔力钩子和原版效率筛选，并通过 ASM verifier。Forge 1.12.2 audit 为 0 ERROR、6 条既有 `packet-thread` WARNING。带 `JAVA_TOOL_OPTIONS=-Dfml.coreMods.load=...` 的 60 秒 `runServer` 启动已实际发现并入队该 Coremod，时限到达前尚未完成模组/世界加载；未接受 EULA。最终 `compileJava processResources build` PASS，确认 release JAR 含 Coremod manifest/三项 core 类、不含已删除的剪刀专属效率类，大小 404,092 bytes，SHA-256 `a0b82c6dcfbc5e680c39d18f9f4e0643f7f77635521fc4c9bdff5fadc5efb641`，`unzip -t` PASS。

## 2026-09-05 修复：Forge 1.12.2 Coremod 启动与 StackMap 验证错误

根因：首轮 Coremod 用固定 MCP 名称和单一 `Item` 方法布局定位目标；用户的 Forge `14.23.5.2847` 运行时布局/映射差异使其 fail-fast，进而导致 `NoClassDefFoundError: net.minecraft.item.Item`。后续兼容版虽已正确找到现代 `canApplyAtEnchantingTable`，却在方法前插入了新的 `IFEQ` 跳转；`ClassWriter.COMPUTE_MAXS` 只重算栈深度、不会为新分支生成 Java 8 必需的 StackMap frame，因此用户客户端报 `VerifyError: Expecting a stackmap frame at branch target 10`。

修复：Coremod 现在按已发现的 `Item` 方法布局选择 `ItemStack` 钩子或旧式无参附魔力路径，helper 全部使用 `Object` 参数描述符，消除 MCP/SRG 内部类名差异。现代效率筛选不再插入任何跳转：它在每个既有 `IRETURN` 前保存原版布尔结果，再调用 `(Object,Object,boolean)` helper 合并剪刀原版效率资格，因此原有控制流和 StackMap frame 保持有效。未知布局改为记录错误且返回原字节码，不再阻止客户端或服务端启动。自测覆盖现代、SRG 和旧式布局，并断言现代效率补丁无跳转。

验证：Temurin Java 8 `1.8.0_504` 下 `enchantmentSelfTest`（含现代、SRG 和旧式 Coremod 注入及无新增跳转断言）与 `milkCooldownSelfTest` PASS。Forge 静态 audit 为 0 ERROR、6 条既有 `packet-thread` WARNING；最终 `compileJava processResources build`（含 `test`、`reobfJar`、`exportReleaseJar`）PASS。重混淆 release JAR 为 405,030 bytes，SHA-256 `80ad3e648fc357365333c536dc17c6b9d764fe89f91ec102bc597d9d71f40358`，`unzip -t` PASS，manifest 含 `FMLCorePlugin`。实际 Windows Forge 2847 客户端启动仍待用户用本次 JAR 验证。

## 2026-09-05 修复：附魔力实参与挤奶客户端预测

根因：附魔力注入在调用 `(Object,int)` helper 前错误压入了 `ALOAD 1`，导致 helper 实际收到 `ItemStack` 而不是 `this Item`；剪刀和五种锄头的身份比较始终失败，附魔力继续为 0，所以补丁虽已加载却没有候选。挤奶冷却只在逻辑服务端拒绝交互，但 1.12.2 客户端在发出 `CPacketUseEntity` 后还会本地执行 `EntityCow` 的桶替换；被服务端拒绝的交互因此在客户端生成无服务端状态支持的假牛奶桶，继而表现为同牛反复挤奶、其他牛无法挤奶和牛奶无法饮用。

修复：附魔力方法现在只压入 `this Item`、原版无参附魔力并调用 helper；自测验证 helper 前两个对象来源均为本地变量 0、没有 `ALOAD 1`，并额外对 Forge 14.23.5.2847 的真实 binpatch 后 `ain.class` 执行转换检查。启用挤奶冷却时，客户端事件在交互包发出后取消本地牛奶桶预测；逻辑服务端仍按目标牛自身 NBT 判定，合法首次交互继续由原版 `EntityCow` 创建和同步真实牛奶桶。关闭冷却时不取消客户端原版路径。

验证：Temurin Java 8 `1.8.0_504` 下 `enchantmentSelfTest` 和 `milkCooldownSelfTest` PASS；Forge 1.12.2 静态审计为 0 ERROR、6 条既有 `packet-thread` WARNING；最终 `compileJava processResources build`（含 `test`、`reobfJar`、`exportReleaseJar`）PASS，已导出 405,205-byte release JAR。游戏内附魔台、两牛连续挤奶和 Dedicated Server 世界测试仍为 NOT RUN，需实际可操作环境确认。

## 2026-09-05 修复：挤奶服务端结算重构

根因：此前只在客户端取消本地预测、服务端仍依赖后续 `EntityCow` 原版分支。1.12.2 客户端会在发送 `CPacketUseEntity` 后先运行本地实体交互；服务端拒绝冷却目标时，两侧库存变化可能先后覆盖，表现为牛奶桶和空桶一起消失、同牛重复挤奶或牛奶桶无法饮用。

修复：冷却启用时 `MilkingCooldownEventHandler` 在两侧都取消原版桶交互。客户端不修改物品栏，只返回 `SUCCESS` 结束预测；服务端先验证成年目标、空桶和实体 NBT 冷却，再独立执行原版等价事务：播放 `ENTITY_COW_MILK`，扣一只空桶，手中耗尽则替换牛奶桶，否则尝试加入主背包，满包时按原版丢出。事务完成后才写入最近成功 tick；冷却命中不扣桶、不产奶、不更新 tick。关闭配置时完全跳过处理器并走原版逻辑。新增纯规则断言覆盖服务端接管、冷却拒绝、成功后记录和客户端延后库存同步。

验证：Temurin Java 8 `1.8.0_504` 下 `compileJava`、`compileTestJava`、`milkCooldownSelfTest`、`compileJava processResources build` 均 PASS；最终构建包含 `test`、`reobfJar` 和 `exportReleaseJar`，release JAR 已覆盖导出且非空。Forge 1.12.2 工具链检查为 0 问题；静态审计为 0 ERROR、6 条既有 `packet-thread` WARNING。游戏内客户端和 Dedicated Server 仍需用户实际验证。
## 2026-09-07 维护：每日挤奶、伐木耐久附魔核验与区块加载器

完成：成年牛/哞菇的挤奶限制从每实体 6,000 tick 改为“当前世界日每只成功一次”，世界日按 `World#getWorldTime() / 24,000` 计算，下一世界日统一恢复；客户端预测抑制、服务端桶结算与配置旁路保持。新增 `chunk_loader` 方块、ItemBlock、TileEntity、双状态模型/双语名称、有序配方和两张 AI 绘制后缩放为 16×16 RGBA 的方块贴图。红石供电时由服务端申请 Forge NORMAL ticket，仅强制所在 `ChunkPos`；断电、破坏或重载后无供电释放，既有模组唯一 ticket callback 按 modData 恢复。充能状态光照为 7。

伐木核验：没有新增手工耐久扣除。`TreeFellingEventHandler` 已对每个二级原木调用原版 `PlayerInteractionManager#tryHarvestBlock`；Forge 1.12.2 源码确认此路径逐格调用主手 `ItemStack#onBlockDestroyed`，由原版斧头 `damageItem` 对每格独立结算耐久附魔。编译后 `javap` 也确认二级原木与树叶采掘点均为该原版调用；树叶既有损伤恢复保持不变。

已验证：Temurin Java 8 `1.8.0_504` 下 `compileJava`、`compileTestJava`、`processResources`、`milkCooldownSelfTest`、`toolDurabilitySelfTest` 与 `chunkLoaderSelfTest` 均 PASS；两张 PNG 均为 16×16、8-bit RGBA；Forge 1.12.2 静态审计为 0 ERROR、7 条既有 `packet-thread` WARNING。最终 `./gradlew build --no-daemon --console=plain` PASS，含 `reobfJar`、`exportReleaseJar` 和 249 个 Java 8 生产类核验；旧 release JAR 已备份到 `release/backup/backup_20260907-132027.jar`。新 `release/LisBam_PastoralEconomy-1.5.jar` 为 516,800 bytes，SHA-256 为 `9ccc32785b3c28b2d9f0e78528db8db359af9db366e7e057ea8cc25afc4ee702`，`unzip -t` PASS。

NOT RUN：游戏内红石、断电、破坏、重启恢复、多人每日挤奶和 Dedicated Server 世界验证仍待可进入的 Forge 世界。
