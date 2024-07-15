package net.tclproject.mysteriumlib;

import chylex.hee.render.model.ModelEndermanHeadBiped;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.tclproject.mysteriumlib.asm.annotations.Fix;
import net.tclproject.mysteriumlib.asm.common.CustomLoadingPlugin;
import net.tclproject.mysteriumlib.asm.common.FirstClassTransformer;
import org.lwjgl.opengl.GL11;

public class PlaceholderCoremod extends CustomLoadingPlugin {
    // Required in order for MysteriumLib to be recognized as a coremod and for other coremods to import it.

    @Fix(insertOnInvoke = "org/lwjgl/opengl/GL11;glScalef(FFF)V", insertOnLine = 1)
    public static void renderEquippedItems(RenderPlayer c, AbstractClientPlayer p, float f) {
        float n = 1.06F;
        GL11.glScalef(n, n, n);
    }

    @Fix(insertOnInvoke = "org/lwjgl/opengl/GL11;glScalef(FFF)V")
    public static void render(ModelEndermanHeadBiped c, Entity entity, float limbSwing, float limbSwingAngle, float entityTickTime, float rotationYaw, float rotationPitch, float unitPixel) {
        float n = 1.1F;
        GL11.glScalef(n, n, n);
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{FirstClassTransformer.class.getName()};
    }

    @Override
    public void registerFixes() {
        registerClassWithFixes("net.tclproject.mysteriumlib.PlaceholderCoremod");
    }
}
