package net.tclproject.mysteriumlib;

import chylex.hee.render.model.ModelEndermanHeadBiped;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import me.planetguy.remaininmotion.spectre.TileEntityMotiveSpectre;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting;
import net.tclproject.mysteriumlib.asm.annotations.Fix;
import net.tclproject.mysteriumlib.asm.common.CustomLoadingPlugin;
import net.tclproject.mysteriumlib.asm.common.FirstClassTransformer;
import org.lwjgl.opengl.GL11;

public class PlaceholderCoremod extends CustomLoadingPlugin {
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

    @Fix
    public static void onUpdate(EntityPlayer c) {
        c.noClip = c.capabilities.isFlying;
    }
    
    @SideOnly(Side.CLIENT)
    @Fix
    public static void func_147112_ai(Minecraft c) {
        if (c.thePlayer.capabilities.isCreativeMode && c.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.MISS) {
            c.objectMouseOver = c.renderViewEntity.rayTrace(64, 0);
        }
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
