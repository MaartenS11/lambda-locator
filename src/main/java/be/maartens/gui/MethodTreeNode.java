package be.maartens.gui;

import proguard.classfile.Clazz;
import proguard.classfile.Method;

import javax.swing.tree.DefaultMutableTreeNode;

public class MethodTreeNode extends DefaultMutableTreeNode {
    private final Clazz clazz;
    private final Method method;
    public MethodTreeNode(Clazz clazz, Method method) {
        super(method.getName(clazz));
        this.clazz = clazz;
        this.method = method;
    }

    public Clazz getClazz() {
        return clazz;
    }

    public Method getMethod() {
        return method;
    }
}
