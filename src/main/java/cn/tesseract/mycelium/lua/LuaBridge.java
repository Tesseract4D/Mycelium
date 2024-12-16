package cn.tesseract.mycelium.lua;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;

import java.util.ArrayList;
import java.util.HashMap;

public class LuaBridge {
    private static final ArrayList<LuaHook> luaHooks = new ArrayList<>();
    private static final HashMap<String, Integer> hookMap = new HashMap<>();

    public static Object invokeScriptAll(int method, Object... a) {
        LuaValue[] b = new LuaValue[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = CoerceJavaToLua.coerce(a[i]);
        }
        LuaHook hook = luaHooks.get(method);
        if (hook.error)
            return hook.getDefaultReturnValue();
        try {
            Varargs results = hook.func.invoke(b);
            return CoerceLuaToJava.coerce(results.arg1(), hook.returnType);
        } catch (Exception e) {
            LuaLogger.logger.error(e);
            hook.error = true;
            return hook.getDefaultReturnValue();
        }
    }

    public static void newLuaHook(LuaHook hook, int index) {
        luaHooks.add(hook);
        hookMap.put(hook.name, index);
    }

    public static Object invokeScript(int method, Object a0) {
        return invokeScriptAll(method, a0);
    }

    public static Object invokeScript(int method, Object a0, Object a1) {
        return invokeScriptAll(method, a0, a1);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2) {
        return invokeScriptAll(method, a0, a1, a2);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3) {
        return invokeScriptAll(method, a0, a1, a2, a3);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4) {
        return invokeScriptAll(method, a0, a1, a2, a3, a4);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5) {
        return invokeScriptAll(method, a0, a1, a2, a3, a4, a5);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6) {
        return invokeScriptAll(method, a0, a1, a2, a3, a4, a5, a6);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8) {
        return invokeScriptAll(method, a0, a1, a2, a3, a4, a5, a6, a7, a8);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9) {
        return invokeScriptAll(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10) {
        return invokeScriptAll(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11) {
        return invokeScriptAll(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12) {
        return invokeScriptAll(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13) {
        return invokeScriptAll(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14) {
        return invokeScriptAll(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15) {
        return invokeScriptAll(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15);
    }

    public static Object invokeScript(int method, Object a0, Object a1, Object a2, Object a3, Object a4, Object a5, Object a6, Object a7, Object a8, Object a9, Object a10, Object a11, Object a12, Object a13, Object a14, Object a15, Object a16) {
        return invokeScriptAll(method, a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16);
    }
}
