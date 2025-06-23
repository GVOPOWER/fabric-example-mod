package com.example.dimension.client;

import com.example.ExampleMod;
import com.example.dimension.NightDimension;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.random.Random;

/**
 * Handles spawning ambient particles in the Night Dimension.
 * This class is client-side only.
 */
@Environment(EnvType.CLIENT)
public class NightDimensionParticleHandler {

    private static final int PARTICLE_SPAWN_INTERVAL = 5; // Spawn particles every 5 ticks
    private static int tickCounter = 0;

    /**
     * Registers the client tick event handler for spawning particles.
     */
    public static void register() {
        ExampleMod.LOGGER.info("Registering Night Dimension particle handler");

        // Register client tick event for particles
        ClientTickEvents.END_CLIENT_TICK.register(NightDimensionParticleHandler::onClientTick);
    }

    /**
     * Handles client-side tick events.
     * Used to spawn ambient particles in the Night Dimension.
     */
    private static void onClientTick(MinecraftClient client) {
        // Check if player is in-game and in the night dimension
        if (client.world == null || client.player == null || 
            client.world.getRegistryKey() != NightDimension.DIMENSION_KEY) {
            return;
        }

        tickCounter++;

        // Spawn particles periodically
        if (tickCounter % PARTICLE_SPAWN_INTERVAL == 0) {
            Random random = client.world.getRandom();
            PlayerEntity player = client.player;

            // Spawn particles around the player
            for (int i = 0; i < 7; i++) { // Spawn 7 particles per batch (40% increase from 5)
                // Calculate random position around player
                double x = player.getX() + (random.nextDouble() - 0.5) * 16.0;
                double y = player.getY() + (random.nextDouble() - 0.5) * 16.0;
                double z = player.getZ() + (random.nextDouble() - 0.5) * 16.0;

                // Calculate random velocity
                double vx = (random.nextDouble() - 0.5) * 0.1;
                double vy = (random.nextDouble() - 0.5) * 0.1;
                double vz = (random.nextDouble() - 0.5) * 0.1;

                // Spawn dark particle (using smoke as it's dark)
                client.world.addParticle(
                    ParticleTypes.SMOKE, // Dark smoke particle
                    x, y, z, // Position
                    vx, vy, vz // Velocity
                );
            }
        }
    }
}
