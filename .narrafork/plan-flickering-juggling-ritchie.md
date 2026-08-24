# 复刻 IAF 纯随机散布生成

## 目标

让龙巢/龙穴与 IAF 原模组一致：**每区块独立概率、密度不均、偶尔扎堆**（被 300 格同类型间距约束截断），而非当前的确定性网格均匀分布。

## 可行性结论（回答"能否直接调用原模组算法"）

| 环节 | 能否直接调用原模组 | 说明 |
|------|------------------|------|
| 概率（每区块 1/N） | ❌ 不能 | 原模组在 Feature 的 `place()` 里用区块生成时传入的 `RandomSource` 掷概率。Structure 的 placement 判定层**没有** Level/RandomSource 上下文，且 placement 必须是确定性的（否则 `/locate` 与区块生成结果不一致）。 |
| 距离检查（spawn 距离 / 300 格间距） | ✅ 能 | `IafWorldRegistry.isFarEnoughFromSpawn`、`isFarEnoughFromDangerousGen` 都是 public static，项目编译期依赖 IAF，可在 `postProcess`（有 WorldGenLevel 上下文）直接调用，100% 复刻含 `IafWorldData` 持久化记录。 |

**结论**：概率用等价实现（`WorldgenRandom.setLargeFeatureWithSalt` 做确定性每区块掷骰）；距离直接调用原模组。

## 重要修正（之前认知错误）

IAF 的 `Dangerous World Gen Dist Seperation`（300 格）是**同类型**间距——`IafWorldData.check` 按 `FeatureType`（SURFACE / UNDERGROUND）分组：
- 龙穴之间（UNDERGROUND）互相 300 格
- 龙巢之间（SURFACE）互相 300 格
- 龙穴与龙巢之间**无**约束

当前代码 `isFarEnoughFromOtherType` 实现的是**跨类型**间距，方向相反。改为直接调用原模组方法即自动修正。

## 改动清单

### 1. 新增 `ScatteredPlacement`（`src/main/java/com/iafdragonfix/structure/ScatteredPlacement.java`）

继承 `net.minecraft.world.level.levelgen.structure.placement.StructurePlacement`：

- 字段：`int chance`
- CODEC：`placementCodec(instance)` 的 5 个公共字段（`locate_offset` / `frequency_reduction_method` / `frequency` / `salt` / `exclusion_zone`）+ `chance`（`Codec.intRange(1, 4096).fieldOf("chance")`）
- 构造函数：调用 `super(locateOffset, freqMethod, frequency, salt, exclusionZone)` 并保存 chance
- `isPlacementChunk(ChunkGeneratorStructureState, int chunkX, int chunkZ)`：
  ```
  WorldgenRandom r = new WorldgenRandom(new LegacyRandomSource(0L));
  r.setLargeFeatureWithSalt(state.getLevelSeed(), chunkX, chunkZ, salt());
  return r.nextInt(effectiveChance()) == 0;
  ```
- `effectiveChance()`：按 salt 读 IAF 配置（cave salt=198273645 → `IafConfig.generateDragonDenChance`；roost salt=372918564 → `IafConfig.generateDragonRoostChance`），取不到时回退到 JSON 的 `chance`。
- `type()` 返回 `IafDragonFix.SCATTERED_PLACEMENT_TYPE.get()`

### 2. 注册 placement 类型（`IafDragonFix.java`）

```java
public static final DeferredRegister<StructurePlacementType<?>> STRUCTURE_PLACEMENT_TYPES =
        DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, MODID);

public static final RegistryObject<StructurePlacementType<ScatteredPlacement>> SCATTERED_PLACEMENT_TYPE =
        STRUCTURE_PLACEMENT_TYPES.register("scattered_random", () -> () -> ScatteredPlacement.CODEC);
```

构造器中 `STRUCTURE_PLACEMENT_TYPES.register(modBus)`。

### 3. 更新 structure_set JSON

`dragon_caves.json`：
```json
"placement": {
  "type": "iafdragonfix:scattered_random",
  "salt": 198273645,
  "chance": 260
}
```
`dragon_roosts.json`：
```json
"placement": {
  "type": "iafdragonfix:scattered_random",
  "salt": 372918564,
  "chance": 480
}
```
移除 `spacing` / `separation` / `frequency`。

### 4. postProcess 距离检查改为直接调用原模组（`DragonDenPiece.java`）

- 删除手动 spawn 距离计算、跨类型检查方法 `isFarEnoughFromOtherType` 及 `CAVE_STRUCTURES` / `ROOST_STRUCTURES` 常量
- 改为：
  ```java
  if (!IafWorldRegistry.isFarEnoughFromSpawn(level, origin)) { skip; }
  String id = dragonType.isCave() ? "dragon_cave" : "dragon_roost";
  FeatureType ft = dragonType.isCave() ? FeatureType.UNDERGROUND : FeatureType.SURFACE;
  if (!IafWorldRegistry.isFarEnoughFromDangerousGen(level, origin, id, ft)) { skip; }
  ```
- 保留 roost 液体检查（`surface.above()` 非流体）

### 5. 简化配置（`DragonDenConfig.java`）

- 删除 `roost/cave` 的 `spacing`、`separation`、`frequency`、`spawnDistance`
- 删除 `crossTypeSeparation`
- 保留 `chance` 作为 JSON 兜底值（`roost.chance=480`、`cave.chance=260`），注释说明：**实际生效值跟随 IAF 的 `iceandfire-common.toml`（Generate Dragon Den Chance / Generate Dragon Roost Chance）**

### 6. 删除 `MixinRandomSpreadPlacement.java`

- 移除文件，并在 `iafdragonfix.mixins.json` 中删除 `"MixinRandomSpreadPlacement"` 条目

## 验证

- `gradle build` 编译通过
- 手动检查 jar 内 JSON 与 mixins 配置
- 提交并推送
