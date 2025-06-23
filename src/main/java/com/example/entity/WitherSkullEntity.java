package com.example.entity;

import com.example.ExampleMod;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;

public class WitherSkullEntity extends ProjectileEntity {
    public static final Identifier ID = new Identifier(ExampleMod.MOD_ID, "wither_skull");

    // Register the entity type
    public static final EntityType<WitherSkullEntity> WITHER_SKULL_ENTITY_TYPE = Registry.register(
        Registries.ENTITY_TYPE,
        ID,
        EntityType.Builder.<WitherSkullEntity>create(WitherSkullEntity::new, net.minecraft.entity.SpawnGroup.MISC)
            .setDimensions(0.75F, 0.75F) // Size of the entity (increased by 2.5x)
            .maxTrackingRange(8)
            .trackingTickInterval(3)
            .build(ID.toString())
    );

    private Entity target;
    private float damage = 8.0F;
    private int homingTicks = 0;
    private static final int MAX_HOMING_TICKS = 100; // Maximum time to track a target

    public WitherSkullEntity(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    public WitherSkullEntity(World world, LivingEntity owner) {
        super(WITHER_SKULL_ENTITY_TYPE, world);
        setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        setOwner(owner);
    }

    public void setTarget(Entity target) {
        this.target = target;
        this.homingTicks = 0;
    }

    @Override
    protected void initDataTracker() {
        // No data to track for this simple entity
    }

    @Override
    public void tick() {
        super.tick();

        // Spawn wither particles as the skull moves
        if (this.getWorld().isClient) {
            // Create a skull-like shape with particles
            createSkullParticles();
        }

        // Check for collisions
        HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onCollision(hitResult);
        }

        // Update velocity if we have a target
        if (target != null && target.isAlive() && homingTicks < MAX_HOMING_TICKS) {
            // Calculate direction to target
            Vec3d targetPos = target.getPos().add(0, target.getHeight() / 2, 0);
            Vec3d direction = targetPos.subtract(this.getPos()).normalize();

            // Gradually adjust velocity towards target
            Vec3d currentVelocity = this.getVelocity();
            Vec3d newVelocity = currentVelocity.add(
                direction.multiply(0.2) // Adjust strength of homing effect
            ).normalize().multiply(1.0); // Maintain consistent speed

            this.setVelocity(newVelocity);
            homingTicks++;
        }

        // Update position
        Vec3d velocity = this.getVelocity();
        double newX = this.getX() + velocity.x;
        double newY = this.getY() + velocity.y;
        double newZ = this.getZ() + velocity.z;
        this.setPosition(newX, newY, newZ);

        // Apply very minimal drag
        this.setVelocity(velocity.multiply(0.99));

        // Remove if it's been alive too long (10 seconds)
        if (this.age > 200) {
            this.discard();
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
            // Create explosion effect
            this.getWorld().createExplosion(
                this,
                this.getX(), this.getY(), this.getZ(),
                1.0F, // Small explosion
                false,
                World.ExplosionSourceType.NONE
            );

            this.getWorld().sendEntityStatus(this, (byte)3);
            this.discard();
        }
    }

    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity entity = entityHitResult.getEntity();
        if (!this.getWorld().isClient && entity instanceof LivingEntity) {
            // Deal damage to the entity
            Entity owner = this.getOwner();
            entity.damage(this.getDamageSources().mobProjectile(this, owner instanceof LivingEntity ? (LivingEntity)owner : null), damage);

            // Apply wither effect
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).addStatusEffect(
                    new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.WITHER, 
                        100, // 5 seconds
                        1   // Level 2
                    )
                );
            }

            // Play wither sound
            this.getWorld().playSound(
                null, 
                entity.getX(), entity.getY(), entity.getZ(), 
                SoundEvents.ENTITY_WITHER_HURT, 
                SoundCategory.HOSTILE, 
                1.0F, 
                1.0F
            );
        }
    }

    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (!this.getWorld().isClient) {
            // Play wither sound
            this.getWorld().playSound(
                null, 
                this.getX(), this.getY(), this.getZ(), 
                SoundEvents.ENTITY_WITHER_SHOOT, 
                SoundCategory.HOSTILE, 
                1.0F, 
                1.0F
            );

            // Spawn particles
            if (this.getWorld() instanceof ServerWorld) {
                ((ServerWorld) this.getWorld()).spawnParticles(
                    ParticleTypes.SMOKE,
                    this.getX(), this.getY(), this.getZ(),
                    20,  // count
                    0.5, 0.5, 0.5,  // spread
                    0.1  // speed
                );
            }
        }
    }

    @Override
    public boolean hasNoGravity() {
        // No gravity for wither skulls
        return true;
    }

    /**
     * Creates a pattern of particles that resembles a wither skull
     */
    private void createSkullParticles() {
        // Center particle (core of the skull)
        this.getWorld().addParticle(
            ParticleTypes.SMOKE,
            this.getX(), this.getY(), this.getZ(),
            0, 0, 0
        );

        // Create a skull-like shape with particles
        double size = 0.35; // Reduced from 0.5 to make particles closer to the skull

        // Eyes (two particles)
        this.getWorld().addParticle(
            ParticleTypes.SMOKE,
            this.getX() - size, this.getY() + size, this.getZ() - size,
            0, 0, 0
        );
        this.getWorld().addParticle(
            ParticleTypes.SMOKE,
            this.getX() + size, this.getY() + size, this.getZ() - size,
            0, 0, 0
        );

        // Mouth (three particles in a row)
        this.getWorld().addParticle(
            ParticleTypes.SMOKE,
            this.getX() - size, this.getY() - size, this.getZ() - size,
            0, 0, 0
        );
        this.getWorld().addParticle(
            ParticleTypes.SMOKE,
            this.getX(), this.getY() - size, this.getZ() - size,
            0, 0, 0
        );
        this.getWorld().addParticle(
            ParticleTypes.SMOKE,
            this.getX() + size, this.getY() - size, this.getZ() - size,
            0, 0, 0
        );

        // Outline particles (create a spherical shape)
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            this.getWorld().addParticle(
                ParticleTypes.SMOKE,
                this.getX() + Math.cos(angle) * size, 
                this.getY() + Math.sin(angle) * size, 
                this.getZ(),
                0, 0, 0
            );
        }
    }
}
