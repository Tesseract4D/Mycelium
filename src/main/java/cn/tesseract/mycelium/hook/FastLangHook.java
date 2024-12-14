package cn.tesseract.mycelium.hook;

import cn.tesseract.mycelium.asm.Hook;
import net.minecraft.client.Minecraft;

public class FastLangHook {
    @Hook(targetClass = "net.minecraft.client.gui.GuiLanguage$List", injectOnInvoke = "Lnet/minecraft/client/Minecraft;refreshResources()V", redirect = true)
    public static void elementClicked(Object c, int x, boolean y, int z, int w) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());
    }
}
