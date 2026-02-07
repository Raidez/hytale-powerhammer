package dev.raidez;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class PowerHammerConfig {

    public static final BuilderCodec<PowerHammerConfig> CODEC = BuilderCodec
            .builder(PowerHammerConfig.class, PowerHammerConfig::new)
            .append(new KeyedCodec<>("SwapSet", Codec.STRING_ARRAY),
                    (data, value) -> data.swapSet = value,
                    (data) -> data.swapSet)
            .add()
            .build();

    private String[] swapSet = {
            "Wood_.+_(Decorative|Ornate|Planks)",
            "Wood_.+_Beam",
            "Wood_.+_Stairs",
            "Wood_.+_Planks_Half",
            "Wood_.+_Fence",
            "Wood_.+_Fence_Gate",
            "Wood_.+_Roof",
            "Wood_.+_Roof_Flat",
            "Wood_.+_Roof_Hollow",
            "Wood_.+_Roof_Shallow",
            "Wood_.+_Roof_Steep",
            "Rock_.+_Brick(_(Decorative|Ornate|Smooth))?",
    };

    public String[] getSwapSet() {
        return swapSet;
    }

}
