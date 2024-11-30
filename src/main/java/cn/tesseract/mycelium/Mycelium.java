package cn.tesseract.mycelium;

import cn.tesseract.mycelium.asm.minecraft.HookLoader;
import cn.tesseract.mycelium.asm.minecraft.PrimaryClassTransformer;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.util.Collections;

@Mod(modid = Mycelium.MODID, useMetadata = true, version = Mycelium.VERSION, name = Mycelium.NAME)
public class Mycelium extends HookLoader {
    public static final String MODID = "mycelium";
    public static final String NAME = "Mycelium";
    public static final String VERSION = "2.0";
    public static String luaHookClass = "cn.tesseract.mycelium.lua.LuaHook";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(this);
        e.getModMetadata().autogenerated = false;

        e.getModMetadata().name = Mycelium.NAME;
        e.getModMetadata().version = Mycelium.VERSION;
        e.getModMetadata().credits = "Gloomy Folken";

        Collections.addAll(e.getModMetadata().authorList, "Tesseract");

        e.getModMetadata().url = "";

        e.getModMetadata().description = "A library mod used for multiple things including easy ASM fixes and more.";
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{PrimaryClassTransformer.class.getName()};
    }

    @Override
    protected void registerHooks() {
        Globals globals = JsePlatform.standardGlobals();
        globals.set("a", CoerceJavaToLua.coerce(this));

        LuaValue chunk = globals.load("""          
                function b(a,b,c,d)
                 print(a)
                 return a+1,a+2
                end
                                
                                
                """);

        chunk.call();

        System.out.println(globals.get("b").invoke(new LuaValue[]{}));
        System.out.println("&&&");

        ((String) null).length();

        registerHookContainer("cn.tesseract.mycelium.hook.MinecraftHook");
    }
}

