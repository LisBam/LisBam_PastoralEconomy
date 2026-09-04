# 测试清单

## 构建

- [x] `./gradlew compileJava`、`compileTestJava` — PASS（2026-09-04，临时 Temurin Java 8 `1.8.0_504`）
- [x] `./gradlew processResources`、`build` — PASS（2026-09-04，含 `reobfJar`、`exportReleaseJar`；release JAR 324,313 bytes，SHA-256 `5c5dfa7073cec1251675e1f626b9672a9aa0ef388af11bbc505b6e19815ed2c3`，`unzip -t` PASS）
- [x] Forge 1.12.2 static audit — 0 ERROR；5 条 `packet-thread` WARNING 已审查（通用注册不处理消息；四个 S2C Proxy 桥没有直接修改状态，客户端实际写入均调度至主线程；交通 C2S handler 调度至服务端主线程）。

## 维护：蟹笼专用槽与交通交互

- [x] `crabTrapSelfTest`：0 号槽只接受钓竿、1 号槽只接受合法生肉，2～19 接受任意物品；所有面向公开 20 槽，Hopper 插入遵循同一校验且产出位可取。PASS（2026-09-04，Temurin Java 8）。
- [x] `transportCoreSelfTest`：移出节点直接删除 PlayerData 映射，NBT 与死亡 Clone 仅保留有效节点；物理世界节点删除和 Packet 往返保持。PASS（2026-09-04，Temurin Java 8）。
- [x] `transportTravelSelfTest`：村庄候选身份和接入/旅行费用检查点保持不变，确认层只使用已有服务端权威 `CONNECT_VILLAGE` 结算路径。PASS（2026-09-04，Temurin Java 8）。
- [x] `processResources` 与 release JAR：交通方块配方为 `RIR/ICI/RIR`（R=红石块，I=铁块，C=指南针），双语确认窗口与三坐标显示键已打包。PASS（2026-09-04）。
- [ ] 游戏内：蟹笼 0/1 槽与各向 Hopper 拒绝错误物品、2～19 可存任意物品、原版 Tooltip；交通配方；村庄确认层余额不足禁用与服务器重新定价；移出/拆除后节点列表立即消失；320×240 下文字、绿色选中行与无 `d0` 显示。NOT RUN：当前环境没有可操作 Forge 客户端。

## 维护：商人整组、行情书与蟹笼规则

- [x] `merchantCatalogSelfTest`：每条购买目录的组大小严格等于目标物品原版最大堆叠数；所有有限库存按组计，保存的 `remainingBundles` 仍可读取为剩余组数。PASS（2026-09-04，Temurin Java 8）。
- [x] `marketCoreSelfTest`、`marketPacketSelfTest`：准确追踪并首开预取全部 22 种商人收购商品，16 色羊毛只占共享的一条行情。PASS（2026-09-04，Temurin Java 8）。`processResources` 与 release JAR 资源清单确认无序配方包含 `minecraft:wool` wildcard metadata 且输出 4 根线。
- [x] `crabTrapSelfTest`：基础等待为 2,000～12,000 tick，Lure 每级减少 2,000 tick；其后续专用槽回归规则见上方维护项。PASS（2026-09-04，Temurin Java 8）。
- [ ] 游戏内：验证商人的 64/16/1 物品组、所有 22 条行情书选择项、任意颜色羊毛配方、蟹笼手动与 Hopper 存取及 Lure I～III 等待时间。NOT RUN：当前环境没有可操作 Forge 客户端。

## 第 01 批启动与资源

- [ ] 开发客户端启动并进入主菜单。NOT RUN：WSL 无可用 LWJGL 显示模式。
- [ ] Mods 列表识别 `LisBam_PastoralEconomy`、正确 modid、版本和 Logo。NOT RUN：客户端窗口未创建。
- [ ] 新世界可以创建并进入，无本批 Missing Model/Registry 错误。NOT RUN：客户端窗口未创建。
- [ ] Dedicated Server 基本启动且 common 代码无客户端类硬引用。NOT RUN：实际 Forge 引导在 WSL 中停滞，`eula=false` 未改动；静态检查与 Jar 审查未发现 common 客户端 import。
- [x] 最终 Jar 包含 Java 类、`mcmod.info`、`pack.mcmeta`、语言文件和 `textures/logo.png`。
- [x] 最终 Jar 不含 `mods.toml`、测试物品或测试方块资源，且元数据变量已展开。

## 第 02 批持久化、金币与 HUD

