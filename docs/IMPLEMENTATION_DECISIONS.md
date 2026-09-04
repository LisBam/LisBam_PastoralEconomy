# 实施决策

## DEC-001 工程身份冻结

决定：工程名与 artifact 使用 `LisBam_PastoralEconomy`；Mod ID 固定为 `lisbam_pastoral_economy`；Java 根包固定为 `lisbam.pastoraleconomy`。

原因：第 01 批仍未发布任何注册内容，是冻结稳定 namespace 的安全时点。之后这些标识会影响注册名、资源路径、存档和网络兼容性。

## DEC-002 公共接入点

决定：注册使用一个静态 `RegistrationHandler`；网络使用一个名为 `lb_pastoral` 的 `SimpleNetworkWrapper`；GUI 使用一套 `GuiIds` 和 `ModGuiHandler`。

原因：避免市场、商人、交通和蟹笼在后续批次各自创建不兼容的 Registry、Channel 或 GUI ID 系统。Packet ID 仅在真实 Packet 加入时显式分配并保持稳定。

## DEC-003 后续持久数据的所有权

决定：玩家长期数据使用 Player Capability；世界级数据使用带 schema version 的 WorldSavedData；方块实例使用 TileEntity + NBT；商人实例使用实体 NBT。

原因：这些数据的生命周期不同。当前只记录边界，未创建任何 Capability、WorldSavedData、TileEntity 或实体字段。

## DEC-004 基础日志 API

决定：第 01 批基础日志使用 Forge 1.12.2 的 `FMLLog.info`。

原因：它是当前 Forge 映射开发依赖直接提供的 legacy API，并避免在主入口中额外保存日志实现类型。日志仅记录模组和 common 基础设施生命周期，不承载玩法逻辑。

## DEC-005 第 02 批玩家数据与金币边界

决定：所有玩家绑定长期状态集中在 `lisbam_pastoral_economy:player_data` Player Capability，schema `dataVersion=1`。金币使用 `long`，业务只能通过逻辑服务端 `CoinService` 的 `getBalance`、`canAfford`、`addCoins`、`trySpend` 访问；不存在公共 setBalance。

原因：货币、交通解锁、线路和别名都有同一玩家生命周期。集中 API 能保证拒绝负数、扣款原子化、long 溢出不回绕，并让后续市场、商人与交通不各自改 NBT。Clone 深复制完整 Capability，所以死亡及 End 返回不丢失长期数据。

影响：第 02 批仅启用 coins；交通字段只做带稳定字符串 ID 的 NBT 预留，未提供解锁、线路或别名业务接口。

## DEC-006 第 02 批世界数据根与一次性规则初始化

决定：全存档只使用 `PastoralWorldData`（名称 `lisbam_pastoral_economy_world_data`，schema `dataVersion=1`）作为世界共享根。统一访问器从任意服务端维度解析至主世界 `MapStorage`。首次加载主世界时仅在持久化标记未设置的情况下写入 `keepInventory=true` 并立即 `markDirty`。

原因：市场、商人和交通最终是跨维度的世界状态，不能有每维度独立根。首次初始化是世界生命周期而不是玩家登录行为；保存标记后永远不再接触 GameRule，服主手动关闭能够保持。

影响：market、merchant、transport 只保留空的稳定 NBT Section；本批没有行情、商人、节点或库存。

## DEC-007 第 02 批金币同步与客户端显示

决定：稳定 Packet 0 为 S2C `SyncCoinsMessage(long balance)`。服务端仅在登录、重生、维度切换或成功余额变化时向对应 `EntityPlayerMP` 同步。Packet Common Handler 只经 Proxy 转发；ClientProxy 在客户端主线程更新非持久缓存。HUD 只读取该缓存。

原因：余额权威数据无需广播或每 Tick 传输。Proxy 桥接避免 Dedicated Server 加载 `net.minecraft.client.*`，主线程调度避免 Netty 线程修改客户端状态。连接和断开时清空缓存，防止跨服务器残留。

