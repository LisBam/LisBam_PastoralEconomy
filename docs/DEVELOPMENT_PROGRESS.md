# 当前状态

当前完成批次：第 15 批。

最近成功构建：

- `env JAVA_HOME=/tmp/lisbam-jdk8-UlgK66/jdk8u504-b01 PATH=<Temurin-8-bin> ./gradlew build`
- 日期：2026-09-04
- 结果：PASS（Forge 14.23.5.2859 / Temurin Java 8 `1.8.0_504`；`build` 包含 `reobfJar` 与 `exportReleaseJar`。`release/LisBam_PastoralEconomy-1.0.jar` 为 322,628 bytes，SHA-256 `a167aeaa18064cf090acfc233929632fb0490f4acda827cc146e1ef5ed957e1f`，`unzip -t` PASS。）

## 发行：版本 1.0（2026-09-04）

`build.gradle` 的项目版本更新为 `1.0`；既有 `processResources` 展开使 `mcmod.info`、JAR Manifest 的 Specification/Implementation Version 与发布文件名保持一致。该变更不涉及注册 ID、网络协议、存档、经济或玩法数据。

验证：Temurin Java 8 `1.8.0_504` 下 `check_toolchain.py`、`compileJava`、`processResources` 与 `build` PASS。Forge 1.12.2 strict audit 为 0 ERROR、5 条既有 `packet-thread` WARNING。正式重混淆 JAR 已导出为 `release/LisBam_PastoralEconomy-1.0.jar`，`mcmod.info` 和 Manifest 均确认显示 `1.0`，压缩包完整性检查通过。

## 维护：商人中文姓名与作物商人皮肤（2026-09-04）

实现：新增服务端 `MerchantNameGenerator`，从常用百家姓和常用中文名字符池生成“1 姓 + 1--2 名”显示姓名。`EntityMerchant` 在新绑定和旧实体 NBT 读取时只在逻辑服务端补齐缺失/旧通用“商人”名称，直接复用原版 `CustomName` 实体 NBT；UUID、`MerchantRecord`、交易、价格和库存均未改动。实体 NBT 新增可选 `merchantSkin`，服务端随机选择 0--4 后由 1.12.2 `EntityDataManager` 同步；0 是原版 Steve，另外四张内置 64×64 PNG 分别为草帽工装、绿格衬衫、蓝围裙和收获背心。交易 GUI 标题显示实体已同步姓名，客户端不会生成名称或皮肤随机数。

兼容性与影响：旧存档中没有姓名/皮肤字段的商人在首次服务端加载时补写一次，已有非通用自定义姓名原样保留；`merchantSkin` 缺失或非法也只补写一次。没有新 Packet、registry ID、WorldSavedData schema 或 MerchantRecord 字段；重名允许，姓名不参与任何主键或交易验证。贴图基于原版 Steve 宽臂 64×64 UV 改绘，未修改模型、像素尺寸或贴图布局。

验证：源码/PNG 结构检查 PASS：四张服装均为 64×64、8-bit RGBA PNG，且均实际改绘原版 Steve 像素。Forge 1.12.2 strict audit PASS：0 ERROR、5 条既有 packet-thread WARNING。`merchantNameSelfTest`、`compileJava`、`processResources` 和最终 `build` 实际执行但未能启动，均因 `JAVA_HOME`/`java` 缺失而失败；当前环境没有可启动的 Linux JDK，尝试 Windows JBR/DBeaver `java.exe` 也均因 WSL `UtilBindVsockAnyPort` 失败。现有 `release/LisBam_PastoralEconomy-1.0.jar` 和 `build/libs` JAR 未含本次类或贴图，不能作为本次发行包；尚未生成本次 release JAR。游戏内的旧商人首次加载、Chunk reload、重启、姓名显示、四种服装和 Dedicated Server 均待可运行的 Forge 环境验证。

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
