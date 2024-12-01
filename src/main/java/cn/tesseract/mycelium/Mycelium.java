package cn.tesseract.mycelium;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.Collections;

@Mod(modid = Mycelium.MODID, useMetadata = true, version = Mycelium.VERSION, name = Mycelium.NAME)
public class Mycelium {
    public static final String MODID = "mycelium";
    public static final String NAME = "Mycelium";
    public static final String VERSION = "2.0";

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        LuaValue func = MyceliumCoreMod.getLuaGlobals().get("init");
        if (!func.isnil())
            func.invoke(CoerceJavaToLua.coerce(e));
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(this);

        e.getModMetadata().autogenerated = false;

        e.getModMetadata().name = Mycelium.NAME;
        e.getModMetadata().version = Mycelium.VERSION;
        e.getModMetadata().credits = "Gloomy Folken";

        Collections.addAll(e.getModMetadata().authorList, "Tesseract");

        e.getModMetadata().url = "";

        e.getModMetadata().description = "A library mod used for multiple things including easy ASM fixes and more.";

        LuaValue func = MyceliumCoreMod.getLuaGlobals().get("preInit");
        if (!func.isnil())
            func.invoke(CoerceJavaToLua.coerce(e));
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        LuaValue func = MyceliumCoreMod.getLuaGlobals().get("postInit");
        if (!func.isnil())
            func.invoke(CoerceJavaToLua.coerce(e));
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

