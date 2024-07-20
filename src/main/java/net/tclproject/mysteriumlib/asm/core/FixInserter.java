package net.tclproject.mysteriumlib.asm.core;

import net.tclproject.mysteriumlib.asm.common.CustomClassTransformer;
import net.tclproject.mysteriumlib.asm.common.CustomLoadingPlugin;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Custom MethodVisitor that calls the insert method in ASMFix to insert fixes.
 */
public abstract class FixInserter extends AdviceAdapter {

    /**
     * The fix that this visitor needs to insert.
     */
    protected final ASMFix fix;
    /**
     * The class visitor that visited this method and created this MethodVisitor.
     */
    protected final FixInserterClassVisitor classVisitor;
    /**
     * The target method name.
     */
    public final String methodName;
    /**
     * The target method return type.
     */
    public final Type methodType;
    /**
     * If the target method is static.
     */
    public final boolean isStatic;

    protected FixInserter(MethodVisitor mv, int access, String name, String descriptor, ASMFix fix, FixInserterClassVisitor classVisitor) {
        super(Opcodes.ASM5, mv, access, name, descriptor);
        this.fix = fix;
        this.classVisitor = classVisitor;
        isStatic = (access & Opcodes.ACC_STATIC) != 0;
        this.methodName = name;
        this.methodType = Type.getMethodType(descriptor);
    }

    /**
     * Inserts the fix into the bytecode.
     */
    protected final void insertFix() {
        if (!classVisitor.visitingFix) {
            classVisitor.visitingFix = true;
            fix.insertFix(this);
            classVisitor.visitingFix = false;
        }
    }

    /**
     * Inserts the fix when visiting the start of the method.
     */
    public static class OnEnterInserter extends FixInserter {

        public OnEnterInserter(MethodVisitor mv, int access, String name, String desc, ASMFix fix, FixInserterClassVisitor cv) {
            super(mv, access, name, desc, fix, cv);
        }

        /**
         * Inserts the fix into the bytecode.
         */
        @Override
        protected void onMethodEnter() {
            insertFix();
        }

    }

    /**
     * Inserts the fix when visiting every exit from the method, except for exiting through throwing an error (configurable).
     */
    public static class OnExitInserter extends FixInserter {

        public boolean insertOnThrows;

        public OnExitInserter(MethodVisitor mv, int access, String name, String desc, ASMFix fix, FixInserterClassVisitor cv) {
            super(mv, access, name, desc, fix, cv);
            this.insertOnThrows = false;
        }

        public OnExitInserter(MethodVisitor mv, int access, String name, String desc, ASMFix fix, FixInserterClassVisitor cv, boolean insertOnThrows) {
            super(mv, access, name, desc, fix, cv);
            this.insertOnThrows = insertOnThrows;
        }

        /**
         * Inserts the fix into the bytecode.
         */
        @Override
        protected void onMethodExit(int opcode) {
            if (opcode != Opcodes.ATHROW || this.insertOnThrows) {
                insertFix();
            }
        }
    }

    /**
     * Inserts the fix when visiting the specific line number.
     */
    public static class OnLineNumberInserter extends FixInserter {

        private int lineNumber;
        private int startLine = -1;

        public OnLineNumberInserter(MethodVisitor mv, int access, String name, String desc, ASMFix fix, FixInserterClassVisitor cv, int lineNumber) {
            super(mv, access, name, desc, fix, cv);
            this.lineNumber = lineNumber;
        }

        /**
         * Inserts the fix into the bytecode.
         */
        @Override
        public void visitLineNumber(int lineVisiting, Label start) {
            super.visitLineNumber(lineVisiting, start);
            if (startLine == -1)
                startLine = lineVisiting;
            if (lineVisiting - startLine == this.lineNumber) {
                insertFix();
            }
        }
    }

    public static class OnInvokeInserter extends FixInserter {
        private String method;
        private int n;

        public OnInvokeInserter(MethodVisitor mv, int access, String name, String desc, ASMFix fix, FixInserterClassVisitor cv, String method, int n) {
            super(mv, access, name, desc, fix, cv);
            this.method = method;
            this.n = n;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (CustomLoadingPlugin.isObfuscated()) {
                String deobfName = CustomClassTransformer.methodsMap.get(CustomClassTransformer.getMethodIndex(name));
                if (deobfName != null)
                    name = deobfName;
            }
            if (method.equals(owner + ";" + name + desc))
                if (n != -1 && (n == -2 || n-- == 0))
                    insertFix();
        }
    }
}
