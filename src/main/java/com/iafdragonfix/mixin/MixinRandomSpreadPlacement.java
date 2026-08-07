package com.iafdragonfix.mixin;

import com.iafdragonfix.config.DragonDenConfig;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Override spacing/separation of our structure sets with values from the server config.
 *
 * RandomSpreadStructurePlacement is a record whose fields are set by its canonical
 * constructor called from the JSON codec.  We intercept the constructor arguments
 * when they match our known salts:
 *   roosts  salt = 372918564
 *   caves   salt = 198273645
 */
@Mixin(RandomSpreadStructurePlacement.class)
public class MixinRandomSpreadPlacement {

    // ── space ──────────────────────────────────────────────────────────────
    // This method name remaps to the constructor of the record.
    // Mixin matches constructors by signature, so use the plain argument list.

    /**
     * Intercept the spacing argument in the canonical constructor of the record.
     * Because the constructor is synthetic for records, the simple name <init>
     * with the correct descriptor works on both SRG and mapped environments.
     */
    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int iafdragonfix$overrideSpacing(int original) {
        return getOverriddenSpacing(original);
    }

    /**
     * Intercept the separation argument in the canonical constructor (ordinal = 1).
     */
    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private int iafdragonfix$overrideSeparation(int original) {
        return getOverriddenSeparation(original);
    }

    /**
     * Intercept the frequency argument (ordinal = 4).
     */
    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 4, argsOnly = true)
    private float iafdragonfix$overrideFrequency(float original) {
        Integer salt = currentSalt.get();
        if (salt == null) return original;
        if (salt == 372918564) return DragonDenConfig.roostFrequency.get().floatValue();
        if (salt == 198273645) return DragonDenConfig.caveFrequency.get().floatValue();
        return original;
    }

    // ── internal helpers ──────────────────────────────────────────────────

    // Cache the salt so we can detect which structure-set we're building.
    // The salt arg is ordinal 3 (spacing=0, separation=1, spreadType=2, salt=3).
    private static final ThreadLocal<Integer> currentSalt = new ThreadLocal<>();

    @ModifyVariable(method = "<init>", at = @At("HEAD"), ordinal = 3, argsOnly = true)
    private int iafdragonfix$captureSalt(int salt) {
        currentSalt.set(salt);
        return salt; // unchanged
    }

    private static int getOverriddenSpacing(int original) {
        return overriddenValue(original, true);
    }

    private static int getOverriddenSeparation(int original) {
        return overriddenValue(original, false);
    }

    private static int overriddenValue(int original, boolean isSpacing) {
        Integer salt = currentSalt.get();
        if (salt == null) return original;

        // Match our known salts
        if (salt == 372918564) {               // dragon roosts
            return isSpacing ? DragonDenConfig.roostSpacing.get()
                             : DragonDenConfig.roostSeparation.get();
        }
        if (salt == 198273645) {               // dragon caves
            return isSpacing ? DragonDenConfig.caveSpacing.get()
                             : DragonDenConfig.caveSeparation.get();
        }
        return original;
    }
}
