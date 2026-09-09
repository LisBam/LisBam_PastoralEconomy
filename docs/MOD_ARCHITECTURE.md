# 模组当前架构

当前状态：维护阶段（出货箱）。

当前发行版本：`1.7`（Minecraft Forge 1.12.2 / 14.23.5.2859，Java 8）。

### 2026-09-09 平衡维护：附魔台稀有度与背包透明图标

- `ModEnchantments` 对十二种可进附魔台的模组附魔显式保存 `Enchantment.Rarity`：攻速/范围为 COMMON，耕地行者/田园眷顾/捷足为 UNCOMMON，精耕/屠宰/丰收/夜视为 RARE，掉落吸附/百炼如新/伐木为 VERY_RARE。原有束锋诅咒仍为不进入附魔台的 VERY_RARE 宝藏诅咒。所有 registry ID、最大等级、物品白名单、互斥、宝藏/诅咒与商人书本目录不变；既有 `AnvilFirstUseRules` 继续自然使用同一 rarity 档位计算费用。
- `backpack.png`、`advanced_backpack.png`、`super_backpack.png` 均保持同一 generated item model 路径，改为硬边 16×16 RGBA 像素图。图案像素为完全不透明，空白背景和四角为完全透明；`BackpackSelfTest` 检查尺寸、RGBA、无半透明值、可见主体和透明四角。资源调整不改变 Item ID、背包 NBT、玩家 Capability、GUI/Packet 或存档格式。

### 2026-09-08 维护：萤石粉与掉落吸附

- `MarketCatalog` 与 `TradeCatalog` 的稳定 `buy/rare/glowstone_dust` key 基础价同步为 400；没有改写 `PastoralWorldData` 中已保存的当日价格、价格历史或 DailyOffer，旧世界会继续由既有链式价格机制向新基础价范围收敛。
- 新增稳定 Enchantment registry ID `drop_attraction`。它为 I 级稀有、非宝藏附魔，范围固定为五种材质的剑/斧/镐/锹/锄和剪刀；商人珍宝书 key 为 `buy/treasure/enchanted_book/drop_attraction_1`，基础价 120,000，与经验修补相同。
- `DropAttractionEnchantmentEventHandler` 只在逻辑服务端把确认的方块收获栈重新生成有追踪的 `EntityItem`；它还在最低优先级的 `LivingDropsEvent` 中标记附魔主手直接近战击杀的非玩家生物既有掉落实体。`ShearingHarvestEnchantmentEventHandler` 在原版剪毛实体加入世界时标记其掉落，并标记丰收的额外掉落。`DropAttractionService` 是非持久的实体→玩家运行时映射：飞行中关闭重力并同步速度，到达后停在玩家位置；原版拾取流程仍是唯一库存写入者，背包满时实体保持在抵达位置。未新增 Player Capability、WorldSavedData、TileEntity NBT、Packet 或客户端权威状态。

## 已实现

### 2026-09-08 维护：肩部背包交互、原版槽位与翅膀调值

- `GuiMarketBook` 的“炒币”页现只显示绿宝石今日/昨日现货价、涨跌和既有 30 日曲线；“价格范围”及“商人买卖手续费”两行和对应语言键均已删除，商人页的实际 4% 服务端结算不变。
- `ShoulderEquipmentGuiHandler` 直接裁取原版 `inventory.png` 的盔甲格源区 `(7,7)`，而非原先的合成格 `(97,17)`；肩部格仍是原版尺寸 18×18，保留资源包兼容。`GuiBackpack` 现在沿用 `GuiChest` 的 `drawScreen`/`renderHoveredToolTip` 路径，为背包及玩家槽显示原版 Tooltip。
- `ShoulderBackpackKeyHandler` 在物理客户端注册可改键的默认 **B**。它只在无界面或原版玩家背包界面、且本地同步肩部确为背包时发送既有 Packet 10；服务端继续要求 `inventoryContainer` 和真实肩部背包，未增加 Packet discriminator、NBT、Capability 或客户端权威状态。
- 三张背包物品图标替换为简约清晰的 16×16 RGBA 硬边像素贴图；图案像素不透明，画布背景与四角透明，`BackpackSelfTest` 验证没有半透明值、存在主体和透明背景。
- 稳定 registry ID `feather_wings` 不变，显示名改为“翅膀”/“Wings”；非创造飞行耐久频率为 20 tick（1 秒）/点，商人珍宝基础买价为 2,888,888。市场目录与实际商人目录同时使用该值，已存档当天价格和历史仍遵循既有“不重写”策略。

### 2026-09-08 维护：背包同步、行情书炒币与锄头铁砧修复

