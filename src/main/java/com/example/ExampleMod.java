package com.example;

import com.example.dimension.NightDimension;
import com.example.dimension.NightDimensionChestHandler;
import com.example.dimension.NightDimensionRegistration;
import com.example.entity.BlockThrowEntity;
import com.example.entity.WaterballEntity;
import com.example.entity.WitherSkullEntity;
import com.example.feature.DeathPrayManager;
import com.example.item.WaterballItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleMod implements ModInitializer {
	public static final String MOD_ID = "nightdimension";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		// Register our dimension
		NightDimensionRegistration.register();

		// Register chest handler to empty chests in the night dimension
		NightDimensionChestHandler.register();

		// Register a command to teleport to the night dimension
		registerCommands();

		// Initialize waterball entity and item
		// The static fields in these classes will trigger the registration
		LOGGER.info("Registering Waterball entity and item");
		WaterballEntity.WATERBALL_ENTITY_TYPE.toString(); // Force class initialization
		WaterballItem.WATERBALL_ITEM.toString(); // Force class initialization

		// Initialize block throw entity
		LOGGER.info("Registering BlockThrow entity");
		BlockThrowEntity.BLOCKTHROW_ENTITY_TYPE.toString(); // Force class initialization

		// Initialize wither skull entity
		LOGGER.info("Registering WitherSkull entity");
		WitherSkullEntity.WITHER_SKULL_ENTITY_TYPE.toString(); // Force class initialization

		// Initialize DeathPray feature
		LOGGER.info("Initializing DeathPray feature");
		DeathPrayManager.initialize();


		// Register server tick event to check and remove expired water blocks
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			// Check each world for expired water blocks
			server.getWorlds().forEach(world -> {
				WaterballEntity.checkAndRemoveExpiredWaterBlocks(world);
			});
		});

		LOGGER.info("Night Dimension mod initialized!");
	}

	/**
	 * Registers commands for the mod.
	 */
	private void registerCommands() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			// Command to teleport to the night dimension
			dispatcher.register(
				CommandManager.literal("nightdimension")
					.executes(context -> {
						// Attempt to teleport the player
						boolean success = NightDimensionRegistration.teleport(context.getSource().getPlayer());

						if (success) {
							context.getSource().sendFeedback(() -> Text.literal("Teleported to " + 
								(context.getSource().getWorld().getRegistryKey() == NightDimension.DIMENSION_KEY ? 
								"Overworld" : "Night Dimension")), false);
						} else {
							context.getSource().sendError(Text.literal("Teleportation failed!"));
						}

						return 1;
					})
			);

			// Command to give the player a waterball item
			dispatcher.register(
				CommandManager.literal("waterball")
					.executes(context -> {
						if (context.getSource().getPlayer() != null) {
							// Give the player a waterball item
							context.getSource().getPlayer().giveItemStack(
								new net.minecraft.item.ItemStack(WaterballItem.WATERBALL_ITEM, 16)
							);
							context.getSource().sendFeedback(() -> Text.literal("Gave 16 Waterballs"), false);
						}
						return 1;
					})
			);

			// Command to throw the block in the player's offhand
			dispatcher.register(
				CommandManager.literal("OriginGVO")
					.then(CommandManager.literal("Blockthrow")
						.executes(context -> {
							if (context.getSource().getPlayer() != null) {
								net.minecraft.entity.player.PlayerEntity player = context.getSource().getPlayer();
								net.minecraft.item.ItemStack offhandStack = player.getOffHandStack();

								// Check if the offhand item is a block
								if (!offhandStack.isEmpty() && offhandStack.getItem() instanceof net.minecraft.item.BlockItem) {
									// Create and throw the block entity
									BlockThrowEntity blockEntity = new BlockThrowEntity(player.getWorld(), player, offhandStack);

									// Set the velocity based on player's look direction
									// Use the player's actual pitch for a straight trajectory
									blockEntity.setVelocity(player, player.getPitch(), player.getYaw(), 0.0F, 1.5F, 1.0F);

									// No additional velocity modifications to ensure a straight trajectory
									// Gravity will naturally create an arch

									player.getWorld().spawnEntity(blockEntity);

									// Play throw sound
									player.getWorld().playSound(
										null, 
										player.getX(), player.getY(), player.getZ(), 
										net.minecraft.sound.SoundEvents.ENTITY_SNOWBALL_THROW, 
										net.minecraft.sound.SoundCategory.PLAYERS, 
										1.0F, 
										0.4F / (player.getWorld().getRandom().nextFloat() * 0.4F + 0.8F)
									);

									// Reduce stack size if not in creative mode
									if (!player.getAbilities().creativeMode) {
										offhandStack.decrement(1);
									}

									context.getSource().sendFeedback(() -> Text.literal("Threw block!"), false);
								} else {
									context.getSource().sendError(Text.literal("You need to hold a block in your offhand!"));
								}
							}
							return 1;
						})
					)
					.then(CommandManager.literal("DeathPray")
						.executes(context -> {
							if (context.getSource().getPlayer() != null) {
								net.minecraft.entity.player.PlayerEntity player = context.getSource().getPlayer();

								// Toggle DeathPray for this player
								boolean enabled = DeathPrayManager.toggleDeathPray(player);

								// Send feedback to the player
								context.getSource().sendFeedback(
									() -> Text.literal("DeathPray " + (enabled ? "enabled" : "disabled")), 
									false
								);
							}
							return 1;
						})
					)
			);
		});
	}
}
