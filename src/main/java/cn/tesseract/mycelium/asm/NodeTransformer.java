package cn.tesseract.mycelium.asm;

import org.objectweb.asm.tree.ClassNode;

public abstract class NodeTransformer {
    public abstract void transform(ClassNode node);
}
