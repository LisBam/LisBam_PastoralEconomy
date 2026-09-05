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

影响：市场维护与显示查询的后续性能语义由 DEC-028～031 覆盖；客户端缓存只在内存中存在且连接生命周期清空。三维度请求通过 `MarketService` → `PastoralWorldData.get(World)` 解析到主世界根，因而读取同一行情。行情书模型现在使用本模组的低分辨率绿皮书贴图；这只改变外观，不影响 Item ID、配方或市场协议。

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

影响：旧 PlayerData v1 和 WorldSavedData v5 读取时创建安全的空交通数据，不推测旧节点。第 14 批只登记/激活/改名/移除和显示节点；不加入传送、路线、村庄发现或后续线路解锁。交通费用的当前冻结规则由 DEC-045 统一规定。

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

决定：替换 DEC-024 中手绘的灰色槽框和凸起面板。所有文字统一保留 Minecraft 1.12.2 原版 `FontRenderer`；`GuiCrabTrap` 与商人的可见物品格直接裁取 `minecraft:textures/gui/container/generic_54.png` 的原版 18×18 槽位像素。商人与交通面板直接使用 `minecraft:textures/gui/demo_background.png`，按钮、文本输入与数量滑条继续使用原版 `GuiButton`、`GuiTextField` 和 Forge 1.12.2 `GuiSlider`。交通选中项仅以深灰原版字体的 `>` 前缀表示，不绘制自制列表框；节点列表区域的鼠标滚轮只改变本地显示偏移。

原因：用 `drawRect` 模拟原版槽框/容器边缘即使颜色接近，也会在缩放和纹理细节上偏离原版视觉；这正是 UI 看起来“不像原版”的根因。原版纹理裁取既保留像素、阴影和边角，也不会重新引入蟹笼不存在的空槽。

影响：交通滚动和选中标记均为纯客户端显示，不改交易/交通 Packet、存档、经济规则或服务端权威边界。蟹笼保留 Slot 编号，0/1 的 Container、直接库存写入和 Hopper 插入统一使用 TileEntity 校验；旧存档中已有的错误功能槽物品仍会原样读入并可取出。

## DEC-026 原版面板边缘与实际库存文案

决定：商人与交通的 `demo_background.png` 不再整体缩放，而是按九宫格固定四角和四条 1 像素原始边缘，仅扩展中心平坦像素；所有静态标签与交易文字使用原版容器的 `FontRenderer` 深灰色。蟹笼顶部的空白背景同样直接从 `generic_54.png` 取原始像素填充，槽位继续逐个裁取。购买有限库存直接显示 `DailyOffer.remainingItems` 的实际物品数，并用“剩余数量”文案，不显示包数。

原因：整体拉伸 `demo_background` 会把原版边缘缩放成非整数像素，且颜色与偏黄文字组合会让原版 `FontRenderer` 看起来像不同字体；蟹笼手填背景与原版纹理明度不同，容易被误认为贴图错位。库存上限以包保存是服务端内部实现，玩家应看到可购买的真实物品数。

影响：这是纯客户端渲染与本地化调整；不改变交易库存扣减、每包交易数量、槽位坐标、Packet、存档或任何服务端权威规则。

## DEC-027 回退不稳定的 demo 背景九宫格

原决定：DEC-026 将 `demo_background.png` 拆分为九宫格，以保留面板边缘。

修改原因：游戏内实际验证显示该分片路径会产生大量错位图块；编译、静态审计和压缩包检查无法覆盖渲染状态下的 UV/批次问题，因此该方案不具备可接受的稳定性。

新决定：`GuiMerchantTrade` 与 `GuiTransportStation` 每帧只使用一次 `drawScaledCustomSizeModalRect`，从原版 `demo_background.png` 完整裁取 248×166 区域绘制响应式面板。继续使用原版 `FontRenderer`、`GuiButton`、`GuiTextField`、`GuiSlider` 与原版物品槽裁切，不再对 `demo_background` 做任何分片组合。

数据兼容影响：无。纯客户端视觉修复；不改变 Packet、Container、TileEntity、NBT、库存、金币或交通数据。

## DEC-028 市场按需刷新与 30 天有界历史

决定：`PastoralWorldData` 升为 v7。保留 marketSeed、首次市场日和当前/昨日价格快照，移除无限 `processedDays` 持久化；14 种作物历史仅保存并同步当前世界日前 30 天。价格仍由稳定 seed/day/key 公式计算，所以任意向前跳时不逐日循环，只重建该 30 天窗口。`MarketService.tick` 从世界 tick 生命周期移除，打开中的 `ContainerMarketBook` 请求或商人实际读取价格时才在逻辑服务端惰性刷新市场。行情书请求仍使用 Packet 1/2 的既有格式和 discriminator，但服务端只接受实际打开的书本 Container，单会话每 2 tick 最多生成一次快照；客户端关闭书本即清缓存并拒绝迟到快照。

原因：旧实现每个主世界 tick 都进入市场服务、检查当前价格，并把处理日与每种作物曲线永久追加到 `WorldSavedData`。这同时制造持续 CPU 开销、存档增长和客户端分页缓存增长，而 GUI 的单个可见窗口本来就最多 30 点。确定性价格生成已足以重建任何合法的当前窗口，不需要保存完整历史。

兼容性与影响：v3--v6 存档可直接读取，首次市场访问会按旧 marketSeed 重建当日窗口，下一次保存写 v7；第 31 天及更早的作物曲线会永久丢弃，当前/昨日价格、经济 key、价格公式、Packet 编码和 registry ID 均不变。市场不再在无人使用时跨日更新；书本保持打开跨日时需重新打开（或进行新的请求）才显示新日，商人交易请求仍会在服务端重新刷新并验证价格。

## DEC-029 商人、GUI 与蟹笼维护负载

决定：商人每日维护把原先的两次 `loadedEntityList` 遍历合为一次，并由 `MarketPriceSnapshot` 一次复制所有当前/昨日价格供单个交易快照使用；`EntityMerchant` 离开站点超过范围时最多每 20 tick 重算一次导航路径。商人 GUI 按客户端 tick 缓存出售持有量，行情书缓存图表几何，避免在渲染帧重复扫描背包或重算 30 点坐标。`TileCrabTrap` 的倒计时仍逐 tick 递减，但仅每 20 tick 和状态转换 `markDirty()`。

