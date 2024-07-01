package net.tclproject.mysteriumlib.asm.core;

import com.google.common.collect.ObjectArrays;
import net.tclproject.mysteriumlib.asm.core.MiscUtils.LogHelper;
import org.objectweb.asm.*;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Class that processes byte[] classes, to be integrated with IFMLLoadingPlugin to get classes passed into transform method.
 */
public class TargetClassTransformer {
    /**
     * Instance of MiscUtils needed in order to make a logger.
     */
    MiscUtils utils = new MiscUtils();
    /**
     * System logger that this class uses (messages will only appear in console, to my knowledge.)
     */
    public LogHelper logger = utils.new SystemLogHelper();
    /**
     * Map of "target class name":"List of ASMFix/es to be applied".
     */
    protected HashMap<String, List<ASMFix>> fixesMap = new HashMap<>();

    protected HashMap<String, String[]> STMap = new HashMap<>();
    protected HashMap<String, HashSet<String>> interfacesMap = new HashMap<>();
    /**
     * Class that will parse the fix class and methods.
     */
    private FixParser containerParser = new FixParser(this);
    /**
     * MetaReader instance used.
     */
    protected MetaReader metaReader = new MetaReader();

    /**
     * Adds a fix to the list to be inserted.
     */
    public void registerFix(ASMFix fix) {
        if (fixesMap.containsKey(fix.getTargetClassName())) {
            fixesMap.get(fix.getTargetClassName()).add(fix); // If a class is already to be transformed, we just add one more fix to it.
        } else { // If the class doesn't exist in the list to have a fix applied to.
            List<ASMFix> list = new ArrayList<>(2); // Create a new list of fixes to be applied to the class.
            list.add(fix); // Add this fix to the list.
            fixesMap.put(fix.getTargetClassName(), list); // Put the class and the list of fixes for it into the map.
        }
    }

    /**
     * Registers the class with all the fix methods.
     */
    public void registerClassWithFixes(String className) {
        containerParser.parseForFixes(className);
    }

    /**
     * Registers the class with all the fix methods.
     */
    public void registerClassWithFixes(byte[] classBytes) {
        containerParser.parseForFixes(classBytes);
    }

    public void registerSuperclassTransformer(String className, String superName, String transformedName) {
        STMap.put(className, new String[]{superName, transformedName});
    }

    public void registerImplementation(String className, String... interfaces) {
        HashSet<String> i = new HashSet<>(Arrays.asList(interfaces)), j;
        if ((j = interfacesMap.get(className)) != null) {
            j.addAll(i);
        } else {
            interfacesMap.put(className, i);
        }
    }