- [ ] 新玩家首次进入金币为 0。NOT RUN：无可进入的测试世界。
- [ ] 两名玩家金币相互隔离。NOT RUN：无可进入的测试世界。
- [ ] 金币在死亡/Respawn、维度切换、登出/登录、保存重载与 Dedicated Server 重启后保持。NOT RUN：无可进入的测试世界。
- [ ] `PastoralWorldData` 只从主世界 MapStorage 读取，主世界/下界访问为同一根数据。NOT RUN：无可进入的测试世界。
- [ ] 第一次世界加载只设置一次 `keepInventory=true`；手动关闭后的重登、死亡、维度切换和重启均不再改写。NOT RUN：无可进入的测试世界。
- [x] `canAfford`：负数失败、0 成功且无变更、余额不足失败且无变更。PASS：`PlayerDataSelfTest`。
- [x] `trySpend`：负数和超额失败且无变更；恰好扣至 0 成功。PASS：`PlayerDataSelfTest`。
- [x] `addCoins`：负数失败；0 无余额变化；Long.MAX_VALUE 溢出失败且余额不变。PASS：`PlayerDataSelfTest`。真实余额变化触发同步待世界运行验证。
- [ ] 登录、重生、维度切换和真实余额变化时只同步给对应玩家；不每 Tick 同步。NOT RUN：无可进入的测试世界；代码审查确认唯一发送点是 `CoinService.sendTo` 与三个目标生命周期事件。
- [ ] HUD 右对齐且不随 GUI Scale/分辨率越界。NOT RUN：客户端窗口未创建。
- [x] HUD 数字格式 `金币 0`、`金币 999`、`金币 1,000`、`金币 12,580`。PASS：`PlayerDataSelfTest` 验证固定逗号格式，文本使用中文语言键。
- [ ] F1 隐藏普通 HUD 时金币 HUD 同步隐藏。NOT RUN：客户端窗口未创建；代码检查使用 `gameSettings.hideGUI`。
- [x] 连接/断开后客户端金币缓存清空，不残留上一服务器余额。PASS：`PlayerDataSelfTest`。
- [ ] Dedicated Server 无 `net.minecraft.client.*` 加载异常。NOT RUN：Dedicated Server 未完成 Forge 引导；静态审查确认客户端 import 仅在 `client` 包和 `ClientProxy`。

## 维护：敌对生成与动物骨头

- [x] `compileJava`、`processResources`、最终 `build`（含 `test`、`reobfJar`、`exportReleaseJar`）— PASS：2026-09-04，Temurin Java 8 `1.8.0_504`。
- [x] Forge 1.12.2 严格审计 — PASS：0 ERROR；5 条既有 `packet-thread` WARNING 已复核，未由本次引入。
- [x] `release/LisBam_PastoralEconomy-1.0.jar` — PASS：325,829 bytes、SHA-256 `ece4347f519d2adab909099d8772f720e3225c95df1d5d11584300b6aa95e961`、`unzip -t` 通过；包含新的事件类与两个配方，不含已删除的敌对行为类。
- [ ] 主世界夜间、洞穴和区块首次生成均不出现自然 `MONSTER`；下界、末地、动物、水生生物和环境生物保持原版自然生成。NOT RUN：需要可进入的世界。
- [ ] 主世界刷怪笼仍可生成怪物；刷怪蛋、命令与模组直接生成不被拦截。NOT RUN：需要可进入的世界。
- [ ] 已生成的僵尸、骷髅、蜘蛛、末影人、苦力怕及充能苦力怕的索敌、反击、爆炸方块破坏与实体伤害均为 Java 1.12.2 原版行为。NOT RUN：需要可进入的世界。
- [x] `animalBoneDropSelfTest`：大体型 20% 双骨/50% 单骨边界、小体型 25% 单骨边界、成功基础掉落的 Looting `0..等级` 附加量及失败不被 Looting 变为掉落。PASS：2026-09-04，Temurin Java 8。
- [x] `merchantCatalogSelfTest`：粘液球位于 28 项稀有池、4 个/组、8 组库存；读取旧当天罕见槽的无限库存粘液球时，只替换该槽位且保留现有稀有商品。PASS：2026-09-04，Temurin Java 8。
- [x] `pastoralWorldDataSelfTest`：旧 v1 初始化标记保留，已删除的 `farmHarassment` 字段不再写出。PASS：2026-09-04，Temurin Java 8。
- [ ] 牛/哞菇/猪/羊/马/驴/骡/羊驼与鸡/兔/狼/豹猫/鹦鹉按表掉落骨头；抢夺与屠宰的顺序、外部强制并存时的组合、非白名单实体均符合内容书。NOT RUN：需要可进入的世界。
- [ ] Dedicated Server 完整加载新增 common 事件处理器且不加载客户端类。NOT RUN：`timeout 60s ./gradlew runServer` 已进入 Forge/FML 1.12.2 引导与 coremod 阶段，但在模组发现前到达时限；`run/eula.txt` 保持 `eula=false`，未接受 EULA。静态审计为 0 ERROR。

## 第 04 批世界日市场核心

