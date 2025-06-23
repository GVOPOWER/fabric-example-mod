package com.example.entity;

import com.example.ExampleMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class BlockThrowEntity extends ProjectileEntity {
    public static final Identifier ID = new Identifier(ExampleMod.MOD_ID, "blockthrow");

    // Register the entity type
    public static final EntityType<BlockThrowEntity> BLOCKTHROW_ENTITY_TYPE = Registry.register(
        Registries.ENTITY_TYPE,
        ID,
        EntityType.Builder.<BlockThrowEntity>create(BlockThrowEntity::new, net.minecraft.entity.SpawnGroup.MISC)
            .setDimensions(1.0F, 1.0F) // Size of a standard block
            .maxTrackingRange(8)
            .trackingTickInterval(3)
            .build(ID.toString())
    );

    // Track the block item stack for rendering
    private static final TrackedData<ItemStack> BLOCK_STACK = DataTracker.registerData(BlockThrowEntity.class, TrackedDataHandlerRegistry.ITEM_STACK);

    private ItemStack blockStack;
    private BlockState blockState;
    private float damage = 5.0F; // Minimum damage of 5 as per requirements
    private boolean isTNT = false; // Flag to track if this is a TNT block

    // Getter for the block stack (used by the renderer)
    public ItemStack getBlockStack() {
        return this.dataTracker.get(BLOCK_STACK);
    }

    public BlockThrowEntity(EntityType<? extends ProjectileEntity> entityType, World world) {
        super(entityType, world);
    }

    public BlockThrowEntity(World world, LivingEntity owner, ItemStack blockStack) {
        super(BLOCKTHROW_ENTITY_TYPE, world);

        // Calculate position in front of the player's face
        Vec3d lookDir = owner.getRotationVector();
        double offsetDistance = 0.8; // Distance in front of the face

        // Position the entity in front of the player's face
        setPosition(
            owner.getX() + lookDir.x * offsetDistance, 
            owner.getEyeY() + lookDir.y * offsetDistance - 0.1, // Slight adjustment down
            owner.getZ() + lookDir.z * offsetDistance
        );

        setOwner(owner);
        this.blockStack = blockStack.copy();

        // Set the block stack in the data tracker for client-side rendering
        if (this.dataTracker != null) {
            this.dataTracker.set(BLOCK_STACK, blockStack.copy());
        }

        if (blockStack.getItem() instanceof BlockItem) {
            Block block = ((BlockItem) blockStack.getItem()).getBlock();
            this.blockState = block.getDefaultState();

            // Check if this is obsidian (should deal maximum damage)
            if (block == Blocks.OBSIDIAN) {
                this.damage = 16.0F; // Maximum damage for obsidian
            }
            // Check if this is TNT
            else if (block == Blocks.TNT) {
                this.isTNT = true;
                this.damage = 10.0F; // Base damage for TNT (explosion will do additional damage)
            }
            // For other blocks, calculate damage based on hardness
            else {
                float hardness = block.getHardness();
                if (hardness > 0) {
                    // Ensure damage is between 5 and 16
                    this.damage = Math.max(5.0F, Math.min(16.0F, hardness * 4.0F));
                }
            }
        }
    }

    @Override
    protected void initDataTracker() {
        // Initialize the block stack tracker with a default empty item
        this.dataTracker.startTracking(BLOCK_STACK, new ItemStack(Items.STONE));
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        if (this.blockStack != null) {
            nbt.put("BlockItem", this.blockStack.writeNbt(new NbtCompound()));
        }
        nbt.putFloat("Damage", this.damage);
        nbt.putBoolean("IsTNT", this.isTNT);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("BlockItem")) {
            this.blockStack = ItemStack.fromNbt(nbt.getCompound("BlockItem"));
            if (this.blockStack.getItem() instanceof BlockItem) {
                this.blockState = ((BlockItem) this.blockStack.getItem()).getBlock().getDefaultState();

                // Update the data tracker with the loaded block stack
                if (this.dataTracker != null) {
                    this.dataTracker.set(BLOCK_STACK, this.blockStack);
                }
            }
        }
        this.damage = nbt.getFloat("Damage");
        this.isTNT = nbt.contains("IsTNT") ? nbt.getBoolean("IsTNT") : false;
    }

    @Override
    public void tick() {
        super.tick();

        // Check for collisions
        HitResult hitResult = ProjectileUtil.getCollision(this, this::canHit);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onCollision(hitResult);
        }

        // Get current velocity
        Vec3d velocity = this.getVelocity();

        // Apply gravity first
        if (!this.hasNoGravity()) {
            velocity = new Vec3d(velocity.x, velocity.y - 0.08, velocity.z);
        }

        // Apply minimal drag to the updated velocity
        velocity = velocity.multiply(0.98);

        // Set the final velocity
        this.setVelocity(velocity);

        // Update position
        double newX = this.getX() + velocity.x;
        double newY = this.getY() + velocity.y;
        double newZ = this.getZ() + velocity.z;
        this.setPosition(newX, newY, newZ);

        // Create falling block visual effect
        if (this.getWorld().isClient && this.blockState != null) {
            this.getWorld().addParticle(
                new net.minecraft.particle.BlockStateParticleEffect(
                    net.minecraft.particle.ParticleTypes.BLOCK, 
                    this.blockState
                ),
                this.getX(), this.getY(), this.getZ(),
                0, 0, 0
            );
        }

        // Remove if it's been alive too long (30 seconds)
        if (this.age > 600) {
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
            this.getWorld().sendEntityStatus(this, (byte)3);
            this.discard();
        }
    }

    protected void onEntityHit(EntityHitResult entityHitResult) {
        Entity entity = entityHitResult.getEntity();
        if (!this.getWorld().isClient && entity instanceof LivingEntity) {
            // Deal damage based on block hardness
            Entity owner = this.getOwner();
            entity.damage(this.getDamageSources().mobProjectile(this, owner instanceof LivingEntity ? (LivingEntity)owner : null), this.damage);

            // If this is a TNT block, create an explosion
            if (this.isTNT) {
                // Create explosion at the entity's position
                this.getWorld().createExplosion(
                    this, // Source entity
                    entity.getX(), entity.getY(), entity.getZ(), // Position
                    4.0F, // Power (4.0 is similar to TNT)
                    false, // Create fire?
                    World.ExplosionSourceType.TNT // Explosion type
                );

                // Play TNT sound
                this.getWorld().playSound(
                    null, 
                    entity.getX(), entity.getY(), entity.getZ(), 
                    net.minecraft.sound.SoundEvents.ENTITY_GENERIC_EXPLODE, 
                    net.minecraft.sound.SoundCategory.BLOCKS, 
                    1.0F, 
                    1.0F
                );
            } else {
                // Play regular hit sound for non-TNT blocks
                this.getWorld().playSound(
                    null, 
                    entity.getX(), entity.getY(), entity.getZ(), 
                    net.minecraft.sound.SoundEvents.BLOCK_STONE_HIT, 
                    net.minecraft.sound.SoundCategory.PLAYERS, 
                    1.0F, 
                    1.0F
                );
            }
        }
    }

    protected void onBlockHit(BlockHitResult blockHitResult) {
        if (!this.getWorld().isClient) {
            BlockPos hitPos = blockHitResult.getBlockPos();

            // If this is a TNT block, create an explosion
            if (this.isTNT) {
                // Create explosion at the hit position
                this.getWorld().createExplosion(
                    this, // Source entity
                    hitPos.getX() + 0.5, hitPos.getY() + 0.5, hitPos.getZ() + 0.5, // Position (center of block)
                    4.0F, // Power (4.0 is similar to TNT)
                    false, // Create fire?
                    World.ExplosionSourceType.TNT // Explosion type
                );

                // Play TNT sound
                this.getWorld().playSound(
                    null, 
                    hitPos.getX(), hitPos.getY(), hitPos.getZ(), 
                    net.minecraft.sound.SoundEvents.ENTITY_GENERIC_EXPLODE, 
                    net.minecraft.sound.SoundCategory.BLOCKS, 
                    1.0F, 
                    1.0F
                );
            } 
            // For non-TNT blocks, place them if possible
            else if (this.blockState != null) {
                // Get the position adjacent to the block that was hit
                BlockPos placePos = hitPos.offset(blockHitResult.getSide());

                // Check if we can place the block here
                if (this.getWorld().getBlockState(placePos).isAir()) {
                    // Place the block
                    this.getWorld().setBlockState(placePos, this.blockState);

                    // Play place sound
                    this.getWorld().playSound(
                        null, 
                        placePos, 
                        this.blockState.getSoundGroup().getPlaceSound(), 
                        net.minecraft.sound.SoundCategory.BLOCKS, 
                        1.0F, 
                        1.0F
                    );
                }
            }
        }
    }

    @Override
    public boolean hasNoGravity() {
        return false;
    }
}
