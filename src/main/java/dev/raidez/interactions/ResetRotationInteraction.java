package dev.raidez.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class ResetRotationInteraction extends SimpleBlockInteraction {

    public static final BuilderCodec<ResetRotationInteraction> CODEC = BuilderCodec
            .builder(ResetRotationInteraction.class, ResetRotationInteraction::new, SimpleBlockInteraction.CODEC)
            .build();

    @Override
    protected void interactWithBlock(
            World world,
            CommandBuffer<EntityStore> commandBuffer,
            InteractionType interactionType,
            InteractionContext context,
            ItemStack heldItemStack,
            Vector3i targetBlock,
            CooldownHandler cooldownHandler) {

        // 0. Extract data from context
        BlockType targetBlockType = world.getBlockType(targetBlock);
        long chunkIndex = ChunkUtil.indexChunkFromBlock(targetBlock.x, targetBlock.z);
        WorldChunk worldChunk = world.getChunk(chunkIndex);
        int targetBlockIndex = world.getBlock(targetBlock);

        // 2. Set the block's rotation to 0
        worldChunk.setBlock(
                targetBlock.x, targetBlock.y, targetBlock.z,
                targetBlockIndex, targetBlockType,
                0, 0, 0);
    }

    @Override
    protected void simulateInteractWithBlock(
            InteractionType interactionType,
            InteractionContext context,
            ItemStack heldItemStack,
            World world,
            Vector3i targetBlock) {
    }

}