- `BackpackInventory` 不再用 `ItemStack` 的 Java 对象身份判断肩部背包仍在装备。`PlayerData` 与 Packet 9 的所有者快照会按值复制肩部栈，原先客户端收到首次同步副本后会把开放 Container 的所有槽读为 `EMPTY`；现在只验证肩部仍是打开时相同的背包 Item tier，服务端的 NBT 库存和 Container 权威性不变。
- `PastoralWorldData` 升至 schema `v11`。既有 `market` 段追加有界 `emeraldHistory`（最多 30 个实际日价点）；首次绿宝石初始化和此后每个补算的世界日各记录一次。旧 v10 缺该段时只以已经持久化的当前价补出当前日一点，不重算普通市场或绿宝石价格。行情书复用 Packet 1/2 的有界显示快照，以稳定只读 key `lisbam_pastoral_economy:emerald/spot` 提供“炒币”第三页；没有新 Packet discriminator、交易入口或客户端权威状态。
- `GuiMarketBook` 的“炒币”页显示原版绿宝石图标、今日/昨日现货价、涨跌、300～3,000 固定范围、商人 4% 手续费说明和最多 30 点曲线；买卖仍只在服务端验证的商人页面进行。
- Forge 1.12.2 的 `ItemHoe` 继承 `Item#getIsRepairable`（恒为 false），并非近期作物耐久事件造成。`HoeAnvilRepairRules` 因而只为五把原版锄头恢复其材质的原版式铁砧材料修理（每份至多修复最大耐久的 25%）；带 Reforged 的锄头仍由既有首用费用计算处理。没有新的注册项、玩家 NBT、背包 NBT key 或网络包。

### 2026-09-07 新内容：出货箱

- 新增稳定 `shipping_box` Block/ItemBlock/TileEntity ID、GUI ID `5` 与 `TileShippingBox` 27 格库存。它使用标准 `IInventory`/`ISidedInventory`：玩家和箱子一样可自由放取，所有面均可由漏斗输入，但 `canExtractItem` 恒为 `false`，故无法被漏斗抽出。破坏时按通常容器规则掉落内部物品。
- `ShippingBoxService` 仅在逻辑服务端主世界每个世界日的晨间窗口（`World#getWorldTime() % 24000` 为 `0～1000`）并在市场刷新后执行一次。它扫描所有已加载维度中的已加载出货箱，不强制加载玩家未加载的区块；夜间的首次服务端 tick 不会再抢占当天派发标记。每箱只要存在至少一张已绑定的交易凭证，就把其中 `TradeCatalog` 可收购的商品按同一日全局价格售给商人，整箱总价统一按 `floor(70%)` 折算。牛奶桶售出后原槽留下空桶，无法收购的物品和凭证不变。
- 已绑定凭证的 UUID 是收益资格，重复同一 UUID 不重复分份；同箱唯一持证玩家均分折后总额，余数按 UUID 稳定顺序补 1 金币。`PastoralWorldData` schema 升至 `v10`，新增有界 `shippingBoxPayouts`，持久化最后派发世界日和离线/金币余额溢出时的待领收益。每个玩家在所有出货箱完成统一核算后至多收到一次绿色聊天消息 `【出货箱】今日收益+xxx金币！`；上线和随后服务端轮询也会安全发放其待领余额。
- 配方为任意六种原版木板围绕一张 `trade_voucher`，配方不限制凭证 NBT，因此空白和已绑定凭证均可作为中心材料。方块使用 AI 生成像素源图后固定采样的 16×16 RGBA 木箱贴图；`ClientModelRegistry` 只在物理客户端注册其 inventory model。

### 2026-09-07 新内容：绿宝石现货市场与经济调整

- `PastoralWorldData` 升至 schema `v9`。既有 `market` 段追加 `emeraldInitialized`、`emeraldCurrentPrice`、`emeraldPreviousPrice` 与 `emeraldLastUpdateDay`，以同一持久 `marketSeed` 独立生成绿宝石每个世界日的现货价：首次严格为 1,000 金币，范围固定 300～3,000，之后按 `25% ±5`、`45% ±15`、`20% -30～+30`、`8% -50～+60`、`2% -70～+100` 的离散分布逐日变化，不向 1,000 自动回归。时间跳跃逐日补算、同日和时间回拨绝不重抽；旧 v8 存档第一次读取只补写 1,000 的当前/昨日价，不改变普通市场当日快照。
- `MarketCatalog` 与 `TradeCatalog` 将胡萝卜、马铃薯的基础收购价由 40 调为 60；已保存的当天/昨日普通行情仍按原有不重写规则保持，次日才从旧保存价继续演进。羊毛通用 JSON 配方改为任意颜色羊毛 ×1 → 线 ×1。`Items.EMERALD` 已从常规商人购买目录及其未来每日 Offer 池删除，历史持久 Offer 读入时会失效并按当前目录重建；`Blocks.EMERALD_ORE` 的珍宝商品保持不变。
- 商人交易新增独立的“炒币”分页。`MerchantTradeSnapshot`/Packet 4 同步当前/昨日价、持仓和服务器计算的买入/卖出上限；追加的 Packet 11 C2S 请求只含商人会话、方向、数量和 requestId，绝不信任客户端价格、余额或持仓。服务端重新验证窗口、实体、距离、世界日、递增请求号、金币、主物品栏容量与绿宝石数量后，按买入 `ceil(price × amount × 1.04)`、卖出 `floor(price × amount × 0.96)` 原子结算并同步所有正在与同一商人交易的玩家。Packet 0～10 discriminator 保持不变；Packet 4 的追加字段与 Packet 11 要求客户端和服务端一同升级到 1.6。
- `GuiMerchantTrade` 的第三页使用左右独立的原版 `GuiSlider`/`GuiTextField` 组分别控制买入与卖出数量，显示 `EMR/金币`、今日/昨日与涨跌、4% 手续费、各方向上限和预计金额。预览仅用于显示，买卖按钮仍只是向服务端提交数量。

