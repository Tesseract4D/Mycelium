package cn.tesseract.mycelium;

import cn.tesseract.mycelium.command.CommandLoad;
import cn.tesseract.mycelium.command.CommandReload;
import cn.tesseract.mycelium.event.AFEventHandler;
import cn.tesseract.mycelium.hook.BiomeInfoHook;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

@Mod(modid = Mycelium.MODID, useMetadata = true, name = Mycelium.NAME, version = Tags.VERSION)
public class Mycelium {
    public static final String MODID = "mycelium";
    public static final String NAME = "Mycelium";

    @Mod.EventHandler
    public void server(FMLServerStartingEvent e) {
        e.registerServerCommand(new CommandLoad());
        e.registerServerCommand(new CommandReload());
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(this);
        if (MyceliumCoreMod.config.biomeDecorationFix)
            MinecraftForge.EVENT_BUS.register(new AFEventHandler());
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        if (MyceliumCoreMod.config.biomeInfo) {
            try {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < BiomeInfoHook.biomeInfo.length; i++)
                    sb.append(i).append(" -> ").append(BiomeInfoHook.biomeInfo[i]).append('\n');
                FileUtils.writeStringToFile(new File(Launch.minecraftHome, "biomes.txt"), sb.toString());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
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

