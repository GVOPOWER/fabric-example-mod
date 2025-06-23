package com.example.entity;

import com.example.ExampleMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class WaterballEntity extends ProjectileEntity {
    public static final Identifier ID = new Identifier(ExampleMod.MOD_ID, "waterball");

    // Map to store water block positions and their placement times
    private static final Map<BlockPos, Long> PLACED_WATER_BLOCKS = new HashMap<>();
    // Time in milliseconds after which water blocks should disappear (2 seconds)
    private static final long WATER_BLOCK_DURATION = 2000;

    // Register the entity type
    public static final EntityType<WaterballEntity> WATERBALL_ENTITY_TYPE = Registry.register(
        Registries.ENTITY_TYPE,
        ID,
        EntityType.Builder.<WaterballEntity>create(WaterballEntity::new, net.minecraft.entity.SpawnGroup.MISC)
            .setDimensions(0.25F, 0.25F) // Size of the entity
            .maxTrackingRange(4)
            .trackingTickInterval(10)
            .build(ID.toString())
    );

    public WaterballEntity(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    public WaterballEntity(World world, LivingEntity owner) {
        super(WATERBALL_ENTITY_TYPE, world);
        setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        setOwner(owner);
    }

    @Override
    protected void initDataTracker() {
        // No data to track for this simple entity
    }

    @Override
    public void tick() {
        super.tick();

        // Spawn water particles as the waterball moves
        if (this.getWorld().isClient) {
            this.getWorld().addParticle(
                ParticleTypes.DRIPPING_WATER,
                this.getX(), this.getY(), this.getZ(),
                0, 0, 0
            );
        }

        // Check for collisions
        HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onCollision(hitResult);
        }

        // Update position
        Vec3d velocity = this.getVelocity();
        double newX = this.getX() + velocity.x;
        double newY = this.getY() + velocity.y;
        double newZ = this.getZ() + velocity.z;
        this.setPosition(newX, newY, newZ);

        // Apply custom gravity (reduced compared to normal projectiles)
        if (!this.hasNoGravity()) {
            Vec3d currentVelocity = this.getVelocity();
            this.setVelocity(currentVelocity.x, currentVelocity.y - 0.03, currentVelocity.z);
        }

        // Apply very minimal drag (like a trident in water)
        // This makes it not slow down much, even in water
        this.setVelocity(velocity.multiply(0.99));

        // If in water, don't slow down (like a trident)
        FluidState fluidState = this.getWorld().getFluidState(this.getBlockPos());
        if (!fluidState.isEmpty()) {
            // Actually speed up slightly in water to counteract any slowdown
            this.setVelocity(this.getVelocity().multiply(1.02));
        }
    }

    @Override
    protected void onCollision(HitResult hitResult) {
        HitResult.Type type = hitResult.getType();
        if (type == HitResult.Type.ENTITY) {
            this.onEntityHit((EntityHitResult)hitResult);
        } else if (type == HitResult.Type.BLOCK) {
            this.onBlockHit((BlockHitResult)hitResult);
        }

        if (!this.getWorld().isClient) {
            this.getWorld().sendEntityStatus(this, (byte)3);
            this.discard();
        }
    }

    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity entity = entityHitResult.getEntity();
        if (!this.getWorld().isClient && entity instanceof LivingEntity) {
            // Deal 6 damage to the entity
            Entity owner = this.getOwner();
            entity.damage(this.getDamageSources().mobProjectile(this, owner instanceof LivingEntity ? (LivingEntity)owner : null), 6.0F);

            // Place water at the entity's position
            placeWaterBlock(entity.getBlockPos());
        }
    }

    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (!this.getWorld().isClient) {
            // Get the position of the block that was hit
            BlockPos hitPos = blockHitResult.getBlockPos();

            // Place water on top of the block that was hit
            placeWaterBlock(hitPos.offset(blockHitResult.getSide()));
        }
    }

    private void placeWaterBlock(BlockPos pos) {
        World world = this.getWorld();
        if (!world.isClient) {
            // Check if the position is air or a replaceable block
            BlockState existingState = world.getBlockState(pos);
            if (existingState.isAir() || existingState.isReplaceable()) {
                // Place water block
                world.setBlockState(pos, Blocks.WATER.getDefaultState());

                // Add to the map of placed water blocks with current time
                PLACED_WATER_BLOCKS.put(pos.toImmutable(), System.currentTimeMillis());

                // Play water splash sound
                world.playSound(
                    null, 
                    pos, 
                    net.minecraft.sound.SoundEvents.ITEM_BUCKET_EMPTY, 
                    net.minecraft.sound.SoundCategory.BLOCKS, 
                    1.0F, 
                    1.0F
                );

                // Spawn water particles
                if (world instanceof ServerWorld) {
                    ((ServerWorld) world).spawnParticles(
                        ParticleTypes.SPLASH,
                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        20,  // count
                        0.5, 0.5, 0.5,  // spread
                        0.1  // speed
                    );
                }
            }
        }
    }

    /**
     * Checks for water blocks that have been placed for more than the specified duration
     * and removes them.
     */
    public static void checkAndRemoveExpiredWaterBlocks(World world) {
        if (world.isClient) return;

        long currentTime = System.currentTimeMillis();
        Iterator<Map.Entry<BlockPos, Long>> iterator = PLACED_WATER_BLOCKS.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iterator.next();
            BlockPos pos = entry.getKey();
            long placementTime = entry.getValue();

            // Check if the water block has been placed for more than the specified duration
            if (currentTime - placementTime > WATER_BLOCK_DURATION) {
                // Check if the block is still water (it might have been replaced by something else)
                BlockState state = world.getBlockState(pos);
                if (state.isOf(Blocks.WATER)) {
                    // Replace water with air
                    world.setBlockState(pos, Blocks.AIR.getDefaultState());

                    // Play a sound effect for water disappearing
                    world.playSound(
                        null, 
                        pos, 
                        net.minecraft.sound.SoundEvents.BLOCK_WATER_AMBIENT, 
                        net.minecraft.sound.SoundCategory.BLOCKS, 
                        0.5F, 
                        1.0F
                    );
                }

                // Remove from the map
                iterator.remove();
            }
        }
    }

    @Override
    public boolean hasNoGravity() {
        // Slight gravity effect
        return false;
    }

    // Custom gravity implementation
    protected float getGravity() {
        // Reduced gravity compared to normal projectiles
        return 0.03F;
    }
}