### 2026-09-07 维护：每日挤奶、伐木耐久附魔核验与区块加载器

- `MilkingCooldownEventHandler` 不再按每头牛的最近 tick 计算五分钟倒计时；它只保存该牛最近成功挤奶的世界日，并以当前 `World#getWorldTime() / 24000` 在逻辑服务端比较。成年牛（含哞菇）在同一世界日只能成功一次；进入下一世界日后，所有牛统一恢复可挤奶状态。客户端仍不预测物品栏，`disableMilkingCooldown` 仍完整旁路为原版连续挤奶。
- `TreeFellingEventHandler` 的每个二级原木继续调用原版 `EntityPlayerMP#interactionManager.tryHarvestBlock`。Forge 1.12.2 在该路径中逐格调用主手 `ItemStack#onBlockDestroyed`，而斧头随后经原版 `damageItem` 执行耐久附魔判定；因此每个原木已经独立进行耐久附魔掷骰，不需另造一套耐久损耗逻辑。连锁树叶仍在成功采掘后恢复这一次工具损伤，故不计伐木耐久。
- 新增稳定 Block/Item/TileEntity ID `chunk_loader`。`BlockChunkLoader` 的 `powered` 方块状态决定同一深色石质/紫色框架的 16×16 未充能/充能贴图和 0/7 光照；`TileChunkLoader` 只保存其激活状态。逻辑服务端的 `ChunkLoaderService` 为有红石信号的方块申请一张 Forge `NORMAL` ticket、只强制加载自身 `ChunkPos`，并在每个逻辑服务端世界 END tick 复核每张现存票据的方块、TileEntity 和红石信号；断电、方块破坏、无效 ticket 或重载后无信号都释放。它复用模组唯一的票据加载回调，以 ticket modData 恢复已激活实例；没有新增 `PastoralWorldData` 或网络协议。`ItemChunkLoader` 与方块均使用显式 `tile...chunk_loader` 显示键，不会显示多余的 `.name`。

### 2026-09-07 维护：Coremod 类加载隔离与发行完整性

- `core.EnchantingTableCorePlugin` 通过 `@TransformerExclusions("lisbam.pastoraleconomy")` 将完整自有根包排除出 LaunchWrapper 全局转换链。本模组业务类不是任何自有 Transformer 的目标，直接定义可避免第三方 Coremod 或负资源缓存让农业内部类、GUI Container、蟹笼规则等首次延迟加载得到空字节；自有 Transformer 仍只修改明确的原版类目标。
- `verifyReleaseJar` 在 `exportReleaseJar` 后打开实际 `release/LisBam_PastoralEconomy-<version>.jar`，逐项对照 `build/classes/java/main` 的全部生产 class，要求条目存在、非空、可读取且 class major version 为 52。它作为 `build` 的最终任务执行，故漏包、空条目或非 Java 8 字节码不能被当成可发布构建。

### 2026-09-06 维护：农作物事件时序、锄头耐久与伐木耐久

- `FellingDurabilityRules` 是伐木的纯耐久边界：斧头仅剩 1 点耐久时不启动；在触发原木尚未执行原版扣耐久前，二级原木必须额外预留“触发原木 1 点 + 最终 1 点”。`TreeFellingEventHandler` 继续对原木和树叶调用原版 `tryHarvestBlock`，因此保护与掉落不变；成功连锁树叶后恢复该次工具损耗，树叶不会消耗伐木耐久。
- `AgricultureEnchantmentEventHandler` 的玩家级动作只承载成熟作物的 Harvest、Fine Cultivation 和 Pastoral Favor。掉落事件完成种植物扣除后，Fine Cultivation 仅登记补种；服务器 END tick 才在空气+耕地仍有效时写入同作物 age 0，令该状态成为原版收获结束后的最终权威更新；玩家手动种植列表内作物后也会在 END tick 广播最终服务端状态。
- `HoeCropDurabilityEventHandler` 独立监听逻辑服务端 `HarvestDropsEvent`；成功收获现有农业范围的零硬度作物时记录同一快捷栏锄头，服务器 END tick 才调用一次 `ItemStack#damageItem`、处理工具损坏事件和库存同步，并重新广播该坐标的最终服务端状态。硬度非零作物继续只走原版路径，创造模式与自动化不修改。
- 本次不新增注册项、Capability、NBT、世界数据或网络包，旧存档无迁移需要。

### 2026-09-06 新内容：肩部羽毛翅膀与田园眷顾调值

