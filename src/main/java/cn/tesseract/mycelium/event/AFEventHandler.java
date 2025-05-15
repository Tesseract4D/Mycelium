package cn.tesseract.mycelium.event;

import cn.tesseract.mycelium.hook.AlreadyDecoHook;
import cn.tesseract.mycelium.world.DecoratorArgumentsStorage;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class AFEventHandler {
    private static int ticks = 0;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        ticks++;
        if (ticks >= 20) {
            ticks = 0;
            if (!AlreadyDecoHook.toDecorate.isEmpty()) {
                DecoratorArgumentsStorage store = AlreadyDecoHook.toDecorate.get(0);
                if (store.decoratorInstance.currentWorld == null) {
                    if (store.currentBiomeGen != null) {
                        System.out.println("Postponed decoration of biome " + store.currentBiomeGen.biomeName + " taking place");
                    } else {
                        System.out.println("Postponed decoration of null biome taking place");
                    }
                    store.decoratorInstance.decorateChunk(store.currentWorld, store.randomGenerator, store.currentBiomeGen, store.chunk_X, store.chunk_Z);
                    AlreadyDecoHook.toDecorate.remove(0);
                }
            }
        }
    }
}