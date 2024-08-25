package net.tclproject.mysteriumlib.fix;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.tclproject.mysteriumlib.asm.annotations.Fix;

public class FixesCreative {
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
}
