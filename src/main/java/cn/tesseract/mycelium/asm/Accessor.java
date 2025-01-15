package cn.tesseract.mycelium.asm;

import cn.tesseract.mycelium.asm.minecraft.HookLoader;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.objectweb.asm.Opcodes.*;
import static org.objectweb.asm.Type.*;

public class Accessor implements NodeTransformer {
    private final String accessor;

    private final Map<String, String> getterMap = new HashMap<>();
    private final Map<String, String> setterMap = new HashMap<>();

    public Accessor(String accessor) {
        this.accessor = accessor;
    }

    @Override
    public void transform(ClassNode node) {
        byte[] bytecode;
        try {
            bytecode = HookLoader.getTransformer().classMetadataReader.getClassData(accessor);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClassReader cr = new ClassReader(bytecode);
        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                if (name.startsWith("get_")) {
                    getterMap.put(name.substring(4), Type.getReturnType(desc).getDescriptor());
                } else if (name.startsWith("set_")) {
                    setterMap.put(name.substring(4), Type.getArgumentTypes(desc)[0].getDescriptor());
                }
                return super.visitMethod(access, name, desc, signature, exceptions);
            }
        };

        cr.accept(cv, ClassReader.EXPAND_FRAMES);

        node.interfaces.add(accessor.replace('.', '/'));
        getterMap.forEach((name, desc) -> {
            boolean f = true;
            for (FieldNode field : node.fields) {
                if (field.name.equals(name)) {
                    f = false;
                    break;
                }
            }

            if (f) {
                FieldNode fieldNode = new FieldNode(Opcodes.ACC_PUBLIC, name, desc, null, null);
                node.fields.add(fieldNode);
            }

            MethodNode getter = new MethodNode(Opcodes.ACC_PUBLIC, "get_" + name, "()" + desc, null, null);

            getter.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            getter.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, name, desc));
            getter.instructions.add(new InsnNode(getReturn(Type.getType(desc))));
            node.methods.add(getter);
        });

        setterMap.forEach((name, desc) -> {
            boolean f = true;
            for (FieldNode field : node.fields) {
                if (field.name.equals(name)) {
                    f = false;
                    break;
                }
            }

            if (f) {
                FieldNode fieldNode = new FieldNode(Opcodes.ACC_PUBLIC, name, desc, null, null);
                node.fields.add(fieldNode);
            }

            MethodNode setter = new MethodNode(Opcodes.ACC_PUBLIC, "set_" + name, "(" + desc + ")V", null, null);

            setter.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            setter.instructions.add(new VarInsnNode(getLoad(Type.getType(desc)), 1));
            setter.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, node.name, name, desc));
            setter.instructions.add(new InsnNode(Opcodes.RETURN));

            node.methods.add(setter);
        });
    }

    public static int getLoad(Type type) {
        if (type == INT_TYPE || type == BYTE_TYPE || type == CHAR_TYPE ||
            type == BOOLEAN_TYPE || type == SHORT_TYPE) {
            return ILOAD;
        } else if (type == LONG_TYPE) {
            return LLOAD;
        } else if (type == FLOAT_TYPE) {
            return FLOAD;
        } else if (type == DOUBLE_TYPE) {
            return DLOAD;
        } else {
            return ALOAD;
        }
    }

    public static int getReturn(Type type) {
        if (type == INT_TYPE || type == SHORT_TYPE ||
            type == BOOLEAN_TYPE || type == BYTE_TYPE
            || type == CHAR_TYPE) {
            return IRETURN;
        } else if (type == LONG_TYPE) {
            return LRETURN;
        } else if (type == FLOAT_TYPE) {
            return FRETURN;
        } else if (type == DOUBLE_TYPE) {
            return DRETURN;
        } else if (type == VOID_TYPE) {
            return RETURN;
        } else {
            return ARETURN;
        }
    }
}