原因：这些路径不会改变权威规则，却会在商人多、GUI 高帧率或蟹笼多时重复分配集合、扫描实体/背包、执行寻路或持续标脏区块。批处理只减少重复读取，所有交易价格、库存、金币和掉落仍由原有服务端路径决定。

影响：没有新的注册 ID、NBT key、Capability 或 Packet discriminator。商人离站返航的下一次寻路最多延迟 1 秒；蟹笼在非正常停机后最多回退 19 tick 的倒计时持久化进度，正常运行中的倒计时和掉落时机不变。

## DEC-030 行情书限流必须合并而非丢弃

决定：`ContainerMarketBook` 对服务器已验证的请求保留一个最新待处理项。在冷却外的首个请求立即生成快照；冷却内收到的新请求替换待处理项，并由该打开 Container 的下一次 `detectAndSendChanges` 在限流到期后发送。客户端不需要新增失败包或重试定时器。

原因：GUI 在发出请求后先切换到读取状态。旧的限流直接返回会使服务端不回复，而客户端无法区分“请求被限流”与“网络仍在等待”，从而永久停留在读取状态，最常见于打开后立刻切换作物。

影响：同一书本会话仍最多每 2 tick 产生一次最多 30 点的快照，快速多次点击只计算和发送最后一个作物；Packet 1/2 编码、discriminator、价格公式、存档和客户端会话语义均不变。

## DEC-031 行情书按打开会话批量预取

决定：书本首个最新窗口请求（cursor `-1`）在服务器主线程一次准备市场数据，并以既有 Packet 2 分别下发 14 个作物快照；所有快照共享这次请求的 requestId。客户端切换作物时先按该 requestId 读取缓存，只有尚未收到预取窗口时才回退发送单项请求。历史窗口仍固定为最近 30 天，不恢复旧分页历史。

原因：实机症状表明，在小麦首包成功后再走 C2S 切换路径仍会导致界面长期读取。既然一个打开会话本来就只需要 14×30 个有界价格点，把这些数据随首次请求准备完可移除每次按钮点击的容器状态、网络时序和限流依赖；约 14 个小型 S2C 包仅在打开时产生，之后零网络开销。

影响：没有新 Packet、discriminator、NBT、WorldSavedData 或价格规则。首开会比只读小麦多一次有界的 14 作物/30 天准备和传输，关闭时仍清空；后备请求仍使用 DEC-030 的合并限流，防止不完整或异常网络情况下的请求风暴。

## DEC-032 商人实时交易、运行时行为与文字颜色

决定：`GuiMerchantTrade#doesGuiPauseGame` 固定返回 `false`。成功交易后，`MerchantTradeService` 除了保留 `CoinService` 的金币同步和既有 Packet 4 商人快照外，立即调用 `EntityPlayerMP#sendContainerToPlayer(player.inventoryContainer)` 同步窗口 0 的完整玩家背包。`ContainerMerchantTrade` 打开/关闭时分别在 `EntityMerchant` 登记/移除交易者；有交易者时停止导航和水平移动。商人常规 AI 使用 8 格玩家注视，实际受到玩家伤害后仅避开该玩家 200 tick。`GuiMerchantTrade` 的标签、卡片和数量文字采用原版 `GuiButton` 正常浅色；当前页签以及余额不足、库存不足或背包无待售物品的确认按钮必须真实设为 disabled，显示原版深色禁用字体，操作路径继续本地预检并由服务端权威复核。

原因：普通 `GuiScreen` 会暂停单人集成服务端，使 C2S 交易包、库存修改和 S2C 快照只能在关闭窗口后处理。商人 Container 有意不放玩家背包 Slot，因此常规 `openContainer.detectAndSendChanges()` 不会观察到交易服务直接改动的 `InventoryPlayer`；显式发送 `inventoryContainer` 才能让客户端在同一事务 tick 收到物品变化。交易者和受击逃跑均是实体当前生命周期行为，不应污染稳定商人身份或世界存档。可见但仍可点击的不可成交按钮会误导玩家，故恢复 1.12.2 原版 disabled 状态作为明确反馈。

兼容性与影响：没有新增或变更 Packet discriminator、NBT key、WorldSavedData、Capability、registry ID 或经济数值。玩家物品、金币、库存和价格仍只在逻辑服务端校验及修改；客户端只接收同步和发起请求。交易者集合、逃跑目标和倒计时随实体卸载/重启自然丢弃，下一次实体加载不会残留冻结或仇恨。

## DEC-033 交通资源一致性与商人延迟补生

决定：交通 GUI 所有手绘文字和重绘后的原版 `GuiButton` 标签统一采用原版按钮正常浅色；即使按钮因状态不可用而禁用，仍保留浅色文字、原版禁用背景与既有点击限制。`village_station` 的模型和方块 Material 改为石质交通站，并复用 `transport_station.png`。蟹笼和交通站贴图为简约原版风 32×32 PNG。玩家交通方块与其 ItemBlock 明确从无后缀 `tile.lisbam_pastoral_economy.transport_station` 键取得显示名称，避免标准查找链再次显示 `.name`。

`VillageService` 不再在 `WorldEvent.Load` 强制执行商人补生；启动后的首次周期维护只观察/修复站点，并再等待一个 200 tick 周期让 chunk NBT 中的实体加入世界。`MerchantRecord` 可选保存最近观察到的实体 chunk。补生前必须确认村庄站区块以及该记录的最后实体区块都已加载；实体索引还会拒绝 inactive、缺少 roster、Village ID 或 Station ID 不匹配的旧实体 NBT。

原因：村庄站模型曾直接引用原版 `planks_oak`，导致玩家抵达村庄时看见木板；交通方块的实际本地化查找会追加 `.name`，旧键无法命中。原有 1254×1254 贴图远高于原版方块需要，既增加发行包体积也无法保持像素边缘。更严重的是世界加载和目标村庄区块刚载入时，`loadedEntityList` 可能尚未包含已有商人；旧补生逻辑据此生成重复实体，随后区块 NBT 中的原实体加入后才被去重删除，表现为短暂多出又被刷新掉。

兼容性与影响：没有改 registry ID、blockstate ID、Packet discriminator、价格或玩家数据。MerchantRecord 新增的 `entityChunkX/Z` 是可选 NBT 字段，旧存档可直接读取，第一次观察到对应实体后才保存；没有提升根 `PastoralWorldData` dataVersion。首次加载的村庄商人最多延后两个维护周期（约 20 秒）才会在确实缺失时补生，换取不与尚未加入世界的持久实体重复。纹理替换和方块 Material 只改变显示、声音/粒子材质和保护站观感，不改变站点 UUID 或传送规则。

