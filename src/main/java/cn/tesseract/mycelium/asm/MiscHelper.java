package cn.tesseract.mycelium.asm;

import net.minecraft.launchwrapper.Launch;
import org.apache.commons.io.FileUtils;
import org.objectweb.asm.*;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MiscHelper {

    private static ClassMetadataReader classMetadataReader = new ClassMetadataReader();

    public static List<String> listLocalVariables(byte[] classData, final String methodName, Type... argTypes) {
        final List<String> localVariables = new ArrayList<String>();
        String methodDesc = Type.getMethodDescriptor(Type.VOID_TYPE, argTypes);
        final String methodDescWithoutReturnType = methodDesc.substring(0, methodDesc.length() - 1);

        ClassVisitor cv = new ClassVisitor(Opcodes.ASM5) {

            @Override
            public MethodVisitor visitMethod(final int acc, String name, String desc,
                                             String signature, String[] exceptions) {
                if (methodName.equals(name) && desc.startsWith(methodDescWithoutReturnType)) {
                    return new MethodVisitor(Opcodes.ASM5) {
                        @Override
                        public void visitLocalVariable(String name, String desc,
                                                       String signature, Label start, Label end, int index) {
                            String typeName = Type.getType(desc).getClassName();
                            int fixedIndex = index + ((acc & Opcodes.ACC_STATIC) != 0 ? 1 : 0);
                            localVariables.add(fixedIndex + ": " + typeName + " " + name);
                        }
                    };
                }
                return null;
            }
        };

        classMetadataReader.acceptVisitor(classData, cv);
        return localVariables;
    }

    public static List<String> listLocalVariables(String className, final String methodName, Type... argTypes) throws IOException {
        return listLocalVariables(classMetadataReader.getClassData(className), methodName, argTypes);
    }

    public static void printLocalVariables(byte[] classData, String methodName, Type... argTypes) {
        List<String> locals = listLocalVariables(classData, methodName, argTypes);
        for (String str : locals) {
            System.out.println(str);
        }
    }

    public static void printLocalVariables(String className, String methodName, Type... argTypes) throws IOException {
        printLocalVariables(classMetadataReader.getClassData(className), methodName, argTypes);
    }

    public static String getMethodDescriptor(Method m) {
        return "L" + m.getDeclaringClass().getName().replace('.', '/') + ";" + m.getName() + Type.getMethodDescriptor(m);
    }

    public static List<String> getMethodDescriptors(Class<?> c) {
        final List<String> descs = new ArrayList<>();
        for (Method m : c.getDeclaredMethods()) {
            descs.add(getMethodDescriptor(m));
        }
        return descs;
    }

    public static void printMethodDescriptors(Class<?> c) {
        List<String> descs = getMethodDescriptors(c);
        for (String str : descs) {
            System.out.println(str);
        }
    }
}
