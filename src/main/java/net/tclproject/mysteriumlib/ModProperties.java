package net.tclproject.mysteriumlib;

import net.minecraft.util.EnumChatFormatting;

public class ModProperties {
    // This class will be used to define mod properties in order to access them from anywhere.

    // General values
    public static final String MODID = "MysteriumLib";
    public static final String NAME = "MysteriumLib";
    public static final String VERSION = "1.4";
    public static final String MC_VERSION = "1.7.10";
    public static final String URL = "";
    public static final String VERSION_CHECKER_URL = "";

    // Mod info page
    public static final String COLORED_NAME = EnumChatFormatting.DARK_PURPLE + "Mysterium"
        + EnumChatFormatting.GREEN
        + "Lib ";

    public static final String COLORED_VERSION = EnumChatFormatting.GRAY + VERSION;
    public static final String COLORED_URL = EnumChatFormatting.GRAY + URL;

    public static final String CREDITS = EnumChatFormatting.DARK_PURPLE + "Matrix (TCLProject)" + EnumChatFormatting.WHITE + " , " + EnumChatFormatting.RED + "HRudyPlayZ";

    public static final String[] AUTHORS = new String[]{"Tesseract"};

    public static final String DESCRIPTION = EnumChatFormatting.GRAY
        + "A library mod used for multiple things including easy ASM fixes and more.";

    public static final String[] SPLASH_OF_THE_DAY = new String[]{"Please patch me!",
        "Finally, we can easily patch mods!",
        "Only made possible by Notch's most realistic LEGO Simulator built so far.", "I love it.",
        "We've been waiting for something like this for years!", "Mitochondria is the powerhouse of the cell.",
        "And it's not made in MCreator!", "Creeper? Awww man.", "Also try ImmersiveCavegen.", "Also try RPGStamina.",
        "Also try NoLeafDecay.", "Also try our other mods.", "Finally released as a standalone mod!",
        "2.0.0.0, also known as the first standalone version or 1.1.", "Better than ever!",
        "Since you're here, you might want to support the mod on Bitbucket and Modrinth!",
        "The revolution in modding history.", "Jeff Bezos's well kept secret.",
        "Why were the dwarves digging a hole? To get to this sooner!", "Elon Musk's hidden fetish.",
        "if thisModWorks() then thatsAwesome() else pleaseReportIssue() end",
        "Why did the chicken cross the road? Because this mod was waiting on the other side.",
        "I would like to first of all thank my two parents, without whom i wouldn't be here.",
        "And i think to myself, what a wonderful world.", "It's like crypto but actually stable!"};

    // Should be equal to null to disable it, otherwise it should just be the file name (ex: "logo.png").
    public static String LOGO = "assets/" + ModProperties.MODID + "/logo.png";
}
