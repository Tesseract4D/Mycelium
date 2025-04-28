package cn.tesseract.mycelium.asm;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AdviceAdapter;

public abstract class HookInjector extends AdviceAdapter {

    protected final AsmHook hook;
    protected final HookInjectorClassVisitor cv;
    public final String methodName;
    public final Type methodType;
    public final boolean isStatic;

    protected HookInjector(MethodVisitor mv, int access, String name, String desc,
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
}
