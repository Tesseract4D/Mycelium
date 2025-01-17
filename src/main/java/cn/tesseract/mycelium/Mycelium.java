package cn.tesseract.mycelium;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;

@Mod(modid = Mycelium.MODID, useMetadata = true, name = Mycelium.NAME, version = Tags.VERSION)
public class Mycelium {
    public static final String MODID = "mycelium";
    public static final String NAME = "Mycelium";

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

