package com.iafdragonfix;

/**
 * ThreadLocal flag to distinguish between normal world gen (where we block IAF features)
 * and our own Structure postProcess calls (where we allow them through).
 */
public class DragonGenFlag {

    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);

    public static void enable() {
        ACTIVE.set(true);
    }

    public static void disable() {
        ACTIVE.set(false);
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }
}