- [x] `marketCoreSelfTest`：100 次同 key/day 调用一致；新增目录项不参与旧 key 随机；8 类波动与最低价正确；小麦基础价为 5。PASS。
- [x] 市场初始化只写当前真实世界日的 14 条作物点；苹果不进入历史。PASS：自检。
- [x] 连续 30 日只保留按世界日升序的最近 30 点；第 31 天及更早点不再提供分页或持久化。PASS：`marketCoreSelfTest`。
- [x] 多日/一百万日跳跃仅重建最近窗口；当天重复调用不重建，NBT 重载、回拨和返回原日均产生无重复的确定性 current/previous/history。PASS：`marketCoreSelfTest`。
- [x] v6 市场读入后写为 v7，且不再写 legacy `processedDays`。PASS：`marketCoreSelfTest`。
- [x] `PastoralWorldData` v2→v3 保留 firstInitializationCompleted 且不伪造 market。PASS：自检。
- [x] Forge 1.12.2 static audit：0 ERROR；本批无 Client import、Packet 或现代 API。PASS（2 条第 02 批 packet-thread WARNING 已审查）。
- [ ] Dedicated Server：市场初始化、睡眠、`/time add`、重启、多玩家、维度与存档重载。NOT RUN：本次实际启动到 Srg→Mcp 映射加载，尚未进入模组加载/EULA/世界；未改动 `run/eula.txt`。
- [ ] Client：进入世界后金币 HUD 回归正常，且没有新的市场 GUI。NOT RUN：本次实际进入 Forge 客户端引导，但在模组发现/窗口创建前停止，无法进入世界。

## 第 05 批市场行情书与历史 GUI

- [x] `market_book` 具有稳定 registry/unlocalized name；书 + 小麦使用无序 JSON 配方、输出严格为 1；模型引用本模组 `textures/items/market_book.png` 绿皮书贴图。PASS：编译、`processResources`、成品 Jar 资源检查。
- [x] Packet 1/2 使用既有 `lb_pastoral` channel、稳定不重排的 discriminator 1/2；商品 key 限制为 128 UTF-8 bytes，历史点计数最大 30；C2S 仅接收 key/游标/请求号，服务端验证实际打开的 `ContainerMarketBook`、14 种历史作物、非负/非未来游标并调度主线程。书本首个 cursor `-1` 请求会下发全部 14 种最新窗口，切换作物不再发包；后备请求最多每 2 tick 生成一次，冷却内快速切换合并为最后一项而非静默丢弃。PASS：代码审查、Forge audit 与编译。
- [x] S2C 快照包含当前日、商品 key、窗口游标、今日/可选昨日价、最多 30 个日/价格/真实前一点价格、前后翻页标记；客户端缓存仅在打开书本期间接收，关闭立即清空并拒绝迟到包，连接内请求号不复用。PASS：代码审查、`marketPacketSelfTest`、编译。
- [x] `marketPacketSelfTest`：合法请求/快照可往返，129-byte key 与 31 点快照被拒绝，首个可见点保留无前日状态，较旧同窗口快照不会替换较新缓存；同一打开请求号的预取小麦/胡萝卜窗口可独立命中缓存；首个后备请求立即处理、冷却内快速切换只保留最后一项并在到期后处理。PASS。
- [x] 行情书打开/经济实际读取才经 `MarketService` 服务端惰性刷新市场；主世界 tick 不再调用市场服务。关闭书本后没有客户端市场缓存或新请求。PASS：代码审查、市场核心自检回归。
- [x] 成品 Jar 包含 `GuiMarketBook`、Packet、行情书 Item、模型、配方、语言、`mcmod.info` 与 `pack.mcmeta`。PASS：Jar 检查。
- [ ] 开发客户端实际进入世界后：获得物品、中文名、模型、无序配方、主/副手无限使用、默认小麦、14 项切换、今日/昨日/趋势、折线、悬停、30 天窗口、GUI Scale 和小窗口。NOT RUN：`runClient` 已实际进入 Forge/FML 与 coremod 发现，但本 WSL 环境在本模组加载/窗口创建前终止。
- [ ] 市场按需初始化、跨日重开、v6 存档首次访问迁移、多人和主世界/下界/末地一致性。NOT RUN：需要可进入测试世界；实现路径均经服务端主世界 `WorldSavedData` 解析，且市场没有后台 tick。
- [ ] 恶意包（无书本 Container、非法商品、负/超大/未来日期、快速切换、低于 2 tick 间隔、关书后的迟到回包）端到端。NOT RUN：需要已连接客户端；代码路径已验证 Container、边界、合并限速和会话缓存。
- [ ] Dedicated Server 完整加载、玩家连接、GUI 请求和无客户端类加载错误。NOT RUN：`runServer` 已实际进入 Forge/FML Server 与 coremod 阶段，但未完成模组加载；`run/eula.txt` 保持 `false`，未接受 EULA。

## 第 06、07 批附魔农业与工具战斗

