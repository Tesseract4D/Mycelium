package cn.tesseract.mycelium.lua;

import cn.tesseract.mycelium.MyceliumCoreMod;
import cn.tesseract.mycelium.asm.*;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LuaBridge {
    private static final List<LuaHookContainer> hookList = new ArrayList<>();

    public static final Map<String, ArrayList<LuaValue>> eventList = new HashMap<>();

    public static int hookIndex = 0;
    public static boolean reloading = false;

    public static Object callLuaHook(int method, Object... a) {
        LuaValue[] b = new LuaValue[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = CoerceJavaToLua.coerce(a[i]);
        }
        return hookList.get(method).call(b);
    }

    public static void callLuaEvent(Object event) {
        callLuaEvent(event.getClass().getName(), event);
    }

    public static void callLuaEvent(String name, Object event) {
        ArrayList<LuaValue> list;
        if ((list = eventList.get(name)) != null) for (LuaValue func : list)
            func.call(CoerceJavaToLua.coerce(event));
    }

    public static void registerLuaHook(LuaHookContainer hook) {
        if (hook.hookIndex != hookList.size()) throw new IllegalStateException();
        hookList.add(hook);
    }

    public static void reload(boolean startup) {
        reloading = true;
        if (!startup) {
            for (LuaHookContainer fc : hookList)
                fc.error = true;
            for (ArrayList<LuaValue> list : LuaBridge.eventList.values())
                list.clear();
        }
        LuaValue func = MyceliumCoreMod.globals.get("reload");
        if (func != LuaValue.NIL) func.call(LuaBoolean.valueOf(startup));
        reloading = false;
    }

    public static void registerLuaHook(String name, LuaValue fn, LuaTable obj) {
        if (!fn.isfunction()) throw new IllegalArgumentException(fn.tojstring() + " not a function!");
        for (LuaHookContainer fc : hookList)
            if (fc.name.equals(name)) {
                if (fc.reloadable) {
                    fc.func = fn;
                    fc.error = false;
                }
                return;
            }

        AsmHook.Builder builder = AsmHook.newBuilder();

        Map<String, Object> map = new HashMap<>();
        for (LuaValue key : obj.keys()) {
            map.put(key.tojstring(), toJava(obj.get(key)));
        }

        if (!map.containsKey("targetMethod")) throw new IllegalArgumentException("targetMethod must be set!");

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
                break;
            case "<cinit>":
                hookMethod += "cinit";
                break;
            default:
                hookMethod += targetMethod;
        }
        hookMethod += "$";

        Type targetReturnType = Type.getReturnType(targetDesc);
        Type[] types = Type.getArgumentTypes(targetDesc);
        for (Type type : types)
            hookDesc.append(type.getDescriptor());

        builder.setTargetMethodReturnType(targetReturnType);

        builder.setHookClass(LuaHookTransformer.luaHookClass);
        builder.setHookMethod(hookMethod + hookIndex);
        builder.addThisToHookMethodParameters();

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

        if (map.containsKey("injector")) {
            String injector = (String) map.get("injector");
            i = injector.indexOf(':');

            if (i == -1) builder.setInjectorFactory(HookInjectorFactory.getFactory(injector));
            else {
                String id = injector.substring(0, i);
                String[] values = injector.substring(i + 1).split(",");

                builder.setInjectorFactory(HookInjectorFactory.getFactory(id));
                builder.setInjectorSettings(values);
            }
        }

        ReturnCondition returnCondition = ReturnCondition.NEVER;
        if (map.containsKey("returnCondition")) {
            returnCondition = ReturnCondition.valueOf((String) map.get("returnCondition"));
            builder.setReturnCondition(returnCondition);
        }

        Type methodType;
        if (map.containsKey("returnType")) methodType = Type.getType((String) map.get("returnType"));
        else if (returnCondition == ReturnCondition.ON_TRUE) methodType = Type.BOOLEAN_TYPE;
        else methodType = Type.VOID_TYPE;

        LuaBridge.registerLuaHook(new LuaHookContainer(name, fn, hookIndex, typeToClass(methodType), reloading));

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
        if ((returnCondition == ReturnCondition.ON_NULL || returnCondition == ReturnCondition.ON_NOT_NULL) && methodType.getSort() != Type.OBJECT && methodType.getSort() != Type.ARRAY) {
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

        LuaHookClassVisitor.createMethod(hookMethod + hookIndex++, hookDesc.toString() + methodType.getDescriptor());

        if (map.containsKey("returnAnotherMethod")) {
            LuaBridge.registerLuaHook(new LuaHookContainer("__" + name, (LuaValue) map.get("returnAnotherMethod"), hookIndex, typeToClass(targetReturnType), reloading));
            String n = hookMethod + hookIndex++;
            LuaHookClassVisitor.createMethod(n, hookDesc.toString() + targetReturnType.getDescriptor());
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

    public static Object invokeScript(int method, Object a0) {
        return callLuaHook(method, a0);
    }

    public static Object invokeScript(int method, Object a0, Object a1) {
        return callLuaHook(method, a0, a1);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2) {
        return callLuaHook(method, a0, a1, a2);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3) {
        return callLuaHook(method, a0, a1, a2, a3);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4) {
        return callLuaHook(method, a0, a1, a2, a3, a4);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5) {
        return callLuaHook(method, a0, a1, a2, a3, a4, a5);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6) {
        return callLuaHook(method, a0, a1, a2, a3, a4, a5, a6);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8) {
        return callLuaHook(method, a0, a1, a2, a3, a4, a5, a6, a7, a8);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9) {
        return callLuaHook(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10) {
        return callLuaHook(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11) {
        return callLuaHook(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12) {
        return callLuaHook(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13) {
        return callLuaHook(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14) {
        return callLuaHook(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15) {
        return callLuaHook(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16) {
        return callLuaHook(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16);
    }
}
