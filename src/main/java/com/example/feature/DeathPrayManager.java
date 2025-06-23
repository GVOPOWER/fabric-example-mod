package com.example.feature;

import com.example.ExampleMod;
import com.example.entity.WitherSkullEntity;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class DeathPrayManager {
    private static final Map<UUID, PlayerDeathPrayData> playerDataMap = new HashMap<>();
    private static final double ORBIT_RADIUS = 2.0;
    private static final double DETECTION_RANGE = 20.0;
    private static final int SKULL_COUNT = 3;
    private static final int COOLDOWN_TICKS = 40; // 2 seconds cooldown between shots

    public static void initialize() {
        // Register server tick event to update the wither skulls
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Process each player with active DeathPray
            Iterator<Map.Entry<UUID, PlayerDeathPrayData>> iterator = playerDataMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, PlayerDeathPrayData> entry = iterator.next();
                UUID playerId = entry.getKey();
                PlayerDeathPrayData data = entry.getValue();

                // Find the player
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
                if (player == null || !player.isAlive()) {
                    // Player is offline or dead, remove their data
                    iterator.remove();
                    continue;
                }

                // Update the player's DeathPray
                updatePlayerDeathPray(player, data);
            }
        });

        ExampleMod.LOGGER.info("DeathPray feature initialized");
    }

    /**
     * Toggles the DeathPray feature for a player
     * @param player The player
     * @return true if the feature was enabled, false if it was disabled
     */
    public static boolean toggleDeathPray(PlayerEntity player) {
        UUID playerId = player.getUuid();

        if (playerDataMap.containsKey(playerId)) {
            // Disable DeathPray
            PlayerDeathPrayData data = playerDataMap.get(playerId);

            // Remove all orbiting skulls
            for (WitherSkullEntity skull : data.orbitingSkulls) {
                skull.discard();
            }

            playerDataMap.remove(playerId);

            // Play disable sound
            player.getWorld().playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WITHER_DEATH,
                SoundCategory.PLAYERS,
                0.5F,
                1.5F
            );

            return false;
        } else {
            // Enable DeathPray
            PlayerDeathPrayData data = new PlayerDeathPrayData();
            playerDataMap.put(playerId, data);

            // Create the orbiting wither skulls
            if (!player.getWorld().isClient) {
                for (int i = 0; i < SKULL_COUNT; i++) {
                    double angle = data.orbitAngle + (i * (Math.PI * 2 / SKULL_COUNT));
                    double x = player.getX() + Math.cos(angle) * ORBIT_RADIUS;
                    double y = player.getY() + 1.5; // Slightly above player's head
                    double z = player.getZ() + Math.sin(angle) * ORBIT_RADIUS;

                    WitherSkullEntity skull = new WitherSkullEntity(player.getWorld(), player);
                    skull.setPosition(x, y, z);
                    skull.setVelocity(0, 0, 0); // No initial velocity
                    player.getWorld().spawnEntity(skull);
                    data.orbitingSkulls.add(skull);
                }
            }

            // Play enable sound
            player.getWorld().playSound(
                null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WITHER_SPAWN,
                SoundCategory.PLAYERS,
                0.5F,
                1.2F
            );

            return true;
        }
    }

    /**
     * Updates the DeathPray for a player
     */
    private static void updatePlayerDeathPray(ServerPlayerEntity player, PlayerDeathPrayData data) {
        ServerWorld world = player.getServerWorld();

        // Update cooldown
        if (data.cooldown > 0) {
            data.cooldown--;
        }

        // Update orbit angle
        data.orbitAngle += 0.05; // Speed of rotation
        if (data.orbitAngle > Math.PI * 2) {
            data.orbitAngle -= Math.PI * 2;
        }

        // Check if we need to recreate any skulls that might have been removed
        if (data.orbitingSkulls.size() < SKULL_COUNT) {
            // Some skulls are missing, recreate them
            while (data.orbitingSkulls.size() < SKULL_COUNT) {
                int index = data.orbitingSkulls.size();
                double angle = data.orbitAngle + (index * (Math.PI * 2 / SKULL_COUNT));
                double x = player.getX() + Math.cos(angle) * ORBIT_RADIUS;
                double y = player.getY() + 1.5; // Slightly above player's head
                double z = player.getZ() + Math.sin(angle) * ORBIT_RADIUS;

                WitherSkullEntity skull = new WitherSkullEntity(world, player);
                skull.setPosition(x, y, z);
                skull.setVelocity(0, 0, 0);
                world.spawnEntity(skull);
                data.orbitingSkulls.add(skull);
            }
        }

        // Remove any skulls that are no longer valid
        data.orbitingSkulls.removeIf(skull -> !skull.isAlive());

        // Update positions for the orbiting skulls
        for (int i = 0; i < data.orbitingSkulls.size(); i++) {
            WitherSkullEntity skull = data.orbitingSkulls.get(i);

            // Calculate new position
            double angle = data.orbitAngle + (i * (Math.PI * 2 / SKULL_COUNT));
            double x = player.getX() + Math.cos(angle) * ORBIT_RADIUS;
            double y = player.getY() + 1.5; // Slightly above player's head
            double z = player.getZ() + Math.sin(angle) * ORBIT_RADIUS;

            // Update skull position
            skull.setPosition(x, y, z);
            skull.setVelocity(0, 0, 0); // Keep velocity at zero for orbiting
        }

        // Find nearby hostile mobs
        if (data.cooldown <= 0 && !data.orbitingSkulls.isEmpty()) {
            Box detectionBox = new Box(
                player.getX() - DETECTION_RANGE, player.getY() - DETECTION_RANGE, player.getZ() - DETECTION_RANGE,
                player.getX() + DETECTION_RANGE, player.getY() + DETECTION_RANGE, player.getZ() + DETECTION_RANGE
            );

            List<Entity> nearbyEntities = world.getOtherEntities(player, detectionBox, 
                entity -> entity instanceof LivingEntity && !(entity instanceof PlayerEntity) && ((LivingEntity) entity).isAlive());

            if (!nearbyEntities.isEmpty()) {
                // Sort by distance to player
                nearbyEntities.sort(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(player)));

                // Target the closest entity
                Entity target = nearbyEntities.get(0);

                // Choose a skull to launch (rotate through them)
                data.lastSkullIndex = (data.lastSkullIndex + 1) % data.orbitingSkulls.size();
                WitherSkullEntity skull = data.orbitingSkulls.get(data.lastSkullIndex);

                // Get the launch position
                Vec3d launchPos = new Vec3d(skull.getX(), skull.getY(), skull.getZ());

                // Calculate direction to target
                Vec3d direction = target.getPos().add(0, target.getHeight() / 2, 0)
                    .subtract(launchPos).normalize();

                // Set velocity and target
                skull.setVelocity(direction.x, direction.y, direction.z, 1.0F, 0.0F);
                skull.setTarget(target);

                // Remove from orbiting list
                data.orbitingSkulls.remove(data.lastSkullIndex);

                // Play launch sound
                world.playSound(
                    null,
                    launchPos.x, launchPos.y, launchPos.z,
                    SoundEvents.ENTITY_WITHER_SHOOT,
                    SoundCategory.PLAYERS,
                    0.5F,
                    1.0F
                );

                // Set cooldown
                data.cooldown = COOLDOWN_TICKS;
            }
        }
    }

    /**
     * Data class to store DeathPray state for a player
     */
    private static class PlayerDeathPrayData {
        double orbitAngle = 0;
        int cooldown = 0;
        int lastSkullIndex = -1;
        List<WitherSkullEntity> orbitingSkulls = new ArrayList<>();
    }
}
