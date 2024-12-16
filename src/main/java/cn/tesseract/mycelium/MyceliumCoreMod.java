package cn.tesseract.mycelium;

import cn.tesseract.mycelium.asm.NodeTransformer;
import cn.tesseract.mycelium.asm.minecraft.HookLibPlugin;
import cn.tesseract.mycelium.asm.minecraft.HookLoader;
import cn.tesseract.mycelium.asm.minecraft.PrimaryClassTransformer;
import cn.tesseract.mycelium.lua.LuaHookLib;
import cn.tesseract.mycelium.lua.LuaHookTransformer;
import cn.tesseract.mycelium.lua.LuaLogger;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.common.config.Configuration;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;

public class MyceliumCoreMod extends HookLoader {
    public static Globals globals;
    public static Field cachedClasses;
    public static String phase = "coremod";
    public static File scriptDir;

    static {
        try {
            cachedClasses = LaunchClassLoader.class.getDeclaredField("cachedClasses");
            cachedClasses.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        scriptDir = new File(Launch.minecraftHome, "lua");
        scriptDir.mkdir();
    }

    public static Globals getLuaGlobals() {
        if (globals == null) {
            globals = JsePlatform.standardGlobals();
            globals.set("hookLib", CoerceJavaToLua.coerce(LuaHookLib.class));
            globals.set("log", CoerceJavaToLua.coerce(new LuaLogger()));
        }
        return globals;
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{LuaHookTransformer.class.getName(), PrimaryClassTransformer.class.getName()};
    }

    @Override
    public String getAccessTransformerClass() {
        return MyceliumAccessTransformer.class.getName();
    }

    @Override
    protected void registerHooks() {
        phase = "hook";
        try {
            File[] files = scriptDir.listFiles();
            if (files != null)
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".lua")) {
                        LuaValue chunk = getLuaGlobals().load(new FileReader(file), file.getName());
                        chunk.call();
                    }
                }
            Class.forName(LuaHookTransformer.luaHookClass);
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        Configuration cfg = new Configuration(new File(Launch.minecraftHome, "config/mycelium.cfg"));
        if (cfg.getBoolean("creativeNoclip", "general", true, "Noclip in creative mode when fly."))
            registerHookContainer("cn.tesseract.mycelium.hook.CreativeHook");
        if (cfg.getBoolean("fastLang", "general", true, "Speed up language reload.")) {
            registerHookContainer("cn.tesseract.mycelium.hook.FastLangHook");
            registerNodeTransformer("net.minecraft.client.gui.GuiLanguage$List", new NodeTransformer() {
                @Override
                public void transform(ClassNode node) {
                    for (MethodNode method : node.methods) {
                        if (HookLibPlugin.getMethodMcpName(method.name).equals("elementClicked"))
                            for (int i = 0; i < method.instructions.size(); i++) {
                                AbstractInsnNode insn = method.instructions.get(i);
                                if (insn instanceof MethodInsnNode minsn)
                                    if (HookLibPlugin.getMethodMcpName(minsn.name).equals("refreshResources"))
                                        minsn.name = "reloadLanguage";
                            }
                    }
                }
            });
        }
        if (cfg.hasChanged())
            cfg.save();
        if (!LuaHookLib.luaEventList.isEmpty())
            registerHookContainer("cn.tesseract.mycelium.hook.ForgeEventHook");
    }
}
