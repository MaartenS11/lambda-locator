package be.maartens.gui;

import com.guardsquare.lambda_locator.LambdaLocator;

import javax.swing.tree.DefaultMutableTreeNode;

public class LambdaTreeNode extends DefaultMutableTreeNode {
    public LambdaTreeNode(LambdaLocator.Lambda lambda) {
        super(lambda);
    }
}
