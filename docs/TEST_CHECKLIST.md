# 测试清单

## 绿宝石现货市场与经济调整（2026-09-07）

- [x] `emeraldTradeRulesSelfTest`：4% 买入向上取整、卖出向下取整、最大可买数量和 long 边界均通过。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `emeraldMarketSelfTest`：五个离散日变动区间的端点、300～3,000 边界、同种子确定性、首次 1,000、跨日逐步推进、同日不重抽和 v8 缺字段迁移均通过。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `merchantCatalogSelfTest`：胡萝卜/马铃薯基础价 60、常规绿宝石目录/Offer 删除、绿宝石矿石保留、旧持久绿宝石 Offer 失效重建，以及扩展 Packet 4 快照编解码均通过。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `marketCoreSelfTest`：普通市场新基础价生效，已保存当日胡萝卜/马铃薯快照不重写，下一日昨日价承接旧保存值。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `pastoralWorldDataSelfTest`、`marketPacketSelfTest`、`modGuiInputSelfTest`、`compileJava`、`compileTestJava`、`processResources`：世界数据兼容、既有行情协议、数量输入和资源/Java 8 编译回归通过。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] Forge 1.12.2 `--strict-warnings` audit：0 ERROR、7 条既有 S2C/Proxy `packet-thread` 保守 WARNING；新增绿宝石 C2S handler 显式切回服务端主线程。PASS（警告已记录）。
- [x] 最终 Java 8 `./gradlew build --no-daemon --console=plain`：PASS（含 `test`、`reobfJar`、`exportReleaseJar`、`verifyReleaseJar`）；核验 256 个生产 class 均存在且为 Java 8。`release/LisBam_PastoralEconomy-1.6.jar` 为 531,118 bytes，SHA-256 `0ae27647b03a706a2d16218f7d13f64f6e2ff501bcac26f46b531dcc0ffadfab`，`unzip -t` PASS；该 1.6 目标在构建前不存在，所以未产生覆盖备份。
- [ ] 游戏内单人：打开商人第三页，确认同一组原版滑条/输入框控制买卖数量，显示今日/昨日/涨跌、持仓、上下限和预计手续费；分别测试余额不足、背包满、持仓不足、跨日、重登与时间回拨，确认没有掉落、复制、吞物或客户端价格结算。NOT RUN：当前环境没有可操作 Forge 客户端世界。
- [ ] Dedicated Server：以 1.6 客户端连接，验证 Packet 4 扩展和 Packet 11 的会话、距离、重复 requestId、余额、背包容量、持仓与多玩家同步均由服务器拒绝/结算。NOT RUN：当前环境没有可进入的 Dedicated Server 世界，且现有开发服务器 EULA 未接受。

## 多 Coremod 环境延迟类加载回归（2026-09-07）

- [x] 崩溃定位：LaunchWrapper 1.12 的源码/字节码确认 `findClass:182` 为 `defineClass(... transformedClass.length ...)`；日志中的底层 NPE 表明转换结果为 `null`，不是蟹笼 tick 或农业方法主动抛错。PASS。
- [x] 历史成品核验：发生崩溃时对应的 497,424-byte 备份和当前 release 均包含非空 `AgricultureRules$Crop.class`、`ContainerCrabTrap.class`、`CrabTrapRules.class`，`unzip -t` 无错误且 class major 为 52。PASS。
- [x] `shoulderEquipmentSelfTest`：Coremod 注解包含完整 `lisbam.pastoraleconomy` 根包；独立 LaunchClassLoader 真实延迟加载上述三个 class；肩部原版目标和既有同步包回归保持。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `crabTrapSelfTest`：蟹笼规则、功能槽、20 槽库存和 NBT 回归保持。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] Forge 1.12.2 常规 audit：0 ERROR、7 条既有 `packet-thread` 保守 WARNING。PASS。
- [x] 最终 Java 8 `./gradlew build --no-daemon --console=plain`：PASS（含 `test`、`reobfJar`、`exportReleaseJar`、`verifyReleaseJar`）；逐项核验 243 个生产 class 均存在、非空、可读且 major 52。release JAR 为 503,927 bytes，SHA-256 `cb76eafbe1d45d4ef561b74cefb2bdef7a60705e9cf6488fcdf5830d0e742527`，`unzip -t` PASS；旧 503,932-byte JAR 已备份为 `release/backup/backup_20260907-090138.jar`。
- [ ] Dedicated Server 世界：带显式 Coremod 的 Java 8 `runServer` 在 120 秒内成功发现并入队本模组 Coremod，根包隔离装载无错误；超时前仍停在开发环境映射载入，未进入模组生命周期/世界，完整验收 NOT RUN。
- [ ] Windows 多 Coremod 客户端：使用唯一一份新 JAR 完全重启游戏后，验证种植、打开蟹笼、蟹笼 tick/保存均不再出现 `AgricultureRules$Crop`、`ContainerCrabTrap`、`CrabTrapRules` 的 `NoClassDefFoundError`。NOT RUN：当前环境没有用户所列的完整 Windows 客户端/Coremod 组合。

## 作物收获时序、锄头耐久与多人同步回归（2026-09-06）

- [x] Forge 1.12.2 字节码/源码核对：确认 `tryHarvestBlock` 在 BreakEvent 前发送破坏者 AIR 包，成功移除后才进入 `Block#harvestBlock` 与 `HarvestDropsEvent`；补种和工具损耗均已移出该原版掉落调用。PASS（Forge `14.23.5.2859` mapped JAR）。
- [x] `toolDurabilitySelfTest`：零硬度小麦/下界疣且使用非创造原版锄头时登记补扣；创造、斧头、非作物和非零硬度西瓜排除；无耐久附魔的确认收获恰好损失 1 点。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `compileJava`、`compileTestJava`、`enchantmentSelfTest`、`goldenBoneMealSelfTest`：事件签名、既有农业附魔和作物分类回归通过。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] Forge 1.12.2 常规 audit：0 ERROR、7 条既有 `packet-thread` 保守 WARNING。PASS（常规模式）。
- [ ] Forge 1.12.2 `--strict-warnings` audit：NOT PASS；仅 7 条既有 `packet-thread` warning，命令返回 2，未掩盖结果。
- [x] 最终 Java 8 `./gradlew build --no-daemon --console=plain`：PASS（含 `reobfJar`、`exportReleaseJar`）。release JAR 为 503,932 bytes，SHA-256 `860ae85ab4c3099b246d687b8bde7d597383d1a3c39f145994d92c1dce38ffea`，`unzip -t` PASS，`HoeCropDurabilityEventHandler.class` 为 Java 8 major version 52；旧 JAR 已备份为 `release/backup/backup_20260906-225134.jar`。
- [ ] Dedicated Server：`runServer` 在 120 秒内以 Java 8 发现并入队本模组 Coremod，未报告本次 common class 加载错误；超时前未进入模组生命周期/世界，`eula=false`，完整验收 NOT RUN。
- [ ] 游戏内单人：分别收获成熟/未成熟小麦、胡萝卜、马铃薯、甜菜、下界疣和可可，确认掉落正常；普通锄头每次成功零硬度收获损失 1 点，耐久附魔逐次判定，非零硬度目标不双扣；精耕补种固定为 age 0 且可继续正常收获。NOT RUN：当前环境没有可操作 Forge 客户端世界。
- [ ] 游戏内多人：两名玩家同时观察同一耕地，确认成熟作物破坏、AIR、手动新苗和精耕 age 0 新苗最终状态一致；新苗对其他玩家立即可见，不出现瞬间成熟、无掉落或只能消失的幽灵作物。NOT RUN：当前环境没有可进入的双客户端 Dedicated Server 世界。

## 伐木耐久下限与锄头作物耐久（2026-09-06）

- [x] `toolDurabilitySelfTest`：剩余 1 点时伐木不启动；剩余 2 点只预留触发原木和最终 1 点；剩余 3 点才允许一个二级原木；零硬度小麦/下界疣补扣锄头耐久，非零硬度西瓜与非作物草方块不重复扣除。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `enchantmentSelfTest`、`compileJava`、`compileTestJava`：既有附魔与农业编译回归通过。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `goldenBoneMealSelfTest`：既有作物分类与 5×5 农业规则回归通过。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `check_toolchain.py`：Forge `14.23.5.2859`、Gradle 4.9、snapshot `20171003-1.12`、Java `1.8.0_504`，0 error/0 warning。PASS。
- [x] Forge 1.12.2 常规 audit：0 ERROR、7 条既有 `packet-thread` 保守 WARNING。PASS（常规模式）。
- [ ] Forge 1.12.2 `--strict-warnings` audit：NOT PASS；7 条既有 `packet-thread` warning 使该命令以非零退出，未掩盖结果。
- [x] 最终 `./gradlew build --console=plain`：PASS（含 `reobfJar`、`exportReleaseJar`）。release JAR 为 497,424 bytes，SHA-256 `7a772a6d1d1db9446f65cf4be6a5e1d9d52ee85ea21cd4fe35e07a64e37af554`，`unzip -t` PASS，`FellingDurabilityRules.class` 为 Java 8 major version 52；覆盖前 JAR 已备份为 `release/backup/backup_20260906-222214.jar`。
- [ ] 游戏内：分别以剩余 1/2/3 点的伐木斧砍正常树，确认 1 点不触发、每次完成后至少保留 1 点，树叶掉落正常且不耗耐久；以带/不带耐久附魔的锄头收获零硬度小麦、胡萝卜、马铃薯、甜菜、下界疣和可可，确认成功收获各按原版耐久附魔规则消耗一次，南瓜/西瓜不双扣。NOT RUN：当前环境没有可操作 Forge 客户端或可进入的 Dedicated Server 世界。

