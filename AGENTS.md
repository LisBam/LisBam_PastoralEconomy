# 聆竹の休闲田园经济 — 维护规范

本模组的分批开发已经完成。后续工作只处理 Bug 修复、平衡调整、体验优化和明确指定的新内容；不得重新引入已删除的分批任务单或提前设计未经确认的玩法。

## 固定技术边界

- Minecraft Java Edition / Forge：1.12.2 / `14.23.5.x`（当前 `14.23.5.2859`）
- Java：源码和目标版本必须为 Java 8；不得使用 Java 9+ 语法或 API。
- 禁止引入 NeoForge、Fabric、`mods.toml`、`DeferredRegister`、`RegistryObject`、`BlockEntity` 或现代网络/数据组件 API。
- 涉及 Forge 代码、资源、构建或调试时，必须遵守已安装的 `minecraft-forge-1-12-2` Skill；不确定的 1.12.2 API 必须查项目源码、Forge 源码或 Gradle 缓存，不能凭现代经验猜测。

## 每次修改前

先执行并检查：

```bash
pwd
git status --short
```

阅读与任务有关的现行资料：

- `docs/MOD_ARCHITECTURE.md`
- `docs/DEVELOPMENT_PROGRESS.md`
- `docs/IMPLEMENTATION_DECISIONS.md`
- `docs/TEST_CHECKLIST.md`

磁盘上的代码是当前实现事实来源；设计内容书是玩法需求来源。保留用户已有修改，不执行未经明确要求的 `reset --hard`、强制覆盖、推送或其他破坏性 Git 操作。

## 实现规则

- 复用现有注册、网络、保存和服务层；不要为修复或新增内容建立重复框架。
- 所有影响钱、交易、库存、掉落、传送、解锁、世界状态和玩家长期数据的结果由逻辑服务端验证并修改。客户端只负责输入和显示。
- 新增状态必须明确其所有权：玩家状态使用既有 Capability，世界状态使用 `PastoralWorldData`，方块实例使用 TileEntity + NBT。
- 保持已有 registry ID、NBT key、WorldSavedData 名称和 Packet discriminator 的兼容性；如必须变更，先评估旧存档影响并记录迁移策略。
- Java 功能与对应 lang、模型、贴图、配方等资源一起完成。Common 代码不得加载客户端类。
- Bug 修复应定位根因，不得以吞异常或仅隐藏症状代替修复。

## 验证与文档

修改 Java、Gradle 或资源后，运行 Forge 1.12.2 静态审计，并尽可能使用项目 Wrapper 执行：

```bash
./gradlew compileJava
./gradlew processResources
./gradlew build
```

按影响范围运行相关 self-test；涉及 common、网络、持久化、GUI、实体或世界逻辑时，额外检查 Dedicated Server 安全性。只有实际运行成功的命令可标为 `PASS`；不能运行时写明 `NOT RUN` 和原因。

每次完成任何代码、资源、Gradle 或配置更新后，必须执行最终 `build` 并将可安装的重混淆 JAR 导出到：

```text
release/LisBam_PastoralEconomy-<version>.jar
```

`build.gradle` 的 `exportReleaseJar` 已作为 `build` 的 finalizer 自动执行；完成时仍须检查该文件真实存在、非空，并在最终报告中说明导出结果。不得把开发环境的未重混淆 JAR 当作 release 成品。

完成后仅更新真实变化对应的长期文档：

- `MOD_ARCHITECTURE.md`：实际架构。
- `DEVELOPMENT_PROGRESS.md`：维护记录、构建和遗留测试。
- `IMPLEMENTATION_DECISIONS.md`：重要边界或兼容性决定。
- `TEST_CHECKLIST.md`：新增系统的回归项与真实验证结果。

## Git 工作区提交

每次修改任意工作区文件后，必须先填写本次实际变化所需的项目文档、完成适用验证并导出 release JAR，然后自动提交 **工作区内所有内容**：

```bash
git status --short
git add -A
git diff --cached --stat
git commit -m "<准确描述本次工作>"
```

不得只挑选部分文件暂存；应提交当时工作区内的全部新增、修改和删除，包括文档、配置、IDE 文件、日志和 release JAR。提交信息必须准确概括本次工作，并在最终报告中说明提交哈希。此规则只要求本地 Git commit，除非用户另外明确要求，否则不得自动 `push`、`rebase`、`reset` 或改写历史。

最终报告简述实现、根因（如为 Bug）、存档影响、验证结果和未运行测试。
