package com.example.mixin.client;

import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class EntityRenderingMixin {
    
    /**
     * This mixin ensures that all entities, including mobs and other players,
     * are properly rendered in the world.
     */
    @Inject(
        method = "renderEntity",
        at = @At("HEAD"),
        cancellable = false
    )
    private void onRenderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, 
                               float tickDelta, net.minecraft.client.util.math.MatrixStack matrices, 
                               net.minecraft.client.render.VertexConsumerProvider vertexConsumers, 
                               CallbackInfo ci) {
        // Log entity rendering for debugging
        if (entity instanceof LivingEntity) {
            // Make sure the entity is visible
            entity.setInvisible(false);
            
            // Log player entities specifically
            if (entity instanceof PlayerEntity) {
                System.out.println("[DEBUG_LOG] Rendering player: " + entity.getName().getString());
            }
        }
    }
}