## 肩部羽毛翅膀与田园眷顾调值（2026-09-06）

- [x] `enchantmentSelfTest`：田园眷顾 I--IV 概率为 20/40/60/80，80 的阈值边界不重复成功。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `featherWingsSelfTest`：最大耐久 500、40 tick/点、肩部白名单、羽毛材料、5 点/根修复、耐久/经验修补/两种诅咒入口、双倍步行 exhaustion 与瞬移保护均通过。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `shoulderEquipmentSelfTest`：Packet 9 对羽毛翅膀保留 ItemStack 耐久，原有鞘翅 Coremod MCP/SRG/发行混淆注入仍通过。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `merchantCatalogSelfTest`：珍宝池为 39 项，羽毛翅膀实际 Item、1200000 基础价、8% 波动及库存 1 通过。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `check_toolchain.py`：Forge `14.23.5.2859`、Gradle 4.9、snapshot `20171003-1.12`、Java `1.8.0_504`，0 error/0 warning。PASS。
- [x] Forge 1.12.2 常规 audit：0 ERROR、7 条既有 `packet-thread` 保守 WARNING。PASS（常规模式）。
- [ ] Forge 1.12.2 `--strict-warnings` audit：NOT PASS；7 条既有 `packet-thread` warning 使该命令以非零退出，未掩盖结果。
- [x] 最终 `./gradlew build --console=plain`：PASS（含 `reobfJar`、`exportReleaseJar`）。release JAR 为 495,524 bytes，SHA-256 `4f394f45aeffa05fc90759ea7a5ca410af17750d1d5882f33caf40c03969241f`，`unzip -t` PASS，`ItemFeatherWings.class` 为 Java 8 major version 52；覆盖前 JAR 已备份为 `release/backup/backup_20260906-210434.jar`。
- [ ] 游戏内：生存/冒险双击跳跃起飞、卸下/耐久耗尽后立即失去飞行；连续 40 tick 耐久（含耐久附魔）、受伤不掉耐久、按飞行距离两倍饥饿、羽毛 1% 修复、两翅膀合并、四种附魔以及本人/追踪者羽翼模型。NOT RUN：当前环境没有可操作 Forge 客户端或可进入的 Dedicated Server 世界。

## 维护：输入框、丰收斧头、田园眷顾与凭证箱加载（2026-09-06）

- [x] `modGuiInputSelfTest`：Esc 在输入焦点中仍关闭；改键背包键在无焦点时关闭、在焦点中不关闭。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `enchantmentSelfTest`：斧头能应用 Harvest 但附魔台拒绝其候选；斧头被视为作物 Harvest 工具；田园眷顾 I--IV 随后维护为 20/40/60/80%，IV 的 79/80 边界正确。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `tradeVoucherSelfTest`、`pastoralWorldDataSelfTest`：既有多库存事务回归通过；新增凭证箱位置的跨维度、精确 BlockPos、去重和 NBT 往返通过。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `compileJava`、`compileTestJava`、`processResources`：PASS（Temurin Java 8 `1.8.0_504`）。Forge 1.12.2 strict audit 为 0 ERROR、7 条既有 `packet-thread` 保守 WARNING；严格模式因 Warning 返回非零，未掩盖该结果。
- [x] 最终 `./gradlew build --no-daemon --console=plain`：PASS（Temurin Java 8 `1.8.0_504`；含 `test`、`reobfJar`、`exportReleaseJar`）。release JAR 为 482,145 bytes，SHA-256 `17249a71b23a4ee63064273221756b99922bea8fe87273a9265b2bdefff21841`，`unzip -t` PASS，`VoucherChestRegistry.class` 为 Java 8 major version 52；覆盖前 472,033-byte JAR 已备份为 `release/backup/backup_20260906-195947.jar`。
- [ ] 游戏内 GUI/附魔：在行情书搜索、商人数量和交通取名框键入 E/改键；确认文字进入输入框而不关窗，Esc 仍关窗。用铁砧/书本把丰收施加到斧头后收获成熟作物；确认附魔台斧头候选没有丰收。各进行足量种植/收获，确认田园眷顾 I--IV 的概率序列为 20/40/60/80%。NOT RUN：当前环境没有可操作 Forge 客户端。
- [ ] 游戏内凭证箱/重启：关闭含绑定凭证的单箱、跨区块大箱及不同维度箱子后离开其区块、退出并重启服务器；确认所有登记箱仍计入出售。移走凭证、破坏箱子、上锁箱和未展开战利品箱后确认不会继续加载或参与；旧存档先重新打开/关闭一次凭证箱后确认登记。NOT RUN：60 秒 JDK 8 `runServer` 已进入 Forge/FML/coremod 引导，但未到本模组生命周期/世界且未接受 EULA，无法进入 Dedicated Server 世界。

## 维护：凭证配方、死亡金币、价格回归与旧物品移除（2026-09-06）

- [x] `deathCoinLossSelfTest`：10%/30% 边界、随机 1000--2000 最低扣款、余额不足时全额封顶和零余额不取随机数均通过。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `animalBoneDropSelfTest`：大体型 `40/300` 出 2、`100/300` 出 1，小体型 `50/300` 出 1，及既有抢夺加成边界均通过。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `marketCoreSelfTest`：青金石/荧石粉/下界石英基础价为 800/200/1600；预调价旧快照当日保持 1600、下一日逐步回归；默认链式和可选稳定模式均不跳到新价格带边缘。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `tradeVoucherSelfTest`、`transportCoreSelfTest`、`shoulderEquipmentSelfTest`：凭证既有授权交易、移除后交通核心，以及肩部 Coremod 回归均通过。`processResources` 已处理新的凭证 JSON 配方；release 内容确认含该配方、不含已删除物品资源。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `compileJava`、`compileTestJava`、`processResources` 与最终 `./gradlew build --no-daemon --console=plain`：PASS（Temurin Java 8 `1.8.0_504`；含 `test`、`reobfJar`、`exportReleaseJar`）。
- [x] Forge 1.12.2 strict audit：0 ERROR、7 条既有保守 `packet-thread` WARNING；新增死亡事件只使用逻辑服务端 `CoinService`，无 common 客户端类引用。PASS。
- [x] 发行产物：`unzip -t` PASS；`DeathCoinLossEventHandler.class` 为 Java 8 major version 52；`release/LisBam_PastoralEconomy-1.5.jar` 为 472,033 bytes、SHA-256 `03eda8154b6c84e014271ed6f529cb2875def0b23e34ada022cea0238d4e260e`。覆盖前的 502,564-byte 成品已备份为 `release/backup/backup_20260906-192806.jar`。
- [ ] 游戏内/多人：验证凭证八金粒配方、两种背包模式的肩部槽都随当前资源包使用一致原版盔甲槽外观、死亡聊天红字/10%--30%和 1000--2000 最低扣款、动物骨头概率以及旧世界加载后已删除物品消失。NOT RUN：当前环境没有可操作 Forge 客户端或可进入世界的 Dedicated Server。

## 新内容：三档肩部背包（2026-09-06）

