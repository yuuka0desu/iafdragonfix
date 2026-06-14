# IAF Dragon Fix - 结构化重写方案

## 目标

禁用 IAF 原版龙巢/龙穴的 Feature 生成，将其改为正式注册的 Structure。在 Structure 的 `postProcess` 中直接调用 IAF 的生成方法（绕过概率检查），使 `/locate` 能精确定位龙巢/龙穴位置。

## 核心方案

### 1. 禁用 IAF 原版 Feature 生成

通过 Mixin 在 `WorldGenDragonCave.m_142674_` 和 `WorldGenDragonRoosts.m_142674_` 的 HEAD 注入，检查 ThreadLocal 标志：
- 标志 OFF（正常世界生成）→ `cir.setReturnValue(false)` 完全禁用
- 标志 ON（从我们的 postProcess 调用）→ 不拦截，让方法正常执行

### 2. 注册自定义 Structure

注册 6 个 Structure（fire/ice/lightning × cave/roost），每个有独立的 StructureSet 控制间隔和概率。

### 3. 在 postProcess 中调用 IAF 生成逻辑

**龙穴（Cave）**：直接调用 public 方法：
- 计算合适的 Y 坐标（复刻 `m_142674_` 中的高度计算逻辑）
- 调用 `feature.generateCave(level, radius, 3, pos, random)`
- 手动创建龙实体（复刻 `createDragon` 的逻辑：创建实体、设置性别/年龄/变体/位置/home）

**龙巢（Roost）**：通过反射调用 private 方法：
- `spawnDragon(FeaturePlaceContext, int size, boolean isMale)`
- `generateSurface(FeaturePlaceContext, int size)`
- `generateShell(FeaturePlaceContext, int size)`
- `hollowOut(FeaturePlaceContext, int size)`
- `generateDecoration(FeaturePlaceContext, int size, boolean isMale)`

需要构造 `FeaturePlaceContext` 传入这些方法。

## 文件修改清单

### 新增文件

1. **`src/main/java/com/iafdragonfix/DragonGenFlag.java`**
   - ThreadLocal<Boolean> 标志类
   - `enable()` / `disable()` / `isActive()` 静态方法

2. **`src/main/java/com/iafdragonfix/structure/DragonDenStructure.java`**
   - 自定义 Structure 类，继承 `Structure`
   - 支持 `type` 参数区分 6 种龙巢/龙穴
   - `findGenerationPoint` 返回一个 `DragonDenPiece`

3. **`src/main/java/com/iafdragonfix/structure/DragonDenPiece.java`**
   - 自定义 StructurePiece
   - `postProcess` 中：设置 ThreadLocal → 调用 IAF 生成逻辑 → 清除 ThreadLocal
   - 对 Cave：直接调用 public `generateCave` + 手动创建龙
   - 对 Roost：反射调用 private 方法

4. **`src/main/java/com/iafdragonfix/structure/DragonType.java`**
   - 枚举：FIRE_CAVE, ICE_CAVE, LIGHTNING_CAVE, FIRE_ROOST, ICE_ROOST, LIGHTNING_ROOST

5. **`src/main/java/com/iafdragonfix/mixin/MixinWorldGenDragonCave.java`**
   - `@Mixin(WorldGenDragonCave.class)`
   - HEAD 注入 `m_142674_`：检查 ThreadLocal，OFF 则返回 false

6. **`src/main/java/com/iafdragonfix/mixin/MixinWorldGenDragonRoosts.java`**
   - `@Mixin(WorldGenDragonRoosts.class)`
   - HEAD 注入 `m_142674_`：检查 ThreadLocal，OFF 则返回 false

7. **`src/main/resources/data/iafdragonfix/worldgen/structure/fire_dragon_cave.json`**
   - Structure JSON（引用我们的 structure type）

8. **`src/main/resources/data/iafdragonfix/worldgen/structure/ice_dragon_cave.json`**
9. **`src/main/resources/data/iafdragonfix/worldgen/structure/lightning_dragon_cave.json`**
10. **`src/main/resources/data/iafdragonfix/worldgen/structure/fire_dragon_roost.json`**
11. **`src/main/resources/data/iafdragonfix/worldgen/structure/ice_dragon_roost.json`**
12. **`src/main/resources/data/iafdragonfix/worldgen/structure/lightning_dragon_roost.json`**

13. **`src/main/resources/data/iafdragonfix/worldgen/structure_set/dragon_caves.json`**
    - StructureSet JSON，控制间隔/概率

14. **`src/main/resources/data/iafdragonfix/worldgen/structure_set/dragon_roosts.json`**

15. **`src/main/resources/data/iafdragonfix/tags/worldgen/biome/has_dragon_cave.json`**
    - 生物群系标签（引用 IAF 的配置或使用 `#is_overworld`）

16. **`src/main/resources/data/iafdragonfix/tags/worldgen/biome/has_dragon_roost.json`**

### 修改文件

17. **`src/main/java/com/iafdragonfix/IafDragonFix.java`**
    - 注册 Structure Type（`DeferredRegister<StructureType<?>>`）
    - 注册 StructurePiece Type（`DeferredRegister<StructurePieceType>`）
    - 注册事件总线

