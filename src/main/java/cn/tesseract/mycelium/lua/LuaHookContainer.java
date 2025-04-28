package cn.tesseract.mycelium.lua;

import cn.tesseract.mycelium.Mycelium;
import cn.tesseract.mycelium.MyceliumCoreMod;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;

public class LuaHookContainer {
    public LuaValue func;
    public final String name;
    public final int hookIndex;
    public final Class returnType;
    public final boolean reloadable;
    public boolean error = false;

    public LuaHookContainer(String name, LuaValue func, int hookIndex, boolean reloadable) {
        this(name, func, hookIndex, Object.class, reloadable);
    }

    public LuaHookContainer(String name, LuaValue func, int hookIndex, Class returnType, boolean reloadable) {
        this.name = name;
        this.func = func;
        this.hookIndex = hookIndex;
        this.returnType = returnType;
        this.reloadable = reloadable;
    }

    public Object call(LuaValue[] obj) {
        if (error)
            return getDefaultReturnValue();
        try {
            Varargs results = func.invoke(obj);
            return CoerceLuaToJava.coerce(results.arg1(), returnType);
        } catch (Exception e) {
            MyceliumCoreMod.logger.error(e);
            error = true;
            return getDefaultReturnValue();
        }
    }

    public Object getDefaultReturnValue() {
        if (returnType == Boolean.class)
            return Boolean.FALSE;
        if (returnType == Byte.class)
            return (byte) 0;
        if (returnType == Short.class)
            return (short) 0;
        if (returnType == Integer.class)
            return (int) 0;
        if (returnType == Long.class)
            return (long) 0;
        if (returnType == Float.class)
            return (float) 0;
        if (returnType == Double.class)
            return (double) 0;
        if (returnType == Character.class)
            return (char) 0;
        return null;
    }
}
