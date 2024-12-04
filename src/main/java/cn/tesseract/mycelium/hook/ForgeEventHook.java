package cn.tesseract.mycelium.hook;

import cn.tesseract.mycelium.asm.Hook;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ForgeEventHook {
    public static final Map<Class<?>, ArrayList<LuaValue>> luaEventList = new HashMap<>();

    @Hook
    public static void post(EventBus c, Event event) {
        ArrayList<LuaValue> list;
        if ((list = luaEventList.get(event.getClass()))!=null)
            for (LuaValue func : list)
                func.call(CoerceJavaToLua.coerce(event));
    }
}
