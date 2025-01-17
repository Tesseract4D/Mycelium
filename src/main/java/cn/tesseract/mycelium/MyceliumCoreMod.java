package cn.tesseract.mycelium;

import cn.tesseract.mycelium.asm.minecraft.HookLibPlugin;
import cn.tesseract.mycelium.asm.minecraft.HookLoader;
import cn.tesseract.mycelium.asm.minecraft.PrimaryClassTransformer;
import cn.tesseract.mycelium.hook.BlackBlockHook;
import cn.tesseract.mycelium.hook.FastLangHook;
import cn.tesseract.mycelium.hook.NoclipHook;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.File;
import java.io.IOException;

public class MyceliumCoreMod extends HookLoader {
    public static MyceliumConfig config = new MyceliumConfig();
    public static boolean dumpTransformedClass = false;

    static {
        config.read();
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{PrimaryClassTransformer.class.getName()};
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