18. **`src/main/java/com/iafdragonfix/mixin/MixinWorldGenRegion.java`**
    - 保留不变（防崩溃保护仍需要）

19. **`src/main/resources/iafdragonfix.mixins.json`**
    - 添加 `MixinWorldGenDragonCave` 和 `MixinWorldGenDragonRoosts`

20. **`build.gradle`**
    - 可能需要确认 IAF jar 在编译时可用（已通过 `compileOnly fileTree` 确认）

## 关键技术细节

### 概率/间隔控制

- StructureSet 使用 `RandomSpreadStructurePlacement`
  - `spacing`: 龙穴 32 chunks，龙巢 24 chunks
  - `separation`: 龙穴 8 chunks，龙巢 6 chunks  
  - `frequency`: 0.6（60% 概率在网格点生成）
- 这些值可以模拟 IAF 原版的 `generateDragonDenChance`/`generateDragonRoostChance` 配置效果

### Y 坐标计算（Cave）

在 `DragonDenPiece.postProcess` 中复刻 IAF 的 Y 坐标查找逻辑：
```
1. 从 pos 出发，20x20 区域扫描 OCEAN_FLOOR_WG heightmap 取最低值
2. y = min_height - 20 - random.nextInt(30)
3. 如果 y < minBuildHeight + 20 → 放弃生成
4. center = new BlockPos(chunkX*16+8, y, chunkZ*16+8)
```

### 龙实体创建（Cave）

复刻 `createDragon` 逻辑（它是 private 的，但逻辑简单）：
```java
EntityDragonBase dragon = dragonType.create(serverLevel);
dragon.setGender(isMale);
dragon.growDragon(dragonAge);  // 75 + random.nextInt(50)
dragon.setAgingDisabled(true);
dragon.setHealth(dragon.getMaxHealth());
dragon.setVariant(random.nextInt(4));
dragon.moveTo(pos.getX()+0.5, pos.getY()+0.5, pos.getZ()+0.5, random.nextFloat()*360, 0);
dragon.setPersistenceRequired(true);  // m_21837_
dragon.homePos = new HomePosition(pos, serverLevel);
dragon.setHunger(50);
level.addFreshEntity(dragon);
```

### Roost 反射调用

需要通过反射获取并调用 5 个 private 方法：
```java
Method spawnDragon = WorldGenDragonRoosts.class.getDeclaredMethod("spawnDragon", FeaturePlaceContext.class, int.class, boolean.class);
Method generateSurface = WorldGenDragonRoosts.class.getDeclaredMethod("generateSurface", FeaturePlaceContext.class, int.class);
Method generateShell = WorldGenDragonRoosts.class.getDeclaredMethod("generateShell", FeaturePlaceContext.class, int.class);
Method hollowOut = WorldGenDragonRoosts.class.getDeclaredMethod("hollowOut", FeaturePlaceContext.class, int.class);
Method generateDecoration = WorldGenDragonRoosts.class.getDeclaredMethod("generateDecoration", FeaturePlaceContext.class, int.class, boolean.class);
```

调用顺序（和原版 `place()` 一致）：
1. `spawnDragon(context, size, isMale)`
2. `generateSurface(context, size)`
3. `generateShell(context, size)`
4. `hollowOut(context, size - 2)`
5. `generateDecoration(context, size + 15 - 2, isMale)`

其中 `size = 12 + random.nextInt(8)`，`isMale = new Random().nextBoolean()`

### FeaturePlaceContext 构造

```java
FeaturePlaceContext<NoneFeatureConfiguration> context = new FeaturePlaceContext<>(
    Optional.empty(),
    (WorldGenLevel) level,
    chunkGenerator,
    random,
    pos,
    NoneFeatureConfiguration.INSTANCE
);
```

### Mixin 配置注意事项

- `remap = false`：因为我们直接使用 SRG 名称
- 目标方法：`m_142674_`（Feature.place 的 SRG 名称）
- `defaultRequire = 1`：确保注入成功否则报错

### IAF Feature 实例获取

通过 IAF 的注册表获取已注册的 Feature 实例：
```java
IafWorldRegistry.FIRE_DRAGON_CAVE.get()  // WorldGenFireDragonCave
IafWorldRegistry.ICE_DRAGON_CAVE.get()   // WorldGenIceDragonCave
IafWorldRegistry.LIGHTNING_DRAGON_CAVE.get()
IafWorldRegistry.FIRE_DRAGON_ROOST.get()
IafWorldRegistry.ICE_DRAGON_ROOST.get()
IafWorldRegistry.LIGHTNING_DRAGON_ROOST.get()
```

## 执行顺序

1. 创建 `DragonGenFlag.java`
2. 创建 `DragonType.java` 枚举
3. 修改 `IafDragonFix.java` 添加注册
4. 创建 `DragonDenStructure.java`
5. 创建 `DragonDenPiece.java`（核心生成逻辑）
6. 创建 `MixinWorldGenDragonCave.java`
7. 创建 `MixinWorldGenDragonRoosts.java`
8. 修改 `iafdragonfix.mixins.json`
9. 创建 datapack JSON 文件（structure、structure_set、biome tags）
10. 编译测试
