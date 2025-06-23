package com.example.entity;

import com.example.ExampleMod;
import com.example.dimension.NightDimension;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ShadowEntity represents a shadow of an entity from the overworld in the night dimension.
 * It appears at the same coordinates as the original entity.
 */
public class ShadowEntity extends ArmorStandEntity {
    public static final Identifier ID = new Identifier(ExampleMod.MOD_ID, "shadow_entity");

    // Map to track which overworld entity each shadow represents
    private static final Map<UUID, UUID> SHADOW_TO_ORIGINAL_MAP = new HashMap<>();

    // The UUID of the original entity this shadow represents
    private UUID originalEntityUuid;
    private String originalEntityName;

    // Register the entity type
    public static final EntityType<ShadowEntity> SHADOW_ENTITY_TYPE = Registry.register(
        Registries.ENTITY_TYPE,
        ID,
        EntityType.Builder.<ShadowEntity>create(ShadowEntity::new, net.minecraft.entity.SpawnGroup.MISC)
            .setDimensions(0.6F, 1.8F) // Default human-like dimensions
            .build(ID.toString())
    );

    public ShadowEntity(EntityType<? extends ArmorStandEntity> entityType, World world) {
        super(entityType, world);
        this.setInvisible(true); // Make the armor stand itself invisible
        this.setNoGravity(true); // No gravity for shadow entities
        this.setInvulnerable(true); // Cannot be damaged
        this.setCustomNameVisible(true); // Show the name
    }

    /**
     * Creates a shadow entity for the given original entity
     */
    public static ShadowEntity createShadow(ServerWorld nightWorld, Entity originalEntity) {
        ShadowEntity shadowEntity = new ShadowEntity(SHADOW_ENTITY_TYPE, nightWorld);

        // Set position to match original entity
        shadowEntity.setPosition(originalEntity.getPos());

        // Store the original entity's UUID
        shadowEntity.originalEntityUuid = originalEntity.getUuid();

        // Set the name to show what entity this is a shadow of
        String entityName = originalEntity instanceof LivingEntity ? 
            ((LivingEntity) originalEntity).getName().getString() : 
            originalEntity.getType().getName().getString();
        shadowEntity.originalEntityName = entityName;
        shadowEntity.setCustomName(Text.literal("Shadow of " + entityName));

        // Track the relationship between shadow and original
        SHADOW_TO_ORIGINAL_MAP.put(shadowEntity.getUuid(), originalEntity.getUuid());

        return shadowEntity;
    }

    /**
     * Updates the shadow entity's position to match its original entity
     */
    public static void updateShadowPosition(ShadowEntity shadowEntity, Entity originalEntity) {
        if (originalEntity != null) {
            shadowEntity.setPosition(originalEntity.getPos());
            shadowEntity.setYaw(originalEntity.getYaw());
            shadowEntity.setPitch(originalEntity.getPitch());
        }
    }

    /**
     * Gets the UUID of the original entity this shadow represents
     */
    public UUID getOriginalEntityUuid() {
        return originalEntityUuid;
    }

    /**
     * Gets the original entity this shadow represents
     */
    public Entity getOriginalEntity(ServerWorld overworldServer) {
        if (originalEntityUuid != null) {
            return overworldServer.getEntity(originalEntityUuid);
        }
        return null;
    }

    /**
     * Checks if this shadow entity's original entity still exists
     */
    public boolean isOriginalEntityValid(ServerWorld overworldServer) {
        return getOriginalEntity(overworldServer) != null;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (originalEntityUuid != null) {
            nbt.putUuid("OriginalEntityUuid", originalEntityUuid);
            nbt.putString("OriginalEntityName", originalEntityName);
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("OriginalEntityUuid")) {
            originalEntityUuid = nbt.getUuid("OriginalEntityUuid");
            originalEntityName = nbt.getString("OriginalEntityName");
            setCustomName(Text.literal("Shadow of " + originalEntityName));

            // Restore the mapping
            SHADOW_TO_ORIGINAL_MAP.put(this.getUuid(), originalEntityUuid);
        }
    }

    /**
     * Gets the original entity UUID for a given shadow entity UUID
     */
    public static UUID getOriginalEntityUuid(UUID shadowEntityUuid) {
        return SHADOW_TO_ORIGINAL_MAP.get(shadowEntityUuid);
    }

    /**
     * Removes the mapping for a shadow entity
     */
    public static void removeShadowMapping(UUID shadowEntityUuid) {
        SHADOW_TO_ORIGINAL_MAP.remove(shadowEntityUuid);
    }

    /**
     * Creates the default attributes for the ShadowEntity
     */
    public static DefaultAttributeContainer.Builder createShadowAttributes() {
        return ArmorStandEntity.createLivingAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0);
    }

    static {
        // Register the entity attributes
        FabricDefaultAttributeRegistry.register(SHADOW_ENTITY_TYPE, createShadowAttributes());
    }
}
