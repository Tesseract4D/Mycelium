package cn.tesseract.mycelium.lua;

import cn.tesseract.mycelium.MyceliumCoreMod;
import cn.tesseract.mycelium.asm.AsmHook;
import org.objectweb.asm.*;

import java.util.ArrayList;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

public class LuaHookClassVisitor {
    private static final ArrayList<String> methods = new ArrayList<>();

    public static byte[] visit() {
        if (!MyceliumCoreMod.phase.equals("hook"))
            throw new IllegalStateException();
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, LuaHookTransformer.luaHookClass.replace('.', '/'), null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        for (int i = 0; i < methods.size(); i += 2)
            injectMethod(cw, methods.get(i), methods.get(i + 1), i / 2);

        cw.visitEnd();

        byte[] bytecode = cw.toByteArray();

        if (MyceliumCoreMod.dumpTransformedClass)
            MyceliumCoreMod.dumpClassFile(bytecode);

        return bytecode;
    }

    public static void createMethod(String name, String desc) {
        methods.add(name);
        methods.add(desc);
    }

    public static void injectMethod(ClassVisitor cw, String name, String desc, int index) {
        StringBuilder desc2 = new StringBuilder("(I");
        Type[] types = Type.getArgumentTypes(desc);
        Type rt = Type.getReturnType(desc);
        int n = types.length, m = n - 1, d = 0;
        for (Type t : types)
            if (isLongOrDoubleType(t))
                m++;
        int[] stores = new int[n];
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC, name, desc, null, null);
        mv.visitCode();
        for (int i = 0; i < types.length; i++) {
            Type type = types[i];
            if (type.getSort() <= 8) {
                stores[i] = ++m;
                AsmHook.injectLoad(mv, type, i + d);
                injectPrimitiveToObject(mv, type);
                mv.visitVarInsn(ASTORE, m);
                if (isLongOrDoubleType(type))
                    d++;
            } else {
                stores[i] = i;
            }
            desc2.append("Ljava/lang/Object;");
        }
        desc2.append(")Ljava/lang/Object;");

        mv.visitLdcInsn(index);
        for (int j : stores) {
            mv.visitVarInsn(ALOAD, j);
        }
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "cn/tesseract/mycelium/lua/LuaBridge", "invokeScript", desc2.toString(), false);
        if (rt.getSort() != VOID) {
            injectObjectToPrimitive(mv, rt);
        }
        AsmHook.injectReturn(mv, rt);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    public static boolean isLongOrDoubleType(Type t) {
        return t == DOUBLE_TYPE || t == LONG_TYPE;
    }

    public static void injectPrimitiveToObject(MethodVisitor mv, Type primitive) {
        switch (primitive.getSort()) {
            case Type.INT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
                break;
            case Type.LONG:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
                break;
            case Type.DOUBLE:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
                break;
            case Type.FLOAT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
                break;
            case Type.BOOLEAN:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
                break;
            case Type.CHAR:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
                break;
            case Type.BYTE:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
                break;
            case Type.SHORT:
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            default:
                throw new UnsupportedOperationException("Unsupported type: " + primitive);
        }
    }

    public static void injectObjectToPrimitive(MethodVisitor mv, Type primitive) {
        switch (primitive.getSort()) {
            case INT:
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Integer");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false);
                break;
            case BOOLEAN:
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Boolean");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
                break;
            case Type.FLOAT:
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Float");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F", false);
                break;
            case Type.DOUBLE:
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Double");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D", false);
                break;
            case BYTE:
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Byte");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B", false);
                break;
            case SHORT:
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Short");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S", false);
                break;
            case CHAR:
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Character");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
                break;
            case Type.LONG:
                mv.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/Long");
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J", false);
                break;
            default:
                mv.visitTypeInsn(Opcodes.CHECKCAST, primitive.getInternalName());
        }
    }
}
