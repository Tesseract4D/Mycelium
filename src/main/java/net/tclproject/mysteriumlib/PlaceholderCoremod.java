package net.tclproject.mysteriumlib;

import chylex.hee.render.model.ModelEndermanHeadBiped;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting;
import net.tclproject.mysteriumlib.asm.annotations.Fix;
import net.tclproject.mysteriumlib.asm.common.CustomLoadingPlugin;
import net.tclproject.mysteriumlib.asm.common.FirstClassTransformer;
import org.lwjgl.opengl.GL11;
import powercrystals.netherores.ores.BlockNetherOres;
import powercrystals.netherores.ores.Ores;

import java.util.ArrayList;

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

    @Fix(createNewMethod = true, returnSetting = EnumReturnSetting.ON_NOT_NULL)
    public static ArrayList<ItemStack> getDrops(BlockNetherOres c, World world, int x, int y, int z, int metadata, int fortune) {
        Ores ore = Ores.values()[c.getBlockIndex() * 16 + metadata];
        String n = ore.getSmeltName();
        if (n.startsWith("ingot")) {
            ArrayList<ItemStack> dict = OreDictionary.getOres("raw" + n.substring(5));
            if (!dict.isEmpty()) {
                ArrayList<ItemStack> drop = new ArrayList<>();
                ItemStack d = dict.get(0);
                drop.add(new ItemStack(d.getItem(), ore.getSmeltCount(), d.getItemDamage()));
                return drop;
            }
        }
        return null;
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
