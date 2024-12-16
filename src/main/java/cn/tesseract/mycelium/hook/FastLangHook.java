package cn.tesseract.mycelium.hook;

import cn.tesseract.mycelium.asm.Hook;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.world.World;

public class FastLangHook {
    @Hook(targetClass = "net.minecraft.client.gui.GuiLanguage$List", injectOnInvoke = "Lnet/minecraft/client/Minecraft;refreshResources()V", redirect = true)
    public static void elementClicked(Object c, int x, boolean y, int z, int w) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
    }

    @Hook(injectOnInvoke = "Lnet/minecraft/block/BlockRedstoneWire;func_150177_e(Lnet/minecraft/world/World;III)V", redirect = true)
    public static void onBlockAdded(BlockRedstoneWire c, World worldIn, int x, int y, int z) {
        System.out.println(7777);
    }
}
