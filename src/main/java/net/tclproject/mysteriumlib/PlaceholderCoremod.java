package net.tclproject.mysteriumlib;

import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.config.Configuration;
import net.tclproject.mysteriumlib.asm.annotations.Fix;
import net.tclproject.mysteriumlib.asm.common.CustomLoadingPlugin;
import net.tclproject.mysteriumlib.asm.common.FirstClassTransformer;

import java.io.File;

public class PlaceholderCoremod extends CustomLoadingPlugin {
    @Fix(replaceInMethods = "net/tclproject/mysteriumlib/ForgeMod;preInit(Lcpw/mods/fml/common/event/FMLPreInitializationEvent;)V", targetReplaceMethods = "net/tclproject/mysteriumlib/ForgeMod;a()V")
    public static void b() {
        System.out.println(9999);
    }
    @Override
    public String[] getASMTransformerClass() {
        return new String[]{FirstClassTransformer.class.getName()};
    }
    @Override
    public void registerFixes() {
        registerClassWithFixes("net.tclproject.mysteriumlib.PlaceholderCoremod");
        Configuration cfg = new Configuration(new File(Launch.minecraftHome, "config/mycelium.cfg"));
        if (cfg.getBoolean("creativeNoclip", "function", false, "Noclip in creative mode when fly."))
            registerClassWithFixes("net.tclproject.mysteriumlib.fix.FixesCreative");
        if (cfg.getBoolean("netherOresEFRCompat", "compat", false, "Nether Ores mod ores drops EFR raw ores."))
            registerClassWithFixes("net.tclproject.mysteriumlib.fix.FixesNetherOres");
        if (cfg.getBoolean("skullClippingFix", "fix", false, "Fix skull model clipping on layered skins."))
            registerClassWithFixes("net.tclproject.mysteriumlib.fix.FixesSkull");
        if (cfg.hasChanged()) cfg.save();
    }
}