- [x] `backpackSelfTest`：普通/高级/超级容量为 27/54/108，页数为 1/2/4，最大堆叠均为 1；第 0/53/107 槽 NBT 往返，背包套娃不写入，带物品普通背包不能用于高级配方。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `shoulderEquipmentSelfTest`、`playerDataSelfTest`：肩部 Coremod 保持原有 MCP/SRG/发行混淆注入；Packet 9 往返保存超级背包第 107 槽内容；PlayerData 接受背包并仍拒绝普通胸甲。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `merchantCatalogSelfTest`：珍宝池由 37 增至 38 项，超级背包实际 Item、200000 基础价、8% 波动和库存 1 均通过。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `compileJava`、`compileTestJava`、`processResources`：PASS；三张物品 PNG 均为 16×16、8-bit RGBA 且透明边角，普通 JSON 配方与高级代码配方均进入资源/类输出。
- [x] Forge 1.12.2 static audit：0 ERROR、7 条既有 `packet-thread` 保守 WARNING；新增打开背包 C2S Handler 可见地调度服务端主线程，客户端类只位于 client 包。PASS。
- [ ] 游戏内：普通/高级/超级的手持右键与 Shift-click 穿戴、肩部互斥、无玩家模型、肩部右键开包、1/2/4 页、拖放/Shift-click、Tooltip、Esc/E 关窗、换装/重登/死亡/换维度持久化和禁止套娃。NOT RUN：当前环境没有可操作 Forge 客户端。
- [ ] Dedicated Server 引导：Temurin Java 8 下 `timeout 180s ./gradlew runServer` 已进入 Forge/FML Server 并到达 `eula=false` 的 Mojang EULA 提示；没有接受 EULA，配置值仍为 `false`（服务器刷新了其中的时间戳）。模组生命周期、世界、恶意 Packet 10、开包后服务端替换背包、越界翻页及多人 NBT 可见性仍为 NOT RUN：需要可进入世界的 Dedicated Server 与客户端。
- [x] 最终 `./gradlew build`：PASS（Temurin Java 8 `1.8.0_504`，含 `reobfJar`、`exportReleaseJar`）。`release/LisBam_PastoralEconomy-1.5.jar` 已实际覆盖导出为 502,564 bytes，SHA-256 `7be4f580e2088e6f60078550b31832edddcfed5d8ff3a767d9886c516a2acf7e`；`unzip -t` PASS，`ItemBackpack.class` major version 52，并确认含新增类、模型、普通配方与三张贴图。覆盖前 454,587-byte 旧 JAR 已备份为 `release/backup/backup_20260906-185437.jar`。

## 交易凭证与箱子出售（2026-09-06）

- [x] `tradeVoucherSelfTest`：空白凭证只绑定一次，UUID 为授权依据、名字/NBT 可复制保存、他人凭证不授权；玩家主物品栏与箱子库存合并计数、玩家优先扣货、多来源回滚和空桶返还通过；两张贴图均为可读 16×16 RGBA 且边角透明。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `merchantCatalogSelfTest`：既有 6+8 商人快照协议往返保持，并验证出售项的既有 `remainingItems` 字段可携带授权箱数量。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `compileJava`、`compileTestJava`、`processResources`、`marketPacketSelfTest` 与最终 `build`：PASS（Temurin Java 8 `1.8.0_504`；正式构建含 `test`、`reobfJar`、`exportReleaseJar`）。严格 Forge 1.12.2 audit 为 0 ERROR、7 条既有 `packet-thread` WARNING；本次无新 Handler。release JAR 为 479,868 bytes，SHA-256 `4405eeb39dc73afe9306d9e60eaf577f72903663ad53e9ce61f345cbd87879bd`，`unzip -t` PASS、class major version 52，并包含新增生产类、模型、两张贴图及双语资源；旧 463,258-byte JAR 已备份为 `backup_20260906-141853.jar`。
- [ ] Dedicated Server 完整加载：180 秒实测已由 Java 8 启动 Forge、发现/注入/运行本模组 Coremod并到达 Minecraft 校验，无新增 common/client 类加载异常；但未到模组生命周期或世界，故完整验收 NOT RUN。`run/eula.txt` 未修改。
- [ ] 游戏内单人/多人：空凭证主/副手右键改名且只绑定一次；放入单箱/大箱后本人可出售“随身+箱内”数量，其他玩家不可用；多个已加载维度、区块卸载/重载、上锁箱、未开奖战利品箱、箱子被破坏和玩家改名行为符合内容书。NOT RUN：当前环境没有可操作 Forge 客户端/测试世界。
- [ ] 游戏内事务：随身货物先消耗、多个箱子稳定扣除；牛奶桶出售能返还全部空桶，空间不足或金币上溢时玩家/所有箱子都原样回滚。NOT RUN：需要可操作客户端与服务器世界。

## 肩部槽 UI、右键穿戴与模型显示（2026-09-06）

- [x] 静态/自检：肩部 Coremod 对实际映射 `LayerElytra` 及模拟发行混淆名均注入肩部读取；Packet 9 的实体 ID、鞘翅物品、损伤值往返一致。PASS（Temurin Java 8 `1.8.0_504`，`shoulderEquipmentSelfTest`）。
- [ ] 游戏内生存模式：肩部槽使用原版槽框并位于副手正上方；主手/副手持鞘翅右键均立即穿上并消耗手持物，肩部已有鞘翅时不改动胸甲或库存。NOT RUN：当前环境没有可操作 Forge 客户端窗口。
- [ ] 游戏内创造模式：仅“生存物品栏”页显示肩部槽，位于该页副手正上方且不与快捷栏、盔甲或人物预览重叠；右键穿戴后手持鞘翅仍保留。NOT RUN：当前环境没有可操作 Forge 客户端窗口。
- [ ] 游戏内渲染/多人：胸甲和肩部鞘翅同时装备时两者模型都显示；本人第三人称及另一名追踪玩家均可见鞘翅，飞行耐久变化与损坏后消失同步正确。NOT RUN：客户端不可操作，Dedicated Server 未完成模组生命周期。

## 构建

- [x] `./gradlew compileJava`、`compileTestJava` — PASS（2026-09-06，Temurin Java 8 `1.8.0_504`）
- [x] `./gradlew processResources`、`build` — PASS（2026-09-06，Temurin Java 8 `1.8.0_504`；含 `test`、`reobfJar`、`exportReleaseJar`；release JAR 为 463,258 bytes，`unzip -t` PASS，SHA-256 `5796b3dc4a9b6f47a1f09f6008824ada318ff30e3d14249039a630f519e557fe`）
- [x] Forge 1.12.2 static audit — 0 ERROR；7 条 `packet-thread` WARNING 已审查（通用注册不处理消息；六个 S2C Proxy 桥没有直接修改状态，客户端实际写入均调度至主线程；交通与市场 Tooltip C2S handler 调度至服务端主线程）。
- [x] 本次 FOV/Tooltip 修复后的 `./gradlew compileJava processResources build` — PASS（Temurin Java 8 `1.8.0_504`；release JAR 已覆盖导出并通过 `unzip -t`）。

## 紧急修复：客户端容器窗口号与不暂停界面（2026-09-06）

- [x] `modGuiInputSelfTest`：Esc 与改键后的背包键进入 `EntityPlayerSP#closeScreen`；行情书、商人和交通 GUI 均继承 `GuiContainer`，会在 Forge 分配模组窗口号前安装独立客户端 Container。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] 源码与 Forge 1.12.2 生命周期审查：三个自定义 `initGui` 首先调用 `GuiContainer#initGui`；商人/交通客户端标记构造器不具有权威 UUID且服务端工厂不使用；交通 GUI 创建不依赖客户端 TileEntity 同步先后。PASS。
- [x] 行情书、商人、交通主界面与交通 `GuiYesNo` 确认层均为 `doesGuiPauseGame=false`；common 侧没有新增客户端类引用。PASS：源码审查、`compileJava`。
- [x] `marketPacketSelfTest`、`merchantCatalogSelfTest`、`transportCoreSelfTest`、`transportTravelSelfTest`、`goldenBoneMealSelfTest`：相关协议、目录、交通结算和骨粉规则回归通过。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] Forge 1.12.2 strict audit：0 ERROR；6 条既有 `packet-thread` 保守 WARNING 已复核，均不是本次生命周期改动引入。PASS（2026-09-06）。
- [x] `./gradlew compileJava processResources build --no-daemon --console=plain`：PASS（Temurin Java 8 `1.8.0_504`；含 `test`、`reobfJar`、`exportReleaseJar`）。`release/LisBam_PastoralEconomy-1.5.jar` 已覆盖导出，406,463 bytes，SHA-256 `fc2f462ea631113b77272b3edfa706550cb165d921026892402f60e96db4ad44`，`unzip -t` PASS，并包含本次 GUI/Container 类。
- [ ] 游戏内单人/多人：依次打开行情书、商人、交通站和交通确认层；保持界面打开时确认世界、实体及服务器请求继续 tick；分别用 Esc/E 关闭后立刻拾取物品、使用普通/金闪闪骨粉、移动背包物品并完成四格合成，关闭背包时合成格物品正常返回。NOT RUN：当前环境无法创建可操作 Forge 客户端或进入 Dedicated Server 世界。

## 前序修复：自定义 GUI 的 Esc 关闭（2026-09-06）

