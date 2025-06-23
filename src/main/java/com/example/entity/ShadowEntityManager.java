package com.example.entity;

import com.example.ExampleMod;
import com.example.dimension.NightDimension;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Manages shadow entities in the night dimension that mirror entities from the overworld.
 */
public class ShadowEntityManager {
    // Map to track shadow entities by their original entity's UUID
    private static final Map<UUID, UUID> ORIGINAL_TO_SHADOW_MAP = new HashMap<>();

    /**
     * Updates all shadow entities to match their original entities.
     * Creates new shadows for entities that don't have one yet.
     * Removes shadows whose original entities no longer exist.
     */
    public static void updateShadowEntities(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(net.minecraft.world.World.OVERWORLD);
        ServerWorld nightDimension = server.getWorld(NightDimension.DIMENSION_KEY);

        if (overworld == null || nightDimension == null) {
            return; // One of the worlds doesn't exist
        }

        // Process all entities in the overworld
        // We can't use getEntitiesByClass with a lambda, so we'll use a different approach
        for (Entity entity : overworld.getPlayers()) {
            // Process players first

            UUID originalUuid = entity.getUuid();

            // Check if this entity already has a shadow
            if (ORIGINAL_TO_SHADOW_MAP.containsKey(originalUuid)) {
                UUID shadowUuid = ORIGINAL_TO_SHADOW_MAP.get(originalUuid);
                Entity shadowEntity = nightDimension.getEntity(shadowUuid);

                if (shadowEntity instanceof ShadowEntity) {
                    // Update existing shadow's position
                    ShadowEntity.updateShadowPosition((ShadowEntity) shadowEntity, entity);
                } else {
                    // Shadow entity no longer exists, remove from map
                    ORIGINAL_TO_SHADOW_MAP.remove(originalUuid);
                    ShadowEntity.removeShadowMapping(shadowUuid);
                }
            } else {
                // Create a new shadow for this entity
                ShadowEntity shadowEntity = ShadowEntity.createShadow(nightDimension, entity);
                nightDimension.spawnEntity(shadowEntity);

                // Track the new shadow
                ORIGINAL_TO_SHADOW_MAP.put(originalUuid, shadowEntity.getUuid());

                ExampleMod.LOGGER.info("Created shadow entity for " + 
                    (entity instanceof PlayerEntity ? 
                        ((PlayerEntity) entity).getName().getString() : 
                        entity.getType().getName().getString()));
            }
        }

        // Clean up shadows whose original entities no longer exist
        cleanupOrphanedShadows(overworld, nightDimension);
    }

    /**
     * Removes shadow entities whose original entities no longer exist
     */
    private static void cleanupOrphanedShadows(ServerWorld overworld, ServerWorld nightDimension) {
        Iterator<Map.Entry<UUID, UUID>> iterator = ORIGINAL_TO_SHADOW_MAP.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, UUID> entry = iterator.next();
            UUID originalUuid = entry.getKey();
            UUID shadowUuid = entry.getValue();

            // Check if original entity still exists
            Entity originalEntity = overworld.getEntity(originalUuid);
            if (originalEntity == null) {
                // Original entity is gone, remove its shadow
                Entity shadowEntity = nightDimension.getEntity(shadowUuid);
                if (shadowEntity != null) {
                    shadowEntity.discard();
                    ExampleMod.LOGGER.info("Removed orphaned shadow entity");
                }

                // Remove from tracking maps
                iterator.remove();
                ShadowEntity.removeShadowMapping(shadowUuid);
            }
        }
    }

    /**
     * Removes all shadow entities from the night dimension
     */
    public static void removeAllShadows(ServerWorld nightDimension) {
        if (nightDimension == null) return;

        for (UUID shadowUuid : ORIGINAL_TO_SHADOW_MAP.values()) {
            Entity shadowEntity = nightDimension.getEntity(shadowUuid);
            if (shadowEntity != null) {
                shadowEntity.discard();
            }
            ShadowEntity.removeShadowMapping(shadowUuid);
        }

        ORIGINAL_TO_SHADOW_MAP.clear();
        ExampleMod.LOGGER.info("Removed all shadow entities");
    }
}
