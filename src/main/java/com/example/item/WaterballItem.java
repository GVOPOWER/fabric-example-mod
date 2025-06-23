package com.example.item;

import com.example.ExampleMod;
import com.example.entity.WaterballEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class WaterballItem extends Item {
    public static final Identifier ID = new Identifier(ExampleMod.MOD_ID, "waterball");
    
    // Register the item
    public static final WaterballItem WATERBALL_ITEM = Registry.register(
        Registries.ITEM,
        ID,
        new WaterballItem(new Item.Settings().maxCount(16))
    );

    public WaterballItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        
        // Play throw sound
        world.playSound(
            null, 
            user.getX(), user.getY(), user.getZ(), 
            SoundEvents.ENTITY_SNOWBALL_THROW, 
            SoundCategory.NEUTRAL, 
            0.5F, 
            0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F)
        );
        
        // Add cooldown
        user.getItemCooldownManager().set(this, 5);
        
        if (!world.isClient) {
            // Create and spawn the waterball entity
            WaterballEntity waterballEntity = new WaterballEntity(world, user);
            waterballEntity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F, 1.0F);
            world.spawnEntity(waterballEntity);
        }
        
        // Increment player's usage statistic
        user.incrementStat(Stats.USED.getOrCreateStat(this));
        
        // Decrease stack size if not in creative mode
        if (!user.getAbilities().creativeMode) {
            itemStack.decrement(1);
        }
        
        return TypedActionResult.success(itemStack, world.isClient());
    }
}