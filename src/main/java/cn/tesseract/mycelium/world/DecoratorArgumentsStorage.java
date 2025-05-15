package cn.tesseract.mycelium.world;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.Random;

public class DecoratorArgumentsStorage {
    public final int chunk_X;
    public final int chunk_Z;
    public final World currentWorld;
    public final Random randomGenerator;
    public final BiomeGenBase currentBiomeGen;
    public final BiomeDecorator decoratorInstance;

    public DecoratorArgumentsStorage(BiomeDecorator c, World world, Random random, BiomeGenBase biome, int x, int z) {
        this.decoratorInstance = c;
        this.currentWorld = world;
        this.randomGenerator = random;
        this.currentBiomeGen = biome;
        this.chunk_X = x;
        this.chunk_Z = z;
    }
}