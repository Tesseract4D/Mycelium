package cn.tesseract.mycelium;

import cn.tesseract.mycelium.command.CommandReload;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.common.MinecraftForge;

@Mod(modid = Mycelium.MODID, useMetadata = true, name = Mycelium.NAME, version = Tags.VERSION)
public class Mycelium {
    public static final String MODID = "mycelium";
    public static final String NAME = "Mycelium";

    @Mod.EventHandler
    public void server(FMLServerStartingEvent e) {
        e.registerServerCommand(new CommandReload());
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(this);
    }


    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onRenderBlockOverlay(RenderBlockOverlayEvent e) {
        if (e.player.noClip)
            e.setCanceled(true);
    }

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    public void onBlockHighlight(DrawBlockHighlightEvent e) {
        if (e.player.isEntityInsideOpaqueBlock())
            e.setCanceled(true);
    }
}

