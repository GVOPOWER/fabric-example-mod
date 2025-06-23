package com.example.mixin.client;

import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public class EntityCullingMixin {

    /**
     * This mixin ensures that entities are not culled incorrectly.
     * It forces the frustum culling check to always return true for entities,
     * ensuring they are always rendered regardless of whether they're in the player's view.
     */
    @Inject(
        method = "isVisible",
        at = @At("RETURN"),
        cancellable = true
    )
    private void onIsVisible(net.minecraft.util.math.Box box, CallbackInfoReturnable<Boolean> cir) {
        // Always return true to ensure entities are not culled
        if (!cir.getReturnValue()) {
            cir.setReturnValue(true);
        }
    }
}