## DEC-034 交通显示、资源与商人规模维护

决定：`transport_station` 的 registry ID 和 NBT 保持不变，仅让 Block 与专用 ItemBlock 直接使用同一个无 `.name` 后缀的本地化键；行情书使用单独的 32×32 绿皮书 Item 贴图；交通站、村庄站和蟹笼沿用稳定模型路径，只替换其 32×32 贴图。蟹笼 JSON 配方固定为铁锭/铁栅栏的三行交错外框和中央 `minecraft:trapped_chest`。村庄商人目标数按 `max(ceil(villagerCount * 2 / 5), 3)` 计算，采用 `long` 中间值避免 `int` 乘法溢出，不设置 8 的上限。

原因：标准 Block/ItemBlock 本地化流程会追加 `.name`，而当前显示链出现了面向玩家的后缀；直接使用一个共享显示键可隔离该异常，同时不会影响稳定注册名。交易界面需要用原版 disabled 反馈不可成交状态；新的物品配方和简化像素材质均是明确玩法/视觉调整。村庄每 5 名村民仅增加一名商人会使大型村庄补充不足，新的 2/5 比例保留最低三人并按人口继续增长。

兼容性与影响：没有新增 Packet、WorldSavedData、Capability、TileEntity NBT 或 registry ID。旧存档内已有交通节点、蟹笼、市场行情书和 MerchantRecord 无需迁移；下一次低频商人维护会自然补足因新公式增加的商人。资源路径保持不变，资源包覆盖点不变；行情书新增的内部纹理路径不会改变物品模型 ID。

## DEC-035 商人中文显示姓名与作物商人皮肤

决定：商人 UUID 和所有交易/库存状态继续只由 `MerchantRecord` 管理；姓名绝不作为键或网络请求字段。`MerchantNameGenerator` 只接受服务端随机源，随机选择一个百家姓姓氏和一至两个常用中文名字符。`EntityMerchant#bind` 为新生成实体赋值，`readEntityFromNBT` 为无姓名或旧版本通用“商人”名称的已加载实体补值；使用原版 `CustomName` 实体 NBT 机制持久化。皮肤改为实体 NBT 的附加整数 `merchantSkin`，新旧实体在服务端首次初始化时随机为 0--4；索引经 1.12.2 `EntityDataManager` 同步，0 固定渲染原版 `steve.png`，1--4 渲染四张 64×64 Steve 宽臂 UV 的内置农作服装。商人 GUI 标题和实体渲染器都只读取同步状态，不自行随机或写入权威数据。

原因：中文姓名是显示属性，不应干扰商人重生、交易记录、库存共享或已有 UUID 绑定；原版实体自定义名称已能随实体区块 NBT 保存和发给客户端。皮肤索引同样属于单实体的展示状态，使用 DataManager 可以避免客户端与服务端独立随机而显示不同，也无需引入新的 Packet 或全局存档段。以原版 Steve PNG 为基底直接改绘保持 ModelPlayer(宽臂) 的像素尺寸和 UV 对应关系。

兼容性与影响：`merchantSkin` 是可选的新增实体 NBT key，缺失、非法值或旧通用名称均在第一次服务端实体加载时只补生成一次；已存在的自定义姓名保持不变。没有改动 registry ID、WorldSavedData schema、MerchantRecord、Packet discriminator、金币、价格、库存或 UUID。允许重名；每个实体持久化自己的姓名和皮肤，Chunk unload/reload、退出重进及服务器重启均不会重新随机。

## DEC-036 商人基础行为、姓名权重与密度调整

决定：`MerchantNameGenerator` 对王、李、张、刘、陈、杨、黄、赵、吴、周采用其常见人口比例的基点权重（794、741、707、538、453、308、267、229、205、202）；余下传统百家姓作为去重的低频池，均分剩余权重。`EntityMerchant` 的最大生命、移动速度和攻击伤害分别固定为原版玩家基础值 20、0.1、1；常规游走/返站以 1.0 导航倍率匹配该移速。复用原版 `EntityAISwimming` 和 `PathNavigateGround#setCanSwim(true)`，使其水中行为与村民一致地尝试上浮。环境、受伤与死亡声音明确返回空值。

皮肤池仅接受持久索引 1--4 的四种作物商人服装，渲染器不再引用 `minecraft:textures/entity/steve.png`。旧的 0 索引被视为无效，实体下次逻辑服务端加载时随机迁移到四种服装之一；已有的 1--4 值保持不变。村庄目标商人数量改为 `max(ceil(villagerCount / 5), 3)`，保留最低三人并取消此前增加到 2/5 人口比例的规则。

原因：原先所有姓氏等概率，无法反映常见百家姓人口分布；商人移速 0.5 远高于玩家基础值，且没有水面上浮 AI。Steve 作为默认皮肤不符合商人视觉要求。把密度系数从 2/5 回调为 1/5 可使同人口村庄的目标商人数减半，而不打破最低三人的既有保障。商人不是村民实体，显式空声音可避免其被误配置为村民声源。

兼容性与影响：不新增 Packet、registry ID、WorldSavedData、MerchantRecord 或持久化 schema。`merchantSkin` key 仍为同一可选实体 NBT key，只有旧索引 0 会发生一次展示迁移。已生成的商人记录在下一轮 reconciliation 按新较低目标数被停用/移除多余实体；交易、库存、价格和 UUID 规则不变。

## DEC-037 敌对生成、动物骨头与粘液球池迁移

决定：删除全部敌对/中立生物行为修改，包括玩家索敌/反击限制、农田骚扰 AI 和 Creeper 爆炸方块列表拦截。主世界敌对自然生成改由 `LivingSpawnEvent.CheckSpawn` 控制：仅逻辑服务端维度 0、`isSpawner=false`、`EnumCreatureType.MONSTER` 的事件设为 `DENY`。动物骨头在服务端 `LivingDropsEvent` 的 `HIGHEST` 优先级加入，然后交给既有 `SlaughterEnchantmentEventHandler` 的正常优先级倍率处理；基础成功后才应用事件给出的 Looting 等级随机 `0..level` 附加量。羊毛转线与附魔金苹果均使用 1.12.2 JSON 配方。粘液球保持既有 market/catalog key、单件基础价 4,800，但从 `BUY_UNCOMMON` 迁入 `BUY_RARE`，每日库存按本批 8 组→2 组并换算为实际物品数。

