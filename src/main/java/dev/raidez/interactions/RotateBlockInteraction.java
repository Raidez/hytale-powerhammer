package dev.raidez.interactions;

import java.util.Optional;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.packets.interface_.NotificationStyle;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.VariantRotation;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;

import it.unimi.dsi.fastutil.Pair;

public class RotateBlockInteraction extends SimpleBlockInteraction {

    public static final BuilderCodec<RotateBlockInteraction> CODEC = BuilderCodec
            .builder(RotateBlockInteraction.class, RotateBlockInteraction::new, SimpleBlockInteraction.CODEC)
            .build();

    public enum RotateType {
        HORIZONTAL,
        VERTICAL;
    }

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
        var ref = context.getEntity();
        PlayerRef playerRef = commandBuffer.getComponent(ref, PlayerRef.getComponentType());
        var movementStates = commandBuffer.getComponent(ref, MovementStatesComponent.getComponentType());
        boolean isCrouching = Optional.ofNullable(movementStates)
                .map(MovementStatesComponent::getMovementStates)
                .map(ms -> ms.crouching).orElse(false);

        // 1. Check if the block is rotatable
        BlockType targetBlockType = world.getBlockType(targetBlock);
        VariantRotation variantRotation = targetBlockType.getVariantRotation();
        if (VariantRotation.None.equals(variantRotation)) {
            NotificationUtil.sendNotification(
                    playerRef.getPacketHandler(),
                    Message.raw("This block cannot be rotated."),
                    NotificationStyle.Warning);
            return;
        }

        // 2. Rotate block
        // 2.1 Get the target chunk
        long chunkIndex = ChunkUtil.indexChunkFromBlock(targetBlock.x, targetBlock.z);
        WorldChunk worldChunk = world.getChunk(chunkIndex);

        // 2.2 Calculate the new rotation index
        int rotationIndex = world.getBlockRotationIndex(targetBlock.x, targetBlock.y, targetBlock.z);
        var newRotation = calculateNewRotationIndex(targetBlock, rotationIndex, isCrouching);
        int newRotationIndex = newRotation.right();

        // 2.3 Set the block with the new rotation index
        int targetBlockIndex = world.getBlock(targetBlock);
        worldChunk.setBlock(
                targetBlock.x, targetBlock.y, targetBlock.z,
                targetBlockIndex, targetBlockType,
                newRotationIndex, 0, 0);
    }

    /**
     * Calculates the new rotation index based on the current rotation index, rotate
     * type, and direction.
     * 
     * @param targetBlock
     * @param rotationIndex
     * @param isCrouching
     * @return
     */
    private Pair<RotationTuple, Integer> calculateNewRotationIndex(
            Vector3i targetBlock,
            int rotationIndex,
            boolean isCrouching) {

        RotationTuple rotationTuple = RotationTuple.get(rotationIndex);
        RotateType rotateType = (!isCrouching) ? RotateType.HORIZONTAL : RotateType.VERTICAL;

        RotationTuple newRotationTuple = rotate(rotationTuple, rotateType);
        int newRotationIndex = newRotationTuple.index();

        return Pair.of(newRotationTuple, newRotationIndex);
    }

    /**
     * Rotates the given rotation tuple based on the rotate type.
     * 
     * @param currentRotation
     * @param rotateType
     * @return
     */
    private RotationTuple rotate(RotationTuple currentRotation, RotateType rotateType) {
        Rotation delta = Rotation.None.subtract(Rotation.Ninety);
        RotationTuple newRotation = RotationTuple.of(
                currentRotation.yaw(),
                currentRotation.pitch(),
                currentRotation.roll());

        return switch (rotateType) {
            case HORIZONTAL -> RotationTuple.of(
                    newRotation.yaw().add(delta),
                    newRotation.pitch(),
                    newRotation.roll());
            case VERTICAL -> RotationTuple.of(
                    newRotation.yaw(),
                    newRotation.pitch(),
                    newRotation.roll().add(delta));
            default -> RotationTuple.of(Rotation.None, Rotation.None, Rotation.None);
        };
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
