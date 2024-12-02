package cn.tesseract.mycelium.asm;

import cn.tesseract.mycelium.asm.minecraft.HookLibPlugin;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

/**
 * Класс, непосредственно вставляющий хук в метод.
 * Чтобы указать конкретное место вставки хука, нужно создать класс extends HookInjector.
 */
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

    /**
     * Вставляет хук в байткод.
     */
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

    /**
     * Вставляет хук в начале метода.
     */
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

    /**
     * Вставляет хук на каждом выходе из метода, кроме выходов через throw.
     */
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

    /**
     * Вставляет хук по номеру строки.
     */
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
        private int n;
        private final boolean m;

        public Invoke(MethodVisitor mv, int access, String name, String desc,
                      AsmHook hook, HookInjectorClassVisitor cv, String method, int n, boolean injectOnExit) {
            super(mv, access, name, desc, hook, cv);
            this.method = method;
            this.n = n;
            this.m = injectOnExit;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
            if (m) super.visitMethodInsn(opcode, owner, name, desc, itf);
            if (method.equals("L" + owner + ";" + HookLibPlugin.getMethodMcpName(name) + desc))
                if (n != -1 && (n == -2 || n-- == 0))
                    visitHook();
            if (!m) super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}
