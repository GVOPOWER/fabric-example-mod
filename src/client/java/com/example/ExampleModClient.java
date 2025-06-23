package com.example;

import com.example.dimension.client.NightDimensionParticleHandler;
import com.example.entity.BlockThrowEntity;
import com.example.entity.WaterballEntity;
import com.example.entity.WitherSkullEntity;
import com.example.entity.client.BlockThrowEntityRenderer;
import com.example.entity.client.WaterballEntityRenderer;
import com.example.entity.client.WitherSkullEntityRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import com.example.ExampleMod;

public class ExampleModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.

		// Register particle handler for the night dimension
		NightDimensionParticleHandler.register();

		// Register entity renderers
		EntityRendererRegistry.register(WaterballEntity.WATERBALL_ENTITY_TYPE, WaterballEntityRenderer::new);
		EntityRendererRegistry.register(BlockThrowEntity.BLOCKTHROW_ENTITY_TYPE, BlockThrowEntityRenderer::new);
		EntityRendererRegistry.register(WitherSkullEntity.WITHER_SKULL_ENTITY_TYPE, WitherSkullEntityRenderer::new);

		// Log that we've registered the renderers
		ExampleMod.LOGGER.info("Registered Waterball entity renderer");
		ExampleMod.LOGGER.info("Registered BlockThrow entity renderer");
		ExampleMod.LOGGER.info("Registered WitherSkull entity renderer");
	}
}
