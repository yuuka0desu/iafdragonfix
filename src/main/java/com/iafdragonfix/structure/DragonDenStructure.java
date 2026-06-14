package com.iafdragonfix.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import com.iafdragonfix.IafDragonFix;

import java.util.Optional;

public class DragonDenStructure extends Structure {

    public static final Codec<DragonDenStructure> CODEC = RecordCodecBuilder.<DragonDenStructure>mapCodec(instance ->
            instance.group(
                    settingsCodec(instance),
                    DragonType.CODEC.fieldOf("dragon_type").forGetter(s -> s.dragonType)
            ).apply(instance, DragonDenStructure::new)
    ).codec();

    private final DragonType dragonType;

    public DragonDenStructure(StructureSettings settings, DragonType dragonType) {
        super(settings);
        this.dragonType = dragonType;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        BlockPos pos = context.chunkPos().getMiddleBlockPosition(0);

        if (dragonType.isCave()) {
            // Cave: generate underground, position Y will be determined in postProcess
            return Optional.of(new GenerationStub(pos, builder -> {
                builder.addPiece(new DragonDenPiece(pos, dragonType));
            }));
        } else {
            // Roost: generate at surface
            int surfaceY = context.chunkGenerator().getFirstOccupiedHeight(
                    pos.getX(), pos.getZ(),
                    Heightmap.Types.WORLD_SURFACE_WG,
                    context.heightAccessor(),
                    context.randomState()
            );
            BlockPos surfacePos = new BlockPos(pos.getX(), surfaceY, pos.getZ());
            return Optional.of(new GenerationStub(surfacePos, builder -> {
                builder.addPiece(new DragonDenPiece(surfacePos, dragonType));
            }));
        }
    }

    @Override
    public StructureType<?> type() {
        return IafDragonFix.DRAGON_DEN_TYPE.get();
    }
}
