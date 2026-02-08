package dev.raidez;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

import dev.raidez.interactions.RotateBlockInteraction;
import dev.raidez.interactions.SwapBlockInteraction;

public class PowerHammerPlugin extends JavaPlugin {

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Config<PowerHammerConfig> config = withConfig("PowerHammerConfig", PowerHammerConfig.CODEC);

    private static PowerHammerPlugin instance;

    public PowerHammerPlugin(JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("PowerHammerPlugin is starting up!");
        config.save();

        getCodecRegistry(Interaction.CODEC)
                .register("SwapBlock", SwapBlockInteraction.class, SwapBlockInteraction.CODEC);
        getCodecRegistry(Interaction.CODEC)
                .register("RotateBlock", RotateBlockInteraction.class, RotateBlockInteraction.CODEC);
    }

    public static PowerHammerConfig getConfig() {
        return instance.config.get();
    }

}
