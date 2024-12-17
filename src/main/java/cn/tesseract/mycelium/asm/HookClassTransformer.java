package cn.tesseract.mycelium.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.*;

public class HookClassTransformer implements ClassFileTransformer {

    public HookLogger logger = new HookLogger.SystemOutLogger();
    public HashMap<String, List<AsmHook>> hooksMap = new HashMap<>();
    public HashMap<String, List<NodeTransformer>> transformerMap = new HashMap<>();
    private final HookContainerParser containerParser = new HookContainerParser(this);
    protected ClassMetadataReader classMetadataReader = new ClassMetadataReader();

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

    public void registerNodeTransformer(String className, NodeTransformer transformer) {
        if (transformerMap.containsKey(className)) {
            transformerMap.get(className).add(transformer);
        } else {
            List<NodeTransformer> list = new ArrayList<>(2);
            list.add(transformer);
            transformerMap.put(className, list);
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        return transform(className.replace('/', '.'), classfileBuffer);
    }

    public byte[] transform(String className, byte[] bytecode) {
        List<AsmHook> hooks = hooksMap.get(className);

        if (hooks != null) {
            Collections.sort(hooks);
            logger.debug("Injecting hooks into class " + className);
            try {
                /*
                 Начиная с седьмой версии джавы, сильно изменился процесс верификации байткода.
                 Ради этого приходится включать автоматическую генерацию stack map frame'ов.
                 На более старых версиях байткода это лишняя трата времени.
                 Подробнее здесь: http://stackoverflow.com/questions/25109942
                */
                int majorVersion = ((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF);
                boolean java7 = majorVersion > 50;


                ClassReader cr = new ClassReader(bytecode);
                ClassWriter cw = createClassWriter(java7 ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS);
                HookInjectorClassVisitor hooksWriter = createInjectorClassVisitor(cw, hooks);
                cr.accept(hooksWriter, java7 ? ClassReader.SKIP_FRAMES : ClassReader.EXPAND_FRAMES);
                bytecode = cw.toByteArray();
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

        List<NodeTransformer> transformers = transformerMap.get(className);

        if (transformers != null) {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(bytecode);

            classReader.accept(classNode, 0);

            Iterator<NodeTransformer> it = transformers.iterator();
            while (it.hasNext()) {
                it.next().transform(classNode);
            }

            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            bytecode = classWriter.toByteArray();
        }

        return bytecode;
    }

    /**
     * Создает ClassVisitor для списка хуков.
     * Метод можно переопределить, если в ClassVisitor'e нужна своя логика для проверки,
     * является ли метод целевым (isTargetMethod())
     *
     * @param cw    ClassWriter, который должен стоять в цепочке после этого ClassVisitor'a
     * @param hooks Список хуков, вставляемых в класс
     * @return ClassVisitor, добавляющий хуки
     */
    protected HookInjectorClassVisitor createInjectorClassVisitor(ClassWriter cw, List<AsmHook> hooks) {
        return new HookInjectorClassVisitor(this, cw, hooks);
    }

    /**
     * Создает ClassWriter для сохранения трансформированного класса.
     * Метод можно переопределить, если в ClassWriter'e нужна своя реализация метода getCommonSuperClass().
     * Стандартная реализация работает для уже загруженных классов и для классов, .class файлы которых есть
     * в classpath, но они ещё не загружены. Во втором случае происходит загрузка (но не инициализация) классов.
     * Если загрузка классов является проблемой, то можно воспользоваться SafeClassWriter.
     *
     * @param flags Список флагов, которые нужно передать в конструктор ClassWriter'a
     * @return ClassWriter, сохраняющий трансформированный класс
     */
    protected ClassWriter createClassWriter(int flags) {
        return new SafeClassWriter(classMetadataReader, flags);
    }
}