影响：客户端无法通过显示缓存、HUD 或 Packet 反向设置服务器余额；本批没有 C2S 经济消息。

## DEC-008 第 03 批怪物选择与反击边界

决定：第 03 批使用精确实体运行时类选择 Zombie、Zombie Villager、Husk、Skeleton、Stray、Creeper、Enderman 和普通 Spider；不以 `instanceof` 扩大到 Cave Spider、Pig Zombie 或模组子类。`EntityJoinWorldEvent` 只移除这些实体玩家目标选择器，`LivingDamageEvent` 以 `DamageSource#getTrueSource` 记录直接玩家攻击，`LivingUpdateEvent` 只允许同一攻击者作为临时反击目标。

原因：Forge 1.12.2 的 `LivingSetAttackTargetEvent` 不可取消，不能安全作为阻止 AI 目标的唯一边界。直接改动经确认的目标任务并在 AI 更新前过滤目标，既保留原版对非玩家、生物互斗和攻击者反击的逻辑，又不会允许群体仇恨、投射物/间接攻击或其他玩家绕过规则。

影响：反击授权是运行时 `WeakHashMap`，不是游戏长期数据；重新加载实体后不会残留仇恨。Enderman 的玩家注视搜索任务一并移除，直接攻击仍由原版 hurt-by-target 行为进入同一过滤规则。

## DEC-009 第 03 批 Creeper 爆炸与农田配额

决定：只对精确 Creeper 的 `ExplosionEvent.Detonate` 调用 `getAffectedBlocks().clear()`，保留实体列表。农田骚扰成功记录放入既有 `PastoralWorldData` v2，使用主世界 Tick 作为跨维度统一时间；单个怪物的下次候选和成功冷却存入实体 ForgeData。

原因：Detonate 的方块列表与实体列表在 Forge 1.12.2 中独立，清空前者可精确保留实体伤害、击退和其他爆炸源。全局配额是跨实体、需要重启后仍有效的世界规则，不能使用 static Map；每只怪物的冷却则随实体实例生命周期保存，适合实体 NBT。

影响：WorldSavedData 从 v1 迁移到 v2 时保留首次初始化标记，创建空历史列表，不会重复设置 `keepInventory`。只有实际变更一格方块后才记配额和启动成功冷却；候选失败不消耗配额或冷却。

## DEC-010 第 04 批世界日市场冻结

决定：市场只使用主世界 `World#getWorldTime()/24,000` 作为世界共享 market day。`PastoralWorldData` v3 持久化 marketSeed、首次市场日、当前/昨日价格快照、processed days 与 14 种作物的无上限日历史；逻辑服务端主世界 END Tick 是唯一主动推进入口。

原因：世界时间会在睡眠和 `/time add` 中直接跨日，不能使用 `getTotalWorldTime` 或现实日期。当前/昨日快照让趋势不需要重建昨日行情；processed days 与逐日补齐确保多日跳跃不会丢失历史，回拨不会重复写入或删除真实已发生的数据。

影响：v2 读入时只初始化空市场段，保留 keepInventory、Farm Harassment 与预留 section。市场第一次在世界的当前日启用，绝不伪造安装前或“昨日”历史。

## DEC-011 第 04 批目录与服务边界

决定：`MarketCatalog` 是唯一冻结目录。每条价格定义使用稳定 `lisbam_pastoral_economy:<path>` key，并保存 Item/meta、基础价、类别/波动率与历史标志；收购单位价与后续商人购买 bundle 价使用不同经济 key。`MarketPriceGenerator` 以 marketSeed、worldDay、key、两条不同 salt 生成 u1/u2，且只在那里执行最终 `Math.round`。`MarketService` 只读、只接受服务端 World，并不处理余额或交易。

