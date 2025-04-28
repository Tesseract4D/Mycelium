package cn.tesseract.mycelium.hook;

import cn.tesseract.mycelium.asm.Hook;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.GL11;

public class BlackBlockHook {
    @Hook
    public static void func_147044_g(InventoryEffectRenderer c) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
    }

    @Hook(injector = "exit", targetMethod = "func_147044_g")
    public static void func_147044_g$1(InventoryEffectRenderer c) {
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }
}
