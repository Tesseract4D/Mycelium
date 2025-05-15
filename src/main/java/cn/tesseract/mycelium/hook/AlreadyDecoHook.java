package cn.tesseract.mycelium.hook;

import cn.tesseract.mycelium.asm.Hook;
import cn.tesseract.mycelium.asm.ReturnCondition;
import cn.tesseract.mycelium.world.DecoratorArgumentsStorage;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeDecorator;
import net.minecraft.world.biome.BiomeGenBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AlreadyDecoHook {
    public static List<DecoratorArgumentsStorage> toDecorate = new ArrayList<>();

    @Hook(returnCondition = ReturnCondition.ON_TRUE)
    public static boolean func_150512_a(BiomeDecorator c, World world, Random random, BiomeGenBase biome, int x, int z) {
        if (c.currentWorld != null) {
            toDecorate.add(new DecoratorArgumentsStorage(c, world, random, biome, x, z));
            if (biome != null) {
                System.out.println("A mod attempted to decorate biome " + biome.biomeName + " while it was already being decorated. This probably means that there is a mod incompatibility. AlreadyDecoratingFix has attempted to fix it and prevented the crash.");
                return true;
            }
            System.out.println("A mod attempted to decorate a null biome while it was already being decorated. This probably means that there is a mod incompatibility. AlreadyDecoratingFix has attempted to fix it and prevented the crash.");
            return true;
        }
        return false;
    }
}
