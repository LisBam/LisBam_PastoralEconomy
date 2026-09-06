# 模组当前架构

当前实际完成批次：第 15 批（交通旅行与村庄解锁）。

当前发行版本：`1.5`（Minecraft Forge 1.12.2 / 14.23.5.2859，Java 8）。

## 已实现

### 2026-09-06 新内容：交易凭证与箱子出售

- `item.ItemTradeVoucher` 以稳定 Item ID `lisbam_pastoral_economy:trade_voucher` 注册一个最大堆叠为 1 的物品。空白状态右键后只由逻辑服务端把使用者 UUID 与绑定时玩家名写入物品自身的 `lisbam_pastoral_economy` 子 NBT；UUID 是授权依据且只能首次写入，名字只负责显示为“交易凭证-玩家名”。客户端 `bound` 模型属性在同一 Item ID 上切换空白/已绑定的两套 16×16 贴图，不新增实体、Capability 或世界数据。
- `merchant.TradeVoucherStorageService` 在服务端商人交易主线程中组合玩家主物品栏与授权箱子。候选只来自所有已加载服务器维度中当前已加载的原版 `TileEntityChest`，不会加载区块；相邻双箱按一个库存处理，并跳过上锁箱子及尚未生成内容的原版战利品箱。箱中任意槽存在与售卖玩家 UUID 相符的凭证即可授权，显示名不参与验证。
- 出售先消费玩家主物品栏，再按维度/坐标稳定顺序消费授权箱子；扣货、牛奶桶返还与金币入账共用一份多库存快照，任一步失败都恢复所有来源。商人快照沿用既有 `remainingItems` 整数字段携带“授权箱子库存数”，客户端与本地玩家实时主背包数相加显示“可出售”，Packet discriminator、编码顺序和服务端权威结算均不变。

### 2026-09-06 维护：肩部槽 UI、快捷穿戴与玩家模型同步

- 生存背包中的肩部槽使用原版 `inventory.png` 槽格并固定在副手槽正上方（物品坐标 `77,44`）；创造模式“生存物品栏”页使用原版 `tab_inventory.png` 槽格，并按该页副手槽坐标固定在 `35,2`。`GuiScreenEvent.DrawScreenEvent.Pre` 在创造页每次重建 `CreativeSlot` 后重新定位包装槽，前景阶段只裁取原版 18×18 槽框、恢复物品与原版悬停遮罩。
- `ShoulderEquipmentEventHandler` 在双端拦截手持鞘翅的 `RightClickItem`：客户端只返回成功以保留挥手反馈和发包，逻辑服务端确认肩部为空后复制一件到既有 Capability；生存模式扣除手持鞘翅，创造模式与原版护甲右键一致保留手持物。肩部被占用或 Capability 不可用时拒绝，绝不退回胸甲槽。
- Coremod 追加补丁原版 `LayerElytra#doRenderLayer` 的胸甲读取，因此胸甲模型与肩部鞘翅模型可同时显示。Packet 9 `SyncShoulderEquipmentMessage` 只从服务端向本人和追踪该玩家的客户端同步实体 ID 与一件鞘翅；登录、重生、换维度、开始追踪、槽位变化以及飞行耐久/损坏造成的栈内变化都会同步，客户端仅在主线程更新展示用远端玩家 Capability。

### 2026-09-06 维护：村庄交通方块指南针与肩部鞘翅栏

- `item.ItemVillageTransportCompass` 注册稳定 Item ID `lisbam_pastoral_economy:village_transport_compass`。逻辑服务端仅在主/副手持有时按 20 tick 刷新所持 ItemStack 的目标 NBT：候选来自同维度的村庄 `StationRecord` 与所有当前加载的物理 `TileVillageStation`，**不读取也不要求** `PastoralWorldData.transport` 的交通节点登记；按水平距离和坐标稳定平局选择最近方块，且不强制加载区块。客户端 `angle` 公式逐字保持原版 `ItemCompass`，只替换目标坐标；目标不存在或不在当前维度时沿用原版随机指针语义。无序配方固定为指南针、铁锭、红石粉与绿宝石各一，双语名与 32 张 16×16 PNG 完整打包；所有帧都以原版指南针为基底，仅把两种红针颜色改为统一绿/青色。
- `PlayerDataCapability` 升至 `dataVersion=3`，新增仅接受 `Items.ELYTRA` 的持久 `shoulder` ItemStack；容器同步直接复用原版 `ContainerPlayer` 的 tracked slot 机制。发行 Coremod 将 1.12.2 `ContainerPlayer` 的四个原版 Armor Slot 改为胸甲位拒绝鞘翅的等价 Slot，并追加可 Shift-click 放入的肩部槽；客户端只在 `GuiInventory` 前景阶段裁取原版 `inventory.png` 的 18×18 槽格，绝不手绘槽框。Coremod 的容器匹配改按 release 方法签名/静态槽位调用识别，避免正式混淆名不匹配时错误回退整项补丁。
- 同一 Coremod 精确替换客户端起飞、服务端 `START_FALL_FLYING` 验证与 `EntityLivingBase#updateElytra` 的胸甲鞘翅读取，使肩部鞘翅继续使用原版飞行与每 20 tick 耐久消耗。所有注入 hook 使用 `Object` 描述符，并同时覆盖 MCP、SRG 和 1.12.2 发行混淆名。旧存档中胸甲位的鞘翅会在登录、重生或换维度时移到空肩部槽；肩部已被占用时安全放入背包，背包满才掉落，绝不覆盖或删除物品。

