package cn.tesseract.mycelium.asm.minecraft;

import cpw.mods.fml.relauncher.CoreModManager;
import cpw.mods.fml.relauncher.FMLRelaunchLog;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class HookLibPlugin implements IFMLLoadingPlugin {
    public static Map<Integer, String> methodNames;
    public static Map<Integer, String> fieldNames;
    private static boolean obf;
    private static boolean checked;

    static {
        try {
            methodNames = loadNames("methods.bin");
            fieldNames = loadNames("fields.bin");
        } catch (IOException e) {
            throw new RuntimeException("Can not load obfuscated method names");
        }
    }

    public static String getMethodMcpName(String srgName) {
        if (HookLibPlugin.getObfuscated()) {
            int index;
            if (srgName.startsWith("func_")) {
                int first = srgName.indexOf('_');
                int second = srgName.indexOf('_', first + 1);
                index = Integer.parseInt(srgName.substring(first + 1, second));
            } else {
                return srgName;
            }
            String mcpName = methodNames.get(index);
            if (mcpName != null)
                return mcpName;
        }
        return srgName;
    }

    public static String getFieldMcpName(String srgName) {
        if (HookLibPlugin.getObfuscated()) {
            int index;
            if (srgName.startsWith("field_")) {
                int first = srgName.indexOf('_');
                int second = srgName.indexOf('_', first + 1);
                index = Integer.parseInt(srgName.substring(first + 1, second));
            } else {
                return srgName;
            }
            String mcpName = fieldNames.get(index);
            if (mcpName != null)
                return mcpName;
        }
        return srgName;
    }

    public static HashMap<Integer, String> loadNames(String name) throws IOException {
        InputStream resourceStream = HookLibPlugin.class.getResourceAsStream("/META-INF/" + name);
        if (resourceStream == null) throw new IOException("Methods dictionary not found");
        DataInputStream input = new DataInputStream(new BufferedInputStream(resourceStream));
        int numMethods = input.readInt();
        HashMap<Integer, String> map = new HashMap<Integer, String>(numMethods);
        for (int i = 0; i < numMethods; i++) {
            map.put(input.readInt(), input.readUTF());
        }
        input.close();
        return map;
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
        return new String[]{PrimaryClassTransformer.class.getName()};
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
    public void injectData(Map<String, Object> data) {}

    public static boolean getObfuscated() {
        if (!checked) {
            try {
                Field deobfField = CoreModManager.class.getDeclaredField("deobfuscatedEnvironment");
                deobfField.setAccessible(true);
                obf = !deobfField.getBoolean(null);
                FMLRelaunchLog.info("[HOOKLIB] " + " Obfuscated: " + obf);
            } catch (Exception e) {
                e.printStackTrace();
            }
            checked = true;
        }
        return obf;
    }
}
