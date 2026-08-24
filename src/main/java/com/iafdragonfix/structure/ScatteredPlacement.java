package com.iafdragonfix.structure;

import com.github.alexthe666.iceandfire.IafConfig;
import com.iafdragonfix.IafDragonFix;
import com.iafdragonfix.config.DragonDenConfig;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.Optional;

/**
 * Pure per-chunk random scattering, replicating IAF's original placement model:
 * every chunk independently rolls {@code 1/chance}, producing uneven density and
 * occasional clusters (trimmed by the 300-block same-type separation check that
 * happens later in {@code DragonDenPiece}).
 *
 * <p>Unlike {@code RandomSpreadStructurePlacement} there is no grid, so the
 * result is exactly the "random scatter" behavior of the vanilla IAF Feature.</p>
 */
public class ScatteredPlacement extends StructurePlacement {

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
        super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone);
        this.chance = chance;
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureWithSalt(state.getLevelSeed(), chunkX, chunkZ, salt());
        return random.nextInt(effectiveChance()) == 0;
    }

    @Override
    public StructurePlacementType<?> type() {
        return IafDragonFix.SCATTERED_PLACEMENT_TYPE.get();
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