原因：使用 `CheckSpawn#isSpawner` 是 Forge 1.12.2 明确区分 WorldSpawner 自然生成与 `MobSpawnerBaseLogic` 刷怪笼的边界，既能禁止主世界自然敌对生成，也不会误伤刷怪笼。骨头必须在屠宰倍率前加入同一死亡掉落列表，才能不复制倍率逻辑而保留正确的抢夺、屠宰及外部附魔组合顺序。粘液球目录和池归属仍由当前唯一目录决定，不为旧 Offer 增加迁移分支。

兼容性与影响：没有新 registry ID、Packet discriminator、Capability 或玩家数据。旧敌对实体不会被删除，加载后恢复原版 AI/爆炸行为；旧实体 ForgeData 的农田骚扰冷却不再被读取。旧 `PastoralWorldData` 的 `farmHarassment` 字段读取时忽略，下一次保存时移除，不影响 market、merchant 或 transport 段，也不需要提升根 dataVersion。任务书明确不考虑旧商人 Offer、价格和库存迁移；缺少新 `remainingItems` 字段的 Offer 由读取层视为无效并在当天按当前目录重生成。

## DEC-038 商人原版整组、完整行情书与通用蟹笼

决定：商人购买全部按单个物品定价和结算；目录价格不再表示整组，有限库存先按任务书将 16 组→4 组、8 组→2 组、4 组→1 组、1 组→1 个，再用原版 `Item#getItemStackLimit()` 换算实际件数。`DailyOffer` 的持久字段改为 `remainingItems`，GUI 只显示实际剩余件数。行情书历史目录扩展为全部 22 种可出售给商人的逻辑商品；16 色羊毛继续共用一条逻辑商品和一条曲线。蟹笼的 20 格全部是通用容器槽，`ISidedInventory` 对所有方向公开全部槽位；槽 0/1 仅在运行时分别读取钓竿/合法生肉，捕捞结果仍只尝试放入 2～19。每轮基础等待改为 2,000～12,000 tick（100～600 秒），Lure 每级减少 2,000 tick（100 秒）。羊毛转线配方用 1.12.2 wildcard metadata 接受全部 16 色羊毛。

原因：固定的目录数量把“组”误实现为自定义小包，导致同一交易单位不符合原版堆叠规则；目录参数也会误导后续维护。行情书只追踪作物会遗漏实际可卖的畜牧和农业商品。蟹笼作为容器时限制存取方向/槽位与其通用存储定位冲突；旧计时又过快，不适合作为长期自动生产。

兼容性与影响：不更改 registry ID、Packet discriminator 或 `PastoralWorldData` 根版本；商人 Offer 的库存 NBT 语义改为 `remainingItems`，缺少该字段或含非法池/等级/重复商品的旧状态不迁移，服务端会重新生成当天列表。蟹笼的既有 20 槽物品和倒计时 NBT 均可读取，但已开始的一轮将在当前规则下继续处理并在下次重新抽取时使用新的等待范围。

## DEC-039 蟹笼专用输入与即时节点清理

决定：蟹笼继续是 20 槽容器，但 0 号槽只能插入钓竿、1 号槽只能插入五种合法生肉，2～19 槽保持通用；`ISidedInventory` 对玩家 Container 和所有方向原版 Hopper 使用同一插入校验，任意有效槽仍可抽取。GUI 不另行实现物品说明，而是保持 `GuiContainer` 的原版 Slot Tooltip 路径。交通方块配方改为红石块/铁块角边围绕中央指南针的固定九宫格。

交通“移出”改为直接删除当前玩家 Capability 中的节点记录；历史 `active=false` 条目在读取和创建快照时清除。自建交通方块破坏时立即撤销世界物理节点登记、清除 Tile UUID，并清理所有在线玩家的对应记录；离线玩家的过期引用在其下次交通快照构造时移除。Packet 5 的 `FIND_VILLAGE` 与 `CONNECT_VILLAGE` ordinal 保持不变：客户端自动请求候选，只保留一个接入按钮和本地确认层，确认包仍只携带候选 Village UUID，服务端重新搜索、计算费用、验证余额并结算。节点行不再显示维度后缀或 Active/Removed 状态，选中行的名称与费用使用绿色；商人和交通所有非按钮文字统一为 `0x404040`。

原因：上一版为通用容器错误地放宽了功能槽的插入规则，导致钓竿和饵料位置无法保证。交通移除只写入 inactive tombstone，物理方块拆除也不会清除个人引用，故快照会出现“已移出”或“节点无效”。村庄发现本来已经由服务端快照提供候选，额外查找按钮和主面板详情没有增加权威能力，却增加了交互步骤。

兼容性与影响：没有 registry ID、Packet discriminator/NBT 字段名称、`PastoralWorldData` 根版本或费用公式变更。旧蟹笼槽中已有非钓竿/非生肉物品不主动删除，仍可取出；新插入会被校验拒绝。旧玩家 NBT 中 inactive 节点在下一次加载时丢弃，缺失世界记录在下一次交通快照构造时丢弃；这释放节点上限且不再向客户端同步假节点。已保存的自建交通方块 UUID、有效 Active 节点、村庄站和首次免费资格保持不变。

## DEC-040 原版标题直绘、交通二次确认与金闪闪的骨粉

决定：`GuiMerchantTrade`、`GuiTransportStation` 的静态非按钮文字统一由 `FontRenderer#drawString(text, x, y, 4210752)` 直接绘制；水平居中先用 `FontRenderer#getStringWidth` 计算左边界，禁止用 `GuiScreen#drawCenteredString`。`GuiCrabTrap` 的全部前景标签也使用相同的 `4210752`。交通节点列表起点下移，令“我的节点”标题完全位于“接入最近村庄”按钮之后。最近村庄和移出节点使用原版 `GuiYesNo` 的背景、白色文本和按钮：移出层只保存待确认的 Station UUID，只有点击确认才发既有 `REMOVE` action；接入层仍只从 Packet 6 快照读取候选展示值并发既有 `CONNECT_VILLAGE` action。