- [x] `enchantmentSelfTest`：8 个 RARE、非宝藏附魔，等级、装备白名单、Harvest/Fortune 与 Slaughter/Sharpness/Smite/Bane/Looting 互斥，以及 Harvest、Fine Cultivation、Pastoral Favor 的 RNG 阈值。PASS。
- [x] `AgricultureRules` 把七种 Harvest 目标和 Pumpkin 排除、主作物/种子身份、Binomial/UniformInt/cocoa metadata 3 公式集中。PASS：代码审查和自检。
- [x] Fine Cultivation 只从最终 HarvestDrops 列表、再从玩家主背包/快捷栏消耗真实种植物，且只在方块仍为空、下方仍为 Farmland 时以默认 age 0 状态补种。PASS：代码审查和编译。
- [x] Felling 只经原版 `tryHarvestBlock` 逐块处理已验证的同树种原木与叶子；没有手工掉落、全局已访问树列表或递归。PASS：代码审查和编译。
- [x] Fleetfoot/Farmland Walker 使用服务器固定 UUID 的非持久 operation 2 属性修饰符；Night Vision 只在 `Side.CLIENT` 渲染帧临时 gamma，不使用 Potion 或改世界光照。PASS：代码审查、Forge audit 和编译。
- [ ] 游戏内：附魔台、附魔书、旧版 Librarian 和原版铁砧的八种附魔候选、max level、费用、同级合并。NOT RUN：客户端/Dedicated Server 未进入可测试世界。
- [ ] 游戏内：Harvest 七种目标/排除项、Fine Cultivation 四种作物与无种子失败、Pastoral Favor 收获/种植、Farmland Walker、Fleetfoot 叠加。NOT RUN：客户端/Dedicated Server 未进入可测试世界。
- [ ] 游戏内：六种自然树、木屋/原木墙拒绝、相邻树冠不串联、逐块耐久/Creative、Slaughter 白名单/排除/伤害/掉落/XP、Night Vision 本地恢复。NOT RUN：客户端/Dedicated Server 未进入可测试世界。
- [ ] Dedicated Server 完整加载且不加载 `client.ClientNightVisionRenderHandler` 或任一 `net.minecraft.client.*` common 引用。NOT RUN：本次 `runServer` 实际进入 Forge/FML/coremod 引导，但在本模组加载前终止且未到 EULA 检查；`run/eula.txt` 保持 `false`。静态审查确认客户端引用被隔离至 `client` 包与 `Side.CLIENT` 订阅器。

## 第 08～10 批村庄、商人和交易

### 静态/构建

- [x] `./gradlew compileJava` — PASS（2026-09-03，Temurin 8）
- [x] `./gradlew processResources` — PASS（2026-09-03）
- [x] `./gradlew compileTestJava` — PASS（2026-09-03，Temurin 8）
- [x] `./gradlew merchantCatalogSelfTest` — PASS（人口边界、普通 56 条、罕见 43 条、有限库存与唯一 key）
- [x] Forge 1.12.2 static audit — PASS（0 ERROR；严格模式的 4 条 packet-thread WARNING 已审查）
- [x] `./gradlew build` — PASS（2026-09-03，含 `test`、`reobfJar`）
- [x] 成品 Jar 检查 — PASS（`build/libs` 与 `release` 均含本批类和资源，不含测试类/现代 metadata）

### Village / Station / Merchant

- [x] 商人服装 PNG 结构检查：4 张内置服装均为 64×64、8-bit RGBA，并保留 Steve UV 画布；运行时皮肤池只接受 1--4，绝不引用原版 Steve 贴图。PASS：源码/资源检查。
- [x] `./gradlew merchantNameSelfTest`：确定性姓名池、UTF-8 往返、1/2 字名字长度、高频姓氏人口权重和皮肤索引迁移边界。PASS（2026-09-04，Temurin Java 8）。
- [ ] 新商人服务端获得按高频百家姓人口比例加权的中文姓名与四种农作服装之一；客户端不自行随机，GUI 标题与实体显示一致。NOT RUN：需要可进入世界。
- [ ] 旧商人无姓名/旧“商人”名称、缺少或 legacy 索引 0 的皮肤时只补生成一次；已有自定义姓名与现有 1--4 农作服装保持；Chunk unload/reload、退出重进、服务器重启后姓名/皮肤不变，UUID、交易、价格和库存不受影响。NOT RUN：需要旧存档与 Dedicated Server/世界。
- [ ] Overworld 旧 VillageCollection、2 名村民门槛、128/64/160 去重、保存重启不重复 — NOT RUN：需要可进入的世界
- [ ] 每有效村庄恰好一个安全站点、12 格搜索、不覆盖箱子/门/农田/TileEntity、异常恢复 — NOT RUN：需要可进入的世界
- [ ] 商人按 `max(ceil(villagers / 5),3)` 的目标人数、人口变化收敛、死亡补足、>32 格归位、无自然 despawn；生命 20、移速 0.1、水中上浮且无村民环境/受伤/死亡声音 — NOT RUN：需要可进入的世界
- [ ] 商人离站后最多每 20 tick 重算一次返航路径，仍能回到站点；高实体数下维护不会重复扫描 merchant 实体。NOT RUN：需要可进入的世界；代码审查确认单次索引扫描。
- [ ] 商人靠近玩家时面向最近的 8 格内玩家；打开有效交易窗口期间原地站住，关闭后恢复常规 AI；实际受该玩家伤害后避开其 200 tick。NOT RUN：需要可进入的世界。
- [x] 商人记录的可选最后实体区块字段可 NBT 往返，重复观测不重复标脏。PASS：`merchantCatalogSelfTest`。
- [ ] 旧存档/重进世界：世界加载期间不补生，第一次周期只等待实体进入；已加载的站点/最后实体区块均确认没有实体后才补生，传送至村庄不会短暂出现又消失的重复商人。NOT RUN：需要可进入世界和旧存档。
- [ ] MerchantRecord/Offer/库存重启、Chunk unload/reload 不复制或刷新 — NOT RUN：需要 Dedicated Server/世界

