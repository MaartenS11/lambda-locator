package be.maartens.gui;

import proguard.classfile.Clazz;

import javax.swing.tree.DefaultMutableTreeNode;

public class ClassTreeNode extends DefaultMutableTreeNode {
    private final Clazz clazz;
    public ClassTreeNode(Clazz clazz) {
        super(clazz.getName());
        this.clazz = clazz;
    }

    public Clazz getClazz() {
        return clazz;
    }
}