### 2026-09-05 维护：挤奶冷却、价格重构与剪刀附魔

- `event.MilkingCooldownEventHandler` 在逻辑服务端检查成年 `EntityCow`（含哞菇）的桶交互；每头牛将最近成功挤奶世界总 tick 持久化到实体 NBT，冷却固定为 6000 tick（5 分钟）。启用冷却时，两侧都取消原版 `EntityCow` 桶交互，客户端只返回成功而不修改库存，服务端独立完成音效、空桶扣减、牛奶桶替换/入包/满包掉落，并在完整成功后写入冷却 tick；冷却中的目标不产生牛奶。客户端始终等待服务端同步，避免本地预测造成假牛奶桶或饮用状态异常。`config.ModSettings.disableMilkingCooldown` 默认 `false`，开启后服务端不接管并恢复原版交互。
- `market.MarketCatalog` 的出售与购买基础价已按《全商品手动调价表_调整后.xlsx》以 1 小麦 = 50 金币换算并冻结；稳定 market key、NBT 和历史兼容保持不变。
- `core.EnchantingTableCorePlugin` 是随发行 JAR 加载的最小 1.12.2 Coremod：按运行时发现的 `Item` 方法布局补丁 `ItemStack` 附魔力重载及效率筛选，或回退补丁旧式无参附魔力。附魔力 helper 的第一个实参固定为被调用的 `Item` 实例，剪刀按铁工具、各锄头按自身材质进入原版候选；剪刀效率直接为原版 `Efficiency`。注入调用只使用 `Object` 描述符，避免 MCP/SRG 名称差异；效率筛选只在既有 `IRETURN` 前合并原版布尔结果，绝不插入跳转或新 StackMap frame。意外布局只记录错误并保留原版 `Item`，不阻止游戏启动。剪刀仍可使用模组 Harvest/Range 以及原版耐久、经验修补、消失诅咒等合法附魔；Harvest 剪羊/剪哞菇增产沿用既有成功后结算流水线。

### 2026-09-05 任务维护

- 新增牛肉、猪排、鸡肉、羊肉、兔肉、鳕鱼和鲑鱼出售条目；商人购买目录商品均写入 30 日历史目录。行情书改为出售/购买双分页，购买页有名称/key 搜索和逐行滚动；只建立可见选择按钮，选中商品优先请求、其余按客户端缓存低频渐进预取。
- 行情书的每个合法 C2S 选择请求都在已经调度的服务端主线程直接返回一个最多 30 点的快照，不再依赖无槽 Container 的延迟 tick；旧存档缺少某商品的当天历史时，最新快照至少包含已冻结的当天价格点。购买页附魔书按其稳定 market key 还原真实附魔 NBT，原版 Tooltip 因而显示具体附魔和等级。背景整块裁取原版 `demo_background.png`，文本仍使用 `FontRenderer`。
- 商人以持久的村庄中心作为 32 格圆形家区与出生范围；超出 32 格时寻路返航，超出 64 格时传送到持久村庄站旁。刷怪蛋生成未绑定商人，进入有效村庄会被收编；村庄身份识别仍使用独立的 128/64/160 格参考值。
- 新增 `PastoralCreativeTab`（金闪闪骨粉图标，显示名“聆竹の休闲田园经济”）以及默认关闭的 `restoreVanillaMonsterSpawns`、`disableAnimalBoneDrops` 设置。
- 行情书价格头部追加基础价并将昨日价/趋势和曲线整体下移一行，保持 320×240 最小缩放下的文字与曲线留白。原版库存 Tooltip 中悬停 29 类可出售物品会显示服务器冻结的今日收购单价；客户端仅缓存当前市场日的显示值，未命中时每商品最多每秒请求一次。服务端只接受合法出售目录 key，不允许客户端自行推算价格。