- [x] `modGuiInputSelfTest`：Esc 与改键后的背包键均进入 `EntityPlayerSP#closeScreen` 路径；无关键不关闭界面。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `marketPacketSelfTest`、`merchantCatalogSelfTest`：行情书与商人既有服务端会话/包边界回归通过。PASS（Temurin Java 8 `1.8.0_504`；商人自检有 12 条既有 Forge alternative-prefix 警告，任务成功）。
- [x] `transportCoreSelfTest`、`transportTravelSelfTest`：交通节点、NBT、同步包及冻结费用检查点通过；`TransportCoreSelfTest` 已从废弃十倍价格断言同步为运行时 `round20(160 + 0.48D)` / `round10(400 + 1.20D)` 数值。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] Forge 1.12.2 strict audit：0 ERROR；6 条既有 `packet-thread` 保守 WARNING 已复核为通用注册或 Proxy 主线程桥接。PASS（2026-09-06）。
- [x] `./gradlew compileJava processResources build`：PASS（Temurin Java 8 `1.8.0_504`；含 `test`、`reobfJar`、`exportReleaseJar`）。`release/LisBam_PastoralEconomy-1.5.jar` 已覆盖导出，406,009 bytes，SHA-256 `d0b1d35a707fb03eb6489bc3d9ebbb6d1a7f61b1dd707b4939124223b276138d`，`unzip -t` PASS。
- [ ] 游戏内单人/多人：分别以 Esc 和默认/改键后的背包键关闭行情书、商人、交通站及交通确认层；随后拾取物品、使用骨粉、打开背包、四格合成并关闭背包，确认服务器已回到玩家背包 Container、合成格物品返回背包且商人 `endTrading` 正常执行。NOT RUN：当前环境无法创建可操作 Forge 客户端或进入 Dedicated Server 世界。

## 维护：行情 Tooltip、附魔与前往费（2026-09-05）

- [x] `marketCoreSelfTest`：29 类可出售 Item/meta 可反查唯一市场条目；任意羊毛色归入共享羊毛行情；不可出售物品不产生提示价格。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `marketPacketSelfTest`：新增 Packet 7/8 的有界 key、市场日与价格往返；Tooltip 缓存不会跨市场日复用，缺失价格每商品最多每秒请求一次。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `enchantmentSelfTest`：疾步只接受护腿、不接受靴子；范围 I/V 精确为 +1.5/+7.5 格。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `transportTravelSelfTest`：D=0/250/500/1,000/2,000/5,000/10,000 的旅行费为 160/280/400/640/1,120/2,560/4,960，接入费与 Y 忽略规则不变。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `compileJava`、`processResources`、最终 `build` 与严格 Forge audit：PASS；audit 为 0 ERROR、6 条 packet-thread WARNING，新增 S2C Handler 仅经 `ClientProxy` 调度客户端主线程，C2S Handler 先调度服务端主线程。release `1.5` JAR 为 396,136 bytes，SHA-256 `1f1f106150703828330c65c3c5bca2bb99e153f617d17acca38e8d03ec384e6e`，`unzip -t` PASS。
- [ ] 游戏内：320×240、常规和高分辨率下行情书基础价/昨日价/趋势/图表无重叠；原版库存 Tooltip 中小麦与任意颜色羊毛显示“今日价：xxx金币”，非收购物品不显示，跨日后不会显示旧价。NOT RUN：当前环境无法创建可操作 Forge 客户端。
- [ ] 游戏内：疾步护腿 I--IV 直接提高移速且穿戴、跳跃、落地均完全不改变 FOV，靴子疾步无效；夜视头盔戴上立即进入 10 秒视觉窗口并每 5 秒续 10 秒，摘下后 gamma 恢复且没有药水图标；范围 I--V 的实体攻击、方块和实体交互均按 +1.5 格/级且 Dedicated Server 无客户端类加载错误。NOT RUN：需要可操作客户端和 Dedicated Server。

## 维护：商人范围、命名牌与交通费用（2026-09-05）

- [ ] `merchantCatalogSelfTest`：断言已回退为村庄维护 200 tick（10 秒），商人活动半径为 32 格、传送阈值为 64 格。NOT RUN：按本次请求不构建、不测试。
- [x] `transportTravelSelfTest`：旅行/接入费的 0、250、500、1,000、2,000、5,000、10,000 格检查点分别为旧值 1/10，Y 高度仍不参与收费。PASS。
- [x] 源码审查：商人手持原版命名牌时返回 1.12.2 `EntityLivingBase` 原生交互；该路径负责已命名校验、`CustomName` 写入和命名牌消耗，常规右键交易不变。PASS。
- [ ] 游戏内：商人仅在村庄中心 32 格圆内出生/活动，33--64 格能寻路返航，超过 64 格传送到村庄站旁；已命名命名牌改名并在重登后保留，空白命名牌不打开交易。NOT RUN：需要可进入世界和 Dedicated Server。

## 维护：商人交易快照与行情书拼音检索（2026-09-05）

- [x] `merchantCatalogSelfTest`：将一份含当前 6 个出售槽和 8 个购买槽的禁用交易快照写入并读回；客户端解码结果完整，不会再因旧 10 槽常量让所有交易项显示“后续内容”。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `pinyinSearchSelfTest`：小麦的 `xiaomai`/`xm`、附魔书的 `fumoshu`、护田行者的 `htxz`、绿宝石的 `lvbaoshi`、带 `ü` 的输入以及不匹配的拼音均通过；全拼、首字母和混合拼音均只在本地过滤。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] Forge 1.12.2 static audit：0 ERROR；5 条既有 `packet-thread` WARNING 已审查为网络注册/Proxy 到客户端主线程的保守提示，本轮没有增加 common 客户端引用或网络处理器。PASS（2026-09-05）。
- [x] `./gradlew clean processResources build` 后再次 `build`：PASS（Temurin Java 8 `1.8.0_504`；含 `test`、`reobfJar`、`exportReleaseJar`）。`release/LisBam_PastoralEconomy-1.0.jar` 已覆盖导出，364,651 bytes，SHA-256 `b88b3d48a83cdace34d0e4a2ff3ebebbd24d8de45c04ea64de1045821f26dd76`，`unzip -t` PASS。
- [ ] 游戏内：以一个既有村庄商人打开交易，确认 6 个收购栏及 8 个出售栏均显示真实快照；在行情书购买页用全拼、首字母、`lvbs` 等混合拼音、原始中文名和 key 搜索，并确认滚动、缓存加载和附魔书 Tooltip 不回归。NOT RUN：当前环境无法创建可操作 Forge 客户端或进入 Dedicated Server 世界。

## 维护：行情书目录、创造标签与村庄商人范围（2026-09-05）

- [x] 本模组创造标签本地化键 `itemGroup.lisbam_pastoral_economy` 为“聆竹の休闲田园经济”。PASS：`processResources` 与语言文件审查。
- [x] `marketCoreSelfTest`、`marketPacketSelfTest`：30 日市场历史、每项有界 Packet、选中窗口与独立低优先缓存窗口通过。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] `merchantCatalogSelfTest`：29 条收购商品包含七种肉类，基础单价为牛肉/猪排/羊肉 120、鸡肉/鳕鱼 80、兔肉 140、鲑鱼 100；所有商人购买目录商品标记为行情书历史商品。PASS（Temurin Java 8 `1.8.0_504`）。
- [x] 源码审查：购买页只创建可见 3 列图标格，名称/key 搜索和滚轮按行滚动不改变服务器价格；选中请求超时 40 tick 重试，预取仍为 4 tick 一项、每个合法服务端请求直接生成有界快照。商人出生点在 32 格圆内，实体保存村庄中心并以该半径限制/寻路返回，超过 64 格时传送至村庄站旁。PASS：`compileJava`。
- [ ] 游戏内：320×240、常规和高分辨率 GUI Scale 下，购买页搜索、滚轮、快速切换、请求重试、附魔书 tooltip 的具体附魔/等级和曲线无越界/重叠；刷怪蛋商人收编、32/64 格返航/传送和保存重载。NOT RUN：当前环境无法创建可操作 Forge 客户端或进入 Dedicated Server 世界。

## 维护：链式市场价格边界与动态回归

- [x] 默认链式市场按类别最低/最高倍率 clamp；偏离基础价越远回归概率为 50%/55%/65%/75%/85%；最低/最高边界下一日强制反弹，取整后保留方向。PASS：`marketCoreSelfTest`。
- [x] `moreStableMarketVolatility=true` 仍调用独立公式但使用新基础价和上下限；配置切换不重算同日快照。PASS：代码审查、市场核心/世界数据自检。
- [ ] 游戏内跨日和旧存档 1 金币快照的实际曲线观察。NOT RUN：当前环境无法创建可操作 Forge 客户端或进入 Dedicated Server 世界。

## 维护：链式市场与界面关闭

