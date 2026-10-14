package top.theillusivec4.curios;

import net.fabricmc.api.ModInitializer;
import net.xiaoyu233.fml.ModResourceManager;
import net.xiaoyu233.fml.reload.event.MITEEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Curios-MITE entry point. The inventory implementation is intentionally kept independent of NeoForge. */
public final class Curios implements ModInitializer {
    public static final String MOD_ID = "curios";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModResourceManager.addResourcePackDomain(MOD_ID);
        MITEEvents.MITE_EVENT_BUS.register(new CuriosNetwork());
        LOGGER.info("Curios-MITE {} initialized", CuriosConstants.VERSION);
    }

    public Curios() {}
}