原因：经济 key 必须独立于本地化和商人槽位，也不能把同一物品的单位收购价误当作整包购买价。独立输入不依赖目录遍历顺序，所以新增商品不会改变旧商品的当日行情。服务边界保证未来行情书只显示服务器数据、商人交易只能读冻结价格并仍由 CoinService 负责资金。

影响：14 个明确作物 key 才写历史；苹果和其他非作物不写曲线。未来行情书通过 MarketService 分页同步，未来 Merchant/Transaction 通过 key 查询价格；本批不新增 Packet、GUI 或交易逻辑。

## DEC-012 第 05 批行情书快照与 GUI 边界

决定：行情书使用稳定 Item ID `lisbam_pastoral_economy:market_book`、GUI ID `0`、既有 `lb_pastoral` 通道的 Packet 1/2。Forge 1.12.2 打开协议由空的 common `ContainerMarketBook` 承载，ClientProxy 才构造 `GuiMarketBook`。客户端请求只发送稳定商品 key、`-1` 最新/非负排他日游标和请求号；服务端主线程从 `MarketService` 的显示专用只读路径返回最多 30 个点的 `MarketHistorySnapshot`。

原因：1.12.2 的 `EntityPlayer#openGui` 需要服务端 Container 才会向远端客户端发送 OpenGui，不应在 Common GUI Handler 直接引用 `GuiScreen`。市场历史无限增长，不能传输整个 WorldSavedData 或客户端提供价格/数量。以商品 key + 游标 + 请求号识别窗口，能在快速切换商品和分页时阻止迟到 S2C 回包覆盖当前界面。

影响：市场仍只由主世界 END Tick 的 `MarketService.tick` 推进；行情书所有显示查询均不会初始化或推进市场，因此不会生成新价格、历史、交易或金币变化。客户端缓存只在内存中存在且连接生命周期清空。三维度请求通过 `MarketService` → `PastoralWorldData.get(World)` 解析到主世界根，因而读取同一行情。模型有意引用原版 `minecraft:items/book_normal`，避免无必要地复制 Minecraft 位图资源。

## DEC-013 第 06、07 批附魔规则与侧边界

决定：八个附魔均由既有 `RegistrationHandler` 的 `RegistryEvent.Register<Enchantment>` 注册，使用稳定 namespace ID、RARE、非宝藏、非诅咒和原版 Enchantment/EnchantmentData 体系；不添加自定义 Librarian、铁砧、网络包或持久化数据。未冻结的附魔台可得性统一采用 `15 + 9 * (level - 1)` 到该值 `+15` 的稀有工具附魔区间。农业效果只由服务端 `AgricultureEnchantmentEventHandler` 的单一流水线处理；移动速度以固定 UUID、operation 2、`setSaved(false)` 的动态属性修饰符实现；Night Vision 仅是 `Side.CLIENT` 的逐渲染帧 gamma 临时显示。

原因：Forge 1.12.2 的原版 Enchantment Registry 会自动供附魔台、附魔书、旧版图书管理员随机候选和原版铁砧读取。额外 hook 会造成重复候选或绕开原版费用/合并规则。HarvestDropsEvent 在原方块移除后、原版掉落实体生成前给出最终 `List<ItemStack>`，使 Harvest 额外产物、Fine Cultivation 种子消耗和补种能在一个可验证顺序中完成。Felling 通过 `PlayerInteractionManager#tryHarvestBlock` 而非手工 spawn 掉落，保留每格保护、Unbreaking、耐久和 Creative 语义。服务端属性会同步为最终权威移动值，而客户端 gamma 不应影响世界光照、怪物生成或其他玩家。

影响：本批没有 Player Capability、WorldSavedData、NBT schema 或网络协议改动；动态速度 modifier 不写入玩家存档。Felling 的有限原木半径、最大原木数、树冠阈值和叶片归属筛选是防止木屋/原木墙/相邻树/无限扫描的技术安全边界，不改变本批冻结的附魔等级、概率或倍率。