- [x] `marketCoreSelfTest`：新市场首日为基础价；高/低于基础价时回归方向概率并非强制；连续链式上行可越过旧 135% 上限；最近 30 日裁剪、保存重载、回拨及 v6→v7 读取路径通过。PASS（2026-09-04，Temurin Java 8 `1.8.0_504`）。
- [x] `marketPacketSelfTest`、`transportCoreSelfTest`、`transportTravelSelfTest`：行情协议边界/缓存、交通费用和服务端确认路径保持。PASS（2026-09-04，Temurin Java 8 `1.8.0_504`）。
- [x] Forge 1.12.2 strict audit：0 ERROR；5 条既有 `packet-thread` WARNING 已审查。`ModSettings` 只在 common 使用 `Configuration`，配置 GUI/事件处理器均隔离在 `client` 包。PASS（2026-09-04）。
- [x] 源码审查：同日 `ensureMarketDay` 仅保留现有快照/补损坏缺项，设置未创建价格重算、C2S 或 NBT 路径；交通接入无余额时仍发送既有 C2S 请求，服务端 `CoinService.canAfford/trySpend` 继续拒绝且同步。PASS。
- [ ] 游戏内：默认 E（及改键后的背包键）分别关闭商人、行情书、交通站与交通确认层，并使商人 `endTrading` 执行；蟹笼原版关闭行为不回归。NOT RUN：当前环境没有可操作 Forge 客户端。
- [ ] 游戏内：右上角金币与其他右上 HUD 共同显示时保留 12 个 scaled-pixel 页边距；商人数量框白字；Mod List 设置、默认链式跨日、启用旧稳定模式的跨日和同日开关不刷新价格。NOT RUN：当前环境没有可操作 Forge 客户端/Dedicated Server。
- [ ] 游戏内：余额不足时“接入最近村庄”主按钮与原版确认“是”按钮可点击；服务端重算候选/费用后拒绝请求、不扣金币且刷新快照。NOT RUN：需要客户端/服务端连接。

## 维护：原版确认、Tooltip、羊毛显示与骨粉入口

- [x] `check_toolchain.py`：ForgeGradle 3、Forge `14.23.5.2859`、Gradle 4.9、snapshot `20171003-1.12`、Temurin Java 8 `1.8.0_504`；0 issue。PASS（2026-09-04）。
- [x] Forge 1.12.2 strict audit：0 ERROR；5 条既有 `packet-thread` WARNING 已审查为通用注册与 Proxy/客户端主线程桥接，本轮无 common 客户端引用或新的网络处理器。PASS（2026-09-04）。
- [x] `goldenBoneMealSelfTest`：中心/外围 5×5 边界、可耕作物和草/花目标分类保持。PASS（2026-09-04，Temurin Java 8）。
- [x] `crabTrapSelfTest`、`merchantCatalogSelfTest`、`marketCoreSelfTest`、`marketPacketSelfTest`、`transportCoreSelfTest`、`transportTravelSelfTest`：相关库存、羊毛共享商品、交通候选/费用和服务端确认路径均未回归。PASS（2026-09-04，Temurin Java 8）。
- [x] 源码审查：蟹笼的 `drawScreen` 先执行 `GuiContainer` 原版槽位绘制、再调用 `renderHoveredToolTip`；交通确认直接实例化 `GuiYesNo`，仅调整原版“是”按钮 enabled；羊毛仅在商人/行情书显示栈重命名；金闪闪骨粉注册 `dyeWhite`、原版白色羊染色委托和发射器增强行为都在 common-safe vanilla/Forge API 中。PASS。
- [ ] 游戏内：确认 `GuiYesNo` 的泥土背景、白色文字与是/否按钮；交通改名白字、选中节点整行绿色；蟹笼内部/背包 Tooltip；商人/行情书“羊毛”；金闪闪骨粉白色染羊、发射器中心满熟/5×5 效果、失败时抛出物品及贴图辨识度。NOT RUN：当前环境没有可操作 Forge 客户端窗口，服务器 EULA 未接受。

## 维护：蟹笼专用槽与交通交互

- [x] `crabTrapSelfTest`：0 号槽只接受钓竿、1 号槽只接受合法生肉，2～19 接受任意物品；所有面向公开 20 槽，Hopper 插入遵循同一校验且产出位可取。PASS（2026-09-04，Temurin Java 8）。
- [x] `transportCoreSelfTest`：移出节点直接删除 PlayerData 映射，NBT 与死亡 Clone 仅保留有效节点；物理世界节点删除和 Packet 往返保持。PASS（2026-09-04，Temurin Java 8）。
- [x] `transportTravelSelfTest`：村庄候选身份和接入/旅行费用检查点保持不变，确认层只使用已有服务端权威 `CONNECT_VILLAGE` 结算路径。PASS（2026-09-04，Temurin Java 8）。
- [x] `processResources` 与 release JAR：交通方块配方为 `RIR/ICI/RIR`（R=红石块，I=铁块，C=指南针），双语确认窗口与三坐标显示键已打包。PASS（2026-09-04）。
- [ ] 游戏内：蟹笼 0/1 槽与各向 Hopper 拒绝错误物品、2～19 可存任意物品、原版 Tooltip；交通配方；村庄确认层余额不足仍可点击且由服务器重新定价/拒绝；移出/拆除后节点列表立即消失；320×240 下文字、绿色选中行与无 `d0` 显示。NOT RUN：当前环境没有可操作 Forge 客户端。

## 维护：商人按件交易、行情书与蟹笼规则

- [x] `merchantCatalogSelfTest`：价格按单个物品；有限库存按 16→4、8→2、4→1 组并以原版堆叠上限换算实际件数，1 组→1 件；`remainingItems` NBT 往返、按件扣减和重复商品拒绝通过。PASS（2026-09-05，Temurin Java 8）。
- [x] `marketCoreSelfTest`、`marketPacketSelfTest`：准确追踪全部 29 种商人收购商品，16 色羊毛只占共享的一条行情；购买目录以选中优先、渐进预取而非首开全量同步。PASS（2026-09-05，Temurin Java 8 `1.8.0_504`）。`processResources` 与 release JAR 资源清单确认无序配方包含 `minecraft:wool` wildcard metadata 且输出 4 根线。
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
- [x] `merchantCatalogSelfTest`：粘液球位于 28 项稀有池，按 8 组→2 组并以原版堆叠上限换算实际库存；旧组库存 Offer 不迁移，按当前规则重生成。PASS：2026-09-05，Temurin Java 8。
- [x] `pastoralWorldDataSelfTest`：旧 v1 初始化标记保留，已删除的 `farmHarassment` 字段不再写出。PASS：2026-09-04，Temurin Java 8。
- [ ] 牛/哞菇/猪/羊/马/驴/骡/羊驼与鸡/兔/狼/豹猫/鹦鹉按表掉落骨头；抢夺与屠宰的顺序、外部强制并存时的组合、非白名单实体均符合内容书。NOT RUN：需要可进入的世界。
- [ ] Dedicated Server 完整加载新增 common 事件处理器且不加载客户端类。NOT RUN：`timeout 60s ./gradlew runServer` 已进入 Forge/FML 1.12.2 引导与 coremod 阶段，但在模组发现前到达时限；`run/eula.txt` 保持 `eula=false`，未接受 EULA。静态审计为 0 ERROR。

## 第 04 批世界日市场核心

- [x] `marketCoreSelfTest`：100 次同 key/day 调用一致；同 Item/meta 买卖变体共享随机身份；8 类上下限、动态回归和边界反弹正确；小麦基础价为 50。PASS（2026-09-05）。
- [x] 市场初始化与旧存档缺失 history list 修复会写入当前真实世界日的全部出售/购买历史目录；小麦和购买页铁锭都会得到当天价格点。PASS：`marketCoreSelfTest`。
- [x] 连续 30 日只保留按世界日升序的最近 30 点；第 31 天及更早点不再提供分页或持久化。PASS：`marketCoreSelfTest`。
- [x] 多日推进逐日承接上一日价格；连续 1,000 日后只保留最近 30 日窗口；当天重复调用不重建，NBT 重载、回拨和返回原日均不重复。PASS：`marketCoreSelfTest`。
- [x] v6 市场读入后写为 v7，且不再写 legacy `processedDays`。PASS：`marketCoreSelfTest`。
- [x] `PastoralWorldData` v2→v3 保留 firstInitializationCompleted 且不伪造 market。PASS：自检。
- [x] Forge 1.12.2 static audit：0 ERROR；本批无 Client import、Packet 或现代 API。PASS（2 条第 02 批 packet-thread WARNING 已审查）。
- [ ] Dedicated Server：市场初始化、睡眠、`/time add`、重启、多玩家、维度与存档重载。NOT RUN：本次实际启动到 Srg→Mcp 映射加载，尚未进入模组加载/EULA/世界；未改动 `run/eula.txt`。
- [ ] Client：进入世界后金币 HUD 回归正常，且没有新的市场 GUI。NOT RUN：本次实际进入 Forge 客户端引导，但在模组发现/窗口创建前停止，无法进入世界。

