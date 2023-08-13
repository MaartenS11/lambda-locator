package be.maartens.gui;

import be.maartens.lambda_locator.Util;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.icons.FlatInternalFrameCloseIcon;
import com.guardsquare.lambda_locator.LambdaLocator;
import com.guardsquare.proguard.assembler.ClassParser;
import com.guardsquare.proguard.assembler.Parser;
import com.guardsquare.proguard.disassembler.ClassPrinter;
import com.guardsquare.proguard.disassembler.Printer;
import org.fife.rsta.ui.search.FindDialog;
import org.fife.rsta.ui.search.SearchEvent;
import org.fife.rsta.ui.search.SearchListener;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;
import proguard.classfile.ClassPool;
import proguard.classfile.Clazz;
import proguard.classfile.Method;
import proguard.classfile.ProgramClass;
import proguard.io.util.IOUtil;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

public class Gui extends JFrame {
    private static boolean disableGui = true;
    private final JTree tree;
    private ClassPool classPool;
    private String classNameFilter;
    private final JTabbedPane tabbedPane;
    private final FindDialog findDialog;

    public Gui() {
        this(true);
    }

    public Gui(boolean exitOnClose) {
        this.classPool = new ClassPool();
        this.classNameFilter = "";

        FlatDarculaLaf.setup();

        setTitle("Lambda locator");
        setMinimumSize(new Dimension(250, 250));
        setPreferredSize(new Dimension(800, 550));
        if (exitOnClose) setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        tabbedPane = new JTabbedPane();

        findDialog = new FindDialog(this, new SearchListener() {
            @Override
            public void searchEvent(SearchEvent e) {
                RTextScrollPane scrollPane = (RTextScrollPane) tabbedPane.getSelectedComponent();
                RTextArea textArea = scrollPane.getTextArea();
                SearchResult result = SearchEngine.find(textArea, e.getSearchContext());
                if (!result.wasFound() || result.isWrapped()) {
                    UIManager.getLookAndFeel().provideErrorFeedback(textArea);
                }
            }

            @Override
            public String getSelectedText() {
                RTextScrollPane scrollPane = (RTextScrollPane) tabbedPane.getSelectedComponent();
                return scrollPane.getTextArea().getSelectedText();
            }
        });

        setupMenuBar();

        this.tree = new JTree(new DefaultMutableTreeNode());

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
                    if (node.getUserObject() instanceof LambdaLocator.Lambda lambda) {
                        disassembleClass(lambda.clazz());
                    }
                    else if (node instanceof ClassTreeNode) {
                        disassembleClass(((ClassTreeNode) node).getClazz());
                    }
                    else if (node instanceof MethodTreeNode) {
                        disassembleClass(((MethodTreeNode) node).getClazz());
                    }
                }
            }
        });
        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree,
                                                          Object value, boolean selected, boolean expanded,
                                                          boolean isLeaf, int row, boolean focused) {
                Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, focused);
                if (value instanceof ClassTreeNode) {
                    setIcon(new FlatSVGIcon(getClass().getResource("/class.svg")));
                } else if (value instanceof MethodTreeNode) {
                    setIcon(new FlatSVGIcon(getClass().getResource("/method.svg")));
                }
                else if (value instanceof LambdaTreeNode) {
                    setIcon(new FlatSVGIcon(getClass().getResource("/lambda.svg")));
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(tree);
        scrollPane.setViewportView(tree);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.add(scrollPane);
        splitPane.add(tabbedPane);

        setContentPane(splitPane);
        pack();
        splitPane.setDividerLocation(0.3);
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createEditMenu());
        setJMenuBar(menuBar);
    }

    private JMenu createFileMenu() {
        JMenu fileMenu = new JMenu("File");

        // Setup open menu.
        JMenuItem openItem = new JMenuItem("Open");
        fileMenu.add(openItem);
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileHidingEnabled(false);
        openItem.addActionListener(actionEvent -> {
            int state = fileChooser.showOpenDialog(this);
            if (state == JFileChooser.APPROVE_OPTION) {
                System.out.println("Open " + fileChooser.getSelectedFile());

                try {
                    loadFile(fileChooser.getSelectedFile().getAbsolutePath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // Setup class filter menu.
        JMenuItem classFilterItem = new JMenuItem("Configure class filter");
        fileMenu.add(classFilterItem);
        classFilterItem.addActionListener(actionEvent -> {
            String result = JOptionPane.showInputDialog(
                    this,
                    "Enter a class name filter:",
                    "Set class name filter", JOptionPane.QUESTION_MESSAGE
            );
            if (result != null) {
                this.classNameFilter = result;
                reload();
            }
        });

        // Set up disassemble menu.
        JMenuItem disassembleItem = new JMenuItem("Disassemble");
        fileMenu.add(disassembleItem);
        disassembleItem.addActionListener(actionEvent -> {
            ClassSelector classSelector = new ClassSelector(this, classPool);
            classSelector.setVisible(true);
            String selectedItem = classSelector.getSelectedItem();
            if (selectedItem != null)
                disassembleClass(selectedItem);
        });

        return fileMenu;
    }

    private JMenu createEditMenu() {
        JMenu editMenu = new JMenu("Edit");

        // Set up find menu.
        JMenuItem findItem = new JMenuItem("Find");
        editMenu.add(findItem);
        findItem.addActionListener(actionEvent -> {
            findDialog.setVisible(true);
            findDialog.requestFocus();
        });

        // Set up close tab menu.
        JMenuItem closeTabItem = new JMenuItem("Close tab");
        editMenu.add(closeTabItem);
        closeTabItem.addActionListener(actionEvent -> tabbedPane.remove(tabbedPane.getSelectedComponent()));

        // Set up close all tabs menu.
        JMenuItem closeAllTabsItem = new JMenuItem("Close all tabs");
        editMenu.add(closeAllTabsItem);
        closeAllTabsItem.addActionListener(actionEvent -> tabbedPane.removeAll());

        // Set up compile menu.
        JMenuItem compileItem = new JMenuItem("Compile");
        compileItem.addActionListener(actionEvent -> {
            try {
                assemble();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        editMenu.add(compileItem);
        return editMenu;
    }

    public Gui(String filename, String classNameFilter) throws IOException {
        this();
        setClassNameFilter(classNameFilter);
        loadFile(filename);
    }

    public Gui(ClassPool classPool, String classNameFilter) {
        this(classPool, classNameFilter, true);
    }

    public Gui(ClassPool classPool, String classNameFilter, boolean exitOnClose) {
        this(exitOnClose);
        setClassNameFilter(classNameFilter);
        this.classPool = classPool;
        reload();
    }

    public void waitForUntilClosed() {
        while (isVisible()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void loadFile(String filename) throws IOException {
        if (filename.endsWith(".apk")) {
            classPool = Util.loadApk(filename);
        } else {
            classPool = Util.loadJar(filename);
        }
        reload();
    }

    private void reload() {
        LambdaLocator lambdaLocator = new LambdaLocator(classPool, classNameFilter);

        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) tree.getModel().getRoot();
        rootNode.removeAllChildren();
        for (Clazz clazz : lambdaLocator.getLambdasByClass().keySet()) {
            DefaultMutableTreeNode classNode = new ClassTreeNode(clazz);
            rootNode.add(classNode);
            for (Method method : lambdaLocator.getLambdasByClass().get(clazz).keySet()) {
                DefaultMutableTreeNode methodNode = new MethodTreeNode(clazz, method);
                classNode.add(methodNode);

                for (LambdaLocator.Lambda lambda : lambdaLocator.getLambdasByClass().get(clazz).get(method)) {
                    methodNode.add(new LambdaTreeNode(lambda));
                }
            }
        }

        ((DefaultTreeModel) tree.getModel()).reload();
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }

        tabbedPane.removeAll();
    }

    public void disassembleClass(Clazz pClass) {
        String tabName = pClass.getName();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getTitleAt(i).equals(tabName)) {
                tabbedPane.setSelectedIndex(i);
                return;
            }
        }

        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setCodeFoldingEnabled(true);
        textArea.setEditable(true);
        textArea.setText(classToString((ProgramClass) pClass));
        textArea.setMarkOccurrences(true);
        textArea.setMarkOccurrencesDelay(150);

        textArea.registerKeyboardAction(
                actionEvent -> findDialog.setVisible(!findDialog.isVisible()),
                KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK),
                JComponent.WHEN_FOCUSED
        );

        try {
            Theme theme = Theme.load(getClass().getResourceAsStream("/dark.xml"));
            //Theme theme = Theme.load(getClass().getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/default.xml"));
            theme.apply(textArea);
        } catch (IOException ioe) { // Never happens
            ioe.printStackTrace();
        }

        RTextScrollPane textScrollPane = new RTextScrollPane(textArea);
        tabbedPane.add(pClass.getName(), textScrollPane);
        tabbedPane.setSelectedComponent(textScrollPane);

        JPanel tabPanel = new JPanel(new BorderLayout(7, 0));
        tabPanel.setOpaque(false);
        tabPanel.add(new JLabel(tabbedPane.getTitleAt(tabbedPane.getSelectedIndex())), BorderLayout.CENTER);
        JButton closeButton = new JButton(new FlatInternalFrameCloseIcon());
        closeButton.setContentAreaFilled(false);
        closeButton.setBorder(null);
        closeButton.addActionListener(actionEvent -> tabbedPane.remove(textScrollPane));
        tabPanel.add(closeButton, BorderLayout.EAST);
        tabbedPane.setTabComponentAt(tabbedPane.getSelectedIndex(), tabPanel);
    }

    public void assemble() throws IOException {
        // Load java standard library
        /*ClassPool libraryClassPool = IOUtil.read("/usr/lib/jvm/java-17-openjdk-amd64/jmods/java.base.jmod", true);
        libraryClassPool.classesAccept(new ClassPoolFiller(classPool));*/
        //preverify(classPool, libraryClassPool);

        // Assemble all open tabs
        // TODO: This current means that closing a tab will cause changes to not be saved, I could perhaps add a warning
        // message for that.
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            String sourceClass = tabbedPane.getTitleAt(i);
            String source = ((RTextScrollPane) tabbedPane.getComponentAt(i)).getTextArea().getText();
            /*classPool.removeClass(classPool.getClass(sourceClass));
            DataEntryReader jbcReader = new JbcReader(new ClassPoolFiller(classPool));
            DataEntry entry = new StreamingDataEntry(sourceClass + ".class", new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
            jbcReader.read(entry);*/

            classPool.getClass(sourceClass).accept(new ClassParser(new Parser(new StringReader(source))));
        }

        int n = JOptionPane.showConfirmDialog(
                this,
                "Would you like to save the current class pool\n as a jar? Be aware that doing this in the middle\n of a pass might result in non functional code.",
                "Save as jar?",
                JOptionPane.YES_NO_OPTION);
        if (n == JOptionPane.YES_OPTION) {
            try {
                IOUtil.writeJar(classPool, "output.jar", "MainKt");
            } catch(Exception e) {
                JOptionPane.showMessageDialog(this,
                        "An error occurred while writing the jar file!",
                        "Save error",
                        JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private void disassembleClass(String className) {
        disassembleClass(classPool.getClass(className));
    }

    public void setClassNameFilter(String classNameFilter) {
        this.classNameFilter = classNameFilter;
    }

    private String classToString(String className) {
        return classToString((ProgramClass) classPool.getClass(className));
    }

    private String classToString(ProgramClass pClass) {
        StringWriter writer = new StringWriter();
        new ClassPrinter(new Printer(writer)).visitProgramClass(pClass);
        return writer.toString();
    }

    public static void open(ClassPool classPool, String classNameFilter) {
        open(classPool, classNameFilter, null);
    }

    public static void open(ClassPool classPool, String classNameFilter, String message) {
        if (disableGui)
            return;

        Gui gui = new Gui(classPool, classNameFilter, false);
        gui.setVisible(true);
        if (message != null)
            JOptionPane.showMessageDialog(gui, message);
        gui.waitForUntilClosed();
        gui.dispose();
    }

    public static void setAllowGui(boolean allowGui) {
        disableGui = !allowGui;
    }
}
