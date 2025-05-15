package cn.tesseract.mycelium.asm;

import cn.tesseract.mycelium.asm.minecraft.HookLibPlugin;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

public abstract class HookInjectorFactory {
    private static final Map<String, HookInjectorFactory> factories = new HashMap<>();

    public boolean isPriorityInverted;

    public static final HookInjectorFactory enter;

    public static final HookInjectorFactory exit;

    public abstract HookInjector create(MethodVisitor mv, int access, String name, String desc, AsmHook hook, HookInjectorClassVisitor cv, String... args);

    static {
        enter = new HookInjectorFactory() {
            @Override
            public HookInjector create(MethodVisitor mv, int access, String name, String desc, AsmHook hook, HookInjectorClassVisitor cv, String... args) {
                return new HookInjector(mv, access, name, desc, hook, cv) {
                    @Override
                    protected void onMethodEnter() {
                        visitHook();
                    }
                };
            }
        };
        exit = new HookInjectorFactory() {
            @Override
            public HookInjector create(MethodVisitor mv, int access, String name, String desc, AsmHook hook, HookInjectorClassVisitor cv, String... args) {
                return new HookInjector(mv, access, name, desc, hook, cv) {
                    @Override
                    protected void onMethodExit(int opcode) {
                        if (opcode != Opcodes.ATHROW)
                            visitHook();
                    }
                };
            }
        };
        exit.isPriorityInverted = true;
        registerFactory("exit", exit);
        registerFactory("enter", enter);
        registerFactory("invoke", new HookInjectorFactory() {
            @Override
            public HookInjector create(MethodVisitor mv, int access, String name, String desc, AsmHook hook, HookInjectorClassVisitor cv, String... args) {
                return new HookInjector(mv, access, name, desc, hook, cv) {
                    String method = args[0];
                    boolean after = args.length > 1 && Boolean.parseBoolean(args[1]);
                    int index = args.length > 2 ? Integer.parseInt(args[2]) : -2;

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        boolean isTarget = false;
                        if (method.equals("L" + owner + ";" + name + desc) || method.equals("L" + owner + ";" + HookLibPlugin.getMethodMcpName(name) + desc))
                            if (index != -1 && (index == -2 || index-- == 0)) {
                                isTarget = true;
                            }
                        if (after) super.visitMethodInsn(opcode, owner, name, desc, itf);
                        if (isTarget) {
                            visitHook();
                        }
                        if (!after) super.visitMethodInsn(opcode, owner, name, desc, itf);
                    }
                };
            }
        });
        registerFactory("simple", new HookInjectorFactory() {
            @Override
            public HookInjector create(MethodVisitor mv, int access, String name, String desc, AsmHook hook, HookInjectorClassVisitor cv, String... args) {
                return new HookInjector(mv, access, name, desc, hook, cv) {
                    final String method = args[0];

                    int index = args.length > 1 ? Integer.parseInt(args[1]) : -2;

                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
                        if (method.equals(name) || method.equals(HookLibPlugin.getMethodMcpName(name)))
                            if (index != -1 && (index == -2 || index-- == 0))
                                visitHook();
                        super.visitMethodInsn(opcode, owner, name, desc, itf);
                    }
                };
            }
        });
        registerFactory("line", new HookInjectorFactory() {
            @Override
            public HookInjector create(MethodVisitor mv, int access, String name, String desc, AsmHook hook, HookInjectorClassVisitor cv, String... args) {
                return new HookInjector(mv, access, name, desc, hook, cv) {
                    final int line = Integer.parseInt(args[0]);

                    @Override
                    public void visitLineNumber(int line, Label start) {
                        super.visitLineNumber(line, start);
                        if (line == this.line)
                            visitHook();
                    }
                };
            }
        });
        registerFactory("opcode", new HookInjectorFactory() {
            @Override
            public HookInjector create(MethodVisitor mv, int access, String name, String desc, AsmHook hook, HookInjectorClassVisitor cv, String... args) {
                return new HookInjector(mv, access, name, desc, hook, cv) {
                    final int opcode = Integer.parseInt(args[0]);
                    int index = args.length > 1 ? Integer.parseInt(args[1]) : -2;

                    @Override
                    public void visitInsn(int opcode) {
                        if (opcode == this.opcode)
                            if (index != -1 && (index == -2 || index-- == 0))
                                visitHook();
                        super.visitInsn(opcode);
                    }
                };
            }
        });
    }

    public static HookInjectorFactory getFactory(String id) {
        return factories.get(id);
    }

    public static void registerFactory(String id, HookInjectorFactory factory) {
        factories.put(id, factory);
    }
}
