package com.example.dimension;

import com.example.ExampleMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Handles emptying chests in the Night Dimension to prevent double loot.
 */
public class NightDimensionChestHandler {
    
    private static final int CHEST_CHECK_INTERVAL = 20; // Check every second (20 ticks)
    private static int tickCounter = 0;
    
    /**
     * Registers the server tick event handler for emptying chests.
     */
    public static void register() {
        ExampleMod.LOGGER.info("Registering Night Dimension chest handler");
        
        // Register server tick event for emptying chests
        ServerTickEvents.END_WORLD_TICK.register(NightDimensionChestHandler::onWorldTick);
    }
    
    /**
     * Handles server-side world tick events.
     * Used to periodically check for and empty chests in the Night Dimension.
     */
    private static void onWorldTick(ServerWorld world) {
        // Only process in the night dimension
        if (world.getRegistryKey() != NightDimension.DIMENSION_KEY) {
            return;
        }
        
        tickCounter++;
        
        // Check for chests periodically to avoid performance impact
        if (tickCounter % CHEST_CHECK_INTERVAL == 0) {
            // Get all players in the dimension
            List<ServerPlayerEntity> players = world.getPlayers();
            
            for (ServerPlayerEntity player : players) {
                // Check blocks around the player
                BlockPos playerPos = player.getBlockPos();
                int radius = 32; // Check in a 32 block radius
                
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            BlockPos pos = playerPos.add(x, y, z);
                            
                            // Skip if the block is too far away (optimization)
                            if (x*x + y*y + z*z > radius*radius) {
                                continue;
                            }
                            
                            // Check if the block is a container
                            BlockEntity blockEntity = world.getBlockEntity(pos);
                            if (blockEntity instanceof Inventory) {
                                Inventory inventory = (Inventory) blockEntity;
                                
                                // Empty the inventory
                                for (int i = 0; i < inventory.size(); i++) {
                                    inventory.setStack(i, ItemStack.EMPTY);
                                }
                                
                                // Mark the block entity as dirty to save changes
                                blockEntity.markDirty();
                            }
                        }
                    }
                }
            }
        }
    }
}