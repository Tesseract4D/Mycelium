package cn.tesseract.mycelium.hook;

import cn.tesseract.mycelium.asm.Hook;
import net.minecraft.client.Minecraft;

public class FastLangHook {
    @Hook(createMethod = true)
    public static void reloadLanguage(Minecraft c) {
        c.getLanguageManager().onResourceManagerReload(c.getResourceManager());
    }
}
