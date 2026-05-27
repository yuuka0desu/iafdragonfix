package com.iafdragonfix;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("iafdragonfix")
public class IafDragonFix {

    public static final String MODID = "iafdragonfix";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public IafDragonFix() {
        LOGGER.info("IAF Dragon Fix loaded - world border crash protection enabled");
    }
}