### Sell / Buy / GUI

- [ ] 6 收购栏同日稳定；非栏位商品拒绝；主背包/快捷栏数量和 16 色羊毛统计 — NOT RUN：需要可进入的世界
- [ ] 牛奶桶出售原子返空桶、满背包失败无副作用 — NOT RUN：需要可进入的世界
- [ ] 普通 4 + 罕见 3、稀有 2 + 珍宝 1、bundle 价格与 Metadata — NOT RUN：需要可进入的世界
- [ ] 有限库存多人共享、重启不恢复、跨日刷新 — NOT RUN：需要 Dedicated Server/多人
- [x] GUI 每栏数量加减、数量/预计总价显示；服务端数量上限与库存/容量复核 — PASS：代码审查、`compileJava`/`build`
- [x] 维护：商人 GUI 不暂停单人集成服务端；成功交易同步窗口 0 玩家背包、CoinService 金币缓存和所有打开的同商人快照。PASS：服务端路径代码审查、`compileJava`、`build`。
- [x] 维护：商人标签、交易卡、禁用数量输入和按钮使用原版 `GuiButton` 的正常浅色，点击仍在本地与服务端重新校验。PASS：`GuiMerchantTrade` 代码审查、`compileJava`、`build`。
- [ ] 游戏内：单人和多人连续买卖后，不关闭窗口即可立即看到背包、金币、价格/库存、持有量更新；交易窗口保持世界运行。NOT RUN：当前环境无法创建可操作的 Forge 客户端窗口。
- [ ] 背包完整容量模拟、溢出、重复 Packet、旧 GUI 跨日拒绝 — NOT RUN：需要客户端/服务器连接
- [ ] Dedicated Server 无客户端类加载错误 — NOT RUN：当前环境未接受 EULA；静态审查通过

## 第 11 批稀有、珍宝与特殊附魔书

### 静态/目录

- [x] Rare Pool 27 项、Treasure 普通 21 项 + 特殊附魔 12 项：唯一 key、Item/meta、bundle、价格、波动和库存校验 — PASS：`merchantCatalogSelfTest`
- [x] 12 张 1.12.2 唱片独立候选，所有基础价 5000、波动 8%、库存 1 — PASS：目录自检
- [x] 四种原版特殊附魔 + 八种模组附魔真实 Registry 引用、等级上限和权重总和 100% — PASS：目录自检与 `enchantmentSelfTest`
- [x] 1.12.2 legacy 映射：Skull metadata、Dragon Head、Enchanted Golden Apple、Dry Sponge、矿石方块 — PASS：目录定义与 ItemStack 自检

### DailyOffer / 交易

- [x] 每日购买结构 Common 4 + Uncommon 3 + Rare 2 + Treasure 1；旧占位迁移只补末尾三栏 — PASS：代码审查、编译
- [x] 多等级附魔先选类型后按权重解析，等级写入 `DailyOffer`，真实 Enchanted Book 输出 — PASS：目录自检、代码审查
- [x] Rare/Treasure 库存统一共享并复用原子购买、容量模拟、溢出与重复请求防护 — PASS：代码审查、`build`
- [x] schema v4→v5；重启/重登不重新 Roll 当日附魔等级 — PASS：NBT 字段与迁移路径审查
- [ ] Rare/Treasure 多人最后库存、服务器重启、跨日刷新、旧 GUI 拒绝 — NOT RUN：需要 Dedicated Server/多人世界

### 构建

- [x] `./gradlew compileJava` — PASS（Temurin 8）
- [x] `./gradlew processResources` — PASS
- [x] `./gradlew build` — PASS（含 `test`、`reobfJar`）
- [x] 成品 Jar 检查 — PASS：`build/libs` 与 `release` 均包含第 11 批生产类/资源，不含测试类或现代 metadata
- [ ] `runClient` / `runServer` — NOT RUN：客户端在 WSL 的 LWJGL `LinuxDisplay.getAvailableDisplayModes` 阶段退出；Dedicated Server 在 Forge/FML coremod 与 Srg→Mcp 阶段 90 秒超时（exit 124），未到模组加载/EULA 检查；`run/eula.txt` 保持 `eula=false`

## 第 12～13 批蟹笼、饵料与自动化

