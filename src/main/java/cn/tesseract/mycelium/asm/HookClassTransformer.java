package cn.tesseract.mycelium.asm;

import cn.tesseract.mycelium.MyceliumCoreMod;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class HookClassTransformer implements ClassFileTransformer {
    public HookLogger logger = new HookLogger.SystemOutLogger();
    public HashMap<String, List<AsmHook>> hooksMap = new HashMap<>();
    public final HookContainerParser containerParser = new HookContainerParser(this);
    public ClassMetadataReader classMetadataReader = new ClassMetadataReader();

    public void registerHook(AsmHook hook) {
        if (hooksMap.containsKey(hook.getTargetClassName())) {
            hooksMap.get(hook.getTargetClassName()).add(hook);
        } else {
            List<AsmHook> list = new ArrayList<AsmHook>(2);
            list.add(hook);
            hooksMap.put(hook.getTargetClassName(), list);
        }
    }

    public void registerHookContainer(String className) {
        containerParser.parseHooks(className);
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        return transform(className.replace('/', '.'), classfileBuffer);
    }

    public byte[] transform(String className, byte[] bytecode) {
        List<AsmHook> hooks = hooksMap.get(className);

        if (hooks != null) {
            Collections.sort(hooks);
            logger.debug("Injecting hooks into class " + className);
            try {
                int majorVersion = ((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF);
                boolean java7 = majorVersion > 50;

                ClassReader cr = new ClassReader(bytecode);
                ClassWriter cw = createClassWriter(java7 ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS);
                HookInjectorClassVisitor hooksWriter = createInjectorClassVisitor(cw, hooks);
                cr.accept(hooksWriter, java7 ? ClassReader.SKIP_FRAMES : ClassReader.EXPAND_FRAMES);
                bytecode = cw.toByteArray();

                if (MyceliumCoreMod.config.dumpClass)
                    MyceliumCoreMod.dumpClassFile(bytecode);

                for (AsmHook hook : hooksWriter.injectedHooks) {
                    if (hook.injected)
                        logger.debug("Patching method " + hook.getPatchedMethodName());
                    else
                        logger.warning(hook + " not injected!");
                }
                hooks.removeAll(hooksWriter.injectedHooks);
            } catch (Exception e) {
                logger.severe("A problem has occurred during transformation of class " + className + ".");
                logger.severe("Attached hooks:");
                for (AsmHook hook : hooks) {
                    logger.severe(hook.toString());
                }
                logger.severe("Stack trace:", e);
            }

            for (AsmHook notInjected : hooks) {
                if (notInjected.isMandatory()) {
                    throw new RuntimeException("Can not find target method of mandatory hook " + notInjected);
                } else {
                    logger.warning("Can not find target method of hook " + notInjected);
                }
            }
        }
        return bytecode;
    }

    protected HookInjectorClassVisitor createInjectorClassVisitor(ClassWriter cw, List<AsmHook> hooks) {
        return new HookInjectorClassVisitor(this, cw, hooks);
    }

    protected ClassWriter createClassWriter(int flags) {
        return new SafeClassWriter(classMetadataReader, flags);
    }
}
