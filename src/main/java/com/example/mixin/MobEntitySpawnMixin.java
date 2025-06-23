package com.example.mixin;

import com.example.dimension.NightDimension;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEntity.class)
public class MobEntitySpawnMixin {

    /**
     * Intercepts the canSpawn method to force hostile mobs to always spawn and prevent 
     * non-hostile mobs from spawning in the Night Dimension.
     */
    @Inject(method = "canSpawn", at = @At("HEAD"), cancellable = true)
    private void onCanSpawn(WorldAccess world, SpawnReason reason, CallbackInfoReturnable<Boolean> cir) {
        // Check if we're in the night dimension
        if (world instanceof net.minecraft.world.World && 
            ((net.minecraft.world.World)world).getRegistryKey() == NightDimension.DIMENSION_KEY) {
            // Get the entity type and spawn group
            MobEntity self = (MobEntity)(Object)this;
            EntityType<?> entityType = self.getType();
            SpawnGroup spawnGroup = entityType.getSpawnGroup();

            // Force hostile mobs to always spawn and prevent non-hostile mobs from spawning
            if (spawnGroup == SpawnGroup.MONSTER) {
                // Force hostile mobs to always spawn
                cir.setReturnValue(true);
            } else {
                // Prevent all non-hostile mobs from spawning
                cir.setReturnValue(false);
            }
        }
    }
}