## 第 05 批市场行情书与历史 GUI

- [x] `market_book` 具有稳定 registry/unlocalized name；书 + 小麦使用无序 JSON 配方、输出严格为 1；模型引用本模组 `textures/items/market_book.png` 绿皮书贴图。PASS：编译、`processResources`、成品 Jar 资源检查。
- [x] Packet 1/2 使用既有 `lb_pastoral` channel、稳定不重排的 discriminator 1/2；商品 key 限制为 128 UTF-8 bytes，历史点计数最大 30；C2S 仅接收 key/游标/请求号，服务端在主线程验证实际打开的 `ContainerMarketBook`、全部 historyTracked 商品和非负/非未来游标后直接生成单项快照，不依赖无槽 Container 的后续 tick。PASS：代码审查、Forge audit 与编译。
- [x] S2C 快照包含当前日、商品 key、窗口游标、今日/可选昨日价、最多 30 个日/价格/真实前一点价格、前后翻页标记；最新窗口即使旧存档的某项历史暂缺，也至少包含已冻结的当天价格点。客户端缓存仅在打开书本期间接收，关闭立即清空并拒绝迟到包，连接内请求号不复用。PASS：代码审查、`marketCoreSelfTest`、`marketPacketSelfTest`、编译。
- [x] `marketPacketSelfTest`：合法请求/快照可往返，129-byte key 与 31 点快照被拒绝，首个可见点保留无前日状态，较旧同窗口快照不会替换较新缓存；同一打开请求号的预取小麦/胡萝卜窗口可独立命中缓存，多个合法显示请求均保持独立可处理。PASS。
- [x] 购买页附魔书由与商人实际输出一致的原版 `ItemEnchantedBook` NBT 构造，Tooltip 可读取附魔和精确等级；普通商品不会被误构造成附魔书。PASS：`merchantCatalogSelfTest`。
- [x] 行情书打开/经济实际读取才经 `MarketService` 服务端惰性刷新市场；主世界 tick 不再调用市场服务。关闭书本后没有客户端市场缓存或新请求。PASS：代码审查、市场核心自检回归。
- [x] 成品 Jar 包含 `GuiMarketBook`、Packet、行情书 Item、模型、配方、语言、`mcmod.info` 与 `pack.mcmeta`。PASS：Jar 检查。
- [ ] 开发客户端实际进入世界后：获得物品、中文名、模型、无序配方、主/副手无限使用、默认小麦、14 项切换、今日/昨日/趋势、折线、悬停、30 天窗口、GUI Scale 和小窗口。NOT RUN：`runClient` 已实际进入 Forge/FML 与 coremod 发现，但本 WSL 环境在本模组加载/窗口创建前终止。
- [ ] 市场按需初始化、跨日重开、v6 存档首次访问迁移、多人和主世界/下界/末地一致性。NOT RUN：需要可进入测试世界；实现路径均经服务端主世界 `WorldSavedData` 解析，且市场没有后台 tick。
- [ ] 恶意包（无书本 Container、非法商品、负/超大/未来日期、快速切换、关书后的迟到回包）端到端。NOT RUN：需要已连接客户端；代码路径已验证 Container、边界和会话缓存。
- [ ] Dedicated Server 完整加载、玩家连接、GUI 请求和无客户端类加载错误。NOT RUN：`runServer` 已实际进入 Forge/FML Server 与 coremod 阶段，但未完成模组加载；`run/eula.txt` 保持 `false`，未接受 EULA。

## 第 06、07 批附魔农业与工具战斗

- [x] `enchantmentSelfTest`：8 个 RARE、非宝藏附魔，等级、装备白名单、Harvest/Fortune 与 Slaughter/Sharpness/Smite/Bane/Looting 互斥，以及 Harvest、Fine Cultivation、Pastoral Favor 的 RNG 阈值。PASS。
- [x] `AgricultureRules` 把七种 Harvest 目标和 Pumpkin 排除、主作物/种子身份、Binomial/UniformInt/cocoa metadata 3 公式集中。PASS：代码审查和自检。
- [x] Fine Cultivation 只从最终 HarvestDrops 列表、再从玩家主背包/快捷栏消耗真实种植物，且只在方块仍为空、下方仍为 Farmland 时以默认 age 0 状态补种。PASS：代码审查和编译。
- [x] Felling 只经原版 `tryHarvestBlock` 逐块处理已验证的同树种原木与叶子；没有手工掉落、全局已访问树列表或递归。PASS：代码审查和编译。
- [x] Fleetfoot/Farmland Walker 使用服务器固定 UUID 的非持久 operation 2 属性修饰符，`ClientFleetfootFovHandler` 只抵消疾步固定速度项；Night Vision 只在 `Side.CLIENT` 以可恢复临时 gamma 实现，不使用药水效果或改世界光照。PASS：代码审查、JDK 8 编译与最终 Gradle build。
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
- [ ] 商人按 `max(ceil(villagers / 5),3)` 的目标人数、死亡补足、32 格活动限制、超过 64 格传送至村庄站、无自然 despawn；生命 20、移速 0.1、水中上浮且无村民环境/受伤/死亡声音 — NOT RUN：需要可进入的世界
- [ ] 商人离开 32 格活动范围后最多每 20 tick 重算一次返航路径，超过 64 格立即传送到站点；高实体数下维护不会重复扫描 merchant 实体。NOT RUN：需要可进入的世界；代码审查确认单次索引扫描。
- [ ] 商人靠近玩家时面向最近的 8 格内玩家；打开有效交易窗口期间原地站住，关闭后恢复常规 AI；实际受该玩家伤害后避开其 200 tick。NOT RUN：需要可进入的世界。
- [x] 商人记录的可选最后实体区块字段可 NBT 往返，重复观测不重复标脏。PASS：`merchantCatalogSelfTest`。
- [ ] 旧存档/重进世界：世界加载期间不补生，第一次周期只等待实体进入；已加载的站点/最后实体区块均确认没有实体后才补生，传送至村庄不会短暂出现又消失的重复商人。NOT RUN：需要可进入世界和旧存档。
- [ ] MerchantRecord/Offer/库存重启、Chunk unload/reload 不复制或刷新 — NOT RUN：需要 Dedicated Server/世界

### Sell / Buy / GUI

- [ ] 6 收购栏同日稳定；非栏位商品拒绝；主背包/快捷栏数量和 16 色羊毛统计 — NOT RUN：需要可进入的世界
- [ ] 牛奶桶出售原子返空桶、满背包失败无副作用 — NOT RUN：需要可进入的世界
- [ ] 8 个购买栏各自按普通 40%、罕见 30%、稀有 20%、珍宝 10% 抽取；当天允许任一品质为 0，且 UI 按普通→罕见→稀有→珍宝分组 — NOT RUN：需要可进入的世界
- [ ] 有限库存多人共享、重启不恢复、跨日刷新 — NOT RUN：需要 Dedicated Server/多人
- [x] GUI 每栏数量加减、数量/预计总价显示；服务端数量上限与库存/容量复核 — PASS：代码审查、`compileJava`/`build`
- [x] 维护：商人 GUI 不暂停单人集成服务端；成功交易同步窗口 0 玩家背包、CoinService 金币缓存和所有打开的同商人快照。PASS：服务端路径代码审查、`compileJava`、`build`。
- [x] 维护：商人标签、交易卡、禁用数量输入和按钮使用原版 `GuiButton` 的正常浅色，点击仍在本地与服务端重新校验。PASS：`GuiMerchantTrade` 代码审查、`compileJava`、`build`。
- [ ] 游戏内：单人和多人连续买卖后，不关闭窗口即可立即看到背包、金币、价格/库存、持有量更新；交易窗口保持世界运行。NOT RUN：当前环境无法创建可操作的 Forge 客户端窗口。
- [ ] 背包完整容量模拟、溢出、重复 Packet、旧 GUI 跨日拒绝 — NOT RUN：需要客户端/服务器连接
- [ ] Dedicated Server 无客户端类加载错误 — NOT RUN：当前环境未接受 EULA；静态审查通过

## 第 11 批稀有、珍宝与特殊附魔书

### 静态/目录

- [x] Rare Pool 28 项、Treasure 普通 21 项 + 特殊附魔 12 项：唯一 key、Item/meta、单件价格、波动和实际库存校验 — PASS：`merchantCatalogSelfTest`
- [x] 12 张 1.12.2 唱片独立候选，所有基础价 5000、波动 8%、库存 1 — PASS：目录自检
- [x] 四种原版特殊附魔 + 八种模组附魔真实 Registry 引用、等级上限和权重总和 100% — PASS：目录自检与 `enchantmentSelfTest`
- [x] 1.12.2 legacy 映射：Skull metadata、Dragon Head、Enchanted Golden Apple、Dry Sponge、矿石方块 — PASS：目录定义与 ItemStack 自检

