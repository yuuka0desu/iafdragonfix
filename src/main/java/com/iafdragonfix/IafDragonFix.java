package com.iafdragonfix;

import com.iafdragonfix.config.DragonDenConfig;
import com.iafdragonfix.structure.DragonDenPiece;
import com.iafdragonfix.structure.DragonDenStructure;
import com.iafdragonfix.structure.ScatteredPlacement;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("iafdragonfix")
public class IafDragonFix {

    public static final String MODID = "iafdragonfix";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, MODID);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, MODID);

    public static final DeferredRegister<StructurePlacementType<?>> STRUCTURE_PLACEMENT_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, MODID);

    public static final RegistryObject<StructureType<DragonDenStructure>> DRAGON_DEN_TYPE =
            STRUCTURE_TYPES.register("dragon_den", () -> () -> DragonDenStructure.CODEC);

    public static final RegistryObject<StructurePieceType> DRAGON_DEN_PIECE_TYPE =
            STRUCTURE_PIECE_TYPES.register("dragon_den_piece",
                    () -> (StructurePieceType.ContextlessType) DragonDenPiece::new);

    public static final RegistryObject<StructurePlacementType<ScatteredPlacement>> SCATTERED_PLACEMENT_TYPE =
            STRUCTURE_PLACEMENT_TYPES.register("scattered_random", () -> () -> ScatteredPlacement.CODEC);

    public IafDragonFix() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        STRUCTURE_TYPES.register(modBus);
        STRUCTURE_PIECE_TYPES.register(modBus);
        STRUCTURE_PLACEMENT_TYPES.register(modBus);
        DragonDenConfig.register();
        LOGGER.info("IAF Dragon Fix loaded - dragon dens registered as structures");
    }
}