## DEC-014 第 08～10 批村庄/商人世界数据所有权

决定：继续使用唯一 `PastoralWorldData`，schema 从 v3 升至 v4，并在 `merchant` section 保存 `VillageRecord`、`StationRecord`、`MerchantRecord`、每日 Offer、共享有限库存和每个 Merchant 的循环轮换游标。`EntityMerchant` NBT 只保存 merchantId、villageId、stationId 和站点坐标。

原因：商人死亡、Chunk 重载和服务器重启都不能刷新当天 Offer 或库存；稳定逻辑身份必须独立于当前实体 UUID。WorldSavedData 能跨维度共享且支持旧 v3 安全迁移，实体 NBT 只用于重新绑定当前活体。

影响：v3 旧存档自动获得空 merchant section，不改变既有 market、farmHarassment 和 keepInventory 数据。后续稀有/珍宝商品只需追加 TradeCatalog 条目，不需要改变存档结构。

## DEC-015 第 08～10 批交易与客户端边界

决定：商人交易使用自定义 slotless `ContainerMerchantTrade`、固定 Packet 3/4 和 `MerchantTradeService`；请求只包含 Merchant/session/slot/数量/期望世界日，价格、库存、背包和金币均由服务端主线程重新验证。`TradeCatalog` 集中保存所有 Item/meta、bundle、波动和库存策略。

原因：原版 Emerald `MerchantRecipe` 无法表达每日 MarketService 价格、无限出售、共享库存和牛奶桶返桶原子性。Container 的单调 requestId 与 committing guard 防止同一会话重复包；购买先做完整容量模拟，出售先移除商品再经 CoinService 加钱，失败恢复原背包和库存。

影响：Packet discriminator 0～2 保持不变；客户端 `ClientMerchantTradeState` 仅显示快照，连接断开即清空。普通/罕见购买池已启用，稀有/珍宝保留禁用槽位。

## DEC-016 第 08～10 批旧村庄与站点规则

决定：仅在 Overworld 低频读取 1.12.2 `VillageCollection`，最低 2 名村民；使用 128 格参考、64 格连接和 160 格去重距离维护稳定 villageId。每个有效记录最多一个 role=VILLAGE 的 StationRecord，站点只在已加载区块 12 格内寻找安全方块；站点保护只适用于村庄站点角色。

原因：避免使用 1.14+ 床、钟、工作站和袭击系统，也避免每 Tick 全世界扫描或为验证站点强制加载远方区块。站点异常消失时保留 stationId，村庄再次活跃后可恢复。

## DEC-017 第 11 批稀有/珍宝与附魔书

决定：扩展现有 `TradeCatalog` 与 `DailyOffer`，将稀有池固定为每日 2 栏、珍宝候选（普通珍宝物品 + 12 种特殊附魔类型）固定为每日 1 栏。附魔候选先按类型进入轮换，再由服务端按权重解析等级；解析后的等级写入 `DailyOffer.enchantmentLevel`，并以对应的 `MarketCatalog` 价格 key 结算。

原因：保持候选类型权重与跨重启稳定性，避免把多等级附魔拆成多个候选或由客户端重新随机。`ItemEnchantedBook.getEnchantedItemStack(EnchantmentData)` 生成真实 1.12.2 附魔书，原版与模组附魔均引用已注册 Singleton。

迁移：`PastoralWorldData` schema 从 v4 升至 v5；读取旧 v4 当日 10-slot 数据时，`MerchantOfferService` 只移除末尾三个禁用占位并补写 Rare 2 + Treasure 1，保留前七栏及其库存，成功后持久化，后续加载不会重复 Roll。

## DEC-018 第 12～13 批蟹笼实例状态与原版捕鱼边界

