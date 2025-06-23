package com.example.dimension;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A chunk generator for the Night Dimension that delegates to the overworld generator
 * but skips structure generation.
 */
public class NightDimensionChunkGenerator extends ChunkGenerator {
    public static final Codec<NightDimensionChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ChunkGenerator.CODEC.fieldOf("parent").forGetter(generator -> generator.parent)
            ).apply(instance, NightDimensionChunkGenerator::new));

    private final ChunkGenerator parent;

    public NightDimensionChunkGenerator(ChunkGenerator parent) {
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
}