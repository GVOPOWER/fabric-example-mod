package com.example.dimension;

import com.example.ExampleMod;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.entity.Entity;

import com.example.dimension.NoStructureChunkGenerator;

import java.util.OptionalLong;

/**
 * Handles registration and teleportation for the Night Dimension.
 */
public class NightDimensionRegistration {

    /**
     * Registers the Night Dimension.
     * In Fabric 1.20.1, dimensions are registered through JSON files in the data directory.
     * We also need to register our custom chunk generator.
     */
    public static void register() {
        ExampleMod.LOGGER.info("Registering Night Dimension");

        // Register our custom chunk generator
        Registry.register(
            Registries.CHUNK_GENERATOR,
            new Identifier(ExampleMod.MOD_ID, "no_structure_generator"),
            NoStructureChunkGenerator.CODEC
        );

        // The actual dimension registration happens through JSON files in:
        // resources/data/nightdimension/dimension/night_dimension.json
        // resources/data/nightdimension/dimension_type/night_dimension.json
    }

    /**
     * Teleports an entity to the Night Dimension or back to the overworld.
     * 
     * @param entity The entity to teleport
     * @return True if teleportation was successful
     */
    public static boolean teleport(Entity entity) {
        if (entity.getWorld() instanceof ServerWorld serverWorld) {
            ServerWorld destination;

            // If in night dimension, go to overworld
            if (serverWorld.getRegistryKey() == NightDimension.DIMENSION_KEY) {
                destination = serverWorld.getServer().getWorld(World.OVERWORLD);
            } 
            // If in overworld, go to night dimension
            else if (serverWorld.getRegistryKey() == World.OVERWORLD) {
                destination = serverWorld.getServer().getWorld(NightDimension.DIMENSION_KEY);
            } 
            // If in neither, do nothing
            else {
                return false;
            }

            // If destination exists, teleport there
            if (destination != null) {
                BlockPos pos = new BlockPos(entity.getBlockPos().getX(), 100, entity.getBlockPos().getZ());

                // Find a safe spot to teleport to
                while (!destination.getBlockState(pos).isAir() || 
                       !destination.getBlockState(pos.up()).isAir() || 
                       destination.getBlockState(pos.down()).isAir()) {
                    pos = pos.down();
                    if (pos.getY() < destination.getBottomY()) {
                        pos = new BlockPos(pos.getX(), 100, pos.getZ());
                        break;
                    }
                }

                TeleportTarget target = new TeleportTarget(
                    new Vec3d(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5),
                    entity.getVelocity(),
                    entity.getYaw(),
                    entity.getPitch()
                );

                FabricDimensions.teleport(entity, destination, target);
                return true;
            }
        }
        return false;
    }
}