新增稳定物品 ID `lisbam_pastoral_economy:golden_bone_meal`。`ItemGoldenBoneMeal` 只在逻辑服务端工作：中心原版可耕作物最多重复 8 次临时白色染料的 `ItemDye.applyBonemeal`，5×5 其余作物或草层中的草方块各调用一次；其他目标也只调用一次。该物品自身仅在至少一次路径成功后消耗一次，创造模式不消耗。`GoldenBoneMealRules` 将作物/草花分类和 X/Z 半径 2 的 25 格范围从 Item 中分离，便于无世界自检；模型、双语键和两份 JSON 无序配方与注册同时加入。

原因：`drawCenteredString` 在 1.12.2 走带阴影绘制，尽管颜色同为 `0x404040`，仍会造成工作台/熔炉标题没有的重影。移出是可恢复但会影响交通网络状态的操作，需要与村庄接入一致的确认节奏；客户端确认不能代替服务端的 Station/余额/候选验证。金闪闪的骨粉若自行复制 `IGrowable` 逻辑，会绕过 Forge 的原版骨粉钩子并容易与其他 1.12.2 模组不兼容，因此复用 `ItemDye.applyBonemeal`。

兼容性与影响：没有更改 Packet discriminator、action ordinal、TileEntity NBT、Capability、WorldSavedData 或金币/交通计算。确认层和列表排版仅是客户端暂态；任何绕过或过期确认仍由既有服务端拒绝。新物品不读写长期状态；旧存档可直接加载，新增配方/物品会按 Forge 正常注册出现。

## DEC-041 原版确认链、物品提示与金闪闪骨粉兼容

## DEC-042 购买行情分页与商人活动维护（2026-09-05）

出售肉类与作物共用次级收购池；购买目录商品全部参与 30 日历史。行情书拆分出售/购买页，仅保留最新窗口，服务端单项同步，客户端会话缓存按选中优先、其余低频预取，避免一次性传输完整购买目录。

刷怪蛋生成未绑定商人，由 `VillageService` 在村庄范围创建 MerchantRecord；随机安全点出生，越界清除绑定并以原版寻路返站，不瞬移。新增创造标签和两个默认关闭的服务器设置；不改既有 Packet、WorldSavedData 或 Capability 身份。

决定：交通确认使用 Java 1.12.2 `GuiYesNo`，仅在 `initGui` 中按现有客户端快照启用/禁用它本来就有的“是”按钮；不再绘制或缩放自定义确认背景。`GuiCrabTrap#drawScreen` 明确执行原版 `GuiChest` 同样的 `renderHoveredToolTip` 调用。商人交易和行情书只给任意颜色羊毛的显示用 `ItemStack` 写入“羊毛”自定义显示名。金闪闪的骨粉在 common `preInit` 注册 `dyeWhite` OreDictionary 条目，白色染料的生物交互委托给临时的原版白色 `ItemDye`，并将自身的增强 5×5 行为注册进 `BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY`。

原因：`GuiContainer#drawScreen` 不会自行调用 Tooltip 渲染；原版 `GuiChest` 的显式补充调用才使普通背包式容器出现物品说明。缩放 `demo_background` 的确认层无法同时保证原版比例和文字对比，直接采用 `GuiYesNo` 才能严格复用原版底图、字体和按钮。羊毛商品逻辑接受全部 metadata，不能因显示名称而更改其匹配或行情身份。物品若只重写方块使用，便会缺少白色染料和发射器两条原版入口；临时白色 `ItemDye` 与无玩家 `applyBonemeal` 继续经过 Forge 1.12.2 骨粉 hook，失败的发射器路径交回原版默认抛出行为。

兼容性与影响：不新增或改变 registry ID、Packet、NBT、Capability 或 `PastoralWorldData`。确认可见性、节点绿色和羊毛显示名均为客户端暂态；羊和方块的最终修改仍在逻辑服务端发生。已有存档中的物品、蟹笼库存、商人 Offer、市场 key 与交通节点无需迁移。

## DEC-042 链式市场与可切换旧稳定模式

决定：市场默认不再按“基础价 × 当日独立倍率”生成。新市场的首个价格为 `MarketCommodity.basePrice`；每个后续世界日以该商品上一日的保存价格为输入，步长为类别波动率的 `25%～100%`。偏离基础价 0/10/25/50% 时回归概率分别为 50%/55%/65%/75%，超过 50% 为 85%；结果按类别最低/最高倍率 clamp，触及边界时下一日强制向区间内部移动，整数取整后保持方向变化。`market.moreStableMarketVolatility` 默认 `false`；启用时逐日改用原有独立三角分布公式，但同样采用新基础价和上下限。

`PastoralWorldData` 沿用 v7 的 market NBT 结构：当前与昨日完整快照仍是唯一权威前驱状态，22 条收购曲线仍最多保存 30 点。加载旧世界时直接采用其已保存快照/曲线；同一世界日的 `ensureMarketDay` 只补齐损坏缺项，绝不按当前配置重算完整快照。因此开关设置、重登、打开行情书或打开商人界面都不能刷新价格。为保持每日链式关系，按需发现跨日会逐日推进；普通前进时每一天都立即写入并裁剪收购历史。时间回拨仍优先恢复保留的收购曲线，其他非书本商品按旧确定性后备公式恢复，这是管理员异常时间操作的兼容路径而非可刷价的普通界面路径。

设置存于 Forge 1.12.2 `Configuration`，common `preInit` 在服务端读取；Mod List 仅以物理客户端 `IModGuiFactory` 暴露编辑界面，不增加 C2S 配置包。远端多人以服务器配置为准。没有新增 Packet discriminator、NBT key、WorldSavedData 名称、Capability、registry ID 或数据版本迁移。

## DEC-043 原版背包键关闭与村庄确认可点性

决定：自定义 `GuiScreen` 统一检查 `GameSettings.keyBindInventory.isActiveAndMatches(keyCode)`，匹配时调用 `EntityPlayerSP#closeScreen`；这尊重 E 的改键并让服务端 `Container#onContainerClosed` 正常收尾。商人数量框的常态/禁用字色均为 `0xFFFFFF`，金币 HUD 采用 12 个 scaled pixels 页边距。交通最近村庄的主按钮与 `GuiYesNo` “是”按钮不再用客户端金币缓存禁用；确认点击无余额短路，仍只发送原有 `CONNECT_VILLAGE` 请求。

