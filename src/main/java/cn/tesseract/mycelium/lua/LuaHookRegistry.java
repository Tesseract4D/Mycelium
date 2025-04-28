package cn.tesseract.mycelium.lua;

import cn.tesseract.mycelium.MyceliumCoreMod;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.ArrayList;

public class LuaHookRegistry {
    public static void registerLuaEvent(String eventType, LuaValue fn) {
        ArrayList<LuaValue> list = LuaBridge.eventList.computeIfAbsent(eventType, k -> new ArrayList<>());
        list.add(fn);
    }

    public static void registerLuaHook(String name, LuaValue fn, LuaTable obj) {
        if (name.startsWith("__"))
            throw new IllegalArgumentException();
        LuaBridge.registerLuaHook(name, fn, obj);
    }

    public static void registerLuaTransformer(String name, LuaValue fn) {
        MyceliumCoreMod.registerNodeTransformer(name, classNode -> fn.call(CoerceJavaToLua.coerce(classNode)));
    }
}
