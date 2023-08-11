package be.maartens.gui;

import proguard.classfile.ClassPool;
import proguard.classfile.util.ClassUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ClassSelector extends JDialog {
    DefaultListModel<String> classListModel;
    private int selectedIndex;

    public ClassSelector(JFrame owner, ClassPool classPool) {
        super(owner, true);
        classListModel = new DefaultListModel<>();
        classPool.classesAccept(clazz -> classListModel.addElement(clazz.getName()));
        JList<String> classList = new JList<>(classListModel);

        selectedIndex = -1;
        classList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedIndex = classList.getSelectedIndex();
                setVisible(false);
            }
        });

        JScrollPane scrollPane = new JScrollPane(classList);
        scrollPane.setViewportView(classList);

        JTextField textField = new JTextField();
        textField.addActionListener(actionEvent -> {
            classListModel.clear();
            String classNameFilter = textField.getText();
            if (classNameFilter.contains(".")) {
                classNameFilter = ClassUtil.internalClassName(classNameFilter);
            }
            classPool.classesAccept(classNameFilter + "**", clazz -> classListModel.addElement(clazz.getName()));
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(textField, BorderLayout.NORTH);

        setContentPane(panel);

        setTitle("Class selector");
        setMinimumSize(new Dimension(250, 250));
        setPreferredSize(new Dimension(500, 300));
        pack();
        setLocation(owner.getX() + owner.getWidth()/2 - getWidth()/2, owner.getY() + owner.getHeight()/2 - getHeight()/2);
    }

    public String getSelectedItem() {
        if (selectedIndex < 0) return null;
        return classListModel.get(selectedIndex);
    }
}
