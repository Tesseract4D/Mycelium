package mods.tesseract.mycelium;

import mods.tesseract.mycelium.fix.ReplaceMethodVisitor;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.Configuration;
import net.tclproject.mysteriumlib.asm.common.CustomLoadingPlugin;
import net.tclproject.mysteriumlib.asm.common.FirstClassTransformer;

import java.io.File;

public class PlaceholderCoremod extends CustomLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{FirstClassTransformer.class.getName()};
    }
    @Override
    public void registerFixes() {
        Configuration cfg = new Configuration(new File(Launch.minecraftHome, "config/mycelium.cfg"));
        if (cfg.getBoolean("creativeNoclip", "function", true, "Noclip in creative mode when fly."))
            registerClassWithFixes("mods.tesseract.mycelium.fix.FixesCreative");
        if (cfg.getBoolean("netherOresEFRCompat", "compat", false, "Nether Ores mod ores drops EFR raw ores."))
            registerClassWithFixes("mods.tesseract.mycelium.fix.FixesNetherOres");
        if (cfg.getBoolean("skullClippingFix", "fix", false, "Fix skull model clipping on layered skins."))
            registerClassWithFixes("mods.tesseract.mycelium.fix.FixesSkull");
        registerClassVisitor("net.minecraft.client.gui.GuiLanguage$List", new ReplaceMethodVisitor("elementClicked", "refreshResources", "reloadLanguage"));
        registerClassWithFixes("mods.tesseract.mycelium.fix.FixesDefault");
        if (cfg.hasChanged()) cfg.save();
    }
}
