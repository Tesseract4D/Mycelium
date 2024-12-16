package cn.tesseract.mycelium.lua;

import net.minecraft.launchwrapper.IClassTransformer;

public class LuaHookTransformer implements IClassTransformer {
    public static final String luaHookClass = "cn.tesseract.mycelium.lua.LuaJavaHook";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (name.equals(luaHookClass))
            return LuaHookVisitor.visit();
        return basicClass;
    }
}