决定：每个 `crab_trap` 使用独立 `TileCrabTrap` + TileEntity NBT，而不扩展玩家 Capability 或 `PastoralWorldData`。固定槽位为钓竿 0、饵料 1、收获 2～19。每一轮开始时快照 Lure、Luck 与是否有合法饵料；remaining ticks 与 pending loot 一并保存。收获生成只调用 Minecraft 1.12.2 的 `LootTableList.GAMEPLAY_FISHING`，用快照 Luck 构造 `LootContext`，不复制或近似实现另一套掉落池。原版 Hopper 兼容使用 `ISidedInventory`：`DOWN` 仅公开收获栏，其他面仅公开饵料槽。

原因：蟹笼的库存、等待、阻塞战利品和饵料消费属于单个方块实例，必须随 Chunk 保存/卸载并在重启后保持同一轮结果。原版 fishing loot table 已冻结 fish/junk/treasure 的 quality 权重及全部弓、钓竿、附魔书的耐久/附魔函数；直接调用可精确保留 Java 1.12.2 行为。该 Forge 版本的原版 Hopper 会对无 item-handler capability 的 `ISidedInventory` 走旧版面向访问路径，因此能可靠区别上/侧入料和下方出料，而不会暴露钓竿或生肉。

影响：没有修改任何已有 WorldSavedData schema、Player Capability、Packet discriminator 或正式注册 ID；新增稳定 Block/Item/TileEntity ID 均为 `lisbam_pastoral_economy:crab_trap`。旧世界没有蟹笼 TileEntity，不需要迁移；损坏或缺失字段以安全空库存、未开始轮次和无 pending loot 读取。

## DEC-019 第 14 批交通节点身份、所有权与费用边界

决定：`transport_station` 的物理实例只由 `TileTransportStation` 保存 UUID 和启动礼物所有者；全局 UUID/Overworld 坐标/类型由 `PastoralWorldData` v6 的 `TransportWorldState` 保存；玩家自己的别名、激活状态、一次性礼物与首建历史由 `PlayerData` v2 保存。`TransportService` 是唯一的服务端业务入口，所有 C2S 请求只表达操作、目标 UUID 和别名，服务器重算最近节点、费用与资格，并仅通过 `CoinService` 修改金币。

原因：同一节点同时有物理方块身份、全局空间索引和玩家私有状态，不能把它们放进 ItemStack、GUI 或客户端缓存。Tile NBT 可被复制，故加载时必须检测冲突并分配新 UUID；方块掉落也不能携带 UUID，否则会把旧节点身份带到新位置。全局节点必须跨玩家、跨维度读取，个人别名则必须支持死亡 Clone 和重启。首建免费资格会影响经济结果，必须持久化并在服务器端一次性消耗。

影响：旧 PlayerData v1 和 WorldSavedData v5 读取时创建安全的空交通数据，不推测旧节点。交通费用仅以 X/Z 平面距离的整数下取整开方计算，价格为 `round10(400 + 1.2D)`；第一个有效自建启动节点免费，首次普通节点会永久耗尽免费机会。第 14 批只登记/激活/改名/移除和显示节点；不加入传送、路线、村庄发现或后续线路解锁。

## DEC-020 第 15 批村庄身份、旅行事务与安全落点

决定：沿用 `MerchantWorldState` 中既有的 `VillageRecord`/`StationRecord` 作为村庄与商人系统的持久身份，并把同一 Station UUID 镜像进 `TransportWorldState`，类型统一为 `VILLAGE`；自建节点使用正式 `SELF_BUILT`（读取旧 `PLAYER` 名称）。最近村庄查询使用 1.12.2 `WorldServer.findNearestStructure("Village", …)` 的有限确定性探测与已有 VillageRecord 去重，预览不生成方块，确认接入时才按需创建唯一站点。

