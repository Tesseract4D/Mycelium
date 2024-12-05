package cn.tesseract.mycelium.lua;

import org.luaj.vm2.LuaValue;

public class LuaHook {
    public LuaValue func;
    public String name;
    public Class returnType = Object.class;
    public boolean error = false;

    public LuaHook(String name, LuaValue func) {
        this.name = name;
        this.func = func;
    }

    public LuaHook(String name, LuaValue func, Class returnType) {
        this.name = name;
        this.func = func;
        this.returnType = returnType;
    }

    public Object getDefaultReturnValue() {
        if (returnType == Boolean.class)
            return false;
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
