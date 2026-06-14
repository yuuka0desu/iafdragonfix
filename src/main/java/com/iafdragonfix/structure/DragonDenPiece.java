package com.iafdragonfix.structure;

import com.iafdragonfix.DragonGenFlag;
import com.iafdragonfix.IafDragonFix;
import com.github.alexthe666.iceandfire.entity.EntityDragonBase;
import com.github.alexthe666.iceandfire.entity.util.HomePosition;
import com.github.alexthe666.iceandfire.world.gen.*;
import com.github.alexthe666.iceandfire.world.IafWorldRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructurePieceAccessor;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.server.level.ServerLevel;

import java.lang.reflect.Method;
import java.util.Optional;

public class DragonDenPiece extends StructurePiece {

    private final DragonType dragonType;
    private boolean generated = false;

    public DragonDenPiece(BlockPos pos, DragonType dragonType) {
        super(IafDragonFix.DRAGON_DEN_PIECE_TYPE.get(), 0,
                new BoundingBox(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()));
        this.dragonType = dragonType;
    }

    public DragonDenPiece(CompoundTag tag) {
        super(IafDragonFix.DRAGON_DEN_PIECE_TYPE.get(), tag);
        this.dragonType = DragonType.valueOf(tag.getString("DragonType"));
        this.generated = tag.getBoolean("Generated");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("DragonType", dragonType.name());
        tag.putBoolean("Generated", generated);
    }

    @Override
    public void postProcess(WorldGenLevel level, net.minecraft.world.level.StructureManager structureManager,
                            ChunkGenerator chunkGenerator, RandomSource random, BoundingBox box,
                            ChunkPos chunkPos, BlockPos pos) {
        if (generated) return;
        generated = true;

        try {
            DragonGenFlag.enable();

            if (dragonType.isCave()) {
                generateCave(level, chunkGenerator, random);
            } else {
                generateRoost(level, chunkGenerator, random);
            }
        } catch (Exception e) {
            IafDragonFix.LOGGER.error("Failed to generate dragon den: {}", dragonType, e);
        } finally {
            DragonGenFlag.disable();
        }
    }

    private void generateCave(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random) {
        BlockPos origin = new BlockPos(this.boundingBox.minX(), 0, this.boundingBox.minZ());

        // Replicate Y-coordinate finding logic from WorldGenDragonCave.m_142674_
        int y = 40;
        for (int dx = 0; dx < 20; dx++) {
            for (int dz = 0; dz < 20; dz++) {
                y = Math.min(y, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX() + dx, origin.getZ() + dz));
            }
        }
        y -= 20;
        y -= random.nextInt(30);

        if (y < level.getMinBuildHeight() + 20) {
            IafDragonFix.LOGGER.debug("Dragon cave Y too low at {}, skipping", origin);
            return;
        }

        ChunkPos cp = new ChunkPos(origin);
        BlockPos cavePos = new BlockPos(cp.x * 16 + 8, y, cp.z * 16 + 8);

        // Get IAF feature instance
        WorldGenDragonCave feature = getCaveFeature();
        if (feature == null) return;

        // Set isMale field
        boolean isMale = random.nextBoolean();
        feature.isMale = isMale;

        // Calculate parameters matching IAF's logic
        int dragonAge = 75 + random.nextInt(50);
        int radius = (int)(dragonAge * 0.2f) + random.nextInt(4);

        // Call public generateCave method
        feature.generateCave(level, radius, 3, cavePos, random);

