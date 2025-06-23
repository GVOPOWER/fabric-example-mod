package com.example.entity.client;

import com.example.entity.WaterballEntity;
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
public class WaterballEntityRenderer extends EntityRenderer<WaterballEntity> {
    private static final ItemStack HEART_OF_THE_SEA = new ItemStack(Items.HEART_OF_THE_SEA);
    private final ItemRenderer itemRenderer;

    public WaterballEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.15F;
        this.shadowOpacity = 0.75F;
    }

    @Override
    public void render(WaterballEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        
        // Scale down the item a bit
        matrices.scale(0.5f, 0.5f, 0.5f);
        
        // Rotate the item to face the camera
        matrices.multiply(this.dispatcher.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
        
        // Render the heart of the sea item as the waterball
        this.itemRenderer.renderItem(
            HEART_OF_THE_SEA,
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
    public Identifier getTexture(WaterballEntity entity) {
        // This is not used since we're rendering an item, but it's required by the abstract class
        return new Identifier("minecraft", "textures/item/heart_of_the_sea.png");
    }
}