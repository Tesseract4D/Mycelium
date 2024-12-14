package cn.tesseract.mycelium.lua;

import cn.tesseract.mycelium.MyceliumCoreMod;
import cn.tesseract.mycelium.asm.*;
import org.apache.commons.io.FileUtils;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.objectweb.asm.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LuaHookLib {
    public static String luaHookClass = "cn.tesseract.mycelium.lua.LuaJavaHook";
    public static int hookIndex = 0;
    public static final Map<String, ArrayList<LuaValue>> luaEventList = new HashMap<>();

    public static void importClass(String className) throws ClassNotFoundException {
        MyceliumCoreMod.getLuaGlobals().set(className.substring(className.lastIndexOf('.') + 1), CoerceJavaToLua.coerce(Class.forName(className)));
    }

    public static void registerLuaEvent(String eventType, LuaValue fn) {
        ArrayList<LuaValue> list = luaEventList.computeIfAbsent(eventType, k -> new ArrayList<>());
        list.add(fn);
    }

    public static void callLuaEvent(Object event) {
        ArrayList<LuaValue> list;
        if ((list = LuaHookLib.luaEventList.get(event.getClass().getName())) != null)
            for (LuaValue func : list)
                func.call(CoerceJavaToLua.coerce(event));
    }

    public static void registerLuaHook(String name, LuaValue fn, LuaTable obj) {
        if (!fn.isfunction())
            throw new IllegalArgumentException(fn.tojstring() + " not a function!");

        AsmHook.Builder builder = AsmHook.newBuilder();

        Map<String, Object> map = new HashMap<>();
        for (LuaValue key : obj.keys()) {
            map.put(key.tojstring(), toJava(obj.get(key)));
        }

        if (!map.containsKey("targetMethod"))
            throw new IllegalArgumentException("targetMethod must be set!");

        String s = (String) map.get("targetMethod");
        int i = s.indexOf(';') + 1;
        Type targetClass = Type.getType(s.substring(0, i));
        s = s.substring(i);
        i = s.indexOf('(');
        String targetMethod = s.substring(0, i);
        String targetDesc = s.substring(i);

        builder.setTargetClass(targetClass.getClassName());
        builder.setTargetMethod(targetMethod);

        StringBuilder hookDesc = new StringBuilder("(" + targetClass.getDescriptor());
        String hookMethod = "";
        switch (targetMethod) {
            case "<init>":
                hookMethod += "init";
            case "<cinit>":
                hookMethod += "cinit";
            default:
                hookMethod += targetMethod;
        }
        hookMethod += "$";

        Type targetReturnType = Type.getReturnType(targetDesc);
        Type[] types = Type.getArgumentTypes(targetDesc);
        for (Type type : types)
            hookDesc.append(type.getDescriptor());

        builder.setTargetMethodReturnType(targetReturnType);

        builder.setHookClass(luaHookClass);
        builder.setHookMethod(hookMethod + hookIndex);
        builder.addThisToHookMethodParameters();

        boolean injectOnExit = Boolean.TRUE.equals(map.get("injectOnExit"));

        int currentParameterId = 1;
        for (i = 0; i < types.length; i++) {
            Type type = types[i];
            builder.addTargetMethodParameters(type);
            builder.addHookMethodParameter(type, currentParameterId);
            currentParameterId += type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE ? 2 : 1;
        }

        if (Boolean.TRUE.equals(map.get("returnValue"))) {
            hookDesc.append(targetReturnType.getDescriptor());
            builder.addReturnValueToHookMethodParameters();
        }

        hookDesc.append(')');

        boolean injectOnLine = false;
        int line = -2;
        if (map.containsKey("injectOnLine")) {
            injectOnLine = true;
            line = (int) map.get("injectOnLine");
        }

        if (map.containsKey("injectOnInvoke")) {
            builder.setInjectorFactory(new HookInjectorFactory.Invoke((String) map.get("injectOnInvoke"), map.containsKey("injectOnLine") ? line : -2, injectOnExit, Boolean.TRUE.equals(map.get("redirect"))));
        } else if (injectOnLine) {
            builder.setInjectorFactory(new HookInjectorFactory.LineNumber(line));
        } else if (injectOnExit) builder.setInjectorFactory(AsmHook.ON_EXIT_FACTORY);

        ReturnCondition returnCondition = ReturnCondition.NEVER;
        if (map.containsKey("returnCondition")) {
            returnCondition = ReturnCondition.valueOf((String) map.get("returnCondition"));
            builder.setReturnCondition(returnCondition);
        }

        Type methodType;
        if (map.containsKey("returnType"))
            methodType = Type.getType((String) map.get("returnType"));
        else if (returnCondition == ReturnCondition.ON_TRUE) methodType = Type.BOOLEAN_TYPE;
        else methodType = Type.VOID_TYPE;

        LuaBridge.newLuaHook(new LuaHook(name, fn, typeToClass(methodType)), hookIndex);

        if (returnCondition != ReturnCondition.NEVER) {
            Object primitiveConstant = map.get("returnConstant");
            if (primitiveConstant != null) {
                builder.setReturnValue(ReturnValue.PRIMITIVE_CONSTANT);
                switch (targetReturnType.getSort()) {
                    case Type.BYTE -> primitiveConstant = ((Number) primitiveConstant).byteValue();
                    case Type.SHORT -> primitiveConstant = ((Number) primitiveConstant).shortValue();
                    case Type.INT -> primitiveConstant = ((Number) primitiveConstant).intValue();
                    case Type.FLOAT -> primitiveConstant = ((Number) primitiveConstant).floatValue();
                    case Type.LONG -> primitiveConstant = ((Number) primitiveConstant).longValue();
                    case Type.DOUBLE -> primitiveConstant = ((Number) primitiveConstant).doubleValue();
                }
                builder.setPrimitiveConstant(primitiveConstant);
            } else if (Boolean.TRUE.equals(map.get("returnNull"))) {
                builder.setReturnValue(ReturnValue.NULL);
            } else if (targetReturnType == Type.VOID_TYPE) {
                builder.setReturnValue(ReturnValue.VOID);
            } else if (map.containsKey("returnAnotherMethod")) {
                builder.setReturnValue(ReturnValue.ANOTHER_METHOD_RETURN_VALUE);
                builder.setReturnMethod(hookMethod + (hookIndex + 1));
            } else if (methodType != Type.VOID_TYPE) {
                builder.setReturnValue(ReturnValue.HOOK_RETURN_VALUE);
            }
        }

        builder.setHookMethodReturnType(methodType);

        if (returnCondition == ReturnCondition.ON_TRUE && methodType != Type.BOOLEAN_TYPE) {
            throw new IllegalArgumentException("Hook method must return boolean if returnCodition is ON_TRUE.");
        }
        if ((returnCondition == ReturnCondition.ON_NULL || returnCondition == ReturnCondition.ON_NOT_NULL) &&
                methodType.getSort() != Type.OBJECT &&
                methodType.getSort() != Type.ARRAY) {
            throw new IllegalArgumentException("Hook method must return object if returnCodition is ON_NULL or ON_NOT_NULL.");
        }

        if (map.containsKey("priority")) {
            builder.setPriority(HookPriority.valueOf((String) map.get("priority")));
        }

        if (map.containsKey("createMethod")) {
            builder.setCreateMethod(Boolean.TRUE.equals(map.get("createMethod")));
        }
        if (map.containsKey("isMandatory")) {
            builder.setMandatory(Boolean.TRUE.equals(map.get("isMandatory")));
        }

        LuaHookVisitor.createMethod(hookMethod + hookIndex++, hookDesc.toString() + methodType.getDescriptor());

        if (map.containsKey("returnAnotherMethod")) {
            LuaBridge.newLuaHook(new LuaHook(name, (LuaValue) map.get("returnAnotherMethod"), typeToClass(targetReturnType)), hookIndex);
            String n = hookMethod + hookIndex++;
            LuaHookVisitor.createMethod(n, hookDesc.toString() + targetReturnType.getDescriptor());
        }

        MyceliumCoreMod.getTransformer().registerHook(builder.build());
    }

    public static Object toJava(LuaValue luajValue) {
        return switch (luajValue.type()) {
            case LuaValue.TNIL -> null;
            case LuaValue.TSTRING -> luajValue.tojstring();
            case LuaValue.TBOOLEAN -> luajValue.toboolean();
            case LuaValue.TUSERDATA -> luajValue.checkuserdata(Object.class);
            case LuaValue.TNUMBER -> luajValue.isinttype() ? (Object) luajValue.toint() : (Object) luajValue.todouble();
            default -> luajValue;
        };
    }

    public static Class<?> typeToClass(Type t) {
        return switch (t.getSort()) {
            case Type.BYTE -> Byte.class;
            case Type.CHAR -> Character.class;
            case Type.SHORT -> Short.class;
            case Type.INT -> Integer.class;
            case Type.LONG -> Long.class;
            case Type.FLOAT -> Float.class;
            case Type.DOUBLE -> Double.class;
            case Type.BOOLEAN -> Boolean.class;
            default -> Object.class;
        };
    }

    public static void dumpClassFile(byte[] bytes) {
        final String[] className = new String[1];
        ClassReader cr = new ClassReader(bytes);
        ClassVisitor cw = new ClassVisitor(Opcodes.ASM5, new ClassWriter(cr, 0)) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                className[0] = name;
                super.visit(version, access, name, signature, superName, interfaces);
            }
        };
        cr.accept(cw, 0);
        String name = className[0].substring(className[0].lastIndexOf('/') + 1);
        File file = new File(System.getProperty("user.dir") + File.separator + name + ".class");
        try {
            FileUtils.writeByteArrayToFile(file, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
