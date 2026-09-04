# 模组当前架构

当前实际完成批次：第 15 批（交通旅行与村庄解锁）。

当前发行版本：`1.0`（Minecraft Forge 1.12.2 / 14.23.5.2859，Java 8）。

## 已实现

- 主入口：`lisbam.pastoraleconomy.LisBamPastoralEconomy`，负责模组身份、生命周期调度与基础日志。
- 代理：`CommonProxy` 不引用客户端类；`ClientProxy` 是物理客户端扩展点。
- 注册：`registry.RegistrationHandler` 是唯一的静态 Forge Registry Event Subscriber；已注册 `item.ModItems.MARKET_BOOK`、`item.ModItems.CRAB_TRAP_ITEM`、`item.ModItems.TRANSPORT_STATION_ITEM`、`block.ModBlocks.VILLAGE_STATION`、`block.ModBlocks.CRAB_TRAP`、`block.ModBlocks.TRANSPORT_STATION` 及八个稳定附魔 ID。`entity.ModEntities` 使用 1.12.2 `EntityRegistry.registerModEntity` 和 `GameRegistry.registerTileEntity` 注册商人、村庄站点、蟹笼与交通节点 TileEntity。
- 网络：`network.ModNetwork` 持有唯一 `SimpleNetworkWrapper`，Channel 为 `lb_pastoral`；Packet 0～2 保持原有金币/行情协议，Packet 3 为有界 C2S 商人交易请求，Packet 4 为 S2C 商人快照，Packet 5/6 为交通节点 C2S 操作/S2C 状态快照。Common Handler 不引用客户端类型，经 Proxy 调用客户端主线程执行器；全部 C2S 请求在服务端主线程验证并结算。
- GUI：`gui.GuiIds.MARKET_BOOK=0`、`MERCHANT_TRADE=1`、`CRAB_TRAP=2`、`TRANSPORT_STATION=3`。`ModGuiHandler` 在服务端返回无槽行情书、绑定实体的商人、坐标绑定的 `ContainerCrabTrap` 或 `ContainerTransportStation`；客户端经 Proxy 返回相应 GUI，common side 不加载 `GuiScreen`。所有 UI 文字使用原版 `FontRenderer`，物品悬停使用原版 `GuiScreen#renderToolTip`。`GuiMerchantTrade`、`GuiTransportStation` 每帧以一次完整裁取绘制原版 `demo_background.png`，不把该纹理拆成九宫格；按钮/输入框/滑条保持原版控件。商人窗口明确不暂停单人集成服务端；静态交易文字和数量文字保持原版按钮正常浅色，但当前页签、余额/库存不足或没有待售物品的确认按钮实际设为 `enabled=false`，由原版深色禁用态表达不可操作；客户端和服务端仍都会复核交易。交通窗口的所有标签、节点、输入框和已禁用按钮文字保持同一浅色；禁用状态仍保留原版按钮背景和点击限制。商人物品底框直接裁取 `generic_54.png`，有限库存显示为实际剩余物品数量（剩余包数 × 每包数量）。`ClientMerchantTradeViewState` 仅在物理客户端保存本次游戏会话最后选择的出售/购买页，绝不参与交易请求或存档。`GuiCrabTrap` 复用 `generic_54` 的标题/玩家背包片段、原始背景像素和逐格裁取的 2 个专用槽/18 个收获槽；绝不渲染没有 Container Slot 的虚假格子，状态文字处于空白区域。`SyncTransportStateMessage` 的每个 `TransportNodeView` 附带服务端计算的目的地旅行费或不可用标记，客户端据此显示费用并禁用不可旅行/余额不足的操作。
- 生成规则：`event.OverworldMonsterSpawnEventHandler` 是 common 静态订阅器。它只在逻辑服务端的维度 0 对 `LivingSpawnEvent.CheckSpawn` 的 `isSpawner=false` 且 `EnumCreatureType.MONSTER` 设为 `DENY`；因此拒绝自然/区块生成敌对生物，保留刷怪笼、刷怪蛋、命令和直接实体生成。
- 资源：`mcmod.info`、`pack.mcmeta`（format 3）、中英文 `.lang`、正式 Logo、行情书资源和蟹笼的 blockstate、Block/Item model、合成配方均已存在；`textures/blocks/crab_trap.png` 与 `transport_station.png` 都是 32×32、最近邻简约原版风 PNG。`village_station` 与玩家可放置的交通站共用后者，不再引用原版橡木木板；玩家交通方块与 ItemBlock 共用显式 `tile...transport_station` 显示键，中文为“交通方块”，不会再附加 `.name`。行情书模型使用自有 `textures/items/market_book.png` 的绿皮书图标。蟹笼合成固定为铁锭/铁栅栏交错外圈、中央陷阱箱。
- 玩家数据：`data.player.PlayerDataCapability`（`lisbam_pastoral_economy:player_data`）是唯一玩家 Capability，`dataVersion=2`。数据保存玩家金币 `long`、启动交通方块一次性发放状态、首个自建站免费资格历史、玩家节点 Active/Removed 状态、别名和自建站序号；Capability NBT 处理默认/旧字段并把非法负金币修正为 0。Clone 时深复制完整数据。
- 世界数据：`data.world.PastoralWorldData`（`lisbam_pastoral_economy_world_data`）是唯一 WorldSavedData 根，`dataVersion=7`。访问器始终解析服务器主世界 MapStorage，保证跨维度只有一份共享数据。v7 `market` 段保存持久 marketSeed、首次市场日、当前/昨日快照和 14 种作物的最近 30 天；不再持久化无限 `processedDays`、无限历史或已移除的 `farmHarassment` 记录。旧存档的该废弃字段读取时忽略、下一次保存时移除；merchant 段保存村庄、站点、商人、Offer、库存、轮换状态和附魔书已解析等级；transport 段保存 SELF_BUILT/VILLAGE 物理节点 UUID、维度、坐标和村庄身份引用。
- 市场：`market.MarketCatalog` 是唯一不可变商品目录，保存稳定经济 key、1.12.2 Item/meta、基础价、8 类固定波动率和历史标志；`MarketPriceGenerator` 用 marketSeed/worldDay/key/两个 salt 独立生成三角分布价格并完成唯一 `Math.round`。`MarketService` 是唯一服务器市场 API：行情书打开请求或实际经济读取才惰性刷新当日确定性快照，主世界不再每 tick 推进市场；跨日跳跃只重建当前日前 30 天。`MarketPriceSnapshot` 为商人单次同步批量提供当前/昨日价格，`MarketHistorySnapshot` 最多 30 点且不再提供更早页。
- 经济：`data.player.CoinService` 是唯一金币写入口。逻辑服务端 `EntityPlayerMP` 才能调用；拒绝负数、拒绝 long 溢出、没有公共 setBalance。成功改动时发送自己的 S2C 同步。
- 客户端：`client.ClientPlayerState` 与 `ClientMarketState` 都是非持久、非权威缓存，连接/断开时清空。市场缓存还有明确书本会话：打开时清空并开始接收、关闭时清空并拒绝迟到包，连接内请求号保持递增；打开书本的首个 Packet 1 请求由服务器以同一请求号下发 14 种作物的最新 30 天窗口，`GuiMarketBook` 切换作物只读取本次会话缓存，不再逐项发送网络请求，关闭后不保留市场工作。`ContainerMarketBook` 对缺失窗口的后备请求仍最多每 2 tick 生成一次历史快照；快速切换期间不再丢弃请求，而是合并为最新一项并在冷却结束后的 Container tick 发送。`GuiMerchantTrade` 的数量滑条、输入框与页签偏好均为本地展示状态，出售持有量按客户端 tick 缓存，仍只发送既有有界数量请求；每次成功交易后的库存窗口、金币同步和商人快照均由服务端在同一逻辑 tick 推送。`ClientModelRegistry` 是物理客户端模型注册。`ClientHudEventHandler` 仍在 `RenderGameOverlayEvent.Text` 中右上角渲染金币。
- 附魔定义：`enchantment.EnchantmentPastoral` 统一实现 RARE、非宝藏、可书本获取、1.12.2 附魔台物品筛选和未冻结的稀有附魔可得性区间；`ModEnchantments` 持有八个正式实例和精确原版装备白名单。Harvest 与 Fortune 互斥；Slaughter 与 Sharpness、Smite、Bane of Arthropods、Looting 互斥；其余兼容性仍交给原版。
- 农业：`agriculture.AgricultureRules` 是成熟作物身份、种植物和全部随机公式的单一来源。`event.AgricultureEnchantmentEventHandler` 在服务端 BreakEvent 捕获玩家的成熟收获动作，并在同一 HarvestDropsEvent 管线中依次加入 Harvest 奖励、按 Fine Cultivation 消耗最终掉落/主背包补种、再单次判定 Pastoral Favor；PlaceEvent 仅对真实玩家的列出作物种植执行 Pastoral Favor。
- 工具与战斗：`event.TreeFellingEventHandler` 对带 Felling 的真实玩家斧头，在有限的同树种原木连通块和匹配树冠确认后，以 `EntityPlayerMP#interactionManager.tryHarvestBlock` 逐块处理原木和叶子，因此保护事件、原版掉落、Unbreaking 与耐久仍逐块生效。`AnimalBoneDropEventHandler` 在服务端 `LivingDropsEvent` 的 `HIGHEST` 优先级按严格动物白名单加入骨头，`AnimalBoneDropRules` 先进行基础掉落、再使用事件提供的 Looting 等级计算附加数量。`SlaughterEnchantmentEventHandler` 随后以既有倍率处理完整掉落列表，因而已成功的骨头和原掉落一致受屠宰影响，且不改 XP。
- 蟹笼：`tile.TileCrabTrap` 是每个蟹笼实例的唯一权威状态所有者，保存 20 槽库存（钓竿 0、饵料 1、收获 2～19）、本轮 remaining ticks、Lure/Luck/有饵快照和 pending loot。逻辑服务端只在接触 1.12.2 原版水/流动水时推进计时；它以原版 `LootTableList.GAMEPLAY_FISHING` 和快照 Luck 生成一次真实战利品，完整插入 18 格收获栏后才按本轮有饵状态独立判定 50% 消耗。倒计时仍逐 tick 精确推进，但只每 20 tick（以及状态转换）标记 TileEntity 脏，避免大量蟹笼持续弄脏区块。`ISidedInventory` 使上方/侧面 Hopper 仅输入五种生肉到槽 1、下方 Hopper 仅抽取 2～19；没有 `IItemHandler` capability 覆盖以确保原版 Hopper 走经验证的 1.12.2 侧面库存路径。`BlockCrabTrap` 负责打开 GUI 以及仅一次地掉落本体、库存和 pending loot。
- 村庄与商人：`merchant.VillageService` 仅在主世界低频观察 1.12.2 `VillageCollection`，按 128/64/160 格参考值去重并维护稳定 `VillageRecord`；每轮维护只扫描一次已加载实体来清除异常/重复商人并建立活动索引。世界加载不强制补生商人，首次周期维护额外等待一轮实体载入；此后只在村庄站和记录的最后实体区块均已加载、仍未找到该实体时补生。`MerchantRecord` 在 merchant 段可选保存最后观测到的实体 Chunk X/Z，旧存档首次见到实体才写入；无效、非 active、跨村庄或不在 roster 的实体 NBT 直接剔除。`StationRecord` 与 `TileVillageStation` 保存受保护村庄站点身份，站点异常消失时在已加载安全位置恢复。`MerchantRecord`、`DailyOfferState`、`OfferRotationState` 和共享库存均在 `PastoralWorldData` v5 的 merchant 段持久化；`EntityMerchant` 保存绑定身份引用及显示状态，死亡/重启由 reconciliation 按 `max(ceil(villagers / 5),3)` 补足且不设上限，离站返航最多每 20 tick 重算一次路径。`MerchantNameGenerator` 只在逻辑服务端按高频百家姓人口权重生成“1 姓 + 1--2 名”中文姓名，并写入原版 `CustomName` 实体 NBT；名称不进入 `MerchantRecord`，UUID 仍是唯一身份。实体还保存附加的 `merchantSkin` 索引，并以 `EntityDataManager` 同步给客户端：仅 1--4 为四种 64×64 Steve UV 的内置作物商人服装，旧索引 0 不再渲染 Steve 且会在实体加载时迁移。`GuiMerchantTrade` 和 `RenderMerchant` 仅读取已同步实体显示状态，不生成随机结果。商人基础生命、移动速度和攻击伤害使用原版玩家的 20/0.1/1 基准；`EntityAISwimming` 与可游泳地面导航使其在水面上浮。商人显式不提供生物环境、受伤或死亡声音。打开的服务器交易 Container 在实体上登记运行时交易者并令其停住，关闭时解除；商人平时观察 8 格内最近玩家，实际受到玩家伤害后只避开该攻击者 200 tick。上述交易者和逃跑状态不写入 NBT。`TradeCatalog` 集中定义 6 条收购池、普通/罕见/稀有/珍宝购买池、1.12.2 Item/meta、bundle、附魔书等级和有限库存。
- 移动与视觉：`MovementEnchantmentEventHandler` 仅在服务端 END Player Tick 更新两个固定 UUID、非持久的移动速度属性修饰符；Fleetfoot 与 Farmland Walker 都使用原版 Speed Potion 同样的 operation 2。`FarmlandTrampleEvent` 处理 Farmland Walker I。`client.ClientNightVisionRenderHandler` 是 `Side.CLIENT` 静态订阅器，只在本地渲染帧内临时提高 gamma 并精确恢复，不发送网络包、不施加 Potion、不改世界光照。
- 实体：模组不再修改敌对或中立生物的 AI、仇恨、反击、爆炸或方块破坏；它们在实际生成后完全走 Java 1.12.2 原版行为。`EntityMerchant` 仍是独立的非敌对实体，保存/交易职责不受敌对生成规则影响。

