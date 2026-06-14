package com.iafdragonfix.structure;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

public enum DragonType implements StringRepresentable {
    FIRE_CAVE("fire_dragon_cave", true),
    ICE_CAVE("ice_dragon_cave", true),
    LIGHTNING_CAVE("lightning_dragon_cave", true),
    FIRE_ROOST("fire_dragon_roost", false),
    ICE_ROOST("ice_dragon_roost", false),
    LIGHTNING_ROOST("lightning_dragon_roost", false);

    private final String id;
    private final boolean isCave;

    public static final Codec<DragonType> CODEC = StringRepresentable.fromEnum(DragonType::values);

    DragonType(String id, boolean isCave) {
        this.id = id;
        this.isCave = isCave;
    }

    public String getId() {
        return id;
    }

    public boolean isCave() {
        return isCave;
    }

    public boolean isRoost() {
        return !isCave;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static DragonType fromId(String id) {
        for (DragonType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return FIRE_CAVE;
    }
}
