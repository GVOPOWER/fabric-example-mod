package com.example.entity.client;

import com.example.entity.BlockThrowEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

@Environment(EnvType.CLIENT)
public class BlockThrowEntityRenderer extends EntityRenderer<BlockThrowEntity> {
    private final ItemRenderer itemRenderer;
    private static final ItemStack FALLBACK_ITEM = new ItemStack(Items.STONE); // Fallback item if no block is available

    public BlockThrowEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.5F; // Shadow for standard block size
        this.shadowOpacity = 0.75F;
    }

    @Override
    public void render(BlockThrowEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        // Get the item stack from the entity using the getter method
        ItemStack blockStack;
        try {
            blockStack = entity.getBlockStack();
            if (blockStack == null || blockStack.isEmpty()) {
                blockStack = FALLBACK_ITEM;
            }
        } catch (Exception e) {
            blockStack = FALLBACK_ITEM;
        }

        // Scale to match a standard block size
        matrices.scale(1.0f, 1.0f, 1.0f);

        // Rotate the item to face the camera and add some rotation for visual effect
        matrices.multiply(this.dispatcher.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));

        // Add a spinning effect
        float spinSpeed = 2.0f;
        float angle = (entity.age + tickDelta) * spinSpeed % 360;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));

        // Render the block item
        this.itemRenderer.renderItem(
            blockStack,
            ModelTransformationMode.GROUND,
            light,
            OverlayTexture.DEFAULT_UV,
            matrices,
            vertexConsumers,
            entity.getWorld(),
            entity.getId()
        );

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(BlockThrowEntity entity) {
        // This is not used since we're rendering an item, but it's required by the abstract class
        return new Identifier("minecraft", "textures/block/stone.png");
    }
}