- [x] `crabTrapSelfTest`：五种生肉有效、四种鱼拒绝、100～600 ticks、Lure 重抽、饵料向上取整、Luck 根权重、20 槽、Hopper 面向规则、满仓预检和库存/pending NBT 往返。PASS（2026-09-03）。
- [x] `compileJava`、`processResources`、严格 Forge audit：PASS（0 ERROR；4 条为既有 Proxy/主线程桥接的 packet-thread WARNING）。
- [x] 资源处理目录含 `crab_trap` BlockState、Block/Item models、recipe、双语键和 `textures/blocks/crab_trap.png`。PASS。
- [ ] 游戏内基础方块：合成、放置、GUI、20 槽和破坏的本体/库存/pending loot。NOT RUN：待可进入世界。
- [ ] 无竿无饵、Lure I/II/III、Luck 0/I/II/III 的真实长样本、钓竿耐久和无经验球。NOT RUN：待可进入世界；等待/权重/loot table 调用由自检与代码审查覆盖。
- [ ] 五种肉、四种鱼拒绝、严格 ×0.50（奇数向上）、每成功结果约 50% 消耗。NOT RUN：待可进入世界；物品资格与取整自检通过。
- [ ] 水环境暂停/恢复、满仓 pending 同一结果、保存退出/重启和 Chunk unload/reload。NOT RUN：待可进入世界/服务器；TileEntity NBT 往返自检通过。
- [ ] 蟹笼运行中倒计时逐 tick 准确；非正常保存/退出最多回退 19 tick，且大量加载蟹笼不每 tick 标脏。NOT RUN：需要可进入服务器与保存检查；代码审查确认 20 tick 持久化节流。
- [ ] 上/侧 Hopper 仅入肉、下 Hopper 仅出 18 收获槽，以及完整箱子→Hopper→蟹笼→Hopper→箱子的长时间流水线。NOT RUN：待可进入世界；`ISidedInventory` 面向规则自检通过。
- [ ] 两玩家/Hopper 并发和 Dedicated Server 无客户端类加载错误。NOT RUN：`runServer` 实际进入 Forge/FML/coremod 引导与 mods 扫描，但在本模组加载前停止，未到 EULA/世界阶段；静态 audit 无客户端类泄漏。
- [ ] `runClient`：NOT RUN：实际进入 Forge/FML/coremod 引导与 mods 扫描，但本 WSL 环境在本模组加载和窗口创建前停止，无法手测模型或 GUI。

## 第 14 批交通节点基础

- [x] `transportCoreSelfTest`：费用 0/250/500/1000/2000/5000/10000 距离检查点、仅 X/Z 距离、极值坐标、24 code point 别名边界、v2 玩家 NBT/Clone、v6 全局节点 NBT 与 Packet 往返。PASS（2026-09-04）。
- [x] `playerDataSelfTest`、`pastoralWorldDataSelfTest`：PASS，覆盖 PlayerData v2 与 WorldSavedData v6 的基础兼容路径。
- [x] `compileJava`、`processResources`、`build` 与严格 Forge audit：PASS（0 ERROR；5 条已审查的 Packet 主线程桥接 WARNING）。
- [x] 成品 Jar：PASS，含 `transport_station` Block/Item/Tile/Packet/GUI 类及 blockstate、模型、配方、贴图、双语资源；不含测试类或现代 metadata。
- [ ] 游戏内：首次进入仅获得一次启动节点；背包满时实体掉落且不会重复；死亡、维度切换、重登与重启不重复。NOT RUN：待可进入世界。
- [ ] 游戏内：Overworld 放置、首个本人启动节点免费、首次普通节点 400、后续按距离费用、余额不足无副作用、重新激活收费。NOT RUN：待可进入世界。
- [ ] 游戏内：下界/末地提示不支持；最近有效节点排除已拆除节点；复制 Tile NBT 不复用 UUID；拆除后 ItemStack 不保存 UUID。NOT RUN：待可进入世界；NBT/服务端路径已代码审查。
- [ ] 游戏内：每玩家独立 home/home#2、自建别名 `#001`、24 code point 限制、空白/控制字符/`§`/重名拒绝；远程改名与移除只能作用于自身节点。NOT RUN：待客户端/服务器连接。
- [ ] 游戏内：两玩家并发激活/移除、保存重启、Dedicated Server 无客户端类加载错误。NOT RUN：`runClient`/`runServer` 均仅进入 Forge/FML coremod 引导；客户端未创建窗口，`run/eula.txt` 仍为 `false`，未接受 EULA。

## 第 15 批交通旅行与村庄解锁

### 静态/构建

- [x] `check_toolchain.py` — PASS（Forge 14.23.5.2859 / Gradle 4.9 / Temurin Java 8 `1.8.0_504`）。
- [x] `transportTravelSelfTest`：旅行费/接入费七个标准点、Y 忽略、VillageRecord 与共享 StationRecord NBT 身份往返。PASS（2026-09-04）。
- [x] `compileJava`、`processResources`、`compileTestJava`、`build`（含 `reobfJar`）：PASS。
- [x] Forge 1.12.2 strict audit：0 ERROR；5 条 packet-thread WARNING 均已审查为 Proxy/S2C 桥接，C2S 交通 Handler 调度服务端主线程。
- [x] 节点物理可用性分支复核：已加载 `VILLAGE` 节点检查 `VILLAGE_STATION`/`TileVillageStation`，自建节点检查 `TRANSPORT_STATION`/`TileTransportStation`；修正后编译与构建通过。
- [x] 正式 JAR 内容检查：PASS，`build/libs/LisBam_PastoralEconomy-0.1.0.jar` 与 `release/LisBam_PastoralEconomy-0.1.0.jar` 均为 2,538,070 bytes、SHA-256 `0968c04fb8c2b922ae1b8140f907fabff68490aa403b7570533f011b190bfeef`；包含村庄/交通类、GUI、网络、TileEntity、`mcmod.info`、`pack.mcmeta`、BlockState、模型、配方、语言与贴图；无测试类、`mods.toml` 或现代 API。