旅行由服务端 `TransportService` 以“验证 GUI/源站 → 验证目标 Active → 计算 X/Z 距离与旅行费 → 加载目标单区块 → 半径 5、Y -2..+4 安全搜索 → 检查余额 → 扣款 → `EntityPlayerMP#setPositionAndUpdate` → 异常/位置失败退款”的顺序执行。安全搜索使用实际 1.12.2 BlockState/Material/碰撞盒，拒绝熔岩、火、岩浆块、仙人掌、液体和不可站立地板。

原因：村庄站必须对所有玩家共享同一世界实体，同时每个玩家的 Active/Removed/Alias 仍是 Capability 私有状态；把预览候选永久复制到玩家存档会破坏多人一致性。扣款后置于安全点确认之后可保证无安全落点不扣钱；退款由服务器调用既有 CoinService，客户端不能声明传送结果。

影响：本批没有新建网络通道或现代结构/传送 API；Packet 5/6 继续承载有界操作与快照，客户端只发送 UUID 请求。村庄站损坏时保留世界身份，VillageService 可在加载区块内恢复；未实现下界/末地网络旅行和后续交通玩法。

## DEC-021 第 15 批远程村庄候选重新验证

决定：`CONNECT_VILLAGE` 请求只接受服务器重新执行有限搜索后仍为“最近未接入村庄”的 UUID；不再从持久 VillageRecord 回退接受任意过期候选。

原因：Packet 只携带 UUID，客户端快照可能在另一玩家接入、当前玩家移除节点或世界结构索引变化后过期。若按 UUID 直接回退，会绕过“最近未接入”规则并允许激活未展示的村庄。

影响：预览快照仍是非权威提示；连接失败不会创建站点或扣除费用。直接到达村庄的本地 `CONNECT` 流程仍由当前站点连接逻辑独立验证。

## DEC-022 村庄节点可用性按方块类型校验

决定：`TransportService.isRecordUsableForNetwork` 对 `VILLAGE` 节点验证 `ModBlocks.VILLAGE_STATION` 与 `TileVillageStation`，对 `SELF_BUILT` 节点验证 `ModBlocks.TRANSPORT_STATION` 与 `TileTransportStation`。

原因：两类节点共享交通注册表但使用不同物理方块。统一先检查自建方块会把已加载的村庄站误判为失效，进而阻止村庄作为最近节点、旅行目的地或接入费用参考。

影响：只修正加载区块内的物理一致性检查，不改变 Station UUID、玩家状态或存档格式；未加载区块仍按全局注册记录参与有界网络计算。

## DEC-023 第 15 批后界面一致性修复

决定：商人数量控件使用 Forge 1.12.2 `GuiSlider` 与 `GuiTextField`，两者只维护当前客户端数量；购买/出售页的最后选择使用物理客户端内存中的 `ClientMerchantTradeViewState`，下次打开任意商人时复用。所有最终交易仍经既有 Packet 3 与 `MerchantTradeService` 由服务端重新校验。商人物品与行情书图标统一调用原版 `GuiScreen#renderToolTip`。蟹笼不再拼接不匹配高度的箱子贴图，而是严格采用原版 `GuiChest` 的四行 `generic_54` 顶部/玩家背包绘制公式，并使 Container 玩家槽位坐标与其一致。JavaCompile 显式使用 UTF-8 源文件编码，以防 Windows 主机默认 GBK 损坏既有中文和趋势符号；source/target 仍固定 Java 8。

原因：原商人五行交易卡会与底部页签重叠，数量只能逐次加减；蟹笼的 20 个实际槽位与六行贴图片段、玩家槽位位置不一致，导致格子错位和文字被背景遮挡。客户端页签和数量不影响任何权威结果，适合作为短生命周期显示状态；库存、余额、价格和数量上限仍必须以服务端计算为准。

影响：没有新增注册 ID、NBT、Capability、WorldSavedData 或 Packet discriminator，也没有改变任何经济数值或交易规则。蟹笼既有 Slot 0--19 编号不变，只将玩家背包显示/点击坐标调整到与原版贴图相同的位置。

