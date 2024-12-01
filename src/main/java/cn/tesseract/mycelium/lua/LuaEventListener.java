package cn.tesseract.mycelium.lua;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.IEventListener;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

public class LuaEventListener implements IEventListener {
    private final LuaValue listener;

    public LuaEventListener(LuaValue listener) {
        this.listener = listener;
    }

    @Override
    public void invoke(Event event) {
        listener.call(CoerceJavaToLua.coerce(event));
    }
}
