package com.example.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A chunk generator that wraps another chunk generator but disables structure generation.
 */
public class NoStructureChunkGenerator extends ChunkGenerator {
    public static final Codec<NoStructureChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ChunkGenerator.CODEC.fieldOf("parent").forGetter(generator -> generator.parent)
            ).apply(instance, NoStructureChunkGenerator::new));

    private final ChunkGenerator parent;

    public NoStructureChunkGenerator(ChunkGenerator parent) {
        super(parent.getBiomeSource());
        this.parent = parent;
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {
        parent.carve(chunkRegion, seed, noiseConfig, biomeAccess, structureAccessor, chunk, carverStep);
    }

    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        parent.buildSurface(region, structures, noiseConfig, chunk);
    }

    @Override
    public void populateEntities(ChunkRegion region) {
        parent.populateEntities(region);
    }

    @Override
    public int getWorldHeight() {
        return parent.getWorldHeight();
    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        return parent.populateNoise(executor, blender, noiseConfig, structureAccessor, chunk);
    }

    @Override
    public int getSeaLevel() {
        return parent.getSeaLevel();
    }

    @Override
    public int getMinimumY() {
        return parent.getMinimumY();
    }

    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        return parent.getHeight(x, z, heightmap, world, noiseConfig);
    }

    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        return parent.getColumnSample(x, z, world, noiseConfig);
    }

    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        parent.getDebugHudText(text, noiseConfig, pos);
    }

    @Override
    public BiomeSource getBiomeSource() {
        return parent.getBiomeSource();
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    // The key difference - we override methods related to structures to disable structure generation

    public void generateStructures(DynamicRegistryManager registryManager, ChunkRegion region, StructureAccessor accessor) {
        // Do nothing to prevent structure generation
    }

    public List<RegistryEntry<StructureSet>> getStructureSets() {
        return Collections.emptyList();
    }
}
