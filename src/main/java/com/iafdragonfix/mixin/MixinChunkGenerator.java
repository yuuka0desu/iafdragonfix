package com.iafdragonfix.mixin;

import com.github.alexthe666.iceandfire.IafConfig;
import com.iafdragonfix.structure.ScatteredPlacement;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Teaches {@code /locate} how to reverse-resolve {@link ScatteredPlacement}.
 *
 * <p>Vanilla's nearest-structure search only understands ConcentricRings and
 * RandomSpread placements.  {@code ScatteredPlacement} extends RandomSpread, so it
 * passes the type check, but vanilla would then call {@code getPotentialStructureChunk}
 * for every chunk and trigger world generation for biome-matching chunks.</p>
 *
 * <p>This mixin intercepts the RandomSpread search ({@code m_223188_}) and, for our
 * placement, filters chunks with the pure deterministic {@code isScatteredAt} roll
 * before consulting {@code getStructureGeneratingAt}.  Only chunks that actually win
 * the 1-in-N roll ever touch world generation, keeping {@code /locate} fast.</p>
 */
@Mixin(ChunkGenerator.class)
public abstract class MixinChunkGenerator {

    @Shadow(remap = false)
    private static Pair<BlockPos, Holder<Structure>> m_223198_(
            Set<Holder<Structure>> structures, LevelReader level, StructureManager structureManager,
            boolean skipKnownStructures, StructurePlacement placement, ChunkPos chunkPos) {
        throw new AssertionError("mixin shadow");
    }

    @Inject(method = "m_223188_", at = @At("HEAD"), cancellable = true, remap = false)
    private static void iafdragonfix$scatteredLocate(
            Set<Holder<Structure>> structures, LevelReader level, StructureManager structureManager,
            int chunkX, int chunkZ, int radius, boolean skipKnownStructures, long seed,
            RandomSpreadStructurePlacement placement,
            CallbackInfoReturnable<Pair<BlockPos, Holder<Structure>>> cir) {
        if (placement instanceof ScatteredPlacement scattered) {
            cir.setReturnValue(searchScattered(structures, level, structureManager,
                    chunkX, chunkZ, radius, skipKnownStructures, seed, scattered));
        }
    }

    /**
     * Mirrors vanilla's ring-walk for one radius step, but instead of delegating to
     * {@code getPotentialStructureChunk} (grid reverse-resolution) it rolls the pure
     * per-chunk chance directly.  Winning chunks are then confirmed via the vanilla
     * {@code getStructureGeneratingAt} helper.
     */
    private static Pair<BlockPos, Holder<Structure>> searchScattered(
            Set<Holder<Structure>> structures, LevelReader level, StructureManager structureManager,
            int chunkX, int chunkZ, int radius, boolean skipKnownStructures, long seed,
            ScatteredPlacement scattered) {
        // Mirror the spawn-distance check performed in DragonDenPiece.postProcess
        // so /locate never reports a den that generation would later skip.
        BlockPos spawn = null;
        double minSpawnDistSq = 0.0D;
        if (level instanceof ServerLevel serverLevel) {
            spawn = serverLevel.getSharedSpawnPos();
            double limit = IafConfig.dangerousWorldGenDistanceLimit;
            minSpawnDistSq = limit * limit;
        }

        for (int i = -radius; i <= radius; i++) {
            boolean iEdge = (i == -radius || i == radius);
            for (int j = -radius; j <= radius; j++) {
                boolean jEdge = (j == -radius || j == radius);
                if (!iEdge && !jEdge) {
                    continue;
                }
                int cx = chunkX + i;
                int cz = chunkZ + j;
                if (!scattered.isScatteredAt(seed, cx, cz)) {
                    continue;
                }
                if (spawn != null) {
                    BlockPos center = new ChunkPos(cx, cz).getMiddleBlockPosition(0);
                    double dx = center.getX() - spawn.getX();
                    double dz = center.getZ() - spawn.getZ();
                    if (dx * dx + dz * dz < minSpawnDistSq) {
                        continue;
                    }
                }
                Pair<BlockPos, Holder<Structure>> result = m_223198_(
                        structures, level, structureManager, skipKnownStructures,
                        scattered, new ChunkPos(cx, cz));
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