依赖方向：主入口 → Proxy；CommonProxy → PlayerDataCapability、ModEntities、ModNetwork、ModGuiHandler；Forge 生命周期事件 → PersistenceEventHandler → PlayerData/PastoralWorldData/CoinService/MarketService/VillageService/TransportService；自然敌对生成 → OverworldMonsterSpawnEventHandler；动物死亡掉落 → AnimalBoneDropEventHandler → SlaughterEnchantmentEventHandler；EntityMerchant 右键 → 服务端 ContainerMerchantTrade/TradeService → Packet 3/4 → ClientProxy → ClientMerchantTradeState → GuiMerchantTrade；玩家右键蟹笼 → ContainerCrabTrap/TileCrabTrap；TileCrabTrap → 1.12.2 原版 GAMEPLAY_FISHING LootTable/ISidedInventory/NBT；玩家右键自建或村庄交通节点 → ContainerTransportStation/TransportService → Packet 5/6 → ClientProxy → ClientTransportState → GuiTransportStation；TransportService → VillageTransportService/TransportCost/CoinService/PlayerData/PastoralWorldData/TileTransportStation/TileVillageStation；VillageTransportService → WorldServer.findNearestStructure/VillageService；VillageService → VillageRecord/StationRecord/TransportWorldState/MerchantRecord/EntityMerchant；TradeService → MarketService/CoinService/Inventory/MerchantRecord。ClientProxy 只在物理客户端加载客户端同步执行器、GUI、商人 Renderer 与模型。

