package cn.tesseract.mycelium.hook;

import cn.tesseract.mycelium.asm.Hook;
import cn.tesseract.mycelium.lua.LuaHookLib;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventBus;

public class ForgeEventHook {
    @Hook
    public static void post(EventBus c, Event event) {
        LuaHookLib.callLuaEvent(event);
    }
}