## DEC-024 后续界面修复：真实槽位绘制与交通费用展示

决定：替换 DEC-023 中“完整四行顶部 `generic_54` 贴图”的蟹笼绘制细节：仍复用原版标题和玩家背包纹理，但顶部只绘制与 `ContainerCrabTrap` 一一对应的 2 个输入槽和 18 个收获槽。商人与交通 GUI 的自定义深绿色面板替换为原版容器使用的中性灰色、凸起/内凹层次；出售卡片固定按 3 行 × 2 列排列。购买页客户端将数量上限同时受库存和同步余额限制，无法买入一整包时禁用所有该商品控件；最终交易仍只由服务端验证并结算。

`TransportNodeView` 新增只读 `travelFee`（`-1` 表示当前不是可用目的地），由 `TransportService#createSnapshot` 仅在当前源站有效/Active、目的节点 Active、主世界且物理记录可用时计算。Packet 6 同步该显示字段；交通 GUI 显示目的地费用，并在费用不可用或客户端余额不足时暗置前往按钮。服务端 `TRAVEL` 路径继续独立重算费用、验证余额与落点，绝不信任该字段。

原因：完整箱子贴图会把蟹笼顶部空白位置伪装为可点格子，并覆盖“正在捕捞”等状态文本；绿色自定义底色也偏离原版容器的视觉语言。交通费用虽已在服务端结算，却未写进客户端快照，玩家无法在点击前得知成本。客户端暗置是即时反馈，不能替代服务器权威校验。

影响：不变更 Slot 编号、物品/方块注册 ID、NBT、Capability、WorldSavedData 或经济公式。Packet 6 的同版本客户端/服务端编解码同时更新，费用字段只用于显示；旧版客户端不能与新版服务端混用，这是 Forge 模组协议正常的版本一致性要求。

## DEC-025 UI 原版纹理与字体回归

决定：替换 DEC-024 中手绘的灰色槽框和凸起面板。所有文字统一保留 Minecraft 1.12.2 原版 `FontRenderer`；`GuiCrabTrap` 与商人的可见物品格直接裁取 `minecraft:textures/gui/container/generic_54.png` 的原版 18×18 槽位像素。商人与交通面板直接使用 `minecraft:textures/gui/demo_background.png`，按钮、文本输入与数量滑条继续使用原版 `GuiButton`、`GuiTextField` 和 Forge 1.12.2 `GuiSlider`。交通选中项仅以原版字体的 `>` 前缀表示，不绘制自制列表框。

原因：用 `drawRect` 模拟原版槽框/容器边缘即使颜色接近，也会在缩放和纹理细节上偏离原版视觉；这正是 UI 看起来“不像原版”的根因。原版纹理裁取既保留像素、阴影和边角，也不会重新引入蟹笼不存在的空槽。

影响：纯客户端显示修复；不改 Container Slot 编号、交易/交通 Packet、存档、经济规则或服务端权威边界。

## DEC-026 原版面板边缘与实际库存文案

决定：商人与交通的 `demo_background.png` 不再整体缩放，而是按九宫格固定四角和四条 1 像素原始边缘，仅扩展中心平坦像素；所有静态标签与交易文字使用原版容器的 `FontRenderer` 深灰色。蟹笼顶部的空白背景同样直接从 `generic_54.png` 取原始像素填充，槽位继续逐个裁取。购买有限库存显示 `remainingBundles * bundleSize` 个实际物品，并用“剩余数量”文案，不显示包数。

原因：整体拉伸 `demo_background` 会把原版边缘缩放成非整数像素，且颜色与偏黄文字组合会让原版 `FontRenderer` 看起来像不同字体；蟹笼手填背景与原版纹理明度不同，容易被误认为贴图错位。库存上限以包保存是服务端内部实现，玩家应看到可购买的真实物品数。