### 村庄/接入/旅行

- [ ] 1.12.2 原版 Village 结构搜索、最近未 Active 村庄、跳过已 Active、Removed 重新候选。NOT RUN：需要可进入世界；服务端有限 `findNearestStructure` 路径已代码审查。
- [ ] 村庄共享 Station UUID、多人不重复创建、按需创建失败不扣费、村庄站保护和损坏恢复。NOT RUN：需要 Dedicated Server/世界。
- [ ] 首村庄 D=0/400 且不消耗 Starter 自建免费资格；村庄接入后从任意 Active 节点旅行；自建/村庄四种方向。NOT RUN：需要可进入世界。
- [ ] Travel 请求服务端重算距离/费用、仅主世界、Source/Destination Active、Removed 拒绝、重复点击防护。NOT RUN：需要客户端/服务器连接；请求字段未包含权威坐标或价格。
- [ ] 安全落点：目标半径 5、Y -2..+4、脚下承载、脚/头碰撞空间、液体/熔岩/火/岩浆块/仙人掌拒绝；无点不扣款。NOT RUN：需要世界地形；`findSafeLanding` 有界代码路径已审查。
- [ ] 扣款后 `setPositionAndUpdate` 异常/位置失败全额退款，余额不足不移动不扣款。NOT RUN：需要可运行服务器；CoinService 事务路径已审查。
- [ ] 保存重启/旧第 14 批存档：Village Identity、Station UUID、Active/Removed、Alias、Starter 历史和费用保持。NOT RUN：需要 Dedicated Server/旧存档。
- [ ] `runClient`：NOT RUN；当前 WSL 在 Forge/FML/coremod 与显示初始化阶段无法创建可操作窗口。
- [ ] `runServer`：NOT RUN；实际进入 Forge/FML Server 与 coremod/Srg→Mcp 阶段后 90 秒超时安全终止，`run/eula.txt` 保持 `false`，未代为接受 EULA，未进入模组世界/玩家阶段；静态审计确认 common 无客户端类泄漏。

## 第 15 批后界面维护