## 第 14 批交通节点基础

- `BlockTransportStation`、`ItemBlock`、`TileTransportStation` 以稳定 `transport_station` ID 注册。TileEntity 只保存物理节点的 UUID 与启动礼物所有者；掉落 ItemStack 只保留启动礼物归属，不复制节点 UUID。
- `PastoralWorldData` schema 为 v6，其中 `TransportWorldState` 保存 Overworld 节点 UUID、坐标和类型；重复 Tile NBT 会取得新 UUID，破坏节点会撤销全局记录。`PlayerData` schema 为 v2，保存一次性启动礼物、首次自建节点和玩家自己的节点别名/激活状态。
- `TransportService` 是唯一的服务端交通节点业务入口：首次进入/重生/换维度只授予一次启动节点物品；连接、改名、远程移除均重新验证当前 Container、方块/Tile、维度、节点归属和金币。客户端只缓存 `SyncTransportStateMessage` 的快照。
- 首个有效自建启动节点免费；首次普通节点与之后重新激活都依据 X/Z 距离按冻结公式结算，金币只经 `CoinService` 修改。当前仅实现节点登记、最近有效节点、名称和费用，不实现传送、路线、村庄发现或线路解锁。

## 第 15 批交通旅行与村庄解锁

- `VillageService` 的持久 `VillageRecord`（村庄身份）与 `StationRecord`（世界物理站）继续服务商人系统；同一 Station UUID 被镜像为 `TransportWorldState` 的 `VILLAGE` 节点。`SELF_BUILT` 是正式自建类型；旧第 14 批 NBT 的 `PLAYER` 值读取为 `SELF_BUILT`，不会丢失 UUID、坐标或玩家节点数据。
- `VillageTransportService` 在逻辑服务端以 1.12.2 `WorldServer.findNearestStructure("Village", …)` 进行 8,192 格有限探测，并与已持久的村庄身份按位置去重；预览只同步候选 UUID/位置/距离/接入费，确认接入才加载目标单区块并幂等建立共享村庄站。
- `TransportService` 统一处理当前村庄直接接入、远程村庄接入、旅行、改名和移出。接入费为现有 `connectionFee`；旅行费为 `travelFee`。旅行只接受目的 Station UUID，服务端重验 Source/Destination Active、物理记录、主世界和余额；安全落点搜索半径 5、相对 Y -2..+4，先于扣款；`EntityPlayerMP#setPositionAndUpdate` 异常或位置校验失败通过 `CoinService` 全额退款。远程村庄接入会重新查询“最近未接入村庄”，过期或不再是当前候选的 UUID 直接拒绝。
- `ContainerTransportStation` 和 `GuiTransportStation` 可绑定自建或受保护村庄 TileEntity。Packet 5 的新增 action 仍在服务端主线程执行；Packet 6 只向客户端快照已接入节点和最近未接入村庄，客户端不保存权威状态或决定费用/坐标。

