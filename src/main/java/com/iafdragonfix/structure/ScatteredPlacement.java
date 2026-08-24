package com.iafdragonfix.structure;

import com.github.alexthe666.iceandfire.IafConfig;
import com.iafdragonfix.IafDragonFix;
import com.iafdragonfix.config.DragonDenConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.Optional;

/**
 * Pure per-chunk random scattering, replicating IAF's original placement model:
 * every chunk independently rolls {@code 1/chance}, producing uneven density and
 * occasional clusters (trimmed by the 300-block same-type separation check that
 * happens later in {@code DragonDenPiece}).
 *
 * <p>This extends {@link RandomSpreadStructurePlacement} so vanilla {@code /locate}
 * recognises the type.  {@code spacing()} is forced to 1, {@code getPotentialStructureChunk}
 * returns its input, and {@code isPlacementChunk} does the deterministic 1-in-N roll
 * from the world seed — so the location can always be reverse-resolved from the seed.</p>
 */
public class ScatteredPlacement extends RandomSpreadStructurePlacement {

    public static final int CAVE_SALT = 198273645;
    public static final int ROOST_SALT = 372918564;

    public static final Codec<ScatteredPlacement> CODEC = RecordCodecBuilder.<ScatteredPlacement>mapCodec(instance -> instance.group(
            Vec3i.offsetCodec(16).optionalFieldOf("locate_offset", Vec3i.ZERO).forGetter(p -> p.locateOffset()),
            StructurePlacement.FrequencyReductionMethod.CODEC
                    .optionalFieldOf("frequency_reduction_method", StructurePlacement.FrequencyReductionMethod.DEFAULT)
                    .forGetter(p -> p.frequencyReductionMethod()),
            Codec.floatRange(0.0F, 1.0F).optionalFieldOf("frequency", 1.0F).forGetter(p -> p.frequency()),
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter(p -> p.salt()),
            StructurePlacement.ExclusionZone.CODEC.optionalFieldOf("exclusion_zone").forGetter(p -> p.exclusionZone()),
            Codec.intRange(1, 4096).fieldOf("chance").forGetter(p -> p.chance)
    ).apply(instance, ScatteredPlacement::new)).codec();

    private final int chance;

    public ScatteredPlacement(Vec3i locateOffset,
                              StructurePlacement.FrequencyReductionMethod frequencyReductionMethod,
                              float frequency,
                              int salt,
                              Optional<StructurePlacement.ExclusionZone> exclusionZone,
                              int chance) {
        super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone,
                1, 1, RandomSpreadType.LINEAR);
        this.chance = chance;
    }

    @Override
    public int spacing() {
        return 1;
    }

    @Override
    public int separation() {
        return 1;
    }

    @Override
    public ChunkPos getPotentialStructureChunk(long seed, int chunkX, int chunkZ) {
        return new ChunkPos(chunkX, chunkZ);
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        return isScatteredAt(state.getLevelSeed(), chunkX, chunkZ);
    }

    @Override
    public StructurePlacementType<?> type() {
        return IafDragonFix.SCATTERED_PLACEMENT_TYPE.get();
    }

    /**
     * Pure, world-state-free roll for one chunk: {@code nextInt(chance) == 0} seeded
     * from the world seed + chunk + salt.  Used by both chunk generation and /locate
     * reverse resolution, so the two always agree.
     */
    public boolean isScatteredAt(long seed, int chunkX, int chunkZ) {
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureWithSalt(seed, chunkX, chunkZ, salt());
        return random.nextInt(effectiveChance()) == 0;
    }

    /**
     * Resolve the effective 1-in-N chance.  When our known structure-set salt is
     * present we defer to IAF's own server config so the density always matches
     * the original mod.  The JSON {@code chance} value is only a fallback for
     * unknown salts; our server config value covers the case where IAF's config
     * resolves to zero/unset.
     */
    private int effectiveChance() {
        int salt = salt();
        if (salt == CAVE_SALT) {
            int c = IafConfig.generateDragonDenChance;
            return c > 0 ? c : DragonDenConfig.caveChance.get();
        }
        if (salt == ROOST_SALT) {
            int c = IafConfig.generateDragonRoostChance;
            return c > 0 ? c : DragonDenConfig.roostChance.get();
        }
        return chance;
    }
}
