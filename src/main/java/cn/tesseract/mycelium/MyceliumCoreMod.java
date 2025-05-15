package cn.tesseract.mycelium;

import cn.tesseract.mycelium.asm.minecraft.HookLibPlugin;
import cn.tesseract.mycelium.asm.minecraft.HookLoader;
import cn.tesseract.mycelium.asm.minecraft.PrimaryClassTransformer;
import cn.tesseract.mycelium.hook.*;
import cn.tesseract.mycelium.lua.LuaAccessTransformer;
import cn.tesseract.mycelium.lua.LuaBridge;
import cn.tesseract.mycelium.lua.LuaHookRegistry;
import cn.tesseract.mycelium.lua.LuaHookTransformer;
import cpw.mods.fml.common.LoaderState;
import net.minecraft.launchwrapper.Launch;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.*;
import java.lang.reflect.Array;

public class MyceliumCoreMod extends HookLoader {
    public static MyceliumConfig config = new MyceliumConfig();
    public static final Globals globals = new Globals();

    public static final Logger logger = LogManager.getLogger("Lua");
    public static final File scriptDir = new File(Launch.minecraftHome, "lua");

    static {
        scriptDir.mkdir();
        globals.load(new JseBaseLib() {
            @Override
            public InputStream findResource(String filename) {
                File f = new File(scriptDir, filename);
                if (!f.exists())
                    return super.findResource(filename);
                try {
                    return new BufferedInputStream(new FileInputStream(f));
                } catch (IOException ioe) {
                    return null;
                }
            }
        });
        globals.load(new PackageLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new JseStringLib());
        globals.load(new CoroutineLib());
        globals.load(new JseMathLib());
        globals.load(new JseIoLib());
        globals.load(new JseOsLib());
        globals.load(new LuajavaLib());
        LoadState.install(globals);
        LuaC.install(globals);

        globals.set("instance", CoerceJavaToLua.coerce(new Object()));

        globals.set("import", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                String className = arg.tojstring();
                try {
                    globals.set(className.substring(className.lastIndexOf('.') + 1), CoerceJavaToLua.coerce(Class.forName(className)));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                return NONE;
            }
        });
        globals.set("importAs", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                try {
                    return CoerceJavaToLua.coerce(Class.forName(arg.tojstring()));
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        globals.set("log", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                logger.info(arg.tojstring());
                return NONE;
            }
        });
        globals.set("char", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                return new LuaUserdata(arg.tojstring().charAt(0));
            }
        });
        globals.set("toArray", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                LuaTable table = arg1.checktable();
                if (arg2.isnil()) {
                    Object[] array = new Object[table.length()];
                    for (int i = 0; i < table.length(); i++) {
                        LuaValue v = arg1.get(i + 1);
                        array[i] = v instanceof LuaUserdata ? v.checkuserdata() : CoerceLuaToJava.coerce(v, Object.class);
                    }
                    return CoerceJavaToLua.coerce(array);
                } else {
                    Class clazz = (Class) arg2.checkuserdata(Class.class);
                    Object array = Array.newInstance(clazz, table.length());
                    for (int i = 0; i < table.length(); i++)
                        Array.set(array, i, CoerceLuaToJava.coerce(arg1.get(i + 1), clazz));
                    return CoerceJavaToLua.coerce(array);
                }
            }
        });
        globals.set("registerHook", new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                LuaHookRegistry.registerLuaHook(args.arg1().tojstring(), args.arg(2), args.arg(3).checktable());
                return NONE;
            }
        });
        globals.set("registerEvent", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                LuaHookRegistry.registerLuaEvent(arg1.tojstring(), arg2);
                return NONE;
            }
        });
        globals.set("registerTransformer", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                LuaHookRegistry.registerLuaTransformer(arg1.tojstring(), arg2);
                return NONE;
            }
        });

        globals.set("preInitEvent", LuaString.valueOf(Mycelium.MODID + ":" + LoaderState.ModState.PREINITIALIZED));
        globals.set("initEvent", LuaString.valueOf(Mycelium.MODID + ":" + LoaderState.ModState.INITIALIZED));
        globals.set("postInitEvent", LuaString.valueOf(Mycelium.MODID + ":" + LoaderState.ModState.POSTINITIALIZED));
    }

    static {
        config.read();
    }

    @Override
    public String getAccessTransformerClass() {
        return LuaAccessTransformer.class.getName();
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{PrimaryClassTransformer.class.getName(), LuaHookTransformer.class.getName()};
    }

    @Override
    protected void registerHooks() {
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
                            if (method.instructions.get(i) instanceof MethodInsnNode insn)
                                if (HookLibPlugin.getMethodMcpName(insn.name).equals("refreshResources")) {
                                    insn.name = "reloadLanguage";
                                    break;
                                }
                        }
                }
            });
        }
        if (config.biomeInfo)
            registerHookContainer(BiomeInfoHook.class.getName());
        if (config.biomeDecorationFix)
            registerHookContainer(AlreadyDecoHook.class.getName());
        registerHookContainer(ForgeEventHook.class.getName());
        for (File file : scriptDir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".lua"))
                try {
                    globals.load(new FileReader(file), file.getName()).call();
                    LuaBridge.reload(true);
                    Class.forName(LuaHookTransformer.luaHookClass);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
        }
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
        File file = new File(System.getProperty("user.dir") + File.separator + "class" + File.separator + name + ".class");
        try {
            FileUtils.writeByteArrayToFile(file, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }
}
