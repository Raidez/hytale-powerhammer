package dev.raidez.interactions;

import java.util.HashMap;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import dev.raidez.PowerHammerPlugin;

public class PowerHammerInteraction extends SimpleBlockInteraction {

    public static final BuilderCodec<PowerHammerInteraction> CODEC = BuilderCodec
            .builder(PowerHammerInteraction.class, PowerHammerInteraction::new, SimpleBlockInteraction.CODEC)
            .build();

    @Override
    protected void interactWithBlock(
            World world,
            CommandBuffer<EntityStore> commandBuffer,
            InteractionType type,
            InteractionContext context,
            ItemStack heldItemStack,
            Vector3i targetBlock,
            CooldownHandler cooldownHandler) {

        var ref = context.getEntity();
        var store = ref.getStore();
        var player = store.getComponent(ref, Player.getComponentType());
        var blockType = world.getBlockType(targetBlock);

        // Get the first block next to the powerhammer
        Integer heldItemIndex = null;
        var blockDistances = new HashMap<String, Integer>();
        var storage = player.getInventory().getHotbar();
        for (short i = 0; i < storage.getCapacity(); i++) {
            var itemStack = storage.getItemStack(i);
            if (itemStack == null) {
                continue;
            }

            // Found the powerhammer in the hotbar
            var itemId = itemStack.getItemId();
            if (itemId.equals(heldItemStack.getItemId())) {
                heldItemIndex = (int) i;
                continue;
            }

            // Found a block item in the hotbar
            var item = Item.getAssetMap().getAsset(itemId);
            var blockId = item.getBlockId();
            if (blockId == null) {
                continue;
            }

            blockDistances.put(blockId, (int) i);
        }

        if (heldItemIndex == null) {
            PowerHammerPlugin.LOGGER.atWarning().log("Power Hammer not found in hotbar!");
            return;
        }

        // Find the closest block in the hotbar to the heldItemIndex
        String selectedBlockId = null;
        Integer closestDistance = null;
        Short blockIndex = null;
        for (var entry : blockDistances.entrySet()) {
            var blockId = entry.getKey();
            var index = entry.getValue();

            var distance = Math.abs(index - heldItemIndex);
            if (selectedBlockId == null || closestDistance == null || distance < closestDistance) {
                selectedBlockId = blockId;
                closestDistance = distance;
                blockIndex = (short) (int) index;
            }
        }

        if (selectedBlockId == null) {
            PowerHammerPlugin.LOGGER.atWarning().log("No blocks found in hotbar!");
            return;
        }

        // Replace the block at targetBlock with the selected block from the hotbar
        world.setBlock(targetBlock.x, targetBlock.y, targetBlock.z, selectedBlockId);

        // Reduce the item count of the selected block in the hotbar by 1
        var selectedBlockIS = storage.getItemStack(blockIndex);
        storage.setItemStackForSlot(blockIndex, selectedBlockIS.withQuantity(selectedBlockIS.getQuantity() - 1));

        // Give the target block back to the player
        storage.addItemStack(new ItemStack(blockType.getId()));
    }

    @Override
    protected void simulateInteractWithBlock(
            InteractionType interactionType,
            InteractionContext interactionContext,
            ItemStack itemStack,
            World world,
            Vector3i vector3i) {
    }

}