        // Create dragon entity (replicating createDragon logic)
        spawnCaveDragon(level, random, cavePos, dragonAge, isMale, feature);
    }

    private void spawnCaveDragon(WorldGenLevel level, RandomSource random, BlockPos pos, int age, boolean isMale, WorldGenDragonCave feature) {
        try {
            EntityType<? extends EntityDragonBase> entityType = feature.getDragonType();
            ServerLevel serverLevel = level.getLevel();
            EntityDragonBase dragon = entityType.create(serverLevel);
            if (dragon == null) return;

            dragon.setGender(isMale);
            dragon.growDragon(age);
            dragon.setAgingDisabled(true);
            dragon.setHealth(dragon.getMaxHealth());
            dragon.setVariant(random.nextInt(4));
            dragon.moveTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    random.nextFloat() * 360.0f, 0.0f);
            dragon.setPersistenceRequired();
            dragon.homePos = new HomePosition(pos, serverLevel);
            dragon.setHunger(50);
            level.addFreshEntity(dragon);
        } catch (Exception e) {
            IafDragonFix.LOGGER.error("Failed to spawn cave dragon at {}", pos, e);
        }
    }

    private void generateRoost(WorldGenLevel level, ChunkGenerator chunkGenerator, RandomSource random) {
        BlockPos origin = new BlockPos(this.boundingBox.minX(), this.boundingBox.minY(), this.boundingBox.minZ());

        // Get IAF feature instance
        WorldGenDragonRoosts feature = getRoostFeature();
        if (feature == null) return;

        // Construct FeaturePlaceContext
        FeaturePlaceContext<NoneFeatureConfiguration> context = new FeaturePlaceContext<>(
                Optional.empty(), level, chunkGenerator, random, origin, NoneFeatureConfiguration.INSTANCE
        );

        // Roost parameters matching IAF's logic
        boolean isMale = new java.util.Random().nextBoolean();
        int size = 12 + random.nextInt(8);

        try {
            // Reflect and call private methods in order
            Method spawnDragon = WorldGenDragonRoosts.class.getDeclaredMethod("spawnDragon",
                    FeaturePlaceContext.class, int.class, boolean.class);
            spawnDragon.setAccessible(true);

            Method generateSurface = WorldGenDragonRoosts.class.getDeclaredMethod("generateSurface",
                    FeaturePlaceContext.class, int.class);
            generateSurface.setAccessible(true);

            Method generateShell = WorldGenDragonRoosts.class.getDeclaredMethod("generateShell",
                    FeaturePlaceContext.class, int.class);
            generateShell.setAccessible(true);

            Method hollowOut = WorldGenDragonRoosts.class.getDeclaredMethod("hollowOut",
                    FeaturePlaceContext.class, int.class);
            hollowOut.setAccessible(true);

            Method generateDecoration = WorldGenDragonRoosts.class.getDeclaredMethod("generateDecoration",
                    FeaturePlaceContext.class, int.class, boolean.class);
            generateDecoration.setAccessible(true);

            // Call in same order as IAF's place() method
            spawnDragon.invoke(feature, context, size, isMale);
            generateSurface.invoke(feature, context, size);
            generateShell.invoke(feature, context, size);
            hollowOut.invoke(feature, context, size - 2);
            generateDecoration.invoke(feature, context, size + 15 - 2, isMale);

        } catch (Exception e) {
            IafDragonFix.LOGGER.error("Failed to generate dragon roost via reflection at {}", origin, e);
        }
    }

    private WorldGenDragonCave getCaveFeature() {
        try {
            switch (dragonType) {
                case FIRE_CAVE: return (WorldGenDragonCave) IafWorldRegistry.FIRE_DRAGON_CAVE.get();
                case ICE_CAVE: return (WorldGenDragonCave) IafWorldRegistry.ICE_DRAGON_CAVE.get();
                case LIGHTNING_CAVE: return (WorldGenDragonCave) IafWorldRegistry.LIGHTNING_DRAGON_CAVE.get();
                default: return null;
            }
        } catch (Exception e) {
            IafDragonFix.LOGGER.error("Failed to get cave feature for {}", dragonType, e);
            return null;
        }
    }

    private WorldGenDragonRoosts getRoostFeature() {
        try {
            switch (dragonType) {
                case FIRE_ROOST: return (WorldGenDragonRoosts) IafWorldRegistry.FIRE_DRAGON_ROOST.get();
                case ICE_ROOST: return (WorldGenDragonRoosts) IafWorldRegistry.ICE_DRAGON_ROOST.get();
                case LIGHTNING_ROOST: return (WorldGenDragonRoosts) IafWorldRegistry.LIGHTNING_DRAGON_ROOST.get();
                default: return null;
            }
        } catch (Exception e) {
            IafDragonFix.LOGGER.error("Failed to get roost feature for {}", dragonType, e);
            return null;
        }
    }
}