- 新增稳定 Item ID `feather_wings`。它继承 Java 1.12.2 `ItemElytra` 以进入原版 BREAKABLE/WEARABLE 附魔边界，但不进入原版鞘翅飞行 hook；胸甲槽和既有肩部 Slot 都显式将其限定为肩部物品。`ItemFeatherWings` 的固定最大耐久为 500，附魔台只允许耐久；经验修补、绑定诅咒和消失诅咒按原版书本/铁砧入口生效。羽毛是唯一材料修复物，`AnvilEnchantmentEventHandler` 以 `FeatherWingsRules` 的 5 点/根公式替换原版每材料 25% 修复，同时不拦截两件同类翅膀的原版合并修复。
- `FeatherWingsFlightEventHandler` 只在逻辑服务端的 END Player Tick 授予/撤销 `allowFlying` 并调用原版能力同步；只有非创造、非旁观的实际飞行会累计 40 tick 后对肩部栈调用 `damageItem(1, player)`。它按每 tick 位移向服务器 FoodStats 加入两倍原版步行 exhaustion，跳过超过 10 格的瞬移差值；肩部栈不在原版 Mending 的装备扫描中，因此 `PlayerPickupXpEvent` 仅在已损坏且带经验修补的羽毛翅膀上接管经验球修复并返还剩余经验。无新增 Capability、WorldSavedData、NBT key 或 Packet discriminator。
- `SlotShoulderEquipment` 使用原版绑定诅咒检查阻止非创造取下；`PersistenceEventHandler` 的玩家死亡 Clone 对消失诅咒肩部栈清空。Packet 9 的追踪者渲染快照现在含羽毛翅膀（背包仍为空快照）；`FeatherWingsRenderHandler` 是物理客户端的 `RenderPlayer` layer，使用 `ModelElytra` 与 `textures/entity/feather_wings.png`，从而与胸甲和原版肩部鞘翅模型隔离。
- `MarketCatalog`/`TradeCatalog` 以追加 `buy/treasure/feather_wings` / `merchant/feather_wings` 条目接入既有服务端商人闭环：基础价 1200000、珍宝 8% 波动、库存 1。`AgricultureRules` 的田园眷顾固定概率改为 I--IV `20/40/60/80`；收获和种植仍各只在服务端判定一次并奖励 1 点经验。

### 2026-09-06 维护：输入焦点、附魔边界与凭证箱持续加载

- `ModGuiInput` 将“打开背包”改键视为文本输入框失焦时的关闭键：行情书搜索、商人数量和交通节点取名框获得焦点后，默认 E（及改键）交给 `GuiTextField`，Esc 仍调用 `EntityPlayerSP#closeScreen` 以同步关闭服务端 Container。
- `HARVEST` 的稳定附魔 ID、等级和时运互斥规则不变；白名单追加五把原版斧头，`AgricultureEnchantmentEventHandler` 因而会在斧头通过书本/铁砧获得丰收后按既有作物流水线结算。`EnchantmentPastoral` 新增逐物品的附魔台排除表，斧头不会得到丰收附魔台候选。`AgricultureRules` 的田园眷顾概率固定为 I--IV `20%/40%/60%/80%`。
- `PastoralWorldData` 升至 schema `v8`，新增有界 `voucherChests` 段，只保存原版凭证箱每个物理半块的维度和 `BlockPos`。`TradeVoucherStorageService` 在关闭原版箱子和交易扫描时登记含已绑定凭证的箱子；每个登记半块持有一个带坐标 modData 的 Forge 1.12.2 NORMAL ticket，重启回调按原坐标重新 `forceChunk`。每秒只复核已登记、已强制加载的箱子；凭证移除、方块失效、上锁或未展开战利品箱会撤销记录和票据。库存计数、原子回滚、Packet 字段和 UUID 授权规则不变。

### 2026-09-06 维护：凭证配方、死亡金币与市场调价

- 空交易凭证新增有序 JSON 配方：纸置中、八个金粒环绕，产出 1 张空凭证；绑定、箱子授权和服务端原子出售流程不变。
- 大型动物的额外骨头基础掉落改为 `40/300` 掉 2、`100/300` 掉 1，小型动物改为 `50/300` 掉 1；各自成功概率及期望掉落均为原规则的 2/3，抢夺与屠宰的既有后续结算顺序不变。
- `DeathCoinLossEventHandler` 仅在逻辑服务端的真实玩家死亡事件末端扣除既有 Player Capability 金币：随机 10%--30%，但不低于随机 1000--2000，且不超过当前余额；随后以红色聊天组件发送“本次死亡失去xxx金币！”。扣款发生在 Clone 前，重生实体与 HUD 同步沿用既有 Capability/Packet 路径。
- 调价表本次将青金石、荧石粉、下界石英的购买基础价改为 800、200、1600。`PastoralWorldData` 从不重写加载到的当日行情快照；以后基础价调整时，下一日仍以旧保存价开始并按原波动逐日向新价格带回归，避免存档加载或调价当天跳价。
- 已删除旧交通定位物品的注册、服务端目标搜索、客户端模型、配方、语言和全部动画资源。保留一条 `MissingMappings<Item>` 忽略规则，使旧存档中该已删除物品安全消失而不阻断加载。

### 2026-09-06 新内容：肩部背包与扩容仓储

