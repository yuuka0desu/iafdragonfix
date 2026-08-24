package com.iafdragonfix.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class DragonDenConfig {

    public static final ForgeConfigSpec SERVER_SPEC;

    // ── Roost (surface) ──
    public static ForgeConfigSpec.IntValue roostSpawnDist;
    public static ForgeConfigSpec.IntValue roostSpacing;
    public static ForgeConfigSpec.IntValue roostSeparation;
    public static ForgeConfigSpec.DoubleValue roostFrequency;

    // ── Cave (underground) ──
    public static ForgeConfigSpec.IntValue caveSpawnDist;
    public static ForgeConfigSpec.IntValue caveSpacing;
    public static ForgeConfigSpec.IntValue caveSeparation;
    public static ForgeConfigSpec.DoubleValue caveFrequency;

    // ── Cross-type separation (IAF: Dangerous World Gen Dist Seperation = 300) ──
    public static ForgeConfigSpec.IntValue crossTypeSeparation;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();

        b.comment("Dragon Roost – surface dens.  Match IAF defaults where possible.");
        b.push("roost");
        roostSpawnDist   = b.comment("Minimum blocks from world spawn").defineInRange("spawnDistance", 800, 0, 100000);
        roostSpacing     = b.comment("Average chunks between attempts").defineInRange("spacing", 24, 1, 1000);
        roostSeparation  = b.comment("Minimum chunks between dens").defineInRange("separation", 6, 1, 1000);
        roostFrequency   = b.comment("0.0 – 1.0 chance per chunk").defineInRange("frequency", 0.6, 0.0, 1.0);
        b.pop();

        b.comment("Dragon Cave – underground dens.");
        b.push("cave");
        caveSpawnDist   = b.comment("Minimum blocks from world spawn").defineInRange("spawnDistance", 800, 0, 100000);
        caveSpacing     = b.comment("Average chunks between attempts").defineInRange("spacing", 32, 1, 1000);
        caveSeparation  = b.comment("Minimum chunks between dens").defineInRange("separation", 8, 1, 1000);
        caveFrequency   = b.comment("0.0 – 1.0 chance per chunk").defineInRange("frequency", 0.6, 0.0, 1.0);
        b.pop();

        crossTypeSeparation = b
                .comment("Minimum blocks between a cave and a roost (IAF Dangerous World Gen Dist Seperation default: 300)")
                .defineInRange("crossTypeSeparation", 300, 0, 100000);

        SERVER_SPEC = b.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC);
    }
}