    /**
     * Takes the original bytecode of a class and returns the modified version of it with fixes applied.
     */
    public byte[] transform(String className, byte[] classBytes) {
        List<ASMFix> fixes = fixesMap.get(className); // gets the fixes for the class

        if (fixes != null) { // if there are any
            Collections.sort(fixes); // sort fixes using method inside ASMFix
            logger.debug("Injecting fixes into class " + className + ".");
            try {
                // Some random java version verification algorithm from google
                int javaVersion = ((classBytes[6] & 0xFF) << 8) | (classBytes[7] & 0xFF);
                boolean java7 = javaVersion > 50;

                ClassReader classReader = new ClassReader(classBytes);
                ClassWriter classWriter = createClassWriter(java7 ? ClassWriter.COMPUTE_FRAMES : ClassWriter.COMPUTE_MAXS); // If java is 7+, compute frames, if not, set everything to max possible
                FixInserterClassVisitor fixInserterVisitor = createInserterClassVisitor(classWriter, fixes);
                classReader.accept(fixInserterVisitor, java7 ? ClassReader.SKIP_FRAMES : ClassReader.EXPAND_FRAMES); // Make the fix inserter class visitor run through the methods and return fix inserter method visitors

                // Chain: register class with fix methods -> parse the class for fix methods and add them to the list to be inserted -> make custom class visitor -> visit target class and return custom method visitors that will be executed -> custom method visitors call ASMFix method to insert fixes from the list -> ASMFix inserts the fixes

                classBytes = classWriter.toByteArray(); // Overwrite the class bytes with the new class bytes

                for (ASMFix fix : fixInserterVisitor.insertedFixes) {
                    logger.debug("Fixed method " + fix.getFullTargetMethodName());
                } // Print out all fixed methods

                fixes.removeAll(fixInserterVisitor.insertedFixes); // remove inserted fixes from the list of fixes to be inserted

            } catch (Exception e) {
                logger.severe("A problem has occurred during transformation of class " + className + ".");
                logger.severe("Fixes to be applied to this class:");
                for (ASMFix fix : fixes) {
                    logger.severe(fix.toString());
                }
                logger.severe("Stack trace:", e);
            }

            for (ASMFix notInserted : fixes) { // since inserted fixes get removed, we can just iterate through ones left
                if (notInserted.isMandatory()) {
                    throw new RuntimeException("Can not find the target method of fatal fix: " + notInserted);
                } else {
                    logger.warning("Can not find the target method of fix: " + notInserted);
                }
            }
        }

        String[] st;
        if ((st = STMap.get(className)) != null) {
            ClassReader classReader = new ClassReader(classBytes);
            ClassWriter classWriter = new ClassWriter(0);
            classReader.accept(new ClassVisitor(Opcodes.ASM5, classWriter) {
                @Override
                public void visit(int version, int access, @Nonnull String name, @Nonnull String signature, @Nonnull String superName, @Nonnull String[] interfaces) {
                    super.visit(version, access, name, signature, st[1], interfaces);
                }

                @Nonnull
                @Override
                public MethodVisitor visitMethod(int access, @Nonnull String name, @Nonnull String desc, @Nonnull String signature, @Nonnull String[] exceptions) {
                    final MethodVisitor old = super.visitMethod(access, name, desc, signature, exceptions);
                    return "<init>".equals(name) ? new MethodVisitor(Opcodes.ASM5, old) {
                        @Override
                        public void visitMethodInsn(int opcode, @Nonnull String owner, @Nonnull String name, @Nonnull String desc, boolean itf) {
                            super.visitMethodInsn(opcode, st[0].equals(owner) ? st[1] : owner, name, desc, itf);
                        }
                    } : old;
                }
            }, 0);
            classBytes = classWriter.toByteArray();
        }
        HashSet<String> i;
        if ((i = interfacesMap.get(className)) != null) {
            ClassReader classReader = new ClassReader(classBytes);
            ClassWriter classWriter = new ClassWriter(0);
            classReader.accept(new ClassVisitor(Opcodes.ASM5, classWriter) {
                @Override
                public void visit(int version, int access, @Nonnull String name, @Nonnull String signature, @Nonnull String superName, @Nonnull String[] interfaces) {
                    i.addAll(Arrays.asList(interfaces));
                    super.visit(version, access, name, signature, superName, i.toArray(new String[]{}));
                }
            }, 0);
            classBytes = classWriter.toByteArray();
        }
        return classBytes;
    }

    /**
     * Creates a custom Class Visitor to return custom method visitors to insert fixes.
     * This method can be overridden, if inside the ClassVisitor, custom logic is needed to check if
     * the methods are the target methods.
     *
     * @param cw    ClassWriter, that needs to save the changes
     * @param fixes List of fixes inserted into the target class
     * @return ClassVisitor that returns the custom method visitors
     */
    public FixInserterClassVisitor createInserterClassVisitor(ClassWriter classWriter, List<ASMFix> fixes) {
        return new FixInserterClassVisitor(this, classWriter, fixes);
    }

    /**
     * Creates a Class Writer for storing the transformed class.
     * This method can be overridden, if a custom implementation of getCommonSuperClass is needed.
     * The standard implementation works only for classes, the .class files of which
     * are already inside classpath, but aren't loaded. The loading (but not init) of classes occurs.
     * If the loading of classes is a problem, you can use SafeCommonSuperClassWriter.
     *
     * @param flags Flags you need to pass into the constructor of the ClassWriter
     * @return ClassWriter, that saves the transformed class
     */
    public ClassWriter createClassWriter(int flags) {
        return utils.new SafeCommonSuperClassWriter(metaReader, flags);
    }
}
