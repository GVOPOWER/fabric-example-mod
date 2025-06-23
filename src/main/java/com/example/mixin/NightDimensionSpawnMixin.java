package com.example.mixin;

import com.example.dimension.NightDimension;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnHelper.class)
public class NightDimensionSpawnMixin {

    /**
     * Intercepts the canSpawn method to control which mobs can spawn in the Night Dimension.
     * Forces hostile mobs to always spawn, prevents all other mobs from spawning.
     */
    @Inject(method = "canSpawn", at = @At("HEAD"), cancellable = true)
    private static void onCanSpawn(ServerWorld world, SpawnGroup spawnGroup, StructureAccessor structureAccessor,
                                  ChunkGenerator chunkGenerator, SpawnSettings.SpawnEntry spawnEntry,
                                  BlockPos.Mutable pos, double squaredDistance, CallbackInfoReturnable<Boolean> cir) {
        // Check if we're in the night dimension
        if (world.getRegistryKey() == NightDimension.DIMENSION_KEY) {
            // Allow only hostile mobs to spawn
            if (spawnGroup == SpawnGroup.MONSTER) {
                // Force hostile mobs to always spawn in the night dimension
                cir.setReturnValue(true);
                return;
            } else {
                // Prevent all non-hostile mobs from spawning
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