原因：普通 `GuiScreen` 默认仅用 Escape 关闭，按 E 会被文本框吞掉或无响应；直接 `displayGuiScreen(null)` 不能明确保证槽位容器关闭包。交通费用显示只是快照，客户端在延迟/余额变动下不应成为操作最终否决者。`TransportService#handleConnectVillage` 已经在逻辑服务端重新定位村庄、重算费用并经 `CoinService` 原子检查余额，故允许点击不会改变资金或解锁权威。

兼容性与影响：没有新增网络、存档或注册结构。打开背包键只关闭 UI；余额不足的接入请求仍被服务端拒绝且同步最新状态，不扣除金币。

## DEC-044 链式市场低价保护与恢复（已由 DEC-045 取代）

历史决定：默认链式市场曾设置 2 金币低价保护并保留独立模式 1 金币下限；该方案已被任务书规定的类别价格下限、边界反弹和按方向取整规则取代。

原因：任务书要求用新基础价格计算类别上下限，并删除旧的专用 1/2 金币保护；方向保持规则仍用于防止整数取整冻结。

兼容性与影响：DEC-044 的专用低价保护不再是当前规则；当前实现不迁移旧价格，已保存当天快照仍保持到自然跨日，之后按 DEC-045 计算。

## DEC-045 经济市场与商人按件交易修复（购买栏品质配额已由 DEC-048 取代）

决定：以任务书最终数值作为当前唯一经济规则，不实现旧玩家余额、旧市场价格、旧商人 Offer 或旧交通费用迁移。市场目录和商人目录全部使用金币×10后的单件基础价；铁锭为 900/个，基础建材采用冻结的降价表。默认链式市场使用类别上下限、动态回归概率和边界反弹；同一 Item/meta 变体通过 `variantIdentity` 共享每日随机方向与幅度来源。

商人购买按件计价、扣库存和发放物品。有限库存由原组数转换为实际物品数，`DailyOffer` 仅保存 `remainingItems`；缺少该字段的状态直接视为无效。服务端 `MerchantOfferService` 跨普通/罕见/稀有/珍宝池在生成源头按 Item/meta/NBT 变体身份去重；旧版固定 4+3+2+1 购买栏品质配额已由 DEC-048 取代。附魔书身份包含附魔注册名和等级。GUI 删除“一组数量”，将“剩余数量”移到原位置并显示实际件数。

原因：旧实现把价格、购买数量和库存都绑定到自定义整组，导致单价语义错误、有限库存过大和 GUI 信息错位；各池独立抽取还会让同一商人的购买页重复。市场无上下限和固定回归概率使低价取整可能冻结。统一单件模型、服务端唯一性、链式限幅/回归和整数方向保护分别修复这些根因。

兼容性与影响：不更改 registry ID、Packet discriminator、WorldSavedData 名称或 Capability；商人库存 NBT 使用新 `remainingItems` key，旧 Offer 被拒绝并重新生成。客户端只接收服务端快照，所有价格、库存、金币、背包和交易结果仍由逻辑服务端验证和修改。

## DEC-046 行情书渐进浏览与商人村庄家区

决定：行情书购买页仅为当前滚动窗口建立三列选择按钮，使用客户端 `GuiTextField` 按本地化显示名或稳定商品 key 过滤；选中商品请求优先，其余目录仍以每 4 tick 一个条目的节奏渐进预取。客户端仍会在选中请求 40 tick 无快照时以新的单调 requestId 重试；服务端直接在主线程回答每个已验证的有界请求，具体取代关系见 DEC-047。创造标签继续使用稳定 key `lisbam_pastoral_economy`，只补充其语言值“聆竹の休闲田园经济”。

商人以 `VillageRecord` 中心和 `VillageService.VILLAGE_REFERENCE_RANGE`（128）作为同一个圆形活动、出生、返航和解绑边界。`EntityMerchant` 将该中心写入可选实体 NBT；旧实体没有该字段时先以旧站点坐标读取，下一次已加载村庄维护再刷新为实际中心。越界只执行地面寻路返航，维护时仍越界才删除 MerchantRecord/roster 绑定，绝不传送。随机出生先采样圆形范围并要求安全、已加载实心地面，不再从范围外的方形角落产生实体。

原因：购买目录远大于出售目录，旧实现为所有商品创建 GUI 按钮，会越过面板并覆盖其他 UI；一次 C2S 请求若被服务端合并而没有回包，则等待状态没有恢复路径。商人服务此前使用 128 格识别/解绑村庄，而实体限制和出生点并不使用同一几何范围，不能满足“同村民村庄范围”活动要求。

兼容性与影响：没有新 registry ID、Packet discriminator、WorldSavedData schema、Capability 或交易数据迁移。新 `villageHomeX/Y/Z` 只在实体 NBT 中可选增加，旧实体、Offer、库存和名称可直接读取；客户端搜索/滚动/重试都是显示缓存行为，价格和市场历史仍完全由逻辑服务器生成和保存。

## DEC-047 行情书直接回包与附魔展示栈（2026-09-05）

决定：`ContainerMarketBook` 仅表示服务端当前确实打开了行情书；Packet 1 已经被调度到逻辑服务端主线程后，立即验证 Container、商品和游标，并直接返回单项、最多 30 点的 Packet 2 快照，不再把请求放入 `Container#detectAndSendChanges` 的延迟队列。`MarketService` 对最新窗口建立“非空当天”不变量：若旧世界有当前冻结价格而该商品的保留历史尚未出现，则显示该当前日价格点；既有 `ensureMarketDay` 仍负责把所有历史目录的当天记录写回 `PastoralWorldData`。

购买页图标通过 `TradeCatalog#createEnchantedBookStackForMarketKey` 从稳定 market key 反查附魔定义和确定等级，构造原版 `ItemEnchantedBook` 栈，而非显示无 NBT 的通用附魔书。

原因：无槽 Container 的 deferred tick 不是可靠的请求完成信号，合法选择会因此没有任何回包；而旧世界缺失新增目录的历史时，裸空列表错误地把已经冻结的当天行情显示为“暂无记录”。附魔书的 Item 名称不包含其 NBT，必须还原真实栈才能让原版 Tooltip 提供玩家需要的附魔信息。

兼容性与影响：Packet 1/2 的 discriminator 与编码完全不变，市场价格、历史写入和所有交易继续由逻辑服务端拥有。没有新增 registry ID、WorldSavedData 版本或 NBT key；仅在旧世界首次读取时由原有修复路径补齐当天历史。