- 主入口：`lisbam.pastoraleconomy.LisBamPastoralEconomy`，负责模组身份、生命周期调度与基础日志。
- 代理：`CommonProxy` 不引用客户端类；`ClientProxy` 是物理客户端扩展点。
- 设置：`config.ModSettings` 在 common `preInit` 读取服务器权威的 `market.moreStableMarketVolatility`（默认 `false`）；Mod List 的物理客户端 `ModGuiFactory` 仅提供同一 Forge 1.12.2 配置文件的编辑入口。`false` 使用链式日波动，`true` 恢复旧的独立三角分布；配置读取和切换绝不重算已经持久化的当天快照。
- 注册：`registry.RegistrationHandler` 是唯一的静态 Forge Registry Event Subscriber；已注册 `item.ModItems.MARKET_BOOK`、`item.ModItems.VILLAGE_TRANSPORT_COMPASS`、`item.ModItems.TRADE_VOUCHER`、`item.ModItems.GOLDEN_BONE_MEAL`、`item.ModItems.CRAB_TRAP_ITEM`、`item.ModItems.TRANSPORT_STATION_ITEM`、`block.ModBlocks.VILLAGE_STATION`、`block.ModBlocks.CRAB_TRAP`、`block.ModBlocks.TRANSPORT_STATION` 及十二个有效附魔 ID。已移除的 `shears_efficiency` 在旧存档加载时通过 `MissingMappings<Enchantment>` 重映射为原版 `Efficiency`。`entity.ModEntities` 使用 1.12.2 `EntityRegistry.registerModEntity` 和 `GameRegistry.registerTileEntity` 注册商人、村庄站点、蟹笼与交通节点 TileEntity。
- 网络：`network.ModNetwork` 持有唯一 `SimpleNetworkWrapper`，Channel 为 `lb_pastoral`；Packet 0～2 保持原有金币/行情协议，Packet 3 为有界 C2S 商人交易请求，Packet 4 为 S2C 商人快照，Packet 5/6 为交通节点 C2S 操作/S2C 状态快照，Packet 7/8 为背包 Tooltip 的有界 C2S 商品 key 请求/S2C 当前世界日价格，Packet 9 为追踪玩家肩部鞘翅的 S2C 展示同步。Common Handler 不引用客户端类型，经 Proxy 调用客户端主线程执行器；全部 C2S 请求在服务端主线程验证并结算。
- GUI：`gui.GuiIds.MARKET_BOOK=0`、`MERCHANT_TRADE=1`、`CRAB_TRAP=2`、`TRANSPORT_STATION=3`。`ModGuiHandler` 在服务端返回无槽行情书、绑定实体的商人、坐标绑定的 `ContainerCrabTrap` 或 `ContainerTransportStation`；客户端经 Proxy 返回相应 GUI，common side 不加载客户端类。`GuiMarketBook`、`GuiMerchantTrade`、`GuiTransportStation` 和 `GuiCrabTrap` 全部继承 `GuiContainer`：前三者各自创建同类型、无权威数据的客户端生命周期 Container，并在 `initGui` 首先调用 `super.initGui()`。因此 Forge `OpenGui` 写入服务端 `windowId` 时目标是专用客户端容器，永久的玩家 `inventoryContainer.windowId` 始终保持 0；商人和交通的逻辑服务端仍只使用绑定真实 UUID/Tile 的权威构造器。交通客户端生命周期容器只需要服务端已验证的 BlockPos，不依赖 TileEntity 抵达客户端的同步时序。行情书、商人、交通及交通确认层全部不暂停单人集成服务端；自定义界面及确认层会把原版 Esc 和“打开背包”键（默认 E，尊重改键）统一映射到 `EntityPlayerSP#closeScreen`，从而发送关闭窗口包并让服务端 Container 一起关闭。所有 UI 文字使用原版 `FontRenderer`，物品悬停使用原版 `GuiScreen#renderToolTip`。商人、交通的静态非按钮文字直调 `FontRenderer#drawString(..., 4210752)`，即工作台/熔炉标题同一无阴影路径；选中的交通节点是唯一的状态例外，名称、坐标和旅行费使用绿色 `0x55FF55`，交通改名和商人数量输入框文字均为白色。商人交易与行情书对“任意颜色羊毛”构造仅显示用途的命名 ItemStack，显示名为“羊毛”，不改变原有 Item/meta 或行情 key。商人/交通每帧以一次完整裁取绘制原版 `demo_background.png`，不把该纹理拆成九宫格；按钮/输入框/滑条保持原版控件。商人物品底框直接裁取 `generic_54.png`，有限库存直接显示剩余实际物品数，不显示组数或每组数量。`ClientMerchantTradeViewState` 仅在物理客户端保存本次游戏会话最后选择的出售/购买页，绝不参与交易请求或存档。`GuiCrabTrap` 明确按原版 `GuiChest` 的 `drawScreen` 路径调用 `renderHoveredToolTip`，使 20 个蟹笼槽和玩家背包槽都走原版 Tooltip；其槽位背景仍复用 `generic_54` 的标题/玩家背包片段、原始背景像素和逐格裁取。两个显式受限 Container Slot 与 TileEntity/Hopper 复用校验，0 仅接受钓竿、1 仅接受合法生肉、2～19 为通用仓储/捕捞产出槽。`GuiTransportStation` 自动请求最近未接入村庄，仅显示一个接入按钮；接入最近村庄与移出节点都使用原版 `GuiYesNo` 的泥土背景、白色文字和 `GuiOptionButton`。最近村庄确认的“是”按钮即使客户端余额不足也保持可点，并照常把请求交给既有服务端重新搜索、重新定价和余额校验；移出层只在确认后发送既有 `REMOVE` 请求。交通面板中“我的节点”行位于第二行动按钮下方；节点列表区域可用鼠标滚轮逐行滚动，坐标没有维度后缀。节点移出即删除个人记录，快照会清理历史无效/未激活记录，不渲染“已移出”或“节点无效”节点。`SyncTransportStateMessage` 的每个 `TransportNodeView` 附带服务端计算的目的地旅行费或不可用标记，客户端据此显示费用并禁用不可旅行/余额不足的操作。
- 生成规则：`event.OverworldMonsterSpawnEventHandler` 是 common 静态订阅器。它只在逻辑服务端的维度 0 对 `LivingSpawnEvent.CheckSpawn` 的 `isSpawner=false` 且 `EnumCreatureType.MONSTER` 设为 `DENY`；因此拒绝自然/区块生成敌对生物，保留刷怪笼、刷怪蛋、命令和直接实体生成。
- 资源：`mcmod.info`、`pack.mcmeta`（format 3）、中英文 `.lang`、正式 Logo、行情书资源和蟹笼的 blockstate、Block/Item model、合成配方均已存在；`textures/blocks/crab_trap.png` 与 `transport_station.png` 都是 32×32、最近邻简约原版风 PNG。`village_station` 与玩家可放置的交通站共用后者，不再引用原版橡木木板；玩家交通方块与 ItemBlock 共用显式 `tile...transport_station` 显示键，中文为“交通方块”，不会再附加 `.name`。行情书模型使用自有 `textures/items/market_book.png` 的绿皮书图标。交易凭证以一套空白卷纸和一套带标记/封印卷纸的 16×16 RGBA 贴图表现同一物品的空白/绑定状态；当前未定义合成配方，只能从本模组创造栏或命令获得。`golden_bone_meal` 有独立 16×16 原版骨粉轮廓的金色/橙色点缀 PNG、generated model、双语名及两份无序配方。蟹笼合成固定为铁锭/铁栅栏交错外圈、中央陷阱箱。
- 玩家数据：`data.player.PlayerDataCapability`（`lisbam_pastoral_economy:player_data`）是唯一玩家 Capability，`dataVersion=3`。数据保存玩家金币 `long`、一个仅接受鞘翅的肩部栈、启动交通方块一次性发放状态、首个自建站免费资格历史、有效玩家节点、别名和自建站序号；移出节点会从映射直接删除，读取旧 `active=false` 条目时丢弃。Capability NBT 处理默认/旧字段并把非法负金币修正为 0。Clone 时深复制完整数据。
- 世界数据：`data.world.PastoralWorldData`（`lisbam_pastoral_economy_world_data`）是唯一 WorldSavedData 根，`dataVersion=7`。访问器始终解析服务器主世界 MapStorage，保证跨维度只有一份共享数据。v7 `market` 段保存持久 marketSeed、首次市场日、当前/昨日快照和全部商人出售/购买历史商品的最近 30 天；不再持久化无限 `processedDays`、无限历史或已移除的 `farmHarassment` 记录。旧存档的该废弃字段读取时忽略、下一次保存时移除；merchant 段保存村庄、站点、商人、Offer、库存、轮换状态和附魔书已解析等级；transport 段保存 SELF_BUILT/VILLAGE 物理节点 UUID、维度、坐标和村庄身份引用。
- 市场：`market.MarketCatalog` 是唯一不可变商品目录，保存稳定经济 key、1.12.2 Item/meta、金币×10 后的基础价、8 类单日波动率、上下限倍率和历史标志。默认 `MarketPriceGenerator` 从前一天持久化价格按类别 25%～100% 的波动率步长上涨或下跌；偏离基础价越远，回归概率按 50%/55%/65%/75%/85% 分档提高，结果始终 clamp 在类别最低/最高价，触及边界的下一天强制向内反弹，并在整数取整后保留可见方向变化。设置开启时仍使用独立的稳定模式公式，但同样使用新基础价和价格上下限。买卖渠道只要是同一 Item/meta 变体就共享稳定随机身份，因此同一天方向和幅度来源一致。`MarketService` 是唯一服务器市场 API：行情书打开请求或实际经济读取才惰性推进到当前日，主世界不再每 tick 推进市场；为保证链式价格，跨多日会逐日推进并只持久化最近 30 天的收购历史。`MarketPriceSnapshot` 为商人单次同步批量提供当前/昨日价格，`MarketHistorySnapshot` 最多 30 点且不再提供更早页。
- 经济：`data.player.CoinService` 是唯一金币写入口。逻辑服务端 `EntityPlayerMP` 才能调用；拒绝负数、拒绝 long 溢出、没有公共 setBalance。成功改动时发送自己的 S2C 同步。
- 客户端：`client.ClientPlayerState`、`ClientMarketState` 与 `ClientMarketTooltipState` 都是非持久、非权威缓存，连接/断开时清空。市场缓存有明确书本会话：打开时清空并开始接收、关闭时清空并拒绝迟到包，连接内请求号保持递增；Packet 1 每次只下发一个商品的最新 30 日窗口，`GuiMarketBook` 分为出售/购买两页，购买商品可按本地化名称或 key 过滤并在受限图标格中滚动。选中商品优先请求，其余商品由会话内客户端缓存按 4 tick 节流渐进预取；选中请求 40 client tick 未返回时重新请求，关闭后不保留市场工作。`ClientMarketTooltipEventHandler` 在原版 `ItemTooltipEvent` 路径为当前客户端玩家的 29 类出售物品添加今日收购价行，以当前 `worldTime / 24000` 严格匹配 Packet 8 市场日，过日绝不显示缓存的昨日价；服务端每个合法 key 请求都在主线程冻结并回包。`ClientNightVisionRenderHandler` 仅在物理客户端以可恢复的临时 gamma 实现 10 秒初始/5 秒续期的夜视视觉，不创建药水效果或修改世界光照。`ContainerMarketBook` 只作为实际打开书本的会话验证标记。`GuiMerchantTrade` 的数量滑条、输入框与页签偏好均为本地展示状态，出售持有量按客户端 tick 缓存，仍只发送既有有界数量请求；每次成功交易后的库存窗口、金币同步和商人快照均由服务端在同一逻辑 tick 推送。`ClientModelRegistry` 是物理客户端模型注册。`ClientHudEventHandler` 仍在 `RenderGameOverlayEvent.Text` 中右上角以 12 个 scaled pixels 页边距渲染金币。
- 附魔定义：`enchantment.EnchantmentPastoral` 统一实现 1.12.2 物品筛选、书本可得性、稀有度、宝藏/诅咒标记、附魔台可得性和互斥规则；`ModEnchantments` 持有十二个稳定实例和精确原版装备白名单。Coremod 只把剪刀原版 `Efficiency` 放回附魔台候选，绝不新增第二个效率 ID 或书本。Harvest 与 Fortune 互斥；Slaughter 与 Sharpness、Smite、Bane、Looting 互斥；Bluntness Curse 与 Sweeping Edge 互斥且为唯一不进入附魔台的宝藏诅咒。Fleetfoot 只适用于皮革/锁链/铁/金/钻石护腿；Attack Speed、Reforged 适用于剑、斧、镐、锹、锄；Range 在此基础上也适用于剪刀，Harvest 适用于锄和剪刀。
- 农业：`agriculture.AgricultureRules` 是成熟作物身份、种植物和全部随机公式的单一来源。`event.AgricultureEnchantmentEventHandler` 在服务端 BreakEvent 捕获玩家的成熟收获动作，并在同一 HarvestDropsEvent 管线中依次加入锄头 Harvest 奖励、按 Fine Cultivation 消耗最终掉落/主背包补种、再单次判定 Pastoral Favor；`ShearingHarvestEnchantmentEventHandler` 在服务端记录带 Harvest 剪刀的羊/哞菇原版剪毛请求，只有原版剪毛实际成功后才同 tick 追加同一随机公式的羊毛或红色蘑菇。PlaceEvent 仅对真实玩家的列出作物种植执行 Pastoral Favor。`ItemGoldenBoneMeal` 只在逻辑服务端写世界：中心的原版可耕作物重复原版 `ItemDye.applyBonemeal` 至成熟（最多 8 次），同层 5×5 其余作物各执行一次原版骨粉；草方块/花以其草层为中心对 5×5 草方块各执行一次原版骨粉，其他目标也保留一次普通骨粉路径。它在 common `preInit` 注册为 Forge `dyeWhite` OreDictionary 条目，使用临时原版白色 `ItemDye` 保留羊的白色染料行为，并把自己的增强骨粉路径注册为发射器行为；失败时沿用原版默认发射器抛出物品。`GoldenBoneMealRules` 是其作物/野生目标分类和 5×5 边界的纯规则来源。
- 工具与战斗：`event.TreeFellingEventHandler` 对带 Felling 的真实玩家斧头，在有限的同树种原木连通块和匹配树冠确认后，以 `EntityPlayerMP#interactionManager.tryHarvestBlock` 逐块处理原木和叶子，因此保护事件、原版掉落、Unbreaking 与耐久仍逐块生效。`AnimalBoneDropEventHandler` 在服务端 `LivingDropsEvent` 的 `HIGHEST` 优先级按严格动物白名单加入骨头，`AnimalBoneDropRules` 先进行基础掉落、再使用事件提供的 Looting 等级计算附加数量。`SlaughterEnchantmentEventHandler` 随后以既有倍率处理完整掉落列表，因而已成功的骨头和原掉落一致受屠宰影响，且不改 XP。
- 蟹笼：`tile.TileCrabTrap` 是每个蟹笼实例的唯一权威状态所有者，保存 20 槽库存（0 为钓竿、1 为合法生肉、2～19 为通用仓储/捕捞产出）、本轮 remaining ticks、Lure/Luck/有饵快照和 pending loot。逻辑服务端只在接触 1.12.2 原版水/流动水时推进计时；它以原版 `LootTableList.GAMEPLAY_FISHING` 和快照 Luck 生成一次真实战利品；每轮基础等待为 2,000～12,000 tick（100～600 秒），Lure 每级减少 2,000 tick（100 秒）。完整插入 2～19 号产出位后才按本轮有饵状态独立判定 50% 消耗。倒计时仍逐 tick 精确推进，但只每 20 tick（以及状态转换）标记 TileEntity 脏，避免大量蟹笼持续弄脏区块。`ISidedInventory` 对每个方向公开全部 20 槽，可从任意槽抽取；插入校验与 GUI 相同，0/1 拒绝非功能物品、2～19 接受任意物品。没有 `IItemHandler` capability 覆盖，确保原版 Hopper 使用该 1.12.2 侧面库存路径。`BlockCrabTrap` 负责打开 GUI 以及仅一次地掉落本体、库存和 pending loot。
- 村庄与商人：`merchant.VillageService` 仅在主世界每 200 tick（10 秒）观察 1.12.2 `VillageCollection`，按 128/64/160 格参考值去重并维护稳定 `VillageRecord`；每轮维护只扫描一次已加载实体来清除异常/重复商人并建立活动索引。世界加载不强制补生商人，首次周期维护额外等待一轮实体载入；此后只在村庄站和记录的最后实体区块均已加载、仍未找到该实体时补生。补生在以 `VillageRecord` 中心为圆心、32 格半径的已加载安全位置随机选择，绝不使用方形边角；未绑定刷怪蛋商人进入 128 格村庄识别范围时收编。`MerchantRecord` 在 merchant 段可选保存最后观测到的实体 Chunk X/Z；无效、非 active、跨村庄或不在 roster 的实体 NBT 直接剔除。`StationRecord` 与 `TileVillageStation` 保存受保护村庄站点身份，站点异常消失时在已加载安全位置恢复。`MerchantRecord`、`DailyOfferState`、`OfferRotationState` 和共享库存均在 `PastoralWorldData` 的 merchant 段持久化；每日购买列表为 8 项，每槽独立按普通 40%、罕见 30%、稀有 20%、珍宝 10% 选择品质，完成后按普通→罕见→稀有→珍宝稳定排序；商品仍在服务端按 Item/meta/NBT 变体身份去重，附魔书身份包含附魔注册名和等级。`TradeCatalog` 的价格均为单个物品价格；有限库存由任务书的 16 组→4 组、8 组→2 组、4 组→1 组、1 组→1 个换算为实际物品数，购买按件扣库存和计价，不再持久化或读取旧 `remainingBundles` 组库存字段。`EntityMerchant` 除绑定身份外还保存可兼容读取的村庄中心 NBT；它以该中心和 32 格半径设置原版限制/游荡 AI，超出 32 格时每 20 tick 最多重算一次返航路径，超出 64 格时以已持久的村庄站作为安全目标传送。死亡/重启由 reconciliation 按 `max(ceil(villagers / 5),3)` 补足且不设上限。`MerchantNameGenerator` 只在逻辑服务端按高频百家姓人口权重生成“1 姓 + 1--2 名”中文姓名，并写入原版 `CustomName` 实体 NBT；原版命名牌也经 `EntityLivingBase` 的原生交互写入同一字段。名称不进入 `MerchantRecord`，UUID 仍是唯一身份。实体还保存附加的 `merchantSkin` 索引，并以 `EntityDataManager` 同步给客户端：仅 1--4 为四种 64×64 Steve UV 的内置作物商人服装，旧索引 0 不再渲染 Steve 且会在实体加载时迁移。`GuiMerchantTrade` 和 `RenderMerchant` 仅读取已同步实体显示状态，不生成随机结果。商人基础生命、移动速度和攻击伤害使用原版玩家的 20/0.1/1 基准；`EntityAISwimming` 与可游泳地面导航使其在水面上浮。商人显式不提供生物环境、受伤或死亡声音。打开的服务器交易 Container 在实体上登记运行时交易者并令其停住，关闭时解除；商人平时观察 8 格内最近玩家，实际受到玩家伤害后只避开该攻击者 200 tick。上述交易者和逃跑状态不写入 NBT。
- 移动、交互与视觉：`MovementEnchantmentEventHandler` 仅在服务端 END Player Tick 更新两个固定 UUID、非持久的移动速度属性修饰符；Fleetfoot（护腿）与 Farmland Walker 都使用原版 Speed Potion 同样的 operation 2，Fleetfoot 修饰符在穿戴期间跨越跳跃保持稳定，`ClientFleetfootFovHandler` 在客户端重算去除 Fleetfoot 固定速度项后的属性值，因此穿戴和跳跃均不会改变 POV，同时保留弓、飞行和其他原版 FOV 变化。`FarmlandTrampleEvent` 在逻辑两侧以最高优先级取消 Farmland Walker 的踩踏，因此耕地和其上的植物都不会被该次跳跃/落地破坏。`ToolEnchantmentEventHandler` 仅在逻辑服务端为主手 Attack Speed/Range 写非持久 `ATTACK_SPEED`/Forge 1.12.2 `REACH_DISTANCE` 属性；Range 每级 +1.5 格，属性会同步给客户端且同时参与原版客户端选取和服务端实体攻击、方块破坏、实体交互距离校验。Night Vision 由 `ClientNightVisionRenderHandler` 在物理客户端以可恢复临时 gamma 实现 200 tick（10 秒）初始、每 100 tick（5 秒）续期；取下头盔立即恢复原始 gamma，不创建药水效果。`CombatEnchantmentEventHandler` 在原版计算横扫资格前以其移动距离边界关闭 Bluntness Curse 的横扫，不改直击伤害。`AnvilEnchantmentEventHandler` 对左槽带 Reforged 的非空右槽组合使用纯 `AnvilFirstUseRules` 生成瞬态输出/费用；对普通改名则只校正现有输出的费用/后续 RepairCost，不修改任一输入 NBT。
- 实体：模组不再修改敌对或中立生物的 AI、仇恨、反击、爆炸或方块破坏；它们在实际生成后完全走 Java 1.12.2 原版行为。`EntityMerchant` 仍是独立的非敌对实体，保存/交易职责不受敌对生成规则影响。

