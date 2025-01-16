package cn.tesseract.mycelium.asm;

import org.objectweb.asm.MethodVisitor;

public abstract class HookInjectorFactory {

    protected boolean isPriorityInverted = false;

    abstract HookInjectorMethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc,
                                                          AsmHook hook, HookInjectorClassVisitor cv);


    public static class MethodEnter extends HookInjectorFactory {

        public static final MethodEnter INSTANCE = new MethodEnter();

        private MethodEnter() {
        }

        @Override
        public HookInjectorMethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc,
                                                            AsmHook hook, HookInjectorClassVisitor cv) {
            return new HookInjectorMethodVisitor.MethodEnter(mv, access, name, desc, hook, cv);
        }

    }

    public static class MethodExit extends HookInjectorFactory {

        public static final MethodExit INSTANCE = new MethodExit();

        private MethodExit() {
            isPriorityInverted = true;
        }

        @Override
        public HookInjectorMethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc,
                                                            AsmHook hook, HookInjectorClassVisitor cv) {
            return new HookInjectorMethodVisitor.MethodExit(mv, access, name, desc, hook, cv);
        }
    }

    public static class LineNumber extends HookInjectorFactory {

        private int lineNumber;

        public LineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
        }

        @Override
        public HookInjectorMethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc,
                                                            AsmHook hook, HookInjectorClassVisitor cv) {
            return new HookInjectorMethodVisitor.LineNumber(mv, access, name, desc, hook, cv, lineNumber);
        }
    }

    public static class Invoke extends HookInjectorFactory {

        private final String method;
        private final int index;
        private final boolean after;

        public Invoke(String method, int index, boolean after) {
            this.method = method;
            this.index = index;
            this.after = after;
        }

        @Override
        public HookInjectorMethodVisitor createHookInjector(MethodVisitor mv, int access, String name, String desc,
                                                            AsmHook hook, HookInjectorClassVisitor cv) {
            return new HookInjectorMethodVisitor.Invoke(mv, access, name, desc, hook, cv, method, index, after);
        }
    }
}