## DEC-048 商人购买栏独立品质抽取（2026-09-05）

决定：每名商人每日出售 8 项商品，取消固定的普通 4、罕见 3、稀有 2、珍宝 1 配额。`MerchantOfferService` 仅在逻辑服务端为每个购买槽分别掷一次 0--99：0--39 为普通、40--69 为罕见、70--89 为稀有、90--99 为珍宝。每槽抽取后继续复用既有商品轮换、附魔等级解析和跨池 Item/meta/NBT 变体去重；所有 8 项生成后稳定排序为普通→罕见→稀有→珍宝。各品质当日允许为 0 项，珍宝不再保证出现。

原因：固定品质数量使每天的出售稀有度组合没有波动，也无法出现“无珍宝日”。在先抽品质、再抽商品的服务端链路中保留轮换和唯一性，既满足独立概率，又避免只靠纯随机导致商品池长期遗漏；排序继续匹配已有交易卡按品质分组的可读性。

兼容性与影响：没有修改 registry ID、Packet discriminator、NBT key、`PastoralWorldData` 名称或数据版本。旧存档含 10 个购买栏的 `DailyOfferState` 会在读取时因当前数量校验失败而成为空状态，下一次商人交互时由逻辑服务端生成当天新的 8 项列表；不迁移旧当天库存，其他商人记录、轮换状态和金币均保持不变。

## DEC-049 商人快照槽位常量与行情书本地拼音检索（2026-09-05）

决定：交易同步包不再拥有出售 6、购买 10 的硬编码解码长度，`SyncMerchantTradeMessage` 的读取端与写入端都以 `MerchantTradeSnapshot.SELL_COUNT`/`BUY_COUNT` 为唯一协议大小。行情书购买页在既有显示名和稳定商品 key 过滤之外，使用只存在于客户端 GUI 包的 `PinyinSearch`；它为原版物品/方块与本模组中文本地化中实际出现的汉字保存紧凑读音表，并缓存每个显示名的全拼、首字母与逐字读音，以支持全拼、首字母和混合拼音输入。

原因：购买栏由 10 项缩减为 8 项后，服务端已经按 8 项写出快照，但客户端仍尝试读取 10 项，导致整个解码失败，交易 GUI 的所有槽位都退回“后续内容”占位。拼音搜索只影响本地目录筛选，使用运行时外部库或服务端查询既无必要，也会扩大客户端/网络边界；逐字缓存可避免每次渲染/输入都重新转换目录名称。

兼容性与影响：Packet discriminator、字段顺序、NBT、WorldSavedData、registry ID 和交易/市场权威规则均不变；仅当前协议长度随既有 8 槽常量正确读取，旧客户端不能与新服务端混用。拼音匹配不会改变商品 key、价格、缓存请求、背包或服务端交易结果；缺少字典读音的资源包自定义汉字仍可按原始名称或 key 搜索。

## DEC-050 1.5 附魔修复与工具交互边界（2026-09-05）

决定：保留既有八个附魔 registry ID、NBT 写法与市场 key，不迁移旧物品；新增稳定 ID `attack_speed`、`range`、`reforged`、`bluntness_curse`。Attack Speed、Range、Reforged 的常规武器/工具范围固定为 Java 1.12.2 原版剑、斧、镐、锹、锄，Range 额外允许剪刀；Harvest 同时允许锄和剪刀。Bluntness Curse 是带 `isTreasureEnchantment`/`isCurse` 标记、明确拒绝附魔台的剑用附魔，并与 Sweeping Edge 互斥。商人书池将四种新书作为稳定商品键加入，攻速/范围等级权重冻结为 40/28/17/10/5，价格分别为 50k/80k/120k/180k/250k 与 60k/100k/150k/210k/280k；百炼如新与束锋诅咒固定为 120k/20k。

攻击速度和交互距离仅由逻辑服务端的非持久 AttributeModifier 写入。Attack Speed I--IV 用 operation 2 将冷却间隔固定为原本 80/60/40/20%，V 用高于一游戏 tick 阈值的 attack-speed modifier 保证连续攻击已充能；Range 用 Forge 1.12.2 原生 `EntityPlayer.REACH_DISTANCE`，所以客户端目标选取和服务端距离验证使用同一属性。Fleetfoot 仍用原生移动属性，物理客户端 `FOVModifier` 只除去该固定 UUID 的倍率，从而不影响弓、飞行等其余 FOV 修正。Night Vision 改由物理客户端可恢复临时 gamma 实现短时视觉窗口，不创建药水效果或改世界光照。FarmlandTrampleEvent 在两侧取消，防止预测端先将耕地/植物破坏。

Reforged 对有右输入的修理、合并和附魔书操作通过 `AnvilUpdateEvent` 以纯 `AnvilFirstUseRules` 生成首次费用输出，左右输入 NBT 永不改写；Forge 对纯改名不发此事件，故在已有原版输出时仅校正 `ContainerRepair` 的显示费用和输出 RepairCost。极端纯改名若原版已因 `>=40` 完全清空输出，无法从 Forge 公开事件安全恢复名称，保持原版拒绝。Harvest 剪毛先记录服务端原版交互，再在同 tick 确认羊已剪毛或哞菇已变形后追加奖励，避免失败交互复制掉落。束锋诅咒在 `AttackEntityEvent` 后、原版横扫判定前只改变横扫资格边界，不改直击结果。

原因：移动药水式 FOV、客户端 gamma 和仅服务端踩踏拦截都只覆盖了视觉/事件链的一部分；依赖 Forge 的属性、原版状态效果和可取消踩踏事件能同时保持多人服务端权威与 1.12.2 原版行为。铁砧规则若直接改背包输入的 `RepairCost` 会永久篡改物品 NBT，故只创建当前输出。剪毛没有可用的后置掉落事件，必须以原版实际结果为条件。

兼容性与影响：新增四个 registry ID 和商人 market key 是追加式变化；现有附魔物品、`PastoralWorldData`、Player Capability、Tile NBT、Packet discriminator 和已有商品 key 均不变。旧存档第一次生成商人新一天出售栏时自然可抽到新增书；不要求数据迁移。百炼如新不修改输入 NBT；普通改名的原版“过于昂贵”空输出是唯一明确保留的边界。

## DEC-051 商人局部范围、原版命名牌与交通费缩放（2026-09-05）

