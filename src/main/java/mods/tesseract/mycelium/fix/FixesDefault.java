package mods.tesseract.mycelium.fix;

import net.minecraft.client.Minecraft;
import net.tclproject.mysteriumlib.asm.annotations.Fix;
import org.lwjgl.opengl.Display;

public class FixesDefault {
    @Fix(createNewMethod = true)
    public static void reloadLanguage(Minecraft c) {
        c.getLanguageManager().onResourceManagerReload(c.getResourceManager());
    }
}
