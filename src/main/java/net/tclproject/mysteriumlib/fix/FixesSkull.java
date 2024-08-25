package net.tclproject.mysteriumlib.fix;

import chylex.hee.render.model.ModelEndermanHeadBiped;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.tclproject.mysteriumlib.asm.annotations.Fix;
import org.lwjgl.opengl.GL11;

public class FixesSkull {
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

}
