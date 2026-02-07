package dev.raidez.interactions;

import java.util.Optional;
import java.util.stream.Stream;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockSoundEvent;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocksound.config.BlockSoundSet;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import dev.raidez.PowerHammerConfig;
import dev.raidez.PowerHammerPlugin;

public class SwapBlockInteraction extends SimpleBlockInteraction {

    private final PowerHammerConfig config;

    public static final BuilderCodec<SwapBlockInteraction> CODEC = BuilderCodec.builder(
            SwapBlockInteraction.class,
            () -> new SwapBlockInteraction(PowerHammerPlugin.getConfig()),
            SimpleBlockInteraction.CODEC).build();

    public SwapBlockInteraction(PowerHammerConfig config) {
        this.config = config;
    }

    @Override
    protected void interactWithBlock(
            World world,
            CommandBuffer<EntityStore> commandBuffer,
            InteractionType interactionType,
            InteractionContext interactionContext,
            ItemStack heldItemStack,
            Vector3i targetBlock,
            CooldownHandler cooldownHandler) {

        // 0. Extract data from context
        var ref = interactionContext.getEntity();
        var store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        short heldItemSlot = interactionContext.getHeldItemSlot();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        ItemContainer hotbar = player.getInventory().getHotbar();
        var storage = player.getInventory().getCombinedEverything();

        // 1. Get the target block
        BlockType targetBlockType = world.getBlockType(targetBlock);
        if (targetBlockType == null) {
            NotificationUtil.sendNotification(
                    playerRef.getPacketHandler(),
                    Message.raw("Target block type is null!"),
                    NotificationStyle.Warning);
            return;
        }

        // 2. Check if the block is swappable
        // String targetBlockId = targetBlockType.getDefaultStateKey();
        String targetBlockId = targetBlockType.getId();
        String blockPattern = isBlockSwappable(targetBlockId);
        if (blockPattern == null) {
            NotificationUtil.sendNotification(
                    playerRef.getPacketHandler(),
                    Message.raw("Target block is not swappable!"),
                    NotificationStyle.Warning);
            return;
        }

        // 3. Get the closest matching block from the hotbar
        short matchedSlot = findClosestMatchingBlock(hotbar, heldItemSlot, blockPattern, targetBlockId);
        if (matchedSlot == -1) {
            NotificationUtil.sendNotification(
                    playerRef.getPacketHandler(),
                    Message.raw("No matching block found in hotbar!"),
                    NotificationStyle.Warning);
            return;
        }

        // 4. Swap the blocks
        ItemStack matchedItemStack = hotbar.getItemStack(matchedSlot);
        swapBlock(world, targetBlock, matchedItemStack.getBlockKey());

        // 5. Update states (block quantity, tool durability, etc.)
        // 5.1. Decrease quantity of the swapped-in block
        int quantity = matchedItemStack.getQuantity();
        hotbar.setItemStackForSlot(matchedSlot, matchedItemStack.withQuantity(quantity - 1));

        // 5.2. Increase quantity of the swapped-out block
        storage.addItemStack(new ItemStack(targetBlockId));

        // 5.3. Apply durability damage to the tool if applicable
        double durabilityLoss = heldItemStack.getItem().getDurabilityLossOnHit();
        player.updateItemStackDurability(ref, heldItemStack, hotbar, heldItemSlot, durabilityLoss, commandBuffer);

        // 6. Play sound effect
        playSoundEffect(ref, world, targetBlock, targetBlockType, commandBuffer);

    }

    // #region

    /**
     * Swap the target block with the selected block
     * 
     * @param world
     * @param targetBlock
     * @param blockId
     */
    @SuppressWarnings({ "removal", "deprecation" })
    private void swapBlock(World world, Vector3i targetBlock, String blockId) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(targetBlock.x, targetBlock.z);
        WorldChunk worldChunk = world.getChunk(chunkIndex);

        int index = BlockType.getAssetMap().getIndex(blockId);
        BlockType blockType = BlockType.getAssetMap().getAsset(index);

        int rotation = worldChunk.getRotationIndex(targetBlock.x, targetBlock.y, targetBlock.z);
        int filler = worldChunk.getBlockChunk().getSectionAtBlockY(targetBlock.y)
                .getFiller(targetBlock.x, targetBlock.y, targetBlock.z);

        worldChunk.setBlock(targetBlock.x, targetBlock.y, targetBlock.z, index, blockType, rotation, filler, 0);
    }

    /**
     * Play the block hit sound effect at the target block location.
     * 
     * @param ref
     * @param world
     * @param targetBlock
     * @param targetBlockType
     * @param commandBuffer
     */
    private void playSoundEffect(
            Ref<EntityStore> ref,
            World world,
            Vector3i targetBlock,
            BlockType targetBlockType,
            CommandBuffer<EntityStore> commandBuffer) {

        // Get the sound event index for the block hit sound
        var soundSet = BlockSoundSet.getAssetMap().getAsset(targetBlockType.getBlockSoundSetIndex());
        int soundEventIndex = Optional.ofNullable(soundSet)
                .map(BlockSoundSet::getSoundEventIndices)
                .map((indices) -> indices.getOrDefault(BlockSoundEvent.Hit, 0))
                .orElse(0);
        if (soundEventIndex == 0) {
            PowerHammerPlugin.LOGGER.atWarning().log("No sound set found for block: " +
                    targetBlockType.getId());
            return;
        }

        // Play the sound event at the block position
        double x = (double) targetBlock.x + 0.5;
        double y = (double) targetBlock.y + 0.5;
        double z = (double) targetBlock.z + 0.5;
        SoundUtil.playSoundEvent3d(ref, soundEventIndex, x, y, z, commandBuffer);
    }

    /**
     * Find the closest matching block in the player's hotbar that matches the
     * swappable block pattern.
     * 
     * @param storage
     * @param itemSlot
     * @param blockPattern
     * @param targetBlockId
     * @return Matched item slot or -1 if not found
     */
    private short findClosestMatchingBlock(
            ItemContainer storage,
            short itemSlot,
            String blockPattern,
            String targetBlockId) {

        short closestSlot = -1;
        short closestDistance = Short.MAX_VALUE;

        for (short i = 0; i < storage.getCapacity(); i++) {
            // Check if the item matches the block pattern
            ItemStack itemStack = storage.getItemStack(i);
            String blockId = itemStack != null ? itemStack.getBlockKey() : null;
            if (blockId == null || blockId.equals(targetBlockId) || !blockId.matches(blockPattern)) {
                continue;
            }

            // Calculate distance from the item slot
            short distance = (short) Math.abs(i - itemSlot);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestSlot = i;
            }
        }

        return closestSlot;
    }

    /**
     * Use regex matching to determine if the block ID matches any pattern in the
     * swap set.
     * 
     * @param blockId
     * @return The matching pattern if swappable, null otherwise.
     */
    private String isBlockSwappable(String blockId) {
        return Stream.of(config.getSwapSet())
                .filter(pattern -> blockId.matches(pattern))
                .findFirst()
                .orElse(null);
    }

    // #endregion

    @Override
    protected void simulateInteractWithBlock(
            InteractionType interactionType,
            InteractionContext interactionContext,
            ItemStack itemStack,
            World world,
            Vector3i vector3i) {
    }

}
