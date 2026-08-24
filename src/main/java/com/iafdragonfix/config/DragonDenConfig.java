package com.iafdragonfix.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class DragonDenConfig {

    public static final ForgeConfigSpec SERVER_SPEC;

    // ── Fallback 1-in-N chance ──
    // These only apply if IAF's own config cannot be read.  Normally the
    // generation chance, spawn distance and separation are all driven by IAF:
    //   - chance  -> iceandfire-common.toml (Generate Dragon Den/Roost Chance)
    //   - spawn   -> Dangerous World Gen Dist From Spawn
    //   - spacing -> Dangerous World Gen Dist Seperation
    public static ForgeConfigSpec.IntValue roostChance;
    public static ForgeConfigSpec.IntValue caveChance;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Fallback values used only when IAF config is unavailable.",
                  "Generation chance = 1 out of N per chunk (pure random scatter).");
        roostChance = b.defineInRange("roostChance", 480, 1, 100000);
        caveChance  = b.defineInRange("caveChance", 260, 1, 100000);

        SERVER_SPEC = b.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
    }
}
