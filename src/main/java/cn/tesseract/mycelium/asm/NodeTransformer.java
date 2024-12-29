package cn.tesseract.mycelium.asm;

import org.objectweb.asm.tree.ClassNode;

@FunctionalInterface
public interface NodeTransformer {
    void transform(ClassNode node);
}