## 未实现但已固定边界

- 后续交通工具、路线动画、旅行时间/冷却、跨维度线路和村庄升级仍未实现；本批只完成即时主世界节点旅行闭环。
- 后续世界数据继续扩展现有 WorldSavedData 根；方块实例使用 TileEntity + NBT；实体实例使用实体 NBT。后续交易只能复用 CoinService，不能直接改 Capability/NBT。
- 后续 Block、Item、Entity、Enchantment 通过现有注册入口，实际玩法事件使用 `event` 包的统一静态订阅策略。
- 所有权威游戏状态由逻辑服务端控制；客户端只显示和发起经过服务端验证的请求。

## 第 06、07 批验证状态

- Java 8 Toolchain check、Forge static audit（0 ERROR）、`compileJava`、`processResources`、`build`（含 `reobfJar`）以及 `enchantmentSelfTest` 均已通过。Jar 检查确认八个附魔生产类和两份本地化资源均已进入成品。
- Audit 的三条 `packet-thread` WARNING 已审查：通用注册器不处理消息；两个 S2C common bridge 只转交 Proxy，真正的客户端缓存写入均在 `Minecraft#addScheduledTask`；C2S 行情 Handler 本身以 `EntityPlayerMP#getServerWorld().addScheduledTask` 切回服务端主线程。
- 客户端与 Dedicated Server 已实际进入 Forge/FML/coremod 引导，但在本 WSL 环境完成本模组加载前终止；客户端没有创建可测试窗口，Server 未到达 EULA 检查，`run/eula.txt` 仍保持 `false`、未代为接受。因此本批游戏内附魔台、Librarian、铁砧、农业、树木、动物、移动、夜视与 Dedicated Server 完整验收仍待可用环境。

