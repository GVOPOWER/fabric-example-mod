package com.example.mixin;

import com.example.dimension.NightDimension;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityTickMixin {

    @Shadow public abstract EntityType<?> getType();
    @Shadow public abstract World getWorld();
    @Shadow public abstract void discard();

    /**
     * Intercepts the tick method to remove non-hostile entities from the Night Dimension.
     * This catches entities that have already spawned or been brought into the dimension.
     * Players, items, experience orbs, and projectiles are exempt from this check to allow
     * for normal gameplay in the dimension.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Entity self = (Entity)(Object)this;

        // Skip this check for players, items, experience orbs, and projectiles
        if (self instanceof PlayerEntity || 
            self instanceof ItemEntity || 
            self instanceof ExperienceOrbEntity ||
            self instanceof ProjectileEntity) {
            return;
        }

        // Check if we're in the night dimension
        if (this.getWorld().getRegistryKey() == NightDimension.DIMENSION_KEY) {
            // Get the entity type and spawn group
            EntityType<?> entityType = this.getType();
            SpawnGroup spawnGroup = entityType.getSpawnGroup();

            // If this is not a hostile mob, remove it from the world
            if (spawnGroup != SpawnGroup.MONSTER) {
                // Only allow hostile mobs in the Night Dimension
                this.discard();
            }
        }
    }
}
