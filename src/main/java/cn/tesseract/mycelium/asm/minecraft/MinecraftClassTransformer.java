package cn.tesseract.mycelium.asm.minecraft;

import cn.tesseract.mycelium.asm.AsmHook;
import cn.tesseract.mycelium.asm.HookClassTransformer;
import cn.tesseract.mycelium.asm.HookInjectorClassVisitor;
import cn.tesseract.mycelium.asm.NodeTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Этот трансформер занимается вставкой хуков с момента запуска майнкрафта. Здесь сосредоточены все костыли,
 * которые необходимы для правильной работы с обфусцированными названиями методов.
 */
public class MinecraftClassTransformer extends HookClassTransformer implements IClassTransformer {

    public static MinecraftClassTransformer instance;
    private static final List<IClassTransformer> postTransformers = new ArrayList<IClassTransformer>();
    public static final HashMap<String, List<NodeTransformer>> transformerMap = new HashMap<>();

    public MinecraftClassTransformer() {
        instance = this;

        this.classMetadataReader = HookLoader.getDeobfuscationMetadataReader();

        this.hooksMap.putAll(PrimaryClassTransformer.instance.getHooksMap());
        PrimaryClassTransformer.instance.getHooksMap().clear();
        PrimaryClassTransformer.instance.registeredSecondTransformer = true;
    }

    @Override
    public byte[] transform(String oldName, String newName, byte[] bytecode) {
        bytecode = transform(newName, bytecode);
        for (int i = 0; i < postTransformers.size(); i++) {
            bytecode = postTransformers.get(i).transform(oldName, newName, bytecode);
        }

        List<NodeTransformer> transformers = transformerMap.get(newName);
        if (transformers != null) {
            ClassNode classNode = new ClassNode();
            ClassReader classReader = new ClassReader(bytecode);
            classReader.accept(classNode, 0);
            for (NodeTransformer transformer : transformers)
                transformer.transform(classNode);
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            classNode.accept(classWriter);
            bytecode = classWriter.toByteArray();
        }

        return bytecode;
    }

    @Override
    protected HookInjectorClassVisitor createInjectorClassVisitor(ClassWriter cw, List<AsmHook> hooks) {
        return new HookInjectorClassVisitor(this, cw, hooks) {
            @Override
            protected boolean isTargetMethod(AsmHook hook, String name, String desc) {
                return super.isTargetMethod(hook, HookLibPlugin.getMethodMcpName(name), desc);
            }
        };
    }

    /**
     * Регистрирует трансформер, который будет запущен после обычных, и в том числе после деобфусцирующего трансформера.
     */
    public static void registerPostTransformer(IClassTransformer transformer) {
        postTransformers.add(transformer);
    }
}