## 第 08～10 批验证状态

- `./gradlew compileJava`、`processResources`、`compileTestJava`、`build` 和六项 self-test 均已在 Temurin 8 下通过；成品 Jar 已输出到 `build/libs` 并复制到 `release`。自检覆盖 2/15/16/20/21/25/26/30/31/35/36 村民人口边界、普通 56 条、罕见 43 条、四项有限库存和重复 key。
- Forge 1.12.2 audit：0 ERROR；新增交易 S2C bridge 与既有 Proxy bridge 同样只把客户端缓存写入调度到客户端主线程；C2S 交易 Handler 调度到服务端主线程。
- `runClient`、`runServer`、真实村庄/多人/重启/跨日交易仍为 NOT RUN：需要可用 LWJGL 显示或已接受 EULA 的 Dedicated Server 环境。

## 第 11 批验证状态

- `check_toolchain.py` 在 Temurin 8 下报告 0 error；`compileJava`、`processResources`、`compileTestJava`、`build` 和全套六项 self-test 均通过。最新 `build/libs` 与 `release` Jar SHA-256 为 `f56e1531a217c60160697e97655863cc6c74fc78e500d1b31851516f849934af`。
- `merchantCatalogSelfTest` 逐项检查 Rare/Treasure 数量、价格、波动、库存、legacy metadata、12 张唱片、12 种附魔、等级权重和 `DailyOffer` 等级 NBT 往返；真实 Enchanted Book 均含 1.12.2 附魔 NBT。
- Forge audit 0 ERROR；严格模式保留 4 条既有 packet-thread 保守 WARNING，已确认 Proxy/主线程桥接路径。`runClient`、`runServer`、多人/重启/跨日库存仍需可用运行环境。

## 第 12～13 批验证状态

- `check_toolchain.py`、严格 Forge 1.12.2 audit（0 ERROR；4 条既有 packet-thread WARNING 已审查）、`compileJava`、`processResources`、`crabTrapSelfTest` 和已有六项 self-test 均通过。
- `CrabTrapSelfTest` 覆盖五种合法肉/四种鱼拒绝、100～600 tick、Lure 非正值重抽、奇数 tick 饵料向上取整、原版根 loot table 的 Luck 权重、20 槽、Hopper 侧面规则、收获满仓预检及库存/pending NBT 往返。
- `runClient`、`runServer` 均实际进入 Forge/FML/coremod 引导与 mods 文件夹扫描，但在本 WSL 环境完成本模组加载前停止，未创建可手测客户端窗口，Server 也未到 EULA/世界阶段；静态路径确认新 common 代码没有 `net.minecraft.client.*` 引用。
