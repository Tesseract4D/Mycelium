package cn.tesseract.mycelium;

import cn.tesseract.mycelium.asm.minecraft.HookLoader;
import cn.tesseract.mycelium.asm.minecraft.PrimaryClassTransformer;
import cn.tesseract.mycelium.lua.LuaHookLib;
import cn.tesseract.mycelium.lua.LuaHookVisitor;
import cn.tesseract.mycelium.lua.LuaLogger;
import cn.tesseract.mycelium.lua.LuaReflection;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.common.config.Configuration;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.security.SecureClassLoader;

public class MyceliumCoreMod extends HookLoader {
    private static Globals globals;
    public static Method defineClass;
    public static String phase = "coremod";

    static {
        try {
            Field f = LaunchClassLoader.class.getDeclaredField("cachedClasses");
            f.setAccessible(true);
            defineClass = SecureClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class, CodeSource.class);
            defineClass.setAccessible(true);
        } catch (NoSuchFieldException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Globals getLuaGlobals() {
        if (globals == null) {
            globals = JsePlatform.standardGlobals();
            globals.set("hookLib", CoerceJavaToLua.coerce(LuaHookLib.class));
            globals.set("log", CoerceJavaToLua.coerce(new LuaLogger()));
            globals.set("reflection", CoerceJavaToLua.coerce(new LuaReflection()));
        }
        return globals;
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{PrimaryClassTransformer.class.getName()};
    }

    @Override
    protected void registerHooks() {
        File scriptDir = new File(Launch.minecraftHome, "lua");
        scriptDir.mkdir();
        try {
            File[] files = scriptDir.listFiles();
            if (files != null)
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".lua")) {
                        LuaValue chunk = getLuaGlobals().load(new FileReader(file), file.getName());
                        chunk.call();
                    }
                }

            byte[] data = LuaHookVisitor.visit();
            //LuaHookLib.dumpClassFile(data);
            defineClass.invoke(Launch.classLoader, LuaHookLib.luaHookClass, data, 0, data.length, null);
        } catch (IllegalAccessException | InvocationTargetException | IOException e) {
            throw new RuntimeException(e);
        }

        Configuration cfg = new Configuration(new File(Launch.minecraftHome, "config/mycelium.cfg"));
        if (cfg.getBoolean("creativeNoclip", "general", true, "Noclip in creative mode when fly."))
            registerHookContainer("cn.tesseract.mycelium.hook.CreativeHook");
        registerHookContainer("cn.tesseract.mycelium.hook.ForgeEventHook");
    }
}
