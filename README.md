# AE2 Auto Pattern Upload · NeoForge 1.21.1

在样板编码终端点一下 ↑，编码好的样板就会直接投放到 AE2 网络内任意能放样板的容器——原版样板供应器、ExtendedAE 扩展供应器与装配矩阵；合成样板默认进分子装配仪或装配矩阵。附带按配方方式预填搜索、F 键把 JEI / EMI / REI 物品名填入 AE 搜索框、÷2 ÷3 ÷5 / ×2 ×3 ×5 批量缩放数量，以及流体桶配方自动从网络取 1B 流体装桶合成。

本工程把 [GaLicn/AE2-Auto-Pattern-Upload](https://github.com/GaLicn/AE2-Auto-Pattern-Upload)（Minecraft 1.12.2 / Forge / AE2 Extended Life）按功能重写到 1.21.1 / NeoForge 21.1.265 / AE2 19.2.19：原版的 rv6 API、Forge 网络与 JEI mixin 在目标版本已全部失效。零 mixin，JEI / EMI / REI 三选一即可。

## 功能

| 功能 | 说明 |
| --- | --- |
| 上传按钮 | 样板编码终端「编码」按钮左侧的 ↑，悬停有提示 |
| 全容器扫描 | 按 AE2 `PatternContainer` 枚举网络内**所有能放样板**的容器：原版样板供应器、ExtendedAE 扩展供应器、装配矩阵等 |
| 合成默认目标 | 合成样板把分子装配仪 / 装配矩阵对应目标置顶并标 ★；界面里按 Enter 上传首项，或按住 Shift 点 ↑ 直接投放 |
| 配方方式预填 | 选择界面搜索框自动填入**配方方式名**（「合成」「烧炼」「锻造」「切石」），优先取 JEI 里该产物所属的配方类别标题，不是产物名 |
| 选择界面 | 按名称分组、显示空槽总数与数量、搜索、分页、右键清空、重新扫描 |
| F 键 | 可绑定按键（默认 F）：在 JEI / EMI / REI 物品上按下，清空并写入 AE 终端搜索框该物品名 |
| 流体桶自动装填 | 合成样板输入含流体桶时自动开启 AE2 流体替代：网络有 1B 流体 + 空桶即可在分子装配仪内装桶后直接合成 |
| 数量缩放 | 结果区左右各 3 个小按钮：÷2 ÷3 ÷5 / ×2 ×3 ×5，批量缩放处理样板所有输入与输出槽的数量（仅处理模式显示） |
| 别名映射 | `config/ae2apu/recipe_names.json`，把配方方式名映射成要搜索的别名，界面内可增删改 |

## 依赖

| mod | 关系 | 用途 |
| --- | --- | --- |
| AE2 19.2.x | 必需 | 终端、网络、样板容器 |
| JEI 19.x | 可选（编译期 API 依赖） | 鼠标下物品名、配方类别标题、搜索框同步 |
| EMI | 可选，反射兼容 | 鼠标下物品名 |
| REI | 可选，反射兼容 | 当前聚焦条目名 |

三者都不装时上传功能仍可用，只是 F 键与配方类别查询降级。JEI 缺失时通过 `Class.forName` 按需加载适配类，服务端不会触碰客户端专用类。

## 构建

前置：JDK 21。依赖 jar 放 `libs/`（见 `libs/README.md`）；`local-maven/` 补齐官方 CDN 缺失的 `minecraft-dependencies:1.21.1` 聚合 POM，勿删。

```bash
./gradlew build          # build/libs/ae2apu-1.0.0.jar
./gradlew runClient      # 开发客户端（含 JEI）
./gradlew runServer      # 专用服务器
```

## 配置

`config/ae2apu/recipe_names.json`：

```json
{
  "合成": "工作台",
  "烧炼": "熔炉",
  "Smelting": "Furnace"
}
```

键是配方方式名（JEI 类别标题或内置名称，大小写不敏感），值是要填入搜索框的别名。也可在选择界面用「搜索框 + 别名输入框 + 添加 / 删除」维护。

## 实现要点

- **全容器扫描**：`IGrid.getMachineClasses()` 过滤 `PatternContainer.class.isAssignableFrom(...)`，再 `getActiveMachines(clazz)`；写入统一走 `getTerminalPatternInventory()`，命名走 `getTerminalGroup().name()`。与 AE2 自带样板访问终端一致，已核实 ExtendedAE 的 `TileAssemblerMatrixPattern` 实现该接口。
- **安全**：服务端按玩家缓存本次扫描的容器引用，客户端只回传列表下标（2 分钟 TTL）；上传时复核容器类型与网络一致性。
- **默认目标**：合成样板（`AECraftingPattern`）时，用 `PatternContainerGroup.icon()` 是否为 `ae2:molecular_assembler` 或分组名含「分子装配 / 装配矩阵 / Molecular Assembler / Assembler Matrix」判定。
- **配方方式**：服务端解码样板得到产物 ID 与编码方式；客户端用 JEI `createRecipeCategoryLookup().limitFocus(OUTPUT focus)` 查类别标题，处理样板优先取非工作台类别；查不到时用本地化方式名。
- **F 键（关键）**：AE2 每帧执行 `MEStorageScreen.updateSearch()`——启用「外部搜索框」时把 `ItemListMod.getSearchText()` 回填到 `Repo`，启用「与搜索框同步」且外部框聚焦时又会用外部文本覆盖 `AETextField`。因此只改控件会被立刻覆盖。现在按 AE2 自身同步链路写入：
  1. `ScreenEvent.KeyPressed.Pre` 直接捕获按键并 `setCanceled(true)`，避免把字母打进搜索框；`ClientTickEvent.Post` 的 `consumeClick()` 作兜底并去重；
  2. `ItemListMod.setSearchText(text)` 走 AE2 的 `ItemListModAdapter`（EMI / REI 原生适配器）；
  3. JEI 侧用 `IIngredientFilter.setFilterText(text)`；若 AE2 未注册任何适配器（纯 JEI 环境），安装一个 `ItemListModAdapter` 回退实现，其 `getSearchText()` 读 JEI 过滤器、`setSearchText()` 写 JEI 过滤器、`hasSearchFocus()` 返回 false，避免回拉覆盖；
  4. `AETextField.setValue("")` → `setValue(text)`，并反射调用私有 `MEStorageScreen.setSearchText(text)` 立即刷新 `Repo`；样板访问终端走 `refreshList()`。
- **流体桶**：客户端在样板编码终端检测到 `BucketItem` / `MilkBucketItem` 且 `ContainerItemStrategies.getContainedStack(..., fluids())` 非空时，自动调 `PatternEncodingTermMenu.setSubstituteFluids(true)`（客户端动作会同步到服务端）。服务端上传时再兜底：读取 `EncodedCraftingPattern` 组件，若含流体桶且 `canSubstituteFluids=false`，用 `RecipeManager.byKey(recipeId)` 取回配方并以 `PatternDetailsHelper.encodeCraftingPattern(..., true)` 重编码。AE2 的 `AECraftingPattern.getItemOrFluidInput()` 在该标志下会把桶换成网络流体，并在合成时消耗空桶装桶。上传前用 `IStorageService.getCachedInventory()` 检查 ≥1B 流体与 ≥1 空桶，结果合并进上传提示。
- **数量缩放**：服务端对 `PatternEncodingLogic.getEncodedInputInv()` / `getEncodedOutputInv()` 的 `ConfigInventory` 逐个槽位 `setStack(new GenericStack(key, amount))`；除数结果最小为 1，乘法防溢出。
- **容器不关闭**：`Minecraft.setScreen` 只触发 `removed()`，客户端 `AbstractContainerMenu.removed(LocalPlayer)` 不关闭服务端容器。

## 验证状态

- `compileJava` / `build` 通过；jar 含 `neoforge.mods.toml`、中英文语言文件、全部新增类。
- `runServer`：AE2、JEI、本模组加载，`Done (7.151s)!`，无异常。
- `runClient`：JEI 完整启动，日志出现 `JEI runtime attached to ae2apu:viewer`；资源重载含 `mod/ae2apu`，OpenAL 正常，无异常。
- **待进世界实测**：F 键写入搜索框（含「使用外部搜索框」开/关两种配置）、含流体桶的样板上传后自动合成、配方方式预填、Shift+↑、处理模式 6 个缩放按钮、ExtendedAE 装配矩阵入列表。

## 已知限制

- 处理样板默认方式名为「烧炼」，机器类处理配方可用别名映射改成对应机器名。
- 流体桶检测限定原版 `BucketItem` / `MilkBucketItem`（与 AE2 自身的判定一致）；模组自定义流体容器不自动开启替代。
- EMI / REI 用反射读取悬停条目，若其 API 结构调整会静默降级；JEI 走正式 API。
- 6 个缩放按钮放在 GUI 左右外侧竖排，以避开 AE2 紧凑的槽位与模式标签；位置集中在 `PatternTerminalButton`，需要改布局只改这一处。
- 原版 F 键的 JEI mixin 未移植，改为可绑定按键 + 运行时 API + AE2 同步桥，行为等价且不写 mixin。

## 许可

MIT。原仓库版权声明见 `LICENSE`，移植部分沿用同一协议。
