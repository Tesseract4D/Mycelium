package cn.tesseract.mycelium;

import cn.tesseract.mycelium.asm.minecraft.HookLibPlugin;
import cn.tesseract.mycelium.asm.minecraft.HookLoader;
import cn.tesseract.mycelium.asm.minecraft.PrimaryClassTransformer;
import cn.tesseract.mycelium.hook.BlackBlockHook;
import cn.tesseract.mycelium.hook.NoclipHook;
import cn.tesseract.mycelium.hook.FastLangHook;
import cn.tesseract.mycelium.hook.ForgeEventHook;
import cn.tesseract.mycelium.lua.LuaHookLib;
import cn.tesseract.mycelium.lua.LuaHookTransformer;
import cn.tesseract.mycelium.lua.LuaLogger;
import net.minecraft.launchwrapper.Launch;
import org.apache.commons.io.FileUtils;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class MyceliumCoreMod extends HookLoader {
    public static Globals globals;
    public static String phase = "coremod";
    public static File scriptDir;
    public static MyceliumConfig config = new MyceliumConfig();
    public static boolean dumpTransformedClass = false;

    static {
        config.read();
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

        if (config.creativeNoclip)
            registerHookContainer(NoclipHook.class.getName());
        if (config.effectBlackBlockFix)
            registerHookContainer(BlackBlockHook.class.getName());
        if (config.fastLang) {
            registerHookContainer(FastLangHook.class.getName());
            registerNodeTransformer("net.minecraft.client.gui.GuiLanguage$List", node -> {
                for (MethodNode method : node.methods) {
                    if (HookLibPlugin.getMethodMcpName(method.name).equals("elementClicked"))
                        for (int i = 0; i < method.instructions.size(); i++) {
                            AbstractInsnNode insn = method.instructions.get(i);
                            if (insn instanceof MethodInsnNode minsn)
                                if (HookLibPlugin.getMethodMcpName(minsn.name).equals("refreshResources"))
                                    minsn.name = "reloadLanguage";
                        }
                }
            });
        }
        if (!LuaHookLib.luaEventList.isEmpty())
            registerHookContainer(ForgeEventHook.class.getName());
    }

    public static File dumpClassFile(byte[] bytes) {
        final String[] className = new String[1];
        ClassReader cr = new ClassReader(bytes);
        ClassVisitor cw = new ClassVisitor(Opcodes.ASM5, new ClassWriter(cr, 0)) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                className[0] = name;
                super.visit(version, access, name, signature, superName, interfaces);
            }
        };
        cr.accept(cw, 0);
        String name = className[0].substring(className[0].lastIndexOf('/') + 1);
        File file = new File(System.getProperty("user.dir") + File.separator + name + ".class");
        try {
            FileUtils.writeByteArrayToFile(file, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }
}
