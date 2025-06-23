package com.example.entity.client;

import com.example.entity.WitherSkullEntity;
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
public class WitherSkullEntityRenderer extends EntityRenderer<WitherSkullEntity> {
    private static final ItemStack WITHER_SKULL_ITEM = new ItemStack(Items.WITHER_SKELETON_SKULL);
    private final ItemRenderer itemRenderer;

    public WitherSkullEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.375F; // Increased by 2.5x from original 0.15F
        this.shadowOpacity = 0.75F;
    }

    @Override
    public void render(WitherSkullEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();

        // Scale the item (increased by 2.5x from original 0.5f)
        matrices.scale(1.25f, 1.25f, 1.25f);

        // Rotate the item to face the camera
        matrices.multiply(this.dispatcher.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));

        // Add a spinning effect
        float spinSpeed = 3.0f;
        float angle = (entity.age + tickDelta) * spinSpeed % 360;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));

        // Render the wither skull item
        this.itemRenderer.renderItem(
            WITHER_SKULL_ITEM,
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
    public Identifier getTexture(WitherSkullEntity entity) {
        // This is not used since we're rendering an item, but it's required by the abstract class
        return new Identifier("minecraft", "textures/item/wither_skeleton_skull.png");
    }
}