### DailyOffer / 交易

- [x] 每日购买为 8 槽独立品质抽取（40%/30%/20%/10%）、生成后 Common→Uncommon→Rare→Treasure 稳定排序；无效、重复或旧 10 槽 Offer 按当前规则重生成 — PASS：`merchantCatalogSelfTest`
- [x] 多等级附魔先选类型后按权重解析，等级写入 `DailyOffer`，真实 Enchanted Book 输出 — PASS：目录自检、代码审查
- [x] Rare/Treasure 库存统一共享并复用原子购买、容量模拟、溢出与重复请求防护 — PASS：代码审查、`build`
- [x] DailyOffer 的 `remainingItems` 与附魔等级 NBT 往返；缺少新字段的旧 Offer 不迁移 — PASS：NBT 字段与读取校验审查
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

## 2026-09-05 任务维护回归

- [x] 肉类出售目录/价格、购买历史标记、行情双分页及渐进缓存 — PASS：代码审查、`compileJava`、`test`。
- [x] 移除行情书前后翻页、商人卡片基础价、商人随机出生/解绑/刷怪蛋、创造标签和两项配置 — PASS：代码审查与编译。
- [ ] 游戏内分页、刷怪蛋绑定/解绑、村民范围游荡和配置开关。NOT RUN：当前环境无法创建可操作 Forge 客户端/世界。

- [x] 商人购买/出售页在 320x240 最小缩放下没有页签、数量控件或卡片越界；滑条、文本框和确认按钮的数量范围仍为 1--4096 且出售上限取玩家主背包/快捷栏持有量。PASS：布局/客户端状态代码审查与编译。
- [x] 商人最后选择的购买/出售页只保存在客户端内存，跨 Merchant GUI 复用，未增加 C2S 字段、NBT 或服务端写入。PASS：代码审查与编译。
- [x] 商人/行情书图标调用原版 `renderToolTip`；蟹笼继承 `GuiContainer` 原版 Slot 悬停提示。PASS：代码审查与编译。
- [x] 蟹笼只显示 2 个输入槽、18 个收获槽和原版玩家背包槽；不存在没有 Container Slot 的顶部空格，状态文字不与槽框重叠。PASS：Container/GUI 坐标审查与编译。
- [x] 交通站操作区在 320x240 最小缩放中不越界；节点文字截断后可悬停读取完整坐标/状态。PASS：布局代码审查与编译。
- [x] 商人出售页按 3 × 2 对称卡片排版；购买余额不足一整包或出售页背包无目标物品时，数量输入和滑条限制范围、确认按钮进入原版深色禁用态；当前出售/购买页签同样禁用，确认操作仍由客户端与服务端复核。PASS：客户端布局/数量上限代码审查与编译。
- [x] 交通节点快照往返保留 `travelFee`；目的地行、悬停提示和前往按钮显示费用，余额不足/路径不可用时前往按钮暗置。PASS：`TransportCoreSelfTest` Packet 往返、`build`。
- [x] `compileJava`、`processResources`、`build`（含 `test`、`reobfJar`）— PASS：2026-09-04 以 Corretto 11 JDK 兼容构建；项目 source/target Java 8，`GuiMerchantTrade.class` 为 major 52。当前环境无 Java 8 JDK，因此仍需用 Temurin 8 复验。
- [x] UTF-8 JavaCompile 编码、Forge 1.12.2 strict audit 0 ERROR、成品 JAR 的 GUI 类和 `mcmod.info` — PASS；5 条既有 `packet-thread` WARNING 已审查。
- [x] 每次 `build` 在 `reobfJar` 后自动执行 `exportReleaseJar`，并导出 `release/LisBam_PastoralEconomy-1.0.jar`。PASS：2026-09-05 构建日志、SHA-256 与 `unzip -t` 完整性检查。
- [x] UI 文字使用原版 `FontRenderer`；商人的静态交易文字与数量文字使用原版 `GuiButton` 正常浅色，而当前页签和不可成交确认按钮按原版 `GuiButton` 禁用状态显示深色。蟹笼 20 个实际槽和商人物品槽直接裁取原版 `generic_54.png`，蟹笼空白背景也取原版纹理像素；商人/交通面板每帧以单次完整 `demo_background.png` 裁取绘制，避免分片错位且没有手绘槽框/凸起边框。PASS：源码审查、`compileJava`、`build` 与 Forge audit。
- [x] 交通 GUI 的标签、节点、文本输入和不可用按钮文字与商人 GUI 一样使用原版 `GuiButton` 正常浅色，仍由原版禁用背景与 `enabled` 限制交互。PASS：源码审查、`compileJava`、`build`。
- [x] 蟹笼、玩家交通站与村庄交通站资源：交通站和蟹笼两张实际 PNG 都是 32×32，村庄模型不再引用 `planks_oak`；玩家交通方块与 ItemBlock 都从无后缀 `tile...transport_station` 显示键得到“交通方块”，不会显示 `.name`。行情书自有绿皮书 32×32 Item PNG；蟹笼配方为铁锭/铁栅栏交错外框和中央陷阱箱。PASS：源码、`processResources`、release JAR 检查。
- [x] 发行版本 `1.0`：`build.gradle`、处理后的 `mcmod.info` 和 JAR Manifest 的 Specification/Implementation Version 均为 `1.0`；重混淆文件导出为 `release/LisBam_PastoralEconomy-1.0.jar`。PASS：`processResources`、`build`、JAR 检查。
- [x] 购买有限库存显示 `remainingItems` 实际剩余物品数量，而非包数；不限量保持显示不限量。PASS：`GuiMerchantTrade` 代码审查、双语资源处理与 `build`。
- [x] `merchantCatalogSelfTest`：村民 2/15 的最低 3 名、16/20/21/35/36 的 `ceil(n/5)` 边界及 100/1000 村民的无上限增长。PASS（2026-09-04，Temurin Java 8）。
- [x] 蟹笼：0/1 号 GUI Slot、TileEntity 直接写入和所有方向 Hopper 都拒绝错误物品；2～19 号槽允许任意物品；既有存档内错误功能槽物品保留且可取出。PASS：`crabTrapSelfTest`（2026-09-04）和最终 `build`。
- [x] GUI：蟹笼沿 `GuiContainer` 原版槽位悬停路径显示蟹笼和玩家背包物品 Tooltip；商人与交通全部静态非按钮文字直接使用 `FontRenderer.drawString(..., 4210752)`，不经带阴影的 `drawCenteredString`；交通节点列表鼠标位于列表区域时可逐行滚动，窄屏面板无重叠。PASS：源码审查、`compileJava`、Forge audit 和最终 `build`；实际游戏内鼠标/视觉验收仍见下方 NOT RUN 项。
- [x] 维护：交通“我的节点”标题位于第二行动按钮下方；接入最近村庄和移出节点均使用原版背景比例的本地二次确认层。移出只在确认后发送既有 `REMOVE`，服务端验证路径和 Packet 编码未改。PASS：`GuiTransportStation` 控制流/布局审查、`transportCoreSelfTest`、`transportTravelSelfTest`、`compileJava`。
- [x] 金闪闪的骨粉：稳定注册、模型、16×16 RGBA 图标、双语键和两份无序 JSON 配方齐全；中心作物至多 8 次原版骨粉、同层 5×5 范围及草/花草层路径的分类/边界正确。PASS：`goldenBoneMealSelfTest`、`compileJava`、`processResources`。
- [ ] 游戏内：金闪闪的骨粉在每类原版作物中心完全成熟、外围作物各一次骨粉、草方块/花 5×5 自然生成、创造模式不消耗、无效目标不消耗；320×240 与常规缩放下确认层无文字重影且“我的节点”不被按钮遮挡。NOT RUN：当前环境无法创建可操作 Forge 客户端窗口。
- [ ] 游戏内：在 320x240、常规 GUI Scale 和高分辨率下验证所有四种 GUI 的文字、按钮、滚动、物品 Tooltip、蟹笼点击/Shift-click、无虚假顶部格子、商人余额/背包不足深色禁用态和当前页签禁用、交通费用与余额暗置，以及商人连续切换后的默认页。NOT RUN：当前环境无法创建可操作的 Forge 客户端窗口。
- [ ] 游戏内：交通方块物品/方块名精确为“交通方块”、两种交通站显示简约石质罗盘贴图、蟹笼显示简约木框铁栅贴图、行情书为绿皮书，且蟹笼新配方正确；传送、重进和 Chunk unload/reload 后没有短暂重复商人。NOT RUN：当前环境无法创建可操作的 Forge 客户端窗口。

