package net.tclproject.mysteriumlib.fix;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;
import net.tclproject.mysteriumlib.asm.annotations.EnumReturnSetting;
import net.tclproject.mysteriumlib.asm.annotations.Fix;
import powercrystals.netherores.ores.BlockNetherOres;
import powercrystals.netherores.ores.Ores;

import java.util.ArrayList;

public class FixesNetherOres {
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
}
