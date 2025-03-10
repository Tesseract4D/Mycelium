package cn.tesseract.mycelium.asm;

import cn.tesseract.mycelium.asm.minecraft.HookLibPlugin;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public abstract class HookInjectorMethodVisitor extends AdviceAdapter {

    protected final AsmHook hook;
    protected final HookInjectorClassVisitor cv;
    public final String methodName;
    public final Type methodType;
    public final boolean isStatic;

    protected HookInjectorMethodVisitor(MethodVisitor mv, int access, String name, String desc,
                                        AsmHook hook, HookInjectorClassVisitor cv) {
        super(Opcodes.ASM5, mv, access, name, desc);
        this.hook = hook;
        this.cv = cv;
        isStatic = (access & Opcodes.ACC_STATIC) != 0;
        this.methodName = name;
        this.methodType = Type.getMethodType(desc);
    }

    protected final void visitHook() {
        if (!cv.visitingHook) {
            cv.visitingHook = true;
            hook.inject(this);
            hook.injected = true;
            cv.visitingHook = false;
        }
    }

    MethodVisitor getBasicVisitor() {
        return mv;
    }

    public static class MethodEnter extends HookInjectorMethodVisitor {

        public MethodEnter(MethodVisitor mv, int access, String name, String desc,
                           AsmHook hook, HookInjectorClassVisitor cv) {
            super(mv, access, name, desc, hook, cv);
        }

        @Override
        protected void onMethodEnter() {
            visitHook();
        }

    }

    public static class MethodExit extends HookInjectorMethodVisitor {

        public MethodExit(MethodVisitor mv, int access, String name, String desc,
                          AsmHook hook, HookInjectorClassVisitor cv) {
            super(mv, access, name, desc, hook, cv);
        }

        @Override
        protected void onMethodExit(int opcode) {
            if (opcode != Opcodes.ATHROW) {
                visitHook();
            }
        }
    }

    public static class LineNumber extends HookInjectorMethodVisitor {

        private int lineNumber;
        private int startLine = -1;

        public LineNumber(MethodVisitor mv, int access, String name, String desc,
                          AsmHook hook, HookInjectorClassVisitor cv, int lineNumber) {
            super(mv, access, name, desc, hook, cv);
            this.lineNumber = lineNumber;
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            super.visitLineNumber(line, start);
            if (startLine == -1)
                startLine = line;
            if (line - startLine == this.lineNumber)
                visitHook();
        }
    }

    public static class Invoke extends HookInjectorMethodVisitor {
        private final String method;
        private int index;
        private final boolean after;

        public Invoke(MethodVisitor mv, int access, String name, String desc,
                      AsmHook hook, HookInjectorClassVisitor cv, String method, int index, boolean after) {
            super(mv, access, name, desc, hook, cv);
            this.method = method;
            this.index = index;
            this.after = after;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            boolean isTarget = false;
            if (method.equals("L" + owner + ";" + HookLibPlugin.getMethodMcpName(name) + desc))
                if (index != -1 && (index == -2 || index-- == 0)) {
                    isTarget = true;
                }
            if (after) super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (isTarget) {
                visitHook();
            }
            if (!after) super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}
