package cn.tesseract.mycelium;

import cn.tesseract.mycelium.asm.minecraft.HookLoader;
import cn.tesseract.mycelium.asm.minecraft.PrimaryClassTransformer;
import cn.tesseract.mycelium.lua.LuaHookLib;
import cn.tesseract.mycelium.lua.LuaHookVisitor;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.CodeSource;
import java.security.SecureClassLoader;

public class MyceliumCoreMod extends HookLoader {
    public static Method defineClass;

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

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{PrimaryClassTransformer.class.getName()};
    }

    @Override
    protected void registerHooks() {
        Globals globals = JsePlatform.standardGlobals();
        globals.set("hookLib", CoerceJavaToLua.coerce(LuaHookLib.class));

        LuaValue chunk = globals.load("""
                function b(a,b,c,d)
                 print '&&&&'
                end
                settings={}
                settings.targetDesc="Lnet/minecraft/client/Minecraft;startGame()V"
                hookLib:registerLuaHook(b,settings)
                """);

        chunk.call();

        try {
            byte[] data = LuaHookVisitor.visit();
            LuaHookLib.dumpClassFile(data);
            defineClass.invoke(Launch.classLoader, LuaHookLib.luaHookClass, data, 0, data.length, null);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        registerHookContainer("cn.tesseract.mycelium.hook.MinecraftHook");
    }
}
