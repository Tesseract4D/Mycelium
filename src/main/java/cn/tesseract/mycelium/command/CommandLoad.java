package cn.tesseract.mycelium.command;

import cn.tesseract.mycelium.MyceliumCoreMod;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.invoke.WrongMethodTypeException;

public class CommandLoad extends CommandBase {
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return MinecraftServer.getServer().isSinglePlayer() || super.canCommandSenderUseCommand(sender);
    }

    public String getCommandName() {
        return "load";
    }

    public int getRequiredPermissionLevel() {
        return 2;
    }

    public String getCommandUsage(ICommandSender sender) {
        return "commands.load.usage";
    }

    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            File script = new File(MyceliumCoreMod.scriptDir, args[0]);
            try {
                sender.addChatMessage(new ChatComponentTranslation(MyceliumCoreMod.globals.load(new FileReader(script), script.getName()).call().tojstring()));
            } catch (FileNotFoundException e) {
                throw new WrongMethodTypeException("找不到文件");
            }
        } else throw new WrongMethodTypeException("commands.load.usage");
    }
}