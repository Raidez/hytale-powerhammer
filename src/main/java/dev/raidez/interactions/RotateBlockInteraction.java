package dev.raidez.interactions;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
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
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.NotificationUtil;

public class RotateBlockInteraction extends SimpleBlockInteraction {

    public static final BuilderCodec<RotateBlockInteraction> CODEC = BuilderCodec
            .builder(RotateBlockInteraction.class, RotateBlockInteraction::new)
            .append(new KeyedCodec<>("RotationType", new EnumCodec<>(RotateType.class)),
                    (data, value) -> data.rotateType = value,
                    (data) -> data.rotateType)
            .add()
            .build();

    public enum RotateType {
        RESET,
        HORIZONTAL,
        VERTICAL;
    }

    private RotateType rotateType = RotateType.HORIZONTAL;

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
        var store = ref.getStore();
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

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
        RotationTuple rotationTuple = RotationTuple.get(rotationIndex);

        var newRotationTuple = switch (rotateType) {
            case HORIZONTAL -> RotationTuple.of(
                    rotationTuple.yaw().subtract(Rotation.Ninety),
                    rotationTuple.pitch(),
                    rotationTuple.roll());
            case VERTICAL -> RotationTuple.of(
                    rotationTuple.yaw(),
                    rotationTuple.pitch(),
                    rotationTuple.roll().subtract(Rotation.Ninety));
            case RESET -> RotationTuple.of(Rotation.None, Rotation.None, Rotation.None);
        };
        var newRotationIndex = newRotationTuple.index();

        // 2.3 Set the block with the new rotation index
        int targetBlockIndex = world.getBlock(targetBlock);
        worldChunk.setBlock(
                targetBlock.x, targetBlock.y, targetBlock.z,
                targetBlockIndex, targetBlockType,
                newRotationIndex, 0, 0);
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
