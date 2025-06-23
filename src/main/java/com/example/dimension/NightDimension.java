package com.example.dimension;

import com.example.ExampleMod;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;

/**
 * Class that holds constants and registry keys for the Night Dimension.
 */
public class NightDimension {
    // The identifier for our dimension
    public static final Identifier DIMENSION_ID = new Identifier(ExampleMod.MOD_ID, "night_dimension");
    
    // Registry key for the dimension
    public static final RegistryKey<World> DIMENSION_KEY = RegistryKey.of(RegistryKeys.WORLD, DIMENSION_ID);
    
    // Registry key for the dimension type
    public static final RegistryKey<DimensionType> DIMENSION_TYPE_KEY = 
            RegistryKey.of(RegistryKeys.DIMENSION_TYPE, DIMENSION_ID);
    
    // Registry key for dimension options
    public static final RegistryKey<DimensionOptions> DIMENSION_OPTIONS_KEY = 
            RegistryKey.of(RegistryKeys.DIMENSION, DIMENSION_ID);
}