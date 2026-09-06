# 聆竹の休闲田园经济 — 维护规范

本模组的分批开发已经完成。后续仅处理 Bug 修复、平衡调整、体验优化和用户明确指定的新内容；不得重新引入已删除的分批任务单，也不得自行扩展未经确认的玩法。

## 固定技术边界

- Minecraft Java Edition / Forge：`1.12.2` / `14.23.5.x`（当前 `14.23.5.2859`）。
- Java 源码和目标版本均为 Java 8；禁止 Java 9+ 语法或 API。
- 禁止 NeoForge、Fabric、`mods.toml`、`DeferredRegister`、`RegistryObject`、`BlockEntity` 及其他现代 Minecraft / Forge API。
- Forge 代码、资源、构建和调试遵守 `minecraft-forge-1-12-2` Skill；不确定的 1.12.2 API 必须查项目源码、Forge 源码或 Gradle 缓存，禁止按现代版本经验猜测。

## 修改前

```bash
pwd
git status --short
```

按任务需要阅读 `docs/MOD_ARCHITECTURE.md`、`DEVELOPMENT_PROGRESS.md`、`IMPLEMENTATION_DECISIONS.md`、`TEST_CHECKLIST.md`。

磁盘代码是当前实现事实来源，`聆竹の休闲田园经济_模组内容书.md` 是玩法需求与冻结数值来源。保留用户已有修改；未经明确要求，不得执行 `reset --hard`、强制覆盖、自动 `push`、`rebase`、改写历史或其他破坏性 Git 操作。

## 实现规则

- 复用现有注册、网络、保存和服务层，不建立重复框架。
- 钱、交易、库存、掉落、传送、解锁、世界状态和长期玩家数据由逻辑服务端验证并修改；客户端仅负责输入和显示。
- 新增持久状态必须明确所有权：玩家使用既有 Capability，世界使用 `PastoralWorldData`，方块实例使用 TileEntity + NBT。
- 保持 registry ID、NBT key、WorldSavedData 名称和 Packet discriminator 兼容；必须变更时先评估旧存档影响并记录迁移策略。
- Java 功能与 lang、模型、贴图、配方等对应资源一并完成；Common 代码不得加载客户端专用类。
- Bug 必须修复根因，不得以吞异常、静默失败或隐藏症状代替修复。
- UI 文字使用原版 `FontRenderer`；物品槽、容器背景和常规控件优先使用原版 GUI 纹理或 `GuiButton`、`GuiTextField`、`GuiSlider`，禁止用 `drawRect` 仿制原版槽框、字体或凸起边框。

## 验证、构建与 Release

修改 Java、Gradle 或资源后，运行 Forge 1.12.2 静态审计和相关 self-test；涉及 common、网络、持久化、GUI、实体或世界逻辑时额外检查 Dedicated Server 安全性。只有实际成功执行的命令可标记为 `PASS`；无法运行时标记 `NOT RUN` 并说明原因。

最终构建前，先确定本次将导出的目标：

```text
release/LisBam_PastoralEconomy-<version>.jar
```

若该文件已经存在，必须在 `build` 覆盖它之前备份到 `release/backup/`，命名为：

```text
backup_YYYYMMDD-HHmmss.jar
```

例如：`backup_20260906-023700.jar`。时间取备份时的本地时间并固定补零；同一秒重名时追加 `_01`、`_02`，禁止覆盖已有备份。

随后必须使用项目 Wrapper 完成一次真实构建：

```bash
./gradlew build
```

`build.gradle` 的 `exportReleaseJar` 会导出可安装的重混淆 JAR。不得沿用旧 JAR，也不得把未重混淆的开发 JAR 当作 release 成品。构建后必须确认新 release JAR 存在且非空；发生覆盖时还要确认旧 JAR 已成功进入 `release/backup/`。

当用户说出 `FORGE1122_JDK8_BUILD_EXPORT`、要求“用 JDK 8 构建并导出 JAR”，或默认环境缺少可用 Java / `JAVA_HOME` 时：优先检查 `/tmp/lbpe-jdk8`，否则查找其他已有 JDK 8；以 `"$JDK8_HOME/bin/java" -version` 确认为 `1.8` 后执行：

```bash
env JAVA_HOME="$JDK8_HOME" PATH="$JDK8_HOME/bin:$PATH" ./gradlew build
```

没有可用 JDK 8 时，报告检查过的路径和第一个实际构建错误；未经用户允许不得下载或安装 JDK，也不得用 JDK 11+ 代替。

## 文档与 Git

只更新与真实变化有关的长期文档：`MOD_ARCHITECTURE.md` 记录架构，`DEVELOPMENT_PROGRESS.md` 记录维护/构建/遗留测试，`IMPLEMENTATION_DECISIONS.md` 记录重要边界和兼容性决定，`TEST_CHECKLIST.md` 记录回归项与真实验证结果。

每次代码、资源、Gradle 或配置发生变化后，都必须同步更新 `聆竹の休闲田园经济_模组内容书.md`，使其准确反映当前实际可体验的玩法、配方、规则和冻结数值。

完成修改、文档同步、验证和 release 导出后，自动提交当时工作区内的全部新增、修改和删除：

```bash
git status --short
git add -A
git diff --cached --stat
git commit -m "<准确描述本次工作>"
```

不得只提交部分文件。最终报告说明提交哈希；除非用户明确要求，否则不得自动 `push`、`rebase`、`reset` 或改写历史。

## 最终报告

简述实际完成内容、Bug 根因与修复（如适用）、架构/存档影响、验证结果、release JAR 与备份结果、未运行测试和 Git 提交哈希。