- [x] 商人购买/出售页在 320x240 最小缩放下没有页签、数量控件或卡片越界；滑条、文本框和确认按钮的数量范围仍为 1--4096 且出售上限取玩家主背包/快捷栏持有量。PASS：布局/客户端状态代码审查与编译。
- [x] 商人最后选择的购买/出售页只保存在客户端内存，跨 Merchant GUI 复用，未增加 C2S 字段、NBT 或服务端写入。PASS：代码审查与编译。
- [x] 商人/行情书图标调用原版 `renderToolTip`；蟹笼继承 `GuiContainer` 原版 Slot 悬停提示。PASS：代码审查与编译。
- [x] 蟹笼只显示 2 个输入槽、18 个收获槽和原版玩家背包槽；不存在没有 Container Slot 的顶部空格，状态文字不与槽框重叠。PASS：Container/GUI 坐标审查与编译。
- [x] 交通站操作区在 320x240 最小缩放中不越界；节点文字截断后可悬停读取完整坐标/状态。PASS：布局代码审查与编译。
- [x] 商人出售页按 3 × 2 对称卡片排版；购买余额不足一整包或出售页背包无目标物品时，数量输入和滑条限制范围、确认按钮进入原版深色禁用态；当前出售/购买页签同样禁用，确认操作仍由客户端与服务端复核。PASS：客户端布局/数量上限代码审查与编译。
- [x] 交通节点快照往返保留 `travelFee`；目的地行、悬停提示和前往按钮显示费用，余额不足/路径不可用时前往按钮暗置。PASS：`TransportCoreSelfTest` Packet 往返、`build`。
- [x] `compileJava`、`processResources`、`build`（含 `test`、`reobfJar`）— PASS：2026-09-04 以 Corretto 11 JDK 兼容构建；项目 source/target Java 8，`GuiMerchantTrade.class` 为 major 52。当前环境无 Java 8 JDK，因此仍需用 Temurin 8 复验。
- [x] UTF-8 JavaCompile 编码、Forge 1.12.2 strict audit 0 ERROR、成品 JAR 的 GUI 类和 `mcmod.info` — PASS；5 条既有 `packet-thread` WARNING 已审查。
- [x] 每次 `build` 在 `reobfJar` 后自动执行 `exportReleaseJar`，并覆盖导出 `release/LisBam_PastoralEconomy-0.1.0.jar`。PASS：2026-09-04 构建日志、SHA-256 与 `unzip -t` 完整性检查。
- [x] UI 文字使用原版 `FontRenderer`；商人的静态交易文字与数量文字使用原版 `GuiButton` 正常浅色，而当前页签和不可成交确认按钮按原版 `GuiButton` 禁用状态显示深色。蟹笼 20 个实际槽和商人物品槽直接裁取原版 `generic_54.png`，蟹笼空白背景也取原版纹理像素；商人/交通面板每帧以单次完整 `demo_background.png` 裁取绘制，避免分片错位且没有手绘槽框/凸起边框。PASS：源码审查、`compileJava`、`build` 与 Forge audit。
- [x] 交通 GUI 的标签、节点、文本输入和不可用按钮文字与商人 GUI 一样使用原版 `GuiButton` 正常浅色，仍由原版禁用背景与 `enabled` 限制交互。PASS：源码审查、`compileJava`、`build`。
- [x] 蟹笼、玩家交通站与村庄交通站资源：交通站和蟹笼两张实际 PNG 都是 32×32，村庄模型不再引用 `planks_oak`；玩家交通方块与 ItemBlock 都从无后缀 `tile...transport_station` 显示键得到“交通方块”，不会显示 `.name`。行情书自有绿皮书 32×32 Item PNG；蟹笼配方为铁锭/铁栅栏交错外框和中央陷阱箱。PASS：源码、`processResources`、release JAR 检查。
- [x] 发行版本 `1.0`：`build.gradle`、处理后的 `mcmod.info` 和 JAR Manifest 的 Specification/Implementation Version 均为 `1.0`；重混淆文件导出为 `release/LisBam_PastoralEconomy-1.0.jar`。PASS：`processResources`、`build`、JAR 检查。
- [x] 购买有限库存显示实际剩余物品数量（剩余包数 × 每包数），而非包数；不限量保持显示不限量。PASS：`GuiMerchantTrade` 代码审查、双语资源处理与 `build`。
- [x] `merchantCatalogSelfTest`：村民 2/15 的最低 3 名、16/20/21/35/36 的 `ceil(n/5)` 边界及 100/1000 村民的无上限增长。PASS（2026-09-04，Temurin Java 8）。
- [x] 蟹笼：0/1 号 GUI Slot、TileEntity 直接写入和所有方向 Hopper 都拒绝错误物品；2～19 号槽允许任意物品；既有存档内错误功能槽物品保留且可取出。PASS：`crabTrapSelfTest`（2026-09-04）和最终 `build`。
- [x] GUI：蟹笼沿 `GuiContainer` 原版槽位悬停路径显示蟹笼和玩家背包物品 Tooltip；商人与交通全部静态非按钮文字直接使用 `FontRenderer.drawString(..., 4210752)`，不经带阴影的 `drawCenteredString`；交通节点列表鼠标位于列表区域时可逐行滚动，窄屏面板无重叠。PASS：源码审查、`compileJava`、Forge audit 和最终 `build`；实际游戏内鼠标/视觉验收仍见下方 NOT RUN 项。
- [x] 维护：交通“我的节点”标题位于第二行动按钮下方；接入最近村庄和移出节点均使用原版背景比例的本地二次确认层。移出只在确认后发送既有 `REMOVE`，服务端验证路径和 Packet 编码未改。PASS：`GuiTransportStation` 控制流/布局审查、`transportCoreSelfTest`、`transportTravelSelfTest`、`compileJava`。
- [x] 金闪闪的骨粉：稳定注册、模型、16×16 RGBA 图标、双语键和两份无序 JSON 配方齐全；中心作物至多 8 次原版骨粉、同层 5×5 范围及草/花草层路径的分类/边界正确。PASS：`goldenBoneMealSelfTest`、`compileJava`、`processResources`。
- [ ] 游戏内：金闪闪的骨粉在每类原版作物中心完全成熟、外围作物各一次骨粉、草方块/花 5×5 自然生成、创造模式不消耗、无效目标不消耗；320×240 与常规缩放下确认层无文字重影且“我的节点”不被按钮遮挡。NOT RUN：当前环境无法创建可操作 Forge 客户端窗口。
- [ ] 游戏内：在 320x240、常规 GUI Scale 和高分辨率下验证所有四种 GUI 的文字、按钮、滚动、物品 Tooltip、蟹笼点击/Shift-click、无虚假顶部格子、商人余额/背包不足深色禁用态和当前页签禁用、交通费用与余额暗置，以及商人连续切换后的默认页。NOT RUN：当前环境无法创建可操作的 Forge 客户端窗口。
- [ ] 游戏内：交通方块物品/方块名精确为“交通方块”、两种交通站显示简约石质罗盘贴图、蟹笼显示简约木框铁栅贴图、行情书为绿皮书，且蟹笼新配方正确；传送、重进和 Chunk unload/reload 后没有短暂重复商人。NOT RUN：当前环境无法创建可操作的 Forge 客户端窗口。
