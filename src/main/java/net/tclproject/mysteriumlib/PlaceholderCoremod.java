package net.tclproject.mysteriumlib;

import net.tclproject.mysteriumlib.asm.common.CustomLoadingPlugin;
import net.tclproject.mysteriumlib.asm.common.FirstClassTransformer;

public class PlaceholderCoremod extends CustomLoadingPlugin {
    // Required in order for MysteriumLib to be recognized as a coremod and for other coremods to import it.

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{FirstClassTransformer.class.getName()};
    }

    @Override
    public void registerFixes() {
    }
}
