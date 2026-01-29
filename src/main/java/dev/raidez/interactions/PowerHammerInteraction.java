package dev.raidez.interactions;

import java.util.Optional;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockSoundEvent;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocksound.config.BlockSoundSet;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
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

        Ref<EntityStore> ref = context.getEntity();
        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        ItemContainer storage = player.getInventory().getHotbar();
        Short heldItemIndex = (short) context.getHeldItemSlot();

        // Check if the target block is wood
        BlockType targetBlockType = world.getBlockType(targetBlock);
        String targetBlockGroup = Optional.ofNullable(targetBlockType).map(BlockType::getGroup).orElse(null);
        if (targetBlockType == null || targetBlockGroup == null || !targetBlockGroup.equals("Wood")) {
            PowerHammerPlugin.LOGGER.atWarning().log("Power Hammer can only be used on wood blocks!");
            return;
        }

        // Get the first block next to the powerhammer
        Short closestIndex = getClosestBlockType(storage, heldItemIndex, targetBlockType.getId(), "Wood");
        ItemStack selectedBlockIS = Optional.ofNullable(closestIndex).map(storage::getItemStack).orElse(null);
        if (closestIndex == null || selectedBlockIS == null) {
            PowerHammerPlugin.LOGGER.atWarning().log("No wood blocks found in hotbar!");
            return;
        }

        // Replace the block at targetBlock with the selected block from the hotbar
        world.setBlock(targetBlock.x, targetBlock.y, targetBlock.z, selectedBlockIS.getBlockKey());

        // Reduce the item count of the selected block in the hotbar by 1
        storage.setItemStackForSlot(closestIndex, selectedBlockIS.withQuantity(selectedBlockIS.getQuantity() - 1));

        // Give the target block back to the player
        storage.addItemStack(new ItemStack(targetBlockType.getId()));

        // Update hammer durability
        player.updateItemStackDurability(ref, heldItemStack, storage, heldItemIndex,
                -heldItemStack.getItem().getDurabilityLossOnHit(), commandBuffer);

        // Play sound effect
        BlockSoundSet soundSet = BlockSoundSet.getAssetMap().getAsset(targetBlockType.getBlockSoundSetIndex());
        int soundEventIndex = Optional.ofNullable(soundSet).map(BlockSoundSet::getSoundEventIndices)
                .map((indices) -> indices.getOrDefault(BlockSoundEvent.Hit, 0)).orElse(0);
        if (soundSet == null || soundEventIndex == 0) {
            PowerHammerPlugin.LOGGER.atWarning().log("No sound set found for block: " +
                    targetBlockType.getId());
            return;
        }
        SoundUtil.playSoundEvent3d(ref, soundEventIndex, (double) targetBlock.x +
                0.5,
                (double) targetBlock.y + 0.5, (double) targetBlock.z + 0.5, commandBuffer);
    }

    @Override
    protected void simulateInteractWithBlock(
            InteractionType interactionType,
            InteractionContext interactionContext,
            ItemStack itemStack,
            World world,
            Vector3i vector3i) {
    }

    private Short getClosestBlockType(ItemContainer storage, Short heldItemIndex, String targetBlockId,
            String group) {
        Short closestIndex = null;
        Integer closestDistance = null;
        for (short i = 0; i < storage.getCapacity(); i++) {
            ItemStack itemStack = storage.getItemStack(i);
            String blockId = Optional.ofNullable(itemStack).map(ItemStack::getBlockKey).orElse(null);
            if (itemStack == null || blockId == null) {
                continue;
            }

            // Check if the item is a wood block
            BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
            String blockGroup = Optional.ofNullable(blockType).map(BlockType::getGroup).orElse(null);
            if (blockType == null || blockGroup == null || !blockGroup.equals(group)
                    || blockType.getId().equals(targetBlockId)) {
                continue;
            }

            // Calculate closest block to the powerhammer
            Integer distance = Math.abs(i - heldItemIndex);
            if (closestIndex == null || distance < closestDistance) {
                closestIndex = i;
                closestDistance = distance;
            }
        }

        return closestIndex;
    }
}
