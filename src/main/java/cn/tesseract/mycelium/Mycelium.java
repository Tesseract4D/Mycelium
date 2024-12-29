package cn.tesseract.mycelium;

import cn.tesseract.mycelium.lua.LuaHookLib;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;

@Mod(modid = Mycelium.MODID, useMetadata = true, name = Mycelium.NAME, version = Tags.VERSION)
public class Mycelium {
    public static final String MODID = "mycelium";
    public static final String NAME = "Mycelium";

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        MyceliumCoreMod.phase = "init";
        LuaHookLib.callLuaEvent(e);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        MyceliumCoreMod.phase = "preInit";
        LuaHookLib.callLuaEvent(e);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        MyceliumCoreMod.phase = "postInit";
        LuaHookLib.callLuaEvent(e);
    }

    @SubscribeEvent
    public void onRenderBlockOverlay(RenderBlockOverlayEvent e) {
        if (e.player.noClip)
            e.setCanceled(true);
    }

    @SubscribeEvent
    public void onBlockHighlight(DrawBlockHighlightEvent e) {
        if (e.player.isEntityInsideOpaqueBlock())
            e.setCanceled(true);
    }
}

