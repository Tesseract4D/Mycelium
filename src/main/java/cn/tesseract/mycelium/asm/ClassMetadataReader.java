package cn.tesseract.mycelium.asm;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;

public class ClassMetadataReader {
    public byte[] getClassData(String className) throws IOException {
        String classResourceName = '/' + className.replace('.', '/') + ".class";
        return IOUtils.toByteArray(ClassMetadataReader.class.getResourceAsStream(classResourceName));
    }

    public void acceptVisitor(byte[] classData, ClassVisitor visitor) {
        new ClassReader(classData).accept(visitor, 0);
    }

    public void acceptVisitor(String className, ClassVisitor visitor) throws IOException {
        acceptVisitor(getClassData(className), visitor);
    }

    public MethodReference findVirtualMethod(String owner, String name, String desc) {
        ArrayList<String> superClasses = getSuperClasses(owner);
        for (int i = superClasses.size() - 1; i > 0; i--) {
            String className = superClasses.get(i);
            MethodReference methodReference = getMethodReference(className, name, desc);
            if (methodReference != null) {
                System.out.println("found virtual method: " + methodReference);
                return methodReference;
            }
        }
        return null;
    }

    private MethodReference getMethodReference(String type, String methodName, String desc) {
        try {
            return getMethodReferenceASM(type, methodName, desc);
        } catch (Exception e) {
            return getMethodReferenceReflect(type, methodName, desc);
        }
    }

    protected MethodReference getMethodReferenceASM(String type, String methodName, String desc) throws IOException {
        FindMethodClassVisitor cv = new FindMethodClassVisitor(methodName, desc);
        acceptVisitor(type, cv);
        if (cv.found) {
            return new MethodReference(type, cv.targetName, cv.targetDesc);
        }
        return null;
    }

    protected MethodReference getMethodReferenceReflect(String type, String methodName, String desc) {
        Class loadedClass = getLoadedClass(type);
        if (loadedClass != null) {
            for (Method m : loadedClass.getDeclaredMethods()) {
                if (checkSameMethod(methodName, desc, m.getName(), Type.getMethodDescriptor(m))) {
                    return new MethodReference(type, m.getName(), Type.getMethodDescriptor(m));
                }
            }
        }
        return null;
    }

    protected boolean checkSameMethod(String sourceName, String sourceDesc, String targetName, String targetDesc) {
        return sourceName.equals(targetName) && sourceDesc.equals(targetDesc);
    }

    public ArrayList<String> getSuperClasses(String type) {
        ArrayList<String> superclasses = new ArrayList<String>(1);
        superclasses.add(type);
        while ((type = getSuperClass(type)) != null) {
            superclasses.add(type);
        }
        Collections.reverse(superclasses);
        return superclasses;
    }

    private Class getLoadedClass(String type) {
        try {
            ClassLoader classLoader = ClassMetadataReader.class.getClassLoader();
            return Class.forName(type.replace('/', '.'), false, classLoader);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getSuperClass(String type) {
        try {
            return getSuperClassASM(type);
        } catch (Exception e) {
            return getSuperClassReflect(type);
        }
    }

    protected String getSuperClassASM(String type) throws IOException {
        CheckSuperClassVisitor cv = new CheckSuperClassVisitor();
        acceptVisitor(type, cv);
        return cv.superClassName;
    }

    protected String getSuperClassReflect(String type) {
        Class loadedClass = getLoadedClass(type);
        if (loadedClass != null) {
            if (loadedClass.getSuperclass() == null) return null;
            return loadedClass.getSuperclass().getName().replace('.', '/');
        }
        return "java/lang/Object";
    }

    private class CheckSuperClassVisitor extends ClassVisitor {

        String superClassName;

        public CheckSuperClassVisitor() {
            super(Opcodes.ASM5);
        }

        @Override
        public void visit(int version, int access, String name, String signature,
                          String superName, String[] interfaces) {
            this.superClassName = superName;
        }
    }

    protected class FindMethodClassVisitor extends ClassVisitor {

        public String targetName;
        public String targetDesc;
        public boolean found;

        public FindMethodClassVisitor(String name, String desc) {
            super(Opcodes.ASM5);
            this.targetName = name;
            this.targetDesc = desc;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if ((access & Opcodes.ACC_PRIVATE) == 0 && checkSameMethod(name, desc, targetName, targetDesc)) {
                found = true;
                targetName = name;
                targetDesc = desc;
            }
            return null;
        }
    }

    public static class MethodReference {

        public final String owner;
        public final String name;
        public final String desc;

        public MethodReference(String owner, String name, String desc) {
            this.owner = owner;
            this.name = name;
            this.desc = desc;
        }

        public Type getType() {
            return Type.getMethodType(desc);
        }

        @Override
        public String toString() {
            return "MethodReference{" +
                    "owner='" + owner + '\'' +
                    ", name='" + name + '\'' +
                    ", desc='" + desc + '\'' +
                    '}';
        }
    }

}