## 1.5 附魔维护回归

- [x] Temurin Java 8 `1.8.0_504`：`compileJava`、`processResources`、`compileTestJava`、`enchantmentSelfTest`、`merchantCatalogSelfTest`、`pinyinSearchSelfTest` 和最终 `build`（含 `reobfJar`、`exportReleaseJar`）均 PASS（2026-09-05）。
- [x] `enchantmentSelfTest`：十二个模组附魔的等级、装备范围、原版效率的剪刀兼容、束锋诅咒的宝藏/诅咒/附魔台排除/横扫互斥、剪刀 Harvest/Range 白名单、攻速/百炼如新不进入剪刀白名单、剪毛增产随机式及百炼如新的首次费用/输入 NBT 不变。PASS。
- [x] `merchantCatalogSelfTest`：珍宝书池共 37 个等级商品，新增攻速、范围、百炼如新、束锋诅咒的等级边界、权重、价格和实际附魔书 NBT。PASS；`pinyinSearchSelfTest` 同时覆盖“耕地行者”和“百炼如新”。PASS。
- [x] Forge 1.12.2 strict audit：0 ERROR；6 条既有 `packet-thread` 保守 WARNING 已复核为通用网络注册/Proxy 主线程桥接，新增附魔逻辑不使用网络包。PASS。
- [x] Release：`release/LisBam_PastoralEconomy-1.5.jar` 非空（401,456 bytes），SHA-256 `f53d2731695a805fff26a3ea497984e8f0dba8cc5c002359992f40d6320fc2e2`，`unzip -t` PASS。
- [ ] 游戏内单人/多人：捷足 I--IV 不改变 FOV，夜视头盔实际明亮且摘下恢复，耕地行者跳跃/落地不伤耕地或作物。NOT RUN：当前环境无法创建可操作 Forge 客户端/世界。
- [ ] 游戏内：攻速 I--V 冷却、范围 I--V 客户端选取与服务器攻击/方块/实体距离、束锋诅咒无横扫且直击正常、与横扫之刃的铁砧拒绝。NOT RUN：需要可运行客户端和 Dedicated Server。
- [ ] 游戏内：百炼如新的修理、装备合并、附魔书、普通改名与 `>=40` 极端改名；确认左右输入不变、输出费用正确。NOT RUN：需要实际铁砧界面。
- [ ] 游戏内：剪刀在附魔台的原版效率/耐久/丰收/范围候选，锄头按材质取得候选；经验修补/消失诅咒书应用；羊和哞菇成功剪毛的额外掉落、已剪羊/失败交互不补发。NOT RUN：需要实际世界。
# 2026-09-05 维护回归

- [x] `MilkCooldownRules`：5 分钟 = 6000 tick，冷却边界、时间回拨和剩余 tick 计算通过代码审查；`compileJava` PASS。
- [x] `MarketCatalog`：出售/购买基础价按调价工作簿 ×50 更新，market key 与 16 色羊毛共享行情保持不变；`merchantCatalogSelfTest`、`marketCoreSelfTest` PASS（2026-09-05，Temurin Java 8）。
- [ ] 游戏内：同一成年牛连续挤奶时第二次不扣空桶、不产牛奶；两头牛可分别挤奶；区块重载/重启后冷却保持；开启“取消挤奶冷却”后恢复原版连续挤奶。NOT RUN：当前环境无可操作 Forge 客户端。
- [x] 静态/自检：Coremod 后的 `Item` 字节码包含剪刀/五种锄头附魔力钩子与原版效率剪刀筛选，并通过 ASM verifier；附魔力 helper 的对象实参来自 `this Item` 而非 `ItemStack`，效率筛选只在既有返回点合并结果且不会新增跳转；`enchantmentSelfTest` 还覆盖原版 BREAKABLE 的经验修补/消失诅咒/耐久、Harvest/Range 白名单与剪毛增产公式。PASS（2026-09-05，Temurin Java 8）。
- [x] Coremod 兼容：SRG 名称布局、现代 `ItemStack` 钩子布局和旧式无参 `Item#getItemEnchantability()` 布局均由 `enchantmentSelfTest` 构造并验证；helper 只使用 mapping-neutral `Object` 描述符。Forge 14.23.5.2847 官方 binpatch 后的真实 `ain.class` 也已执行相同转换，并确认附魔力方法为两个 `ALOAD 0`、零个 `ALOAD 1`。PASS（2026-09-05，Temurin Java 8）。
- [x] `milkCooldownSelfTest`：6000 tick 边界、时间回拨、每牛独立 tick、服务端接管/配置绕过、冷却拒绝不产奶、仅成功交易记录时间，以及客户端不预测库存。PASS（2026-09-05，Temurin Java 8）。
- [x] 静态审计：Forge 1.12.2 audit 为 0 ERROR、6 条既有 `packet-thread` WARNING；Coremod 和挤奶处理器均不引用客户端类。PASS（2026-09-05）。
- [ ] 游戏内：剪刀/锄头附魔台实际候选、剪刀原版效率书本/铁砧和丰收增产。NOT RUN：当前环境无可操作 Forge 客户端。
- [ ] Dedicated Server：完整加载 Coremod 后以空桶依次对两头牛挤奶，确认已挤牛冷却、冷却命中不扣桶不产奶、服务端生成的牛奶桶可饮用、满背包时牛奶桶掉落、区块重载/重启保持且“取消挤奶冷却”会绕过限制。NOT RUN：60 秒 `runServer` 已发现并入队 Coremod，但在完整模组/世界加载前超时；`run/eula.txt` 保持 `eula=false`。
- [x] 最终 release：`compileJava processResources build`（含 `test`、`reobfJar`、`exportReleaseJar`）PASS；`release/LisBam_PastoralEconomy-1.5.jar` 已由本次构建覆盖导出且非空。
# 每日挤奶、伐木耐久附魔与区块加载器（2026-09-07）

- [x] JDK 8 `./gradlew compileJava --no-daemon --console=plain`：PASS；新增 common 方块、TileEntity、Forge ticket 服务、挤奶规则和注册路径均通过 1.12.2 编译。
- [x] `MilkCooldownSelfTest`：以当前 main classes 重新编译并运行，确认世界日为 24,000 tick、同日拒绝、下一日统一恢复、配置旁路和客户端预测抑制规则。PASS。
- [x] `ChunkLoaderSelfTest`：以当前 main classes 重新编译并运行，确认红石激活/撤除边界和 7 级（火把一半）充能光照。PASS。
- [x] 资源检查：`chunk_loader_inactive.png` 与 `chunk_loader_active.png` 均为 16×16、8-bit RGBA PNG；blockstate、两份 block model、item model、双语名称和有序配方已随源文件存在。PASS。
- [x] 伐木耐久附魔代码审查：Forge 1.12.2 `PlayerInteractionManager#tryHarvestBlock` 源码和当前编译类均确认每个二级原木走原版逐格采掘/耐久路径；不重复增加手工耐久损耗。PASS。
- [x] Forge 1.12.2 常规 audit：0 ERROR、7 条既有 `packet-thread` 保守 WARNING。PASS。
- [x] Temurin Java 8 `1.8.0_504`：`compileTestJava`、`processResources`、`milkCooldownSelfTest`、`toolDurabilitySelfTest`、`chunkLoaderSelfTest` 均 PASS；其中伐木自检确认逐块走原版耐久路径，区块加载器自检确认红石边界和光照等级。
- [x] 最终 `./gradlew build --no-daemon --console=plain`：PASS（含 `test`、`reobfJar`、`exportReleaseJar` 与 249 个 Java 8 生产类核验）。构建前已备份 `release/backup/backup_20260907-132027.jar`；新 `release/LisBam_PastoralEconomy-1.5.jar` 为 516,800 bytes，SHA-256 `9ccc32785b3c28b2d9f0e78528db8db359af9db366e7e057ea8cc25afc4ee702`，`unzip -t` PASS。
- [ ] 游戏内单人/多人：同一成年牛在同一世界日第二次挤奶不扣桶、不产奶；任意其他未挤奶牛仍可挤；到下一世界日所有牛一起恢复；关闭配置后连续挤奶恢复原版。NOT RUN：当前无可操作 Forge 世界。
- [ ] 游戏内区块加载器：无红石时不加载；供电后仅自身区块持续 tick/保持加载，贴图切为充能并发出 7 级光；断电和破坏后 ticket 移除；退出重启后有供电实例恢复，且 Forge ticket 上限不足时安全保持未充能。NOT RUN：当前无可进入 Dedicated Server 世界。
- [ ] Dedicated Server：确认 common 代码不加载客户端类，并在专用世界验证多台加载器、跨区块红石边界、保存/重启与 ticket 上限。NOT RUN：最终 JDK 8 build 已通过，待可进入服务器。
