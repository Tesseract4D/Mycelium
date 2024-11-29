package cn.tesseract.mycelium.hook;

import cn.tesseract.mycelium.asm.Hook;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.Display;

public class MinecraftHook {
    @Hook(injectOnInvoke = "Lorg/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V", injectOnExit = true)
    public static void startGame(Minecraft c) {
        Display.setTitle("Custom Title");
    }
}