影响：这是纯客户端渲染与本地化调整；不改变交易库存扣减、每包交易数量、槽位坐标、Packet、存档或任何服务端权威规则。

## DEC-027 回退不稳定的 demo 背景九宫格

原决定：DEC-026 将 `demo_background.png` 拆分为九宫格，以保留面板边缘。

修改原因：游戏内实际验证显示该分片路径会产生大量错位图块；编译、静态审计和压缩包检查无法覆盖渲染状态下的 UV/批次问题，因此该方案不具备可接受的稳定性。

新决定：`GuiMerchantTrade` 与 `GuiTransportStation` 每帧只使用一次 `drawScaledCustomSizeModalRect`，从原版 `demo_background.png` 完整裁取 248×166 区域绘制响应式面板。继续使用原版 `FontRenderer`、`GuiButton`、`GuiTextField`、`GuiSlider` 与原版物品槽裁切，不再对 `demo_background` 做任何分片组合。

数据兼容影响：无。纯客户端视觉修复；不改变 Packet、Container、TileEntity、NBT、库存、金币或交通数据。

## DEC-028 市场按需刷新与 30 天有界历史

决定：`PastoralWorldData` 升为 v7。保留 marketSeed、首次市场日和当前/昨日价格快照，移除无限 `processedDays` 持久化；14 种作物历史仅保存并同步当前世界日前 30 天。价格仍由稳定 seed/day/key 公式计算，所以任意向前跳时不逐日循环，只重建该 30 天窗口。`MarketService.tick` 从世界 tick 生命周期移除，打开中的 `ContainerMarketBook` 请求或商人实际读取价格时才在逻辑服务端惰性刷新市场。行情书请求仍使用 Packet 1/2 的既有格式和 discriminator，但服务端只接受实际打开的书本 Container，单会话每 2 tick 最多处理一次；客户端关闭书本即清缓存并拒绝迟到快照。

原因：旧实现每个主世界 tick 都进入市场服务、检查当前价格，并把处理日与每种作物曲线永久追加到 `WorldSavedData`。这同时制造持续 CPU 开销、存档增长和客户端分页缓存增长，而 GUI 的单个可见窗口本来就最多 30 点。确定性价格生成已足以重建任何合法的当前窗口，不需要保存完整历史。

兼容性与影响：v3--v6 存档可直接读取，首次市场访问会按旧 marketSeed 重建当日窗口，下一次保存写 v7；第 31 天及更早的作物曲线会永久丢弃，当前/昨日价格、经济 key、价格公式、Packet 编码和 registry ID 均不变。市场不再在无人使用时跨日更新；书本保持打开跨日时需重新打开（或进行新的请求）才显示新日，商人交易请求仍会在服务端重新刷新并验证价格。

## DEC-029 商人、GUI 与蟹笼维护负载

决定：商人每日维护把原先的两次 `loadedEntityList` 遍历合为一次，并由 `MarketPriceSnapshot` 一次复制所有当前/昨日价格供单个交易快照使用；`EntityMerchant` 离开站点超过范围时最多每 20 tick 重算一次导航路径。商人 GUI 按客户端 tick 缓存出售持有量，行情书缓存图表几何，避免在渲染帧重复扫描背包或重算 30 点坐标。`TileCrabTrap` 的倒计时仍逐 tick 递减，但仅每 20 tick 和状态转换 `markDirty()`。

原因：这些路径不会改变权威规则，却会在商人多、GUI 高帧率或蟹笼多时重复分配集合、扫描实体/背包、执行寻路或持续标脏区块。批处理只减少重复读取，所有交易价格、库存、金币和掉落仍由原有服务端路径决定。

影响：没有新的注册 ID、NBT key、Capability 或 Packet discriminator。商人离站返航的下一次寻路最多延迟 1 秒；蟹笼在非正常停机后最多回退 19 tick 的倒计时持久化进度，正常运行中的倒计时和掉落时机不变。
