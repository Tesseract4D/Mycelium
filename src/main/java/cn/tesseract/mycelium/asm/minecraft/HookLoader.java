package cn.tesseract.mycelium.asm.minecraft;

import cn.tesseract.mycelium.asm.AsmHook;
import cn.tesseract.mycelium.asm.ClassMetadataReader;
import cn.tesseract.mycelium.asm.HookClassTransformer;
import cn.tesseract.mycelium.asm.NodeTransformer;
import cpw.mods.fml.common.asm.transformers.DeobfuscationTransformer;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class HookLoader implements IFMLLoadingPlugin {

    private static DeobfuscationTransformer deobfuscationTransformer;

    private static ClassMetadataReader deobfuscationMetadataReader;

    static {
        deobfuscationMetadataReader = new DeobfuscationMetadataReader();
    }

    public static HookClassTransformer getTransformer() {
        return PrimaryClassTransformer.instance.registeredSecondTransformer ?
                MinecraftClassTransformer.instance : PrimaryClassTransformer.instance;
    }

    public static void registerHook(AsmHook hook) {
        getTransformer().registerHook(hook);
    }

    public static void registerHookContainer(String className) {
        getTransformer().registerHookContainer(className);
    }

    public static void registerNodeTransformer(String className, NodeTransformer transformer) {
        List<NodeTransformer> list = MinecraftClassTransformer.transformerMap.computeIfAbsent(className, k -> new ArrayList<>());
        list.add(transformer);
    }

    public static ClassMetadataReader getDeobfuscationMetadataReader() {
        return deobfuscationMetadataReader;
    }

    static DeobfuscationTransformer getDeobfuscationTransformer() {
        if (HookLibPlugin.getObfuscated() && deobfuscationTransformer == null) {
            deobfuscationTransformer = new DeobfuscationTransformer();
        }
        return deobfuscationTransformer;
    }

    // 1.6.x only
    public String[] getLibraryRequestClass() {
        return null;
    }

    // 1.7.x only
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        registerHooks();
    }

    protected abstract void registerHooks();
}