依赖方向：主入口 → Proxy；CommonProxy → PlayerDataCapability、ModEntities、ModNetwork、ModGuiHandler；Forge 生命周期事件 → PersistenceEventHandler → PlayerData/PastoralWorldData/CoinService/MarketService/VillageService/TransportService；自然敌对生成 → OverworldMonsterSpawnEventHandler；动物死亡掉落 → AnimalBoneDropEventHandler → SlaughterEnchantmentEventHandler；EntityMerchant 右键 → 服务端 ContainerMerchantTrade/TradeService → Packet 3/4 → ClientProxy → ClientMerchantTradeState → GuiMerchantTrade；玩家右键蟹笼 → ContainerCrabTrap/TileCrabTrap；TileCrabTrap → 1.12.2 原版 GAMEPLAY_FISHING LootTable/ISidedInventory/NBT；玩家右键自建或村庄交通节点 → ContainerTransportStation/TransportService → Packet 5/6 → ClientProxy → ClientTransportState → GuiTransportStation；TransportService → VillageTransportService/TransportCost/CoinService/PlayerData/PastoralWorldData/TileTransportStation/TileVillageStation；VillageTransportService → WorldServer.findNearestStructure/VillageService；VillageService → VillageRecord/StationRecord/TransportWorldState/MerchantRecord/EntityMerchant；TradeService → MarketService/CoinService/Inventory/MerchantRecord。ClientProxy 只在物理客户端加载客户端同步执行器、Tooltip/夜视视觉处理器、GUI、商人 Renderer 与模型。

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