- 新增稳定 Item ID `backpack`、`advanced_backpack`、`super_backpack`，容量分别固定为 27、54、108 格且最大堆叠均为 1。三者复用既有 Player Capability 的单肩部栈，不增加玩家数据字段或版本；背包内容由物品自身 `LisBamBackpack/Items` 有界 NBT 拥有，槽索引最多 107，换装和玩家 Clone 时随 ItemStack 深复制。所有背包槽拒绝背包物品，损坏、重复或越界槽 NBT 不会进入可用库存。
- `ContainerBackpack`/`GuiBackpack` 只公开 27 个当前页真实 Slot，并把 54/108 格容量分页为 2/4 页；使用原版 `generic_54.png` 三行箱子与玩家背包纹理、原版 `GuiButton` 翻页及 `FontRenderer`。玩家在原版背包的肩部槽右键背包后，追加 Packet 10 仅请求打开；Handler 回到服务端主线程后验证当前窗口仍是 `inventoryContainer` 且肩部确有背包。页切换沿用原版 Container button/enchant 包，服务端再次验证打开的实际 Container、玩家与页边界。背包被替换或玩家失效时 `canInteractWith` 立即失败。
- 普通背包使用皮革/箱子/线有序 JSON 配方；高级背包使用皮革、两个普通背包和拴绳的自定义 `ShapedRecipes`，只有两只普通背包均为空才匹配，防止合成吞物。超级背包没有配方，以稳定 market/catalog key 加入珍宝池，基础价 200000、珍宝波动 8%、库存 1。三张 16×16 RGBA 物品贴图由 AI 像素源图色键取透明并固定采样；没有玩家穿戴模型，Elytra Coremod 只在肩部实际为鞘翅时返回渲染/飞行栈。

### 2026-09-06 新内容：交易凭证与箱子出售

- `item.ItemTradeVoucher` 以稳定 Item ID `lisbam_pastoral_economy:trade_voucher` 注册一个最大堆叠为 1 的物品。空白状态右键后只由逻辑服务端把使用者 UUID 与绑定时玩家名写入物品自身的 `lisbam_pastoral_economy` 子 NBT；UUID 是授权依据且只能首次写入，名字只负责显示为“交易凭证-玩家名”。客户端 `bound` 模型属性在同一 Item ID 上切换空白/已绑定的两套 16×16 贴图，不新增实体、Capability 或世界数据。空白凭证的有序配方固定为金粒环绕中央纸张。
- `merchant.TradeVoucherStorageService` 在服务端商人交易主线程中组合玩家主物品栏与授权箱子。候选只来自所有已加载服务器维度中当前已加载的原版 `TileEntityChest`，不会加载区块；相邻双箱按一个库存处理，并跳过上锁箱子及尚未生成内容的原版战利品箱。箱中任意槽存在与售卖玩家 UUID 相符的凭证即可授权，显示名不参与验证。
- 出售先消费玩家主物品栏，再按维度/坐标稳定顺序消费授权箱子；扣货、牛奶桶返还与金币入账共用一份多库存快照，任一步失败都恢复所有来源。商人快照沿用既有 `remainingItems` 整数字段携带“授权箱子库存数”，客户端与本地玩家实时主背包数相加显示“可出售”，Packet discriminator、编码顺序和服务端权威结算均不变。

### 2026-09-06 维护：肩部槽 UI、快捷穿戴与玩家模型同步

