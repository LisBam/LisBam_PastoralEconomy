# 当前状态

当前完成批次：第 15 批。

最近成功构建：

- `JDK8_HOME=/tmp/lbpe-jdk8; env JAVA_HOME="$JDK8_HOME" PATH="$JDK8_HOME/bin:$PATH" ./gradlew compileJava processResources build`
- 日期：2026-09-05
- 结果：PASS（Forge 14.23.5.2859 / Temurin Java 8 `1.8.0_504`；`build` 包含 `reobfJar` 与 `exportReleaseJar`。`release/LisBam_PastoralEconomy-1.0.jar` 为 344,747 bytes，SHA-256 `6be402b55305362b6a7e02b6f4cc1680298e4edb2825a9aaa2a789d341fcbc5a`，`unzip -t` PASS。）

## 维护：链式市场低价保护（2026-09-05）

实现：默认链式市场在基础价大于 1 金币时使用 2 金币低价保护，避免正常下跌把价格反复压到 1；若旧存档或异常状态中的链式价格已经是 1，下一次日推进至少回到 2。对取整后没有增长的正向小波动，额外保证至少增加 1 金币，因此低价不会因百分比步长小于半枚金币而永久不动。`moreStableMarketVolatility=true` 的旧独立三角分布公式保持不变，设置切换仍只影响未来跨日。

根因与兼容性：原链式公式直接 `Math.round(previousPrice * (1 + step))`，价格为 1 或 2 时正向结果常被取整回原值；下跌结果又被 `max(1, ...)` 固定在 1。保护逻辑只位于默认链式单日计算，不新增 NBT、Packet、registry ID 或 WorldSavedData 字段；已保存的当天快照不重算，旧的 1 金币快照会在下一次自然推进时恢复。

验证：`marketCoreSelfTest` 新增低价下跌不低于 2、1 金币恢复、正向最小增量三项回归；Forge 1.12.2 strict audit、Java 8 编译和最终 `build` 结果记录在本文顶部，游戏内跨日与旧存档实测仍需可用运行环境。

## 维护：链式市场波动、设置与界面输入（2026-09-04）

实现：默认市场首次初始化使用冻结基础价，之后 `PastoralWorldData` 按每一遗漏世界日以昨日持久化快照推进；`MarketPriceGenerator` 对低于基础价的价格以 65% 概率上涨、对高于基础价的价格以 65% 概率下跌，单日步长为原类别波动率的 25%～100%。因此市场只有回归倾向、没有固定百分比均价带。新增 common `ModSettings` 和物理客户端 Mod List 配置界面：`market.moreStableMarketVolatility=false` 为新默认，`true` 完整恢复旧的独立三角分布。配置切换只在未来跨日读取，绝不重新生成同日或已保存的历史价格；v7 `PastoralWorldData` 的现有当前/昨日快照和 30 日收购历史直接保留，无 NBT、WorldSavedData、Packet 或 registry 迁移。

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

兼容性与影响：保留 `remainingBundles` NBT key 以兼容旧 Offer，但其数值现在表示剩余原版整组；已有当天 Offer 自加载起按新组大小发放，跨日重新生成。旧市场数据首次访问会重建完整的 22 商品历史窗口。蟹笼既有库存、pending loot 与倒计时均可读取；下一次抽取使用新等待范围。

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