决定：保留 `VillageService` 的 128/64/160 村庄识别与交通去重参数；新增独立的商人活动/出生半径 32 和强制返回阈值 64。`EntityMerchant` 以村庄中心设置 32 格原版限制，超过 32 格时继续按 20 tick 节流寻路，超过 64 格时调用 1.12.2 `Entity#setPositionAndUpdate` 传送到持久村庄站上方。村庄维护节流为 200 tick（10 秒）。对手持 `Items.NAME_TAG` 的交互返回 `false`，让 `EntityLiving` 转交 `EntityLivingBase` 的原版命名牌流程；不自行解析、同步或存储名称。交通费用统一缩放为旧曲线的 1/10：旅行 `round5(40 + 0.12D)`、接入 `round10(400 + 1.20D)`。

原因：村庄身份的宽范围同时服务于旧版 `VillageCollection` 观察和交通站去重，不应因实体活动区缩小而改变；站点已是受保护、持久且安全的返还位置。交还命名牌给 1.12.2 原版路径可保留显示名校验、物品消耗和 `CustomName` 持久化，避免复制物品交互。按公式与最低值同步缩小费用，确保客户端展示和全部服务端接入/旅行结算一致。

兼容性与影响：无 registry、Packet、NBT key、WorldSavedData 或 Capability 迁移。已加载商人立即使用新位置规则；旧实体的村庄中心/站点 NBT 仍有效。费用不持久化，已解锁节点保留而下一次旅行或接入直接使用新价格。

## DEC-052 背包行情提示、附魔穿戴边界与旅行费回调（2026-09-05）

决定：行情书在价格头部显示 `MarketCommodity.basePrice`，并为新增文字行向下移动昨日价、趋势和图表起点。原版 `ItemTooltipEvent` 路径中的可出售 ItemStack 通过追加 Packet 7（C2S 有界 commodity key）与 Packet 8（S2C key、市场日、当前价）显示“今日价：xxx金币”；服务端在主线程只接受 `MarketCatalog` 中的合法出售 key，再由 `MarketService` 读取并回复冻结价格。客户端仅接受与本地 `worldTime / 24000` 相同市场日的瞬态缓存，缺失条目每秒最多请求一次。

疾步的稳定 registry ID 不变但准确白名单和装备槽改为护腿；它只由服务端非持久移动属性修饰符直接改变移速，修饰符在穿戴期间保持跨跳跃稳定，客户端 `FOVModifier` 监听器按去除疾步固定 UUID 后的完整属性值重算，因此穿戴和跳跃均不会改变 POV，同时保留弓、飞行和其他原版视角变化。夜视改由 `ClientNightVisionRenderHandler` 在物理客户端以可恢复临时 gamma 实现首次 200 tick、之后每 100 tick 续 200 tick，不创建药水效果；取下头盔或断开连接立即恢复原始 gamma。范围仍复用 Forge 1.12.2 `EntityPlayer.REACH_DISTANCE`，数值改为每级 +1.5；Forge 对该属性已同时用于客户端射线选取和服务器 `processUseEntity` 的实体距离门槛，无需也不能添加客户端权威攻击包。旅行费用精确改为旧值的四倍，采用 `round20(160 + 0.48D)`；接入费保持 `round10(400 + 1.20D)`。

原因：背包 Tooltip 不能从本地目录推断随机市场的今日价格；此前对界面类型、实体实例和库存槽位的额外限制，以及在悬停槽位选中前运行的 `DrawScreenEvent.Post` 兜底，都会让实际悬停请求或显示无法命中，因此只保留合法出售 key 校验并直接修改 `ItemTooltipEvent` 列表，显示价格仍完全来自服务器冻结快照。图表需要为第四条价格文本预留独立垂直间隔。使用原生可同步属性能让攻击与交互保持相同的服务器权威距离规则；1.12.2 原版会从移动属性自动计算 FOV，因此保留客户端监听器抵消疾步项，才能在直接提速的同时不改变 POV。临时 gamma 让夜视保持客户端本地且不污染玩家药水状态。

兼容性与影响：Packet 7/8 为追加 discriminator，0～6 的编码不变；没有 WorldSavedData、Capability、Tile NBT 或 registry 迁移。旧疾步靴子 NBT 可正常保留但不再有效，护腿需按当前附魔规则获得。前往费不持久化，下一次旅行立即按四倍曲线结算。
# 2026-09-05 维护决定：挤奶与调价

- 成年牛的挤奶冷却归属于牛实体，而非玩家；最近一次成功挤奶 tick 写入 `Entity#getEntityData()`，因此区块卸载、重启和多人共享同一头牛时仍保持 6000 tick 冷却。`disableMilkingCooldown=false` 为默认安全值，开启后不拦截原版交互。非冷却交互绝不再手动扣空桶或制造牛奶桶，而是记录时间后交还 `EntityCow` 的原版成功分支；冷却中的交互才在逻辑服务端拒绝。
- 价格表以工作簿手调小麦值乘 50 转为现有整数金币基准，保留全部既有 market key、历史和存档结构，不迁移旧价格快照；新值仅在新世界日生成时生效，已冻结的历史点不重写。
- Forge 1.12.2 在剪刀/锄头的原始 enchantability 为 0 时，会在附魔台生成候选前直接退出；同时原版 `Efficiency` 的较新 Forge 附魔台筛选不接受剪刀，虽然原版效率书可正常经铁砧应用。公开 Forge 事件无法补回候选，故发行 JAR 用带 manifest 的最小 Coremod：按实际发现的 `Item` 布局补丁 `ItemStack` 重载及效率筛选，或回退补丁旧式无参附魔力。注入 helper 采用 `Object` 描述符，不能再因 MCP/SRG 内部名不同而找不到方法；效率筛选不增加条件跳转，而是在每个既有布尔返回点以 `(Object,Object,boolean)` helper 合并原版结果，从而无需生成新的 Java 8 StackMap frame。遇到未知布局会记录错误并返回未改动 `Item`，绝不使客户端/服务器不能启动。剪刀使用铁工具附魔力、各锄头使用其材质附魔力，剪刀效率直接使用原版 `Efficiency`。不再注册/生成 `shears_efficiency`；旧存档的同名 MissingMapping 重映射到原版效率，避免旧物品失去附魔。耐久直接在附魔台可得；经验修补与消失诅咒保留原版书本/铁砧路径。