- 生存背包中的肩部槽固定在副手槽正上方（物品坐标 `77,44`）；创造模式“生存物品栏”页固定在该页副手槽正上方（`35,2`）。两种模式都从原版 `inventory.png` 裁取同一块原版盔甲槽 18×18 画面，因此会跟随资源包改动；`GuiScreenEvent.DrawScreenEvent.Pre` 仍会在创造页每次重建 `CreativeSlot` 后重新定位包装槽，并恢复原版物品与悬停遮罩。
- `ShoulderEquipmentEventHandler` 在双端拦截手持鞘翅的 `RightClickItem`：客户端只返回成功以保留挥手反馈和发包，逻辑服务端确认肩部为空后复制一件到既有 Capability；生存模式扣除手持鞘翅，创造模式与原版护甲右键一致保留手持物。肩部被占用或 Capability 不可用时拒绝，绝不退回胸甲槽。
- Coremod 追加补丁原版 `LayerElytra#doRenderLayer` 的胸甲读取，因此胸甲模型与肩部鞘翅模型可同时显示。Packet 9 `SyncShoulderEquipmentMessage` 向本人同步完整肩部栈；追踪者只接收渲染需要的鞘翅或空栈，绝不下发背包仓储 NBT。登录、重生、换维度、开始追踪、槽位变化以及飞行耐久/背包内容变化都会同步，客户端仅在主线程更新展示用 Capability。

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
- 注册：`registry.RegistrationHandler` 是唯一的静态 Forge Registry Event Subscriber；已注册 `item.ModItems.BACKPACK`、`item.ModItems.ADVANCED_BACKPACK`、`item.ModItems.SUPER_BACKPACK`、`item.ModItems.MARKET_BOOK`、`item.ModItems.TRADE_VOUCHER`、`item.ModItems.GOLDEN_BONE_MEAL`、`item.ModItems.CRAB_TRAP_ITEM`、`item.ModItems.TRANSPORT_STATION_ITEM`、`item.ModItems.CHUNK_LOADER_ITEM`、`block.ModBlocks.VILLAGE_STATION`、`block.ModBlocks.CRAB_TRAP`、`block.ModBlocks.TRANSPORT_STATION`、`block.ModBlocks.CHUNK_LOADER` 及十二个有效附魔 ID；高级背包的 NBT 安全有序配方也由同一 Recipe Registry Event 注册。已移除的 `shears_efficiency` 在旧存档加载时通过 `MissingMappings<Enchantment>` 重映射为原版 `Efficiency`；已删除的旧交通物品通过 `MissingMappings<Item>` 忽略。`entity.ModEntities` 使用 1.12.2 `EntityRegistry.registerModEntity` 和 `GameRegistry.registerTileEntity` 注册商人、村庄站点、蟹笼、交通节点与区块加载器 TileEntity。
- 网络：`network.ModNetwork` 持有唯一 `SimpleNetworkWrapper`，Channel 为 `lb_pastoral`；Packet 0～2 保持原有金币/行情协议，Packet 3 为有界 C2S 商人交易请求，Packet 4 为 S2C 商人快照（1.6 追加绿宝石行情/持仓/上限字段），Packet 5/6 为交通节点 C2S 操作/S2C 状态快照，Packet 7/8 为背包 Tooltip 的有界 C2S 商品 key 请求/S2C 当前世界日价格，Packet 9 为肩部装备 S2C 同步，Packet 10 为无载荷且严格拒绝多余字节的打开背包 C2S 请求，Packet 11 为有界 C2S 绿宝石买卖请求。Packet 9 向所有者同步完整肩部 ItemStack NBT，向追踪者只同步鞘翅渲染快照；Packet 10/11 Handler 都回到服务端主线程验证真实会话后才执行。Common Handler 不引用客户端类型，经 Proxy 调用客户端主线程执行器；全部 C2S 请求在服务端主线程验证并结算。
- GUI：`gui.GuiIds.MARKET_BOOK=0`、`MERCHANT_TRADE=1`、`CRAB_TRAP=2`、`TRANSPORT_STATION=3`、`BACKPACK=4`。`ModGuiHandler` 在服务端返回无槽行情书、绑定实体的商人、坐标绑定的 `ContainerCrabTrap` 或 `ContainerTransportStation`，或绑定当前肩部物品的 `ContainerBackpack`；客户端经 Proxy 返回相应 GUI，common side 不加载客户端类。`GuiMarketBook`、`GuiMerchantTrade`、`GuiTransportStation`、`GuiCrabTrap` 和 `GuiBackpack` 全部继承 `GuiContainer`：无权威数据的界面创建匹配的客户端生命周期 Container，背包则从已同步的本人肩部栈创建同页数容器，并在 `initGui` 首先调用 `super.initGui()`。因此 Forge `OpenGui` 写入服务端 `windowId` 时目标是专用客户端容器，永久的玩家 `inventoryContainer.windowId` 始终保持 0；商人、交通和背包的逻辑服务端仍只使用绑定真实实体、Tile 或肩部物品的权威构造器。交通客户端生命周期容器只需要服务端已验证的 BlockPos，不依赖 TileEntity 抵达客户端的同步时序。行情书、商人、交通、背包及交通确认层全部不暂停单人集成服务端；自定义界面及确认层会把原版 Esc 和“打开背包”键（默认 E，尊重改键）统一映射到 `EntityPlayerSP#closeScreen`，从而发送关闭窗口包并让服务端 Container 一起关闭。所有 UI 文字使用原版 `FontRenderer`，物品悬停使用原版 `GuiScreen#renderToolTip`。商人、交通的静态非按钮文字直调 `FontRenderer#drawString(..., 4210752)`，即工作台/熔炉标题同一无阴影路径；选中的交通节点是唯一的状态例外，名称、坐标和旅行费使用绿色 `0x55FF55`，交通改名和商人数量输入框文字均为白色。商人交易与行情书对“任意颜色羊毛”构造仅显示用途的命名 ItemStack，显示名为“羊毛”，不改变原有 Item/meta 或行情 key。商人/交通每帧以一次完整裁取绘制原版 `demo_background.png`，不把该纹理拆成九宫格；按钮/输入框/滑条保持原版控件。商人物品底框直接裁取 `generic_54.png`，有限库存直接显示剩余实际物品数，不显示组数或每组数量；背包每页的三行 27 格及玩家背包也直接使用该原版纹理，翻页采用原版 `GuiButton`。`ClientMerchantTradeViewState` 仅在物理客户端保存本次游戏会话最后选择的出售/购买页，绝不参与交易请求或存档。`GuiCrabTrap` 明确按原版 `GuiChest` 的 `drawScreen` 路径调用 `renderHoveredToolTip`，使 20 个蟹笼槽和玩家背包槽都走原版 Tooltip；其槽位背景仍复用 `generic_54` 的标题/玩家背包片段、原始背景像素和逐格裁取。两个显式受限 Container Slot 与 TileEntity/Hopper 复用校验，0 仅接受钓竿、1 仅接受合法生肉、2～19 为通用仓储/捕捞产出槽。`GuiTransportStation` 自动请求最近未接入村庄，仅显示一个接入按钮；接入最近村庄与移出节点都使用原版 `GuiYesNo` 的泥土背景、白色文字和 `GuiOptionButton`。最近村庄确认的“是”按钮即使客户端余额不足也保持可点，并照常把请求交给既有服务端重新搜索、重新定价和余额校验；移出层只在确认后发送既有 `REMOVE` 请求。交通面板中“我的节点”行位于第二行动按钮下方；节点列表区域可用鼠标滚轮逐行滚动，坐标没有维度后缀。节点移出即删除个人记录，快照会清理历史无效/未激活记录，不渲染“已移出”或“节点无效”节点。`SyncTransportStateMessage` 的每个 `TransportNodeView` 附带服务端计算的目的地旅行费或不可用标记，客户端据此显示费用并禁用不可旅行/余额不足的操作。
- 生成规则：`event.OverworldMonsterSpawnEventHandler` 是 common 静态订阅器。它只在逻辑服务端的维度 0 对 `LivingSpawnEvent.CheckSpawn` 的 `isSpawner=false` 且 `EnumCreatureType.MONSTER` 设为 `DENY`；因此拒绝自然/区块生成敌对生物，保留刷怪笼、刷怪蛋、命令和直接实体生成。
- 资源：`mcmod.info`、`pack.mcmeta`（format 3）、中英文 `.lang`、正式 Logo、行情书资源和蟹笼的 blockstate、Block/Item model、合成配方均已存在；`textures/blocks/crab_trap.png` 与 `transport_station.png` 都是 32×32、最近邻简约原版风 PNG。`village_station` 与玩家可放置的交通站共用后者，不再引用原版橡木木板；玩家交通方块与 ItemBlock 共用显式 `tile...transport_station` 显示键，中文为“交通方块”，不会再附加 `.name`。行情书模型使用自有 `textures/items/market_book.png` 的绿皮书图标。交易凭证以一套空白卷纸和一套带标记/封印卷纸的 16×16 RGBA 贴图表现同一物品的空白/绑定状态；其有序配方为金粒环绕中央纸张。`golden_bone_meal` 有独立 16×16 原版骨粉轮廓的金色/橙色点缀 PNG、generated model、双语名及两份无序配方。蟹笼合成固定为铁锭/铁栅栏交错外圈、中央陷阱箱。
- 玩家数据：`data.player.PlayerDataCapability`（`lisbam_pastoral_economy:player_data`）是唯一玩家 Capability，`dataVersion=3`。数据保存玩家金币 `long`、一个接受鞘翅或三种背包的肩部栈、启动交通方块一次性发放状态、首个自建站免费资格历史、有效玩家节点、别名和自建站序号；移出节点会从映射直接删除，读取旧 `active=false` 条目时丢弃。背包槽内容嵌套在该肩部 ItemStack 自身 NBT 中，不增加 Capability key；Capability NBT 处理默认/旧字段并把非法负金币修正为 0，Clone 时深复制完整数据。
- 世界数据：`data.world.PastoralWorldData`（`lisbam_pastoral_economy_world_data`）是唯一 WorldSavedData 根，`dataVersion=9`。访问器始终解析服务器主世界 MapStorage，保证跨维度只有一份共享数据。`market` 段保存持久 marketSeed、首次市场日、当前/昨日普通商品快照和全部商人出售/购买历史商品的最近 30 天，并追加独立的绿宝石初始化标记、当前/昨日价和最后更新日；不再持久化无限 `processedDays`、无限历史或已移除的 `farmHarassment` 记录。旧存档的废弃字段读取时忽略、下一次保存时移除；v8 缺少绿宝石字段时按首次 1,000 金币迁移。merchant 段保存村庄、站点、商人、Offer、库存、轮换状态和附魔书已解析等级；transport 段保存 SELF_BUILT/VILLAGE 物理节点 UUID、维度、坐标和村庄身份引用。
- 市场：`market.MarketCatalog` 是唯一不可变普通商品目录，保存稳定经济 key、1.12.2 Item/meta、金币×10 后的基础价、8 类单日波动率、上下限倍率和历史标志。默认 `MarketPriceGenerator` 从前一天持久化价格按类别 25%～100% 的波动率步长上涨或下跌；偏离基础价越远，回归概率按 50%/55%/65%/75%/85% 分档提高。当前价格已在价格带内时结果 clamp 在类别最低/最高价，触及边界的下一天强制向内反弹；调价后旧存档若位于新价格带外，则不重写快照，而是按同一日步长逐步回归并在重入后恢复钳制。独立 `EmeraldMarketPriceGenerator` 仅从 WorldSavedData 的当前绿宝石价逐日生成有界随机漫步，不接入普通目录、行情书历史或稳定市场设置。`MarketService` 是唯一服务器市场 API：行情书打开请求或实际经济读取才惰性推进到当前日，主世界不再每 tick 推进市场；为保证链式价格，跨多日会逐日推进并只持久化最近 30 天的普通收购历史。`MarketPriceSnapshot` 为商人单次同步批量提供当前/昨日价格，`MarketHistorySnapshot` 最多 30 点且不再提供更早页。
- 经济：`data.player.CoinService` 是唯一金币写入口。逻辑服务端 `EntityPlayerMP` 才能调用；拒绝负数、拒绝 long 溢出、没有公共 setBalance。成功改动时发送自己的 S2C 同步。
- 客户端：`client.ClientPlayerState`、`ClientMarketState` 与 `ClientMarketTooltipState` 都是非持久、非权威缓存，连接/断开时清空。市场缓存有明确书本会话：打开时清空并开始接收、关闭时清空并拒绝迟到包，连接内请求号保持递增；Packet 1 每次只下发一个商品的最新 30 日窗口，`GuiMarketBook` 分为出售/购买两页，购买商品可按本地化名称或 key 过滤并在受限图标格中滚动。选中商品优先请求，其余商品由会话内客户端缓存按 4 tick 节流渐进预取；选中请求 40 client tick 未返回时重新请求，关闭后不保留市场工作。`ClientMarketTooltipEventHandler` 在原版 `ItemTooltipEvent` 路径为当前客户端玩家的 29 类出售物品添加今日收购价行，以当前 `worldTime / 24000` 严格匹配 Packet 8 市场日，过日绝不显示缓存的昨日价；服务端每个合法 key 请求都在主线程冻结并回包。`ClientNightVisionRenderHandler` 仅在物理客户端以可恢复的临时 gamma 实现 10 秒初始/5 秒续期的夜视视觉，不创建药水效果或修改世界光照。`ContainerMarketBook` 只作为实际打开书本的会话验证标记。`GuiMerchantTrade` 的数量滑条、输入框与页签偏好均为本地展示状态，出售持有量按客户端 tick 缓存，仍只发送既有有界数量请求；每次成功交易后的库存窗口、金币同步和商人快照均由服务端在同一逻辑 tick 推送。`ClientModelRegistry` 是物理客户端模型注册。`ClientHudEventHandler` 仍在 `RenderGameOverlayEvent.Text` 中右上角以 12 个 scaled pixels 页边距渲染金币。
- 附魔定义：`enchantment.EnchantmentPastoral` 统一实现 1.12.2 物品筛选、书本可得性、稀有度、宝藏/诅咒标记、附魔台可得性和互斥规则；`ModEnchantments` 持有十二个稳定实例和精确原版装备白名单。Coremod 只把剪刀原版 `Efficiency` 放回附魔台候选，绝不新增第二个效率 ID 或书本。Harvest 与 Fortune 互斥；Slaughter 与 Sharpness、Smite、Bane、Looting 互斥；Bluntness Curse 与 Sweeping Edge 互斥且为唯一不进入附魔台的宝藏诅咒。Fleetfoot 只适用于皮革/锁链/铁/金/钻石护腿；Attack Speed、Reforged 适用于剑、斧、镐、锹、锄；Range 在此基础上也适用于剪刀，Harvest 适用于锄和剪刀。
- 农业：`agriculture.AgricultureRules` 是成熟作物身份、种植物和全部随机公式的单一来源。`event.AgricultureEnchantmentEventHandler` 在服务端 BreakEvent 只捕获具有 Harvest、Fine Cultivation 或 Pastoral Favor 效果的成熟收获，在 HarvestDropsEvent 修改最终掉落/消耗种植物并登记补种；age 0 补种延后到服务器 END tick，使其发生在原版方块移除和掉落结束之后。`HoeCropDurabilityEventHandler` 独立在成功的 HarvestDropsEvent 登记零硬度作物，再于 END tick 对仍为同一快捷栏栈的锄头补扣 1 点、同步库存并广播最终方块状态；硬度非零作物仍只由 1.12.2 `ItemHoe` 原版扣耐久。`ShearingHarvestEnchantmentEventHandler` 在服务端记录带 Harvest 剪刀的羊/哞菇原版剪毛请求，只有原版剪毛实际成功后才同 tick 追加同一随机公式的羊毛或红色蘑菇。PlaceEvent 仅对真实玩家的列出作物种植执行 Pastoral Favor。`ItemGoldenBoneMeal` 只在逻辑服务端写世界：中心的原版可耕作物重复原版 `ItemDye.applyBonemeal` 至成熟（最多 8 次），同层 5×5 其余作物各执行一次原版骨粉；草方块/花以其草层为中心对 5×5 草方块各执行一次原版骨粉，其他目标也保留一次普通骨粉路径。它在 common `preInit` 注册为 Forge `dyeWhite` OreDictionary 条目，使用临时原版白色 `ItemDye` 保留羊的白色染料行为，并把自己的增强骨粉路径注册为发射器行为；失败时沿用原版默认发射器抛出物品。`GoldenBoneMealRules` 是其作物/野生目标分类和 5×5 边界的纯规则来源。
- 工具与战斗：`event.TreeFellingEventHandler` 对带 Felling 的真实玩家斧头，在有限的同树种原木连通块和匹配树冠确认后，以 `EntityPlayerMP#interactionManager.tryHarvestBlock` 逐块处理原木和叶子，因此保护事件、原版掉落与 Unbreaking 仍逐块生效。`FellingDurabilityRules` 在链启动和每个二级原木前确保保留触发原木的一次原版耐久与最终 1 点；连锁树叶造成的原版工具损耗会恢复，故树叶不计伐木耐久。`AnimalBoneDropEventHandler` 在服务端 `LivingDropsEvent` 的 `HIGHEST` 优先级按严格动物白名单加入骨头，`AnimalBoneDropRules` 先进行基础掉落、再使用事件提供的 Looting 等级计算附加数量。`SlaughterEnchantmentEventHandler` 随后以既有倍率处理完整掉落列表，因而已成功的骨头和原掉落一致受屠宰影响，且不改 XP。
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
