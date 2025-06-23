package com.example.mixin.client;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    
    /**
     * This mixin ensures that all entities, including mobs and other players,
     * are properly rendered by the entity render dispatcher.
     */
    @Inject(
        method = "render",
        at = @At("HEAD")
    )
    private void onRenderEntity(Entity entity, double x, double y, double z, 
                               float yaw, float tickDelta, MatrixStack matrices, 
                               VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        // Ensure entity is not invisible
        if (entity.isInvisible()) {
            entity.setInvisible(false);
        }
        
        // Log entity rendering for debugging
        if (entity instanceof MobEntity) {
            System.out.println("[DEBUG_LOG] Rendering mob: " + entity.getType().getName().getString());
        } else if (entity instanceof PlayerEntity) {
            System.out.println("[DEBUG_LOG] Rendering player: " + entity.getName().getString());
        }
    }
}