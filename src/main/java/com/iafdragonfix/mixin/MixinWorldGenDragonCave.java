package com.iafdragonfix.mixin;

import com.github.alexthe666.iceandfire.world.gen.WorldGenDragonCave;
import com.iafdragonfix.DragonGenFlag;
import com.iafdragonfix.IafDragonFix;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Disables IAF's dragon cave Feature generation during normal world gen.
 * When our Structure's postProcess sets the ThreadLocal flag, the original method is allowed through.
 */
@Mixin(value = WorldGenDragonCave.class, remap = false)
public class MixinWorldGenDragonCave {

    @Inject(method = "m_142674_", at = @At("HEAD"), cancellable = true, remap = false)
    private void iafdragonfix$disableCaveFeature(FeaturePlaceContext<NoneFeatureConfiguration> context, CallbackInfoReturnable<Boolean> cir) {
        if (!DragonGenFlag.isActive()) {
            cir.setReturnValue(false);
        }
    }
}
