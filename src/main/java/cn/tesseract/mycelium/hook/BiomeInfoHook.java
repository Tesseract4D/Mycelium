package cn.tesseract.mycelium.hook;

import cn.tesseract.mycelium.asm.Hook;
import net.minecraft.world.biome.BiomeGenBase;

public class BiomeInfoHook {
    public static final String[] biomeInfo = new String[256];

    @Hook(targetMethod = "<init>")
    public static void init(BiomeGenBase c, int id, boolean register) {
        if (biomeInfo[id] == null)
            biomeInfo[id] = c.getClass().getName();
        else
            biomeInfo[id] += " & " + c.getClass().getName();
    }
}
