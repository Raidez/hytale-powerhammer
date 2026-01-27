package dev.raidez;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import dev.raidez.interactions.PowerHammerInteraction;

public class PowerHammerPlugin extends JavaPlugin {

    public final static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public PowerHammerPlugin(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setup Power Hammer plugin!");
        getCodecRegistry(Interaction.CODEC)
                .register("PowerHammer", PowerHammerInteraction.class, PowerHammerInteraction.CODEC);
    }

}