## 经济市场与商品价格维护

- `MarketCatalog` 与 `TradeCatalog` 是唯一价格和商品来源。所有基础金币值已按任务书冻结为新面值；铁锭为 900/个，圆石/沙子为 240/个，玻璃为 400/个，原木为 500/个，泥土为 160/个，黑曜石为 2,000/个。买卖方向可有不同基础价，但同一 Item/meta 变体共用 `variantIdentity`，从而共享当日随机方向和幅度来源。
- `MarketPriceGenerator` 默认链式承接昨日价格，单日步长为类别波动率的 25%～100%，回归概率按偏离基础价 0/10/25/50% 分档为 50/55/65/75/85%。每类商品有明确最低/最高倍率，越界候选会 clamp；触及边界的下一天强制向区间内部移动，整数取整后仍保留方向变化。`moreStableMarketVolatility` 仍可启用独立稳定公式，但也使用新基础价和上下限。
- `TradeCatalogEntry` 以原版 `Item#getItemStackLimit()` 只换算有限库存：16 组→4 组、8 组→2 组、4 组→1 组、1 组→1 个；`DailyOffer` 直接保存 `remainingItems`。`MerchantTradeService` 按单个物品计算总价、扣库存和发放堆叠，服务器再次验证价格、余额、库存和背包容量；GUI 只显示单价、总价和实际剩余物品数，不再显示“一组数量”。
- `MerchantOfferService` 在逻辑服务端为 8 个购买槽分别抽取普通 40%、罕见 30%、稀有 20%、珍宝 10% 的品质，再以商品变体身份跨池去重、按品质稳定排序；各品质均允许当天为 0 个，商品池仍通过确定性轮换防止长期遗漏。附魔书身份包含附魔注册名与等级。`DailyOfferState` 拒绝重复或池类型/等级非法的列表；缺少 `remainingItems` 或仍含 10 个购买栏的旧 Offer NBT 直接视为无效并重新生成，不执行旧库存迁移。
- `MerchantTradeSnapshot` 的出售/购买条目数是交易快照协议的唯一计数来源；`SyncMerchantTradeMessage` 编解码均读取 `SELL_COUNT`/`BUY_COUNT`，不会把已缩减为 8 槽的购买快照按旧 10 槽截断。客户端只把完整、已验证的快照交给交易 GUI。
- `GuiMarketBook` 的购买页仍只在客户端按显示名、稳定商品 key 和本地拼音过滤目录。`PinyinSearch` 缓存显示名的全拼、首字母和逐字读音，支持全拼、首字母及混合输入；其紧凑字典只包含原版物品/方块和本模组本地化实际使用的汉字，不发网络请求、不参与价格或交易判定。

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
- `CrabTrapSelfTest` 覆盖五种合法肉/四种鱼拒绝、2,000～12,000 tick、Lure 非正值重抽、奇数 tick 饵料向上取整、原版根 loot table 的 Luck 权重、20 槽通用存取、全方向 Hopper 规则、产出位满仓预检及库存/pending NBT 往返。
- `runClient`、`runServer` 均实际进入 Forge/FML/coremod 引导与 mods 文件夹扫描，但在本 WSL 环境完成本模组加载前停止，未创建可手测客户端窗口，Server 也未到 EULA/世界阶段；静态路径确认新 common 代码没有 `net.minecraft.client.*` 引用。
