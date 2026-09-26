package electricity.ui;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import electricity.io.CircuitIO;
import electricity.model.Component;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

public class CircuitSim extends JFrame {
    private static final Color BG_APP = Color.decode("#101820");
    private static final Color SURFACE = Color.decode("#172029");
    private static final Color FIELD = Color.decode("#0D1217");
    private static final Color BORDER = Color.decode("#344955");
    private static final Color TEXT = Color.decode("#E8F0F2");
    private static final Color MUTED = Color.decode("#9BAEB5");
    private static final Color MINT = Color.decode("#63D6A2");
    private static final Color CYAN = Color.decode("#35C6D8");
    private static final Color BLUE = Color.decode("#6DA8E8");

    private CanvasPanel   canvasPanel;
    private Timer         updateTimer;
    private OscilloscopeDialog oscilloscopeDialog;
    private GraphDialog        graphDialog;

    // Last-saved file reference (for Ctrl+S quick-save)
    private File lastSavedFile = null;

    public CircuitSim() {
        super("Circuit Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(true);

        // Create canvas
        canvasPanel = new CanvasPanel();

        // Create floating dialogs
        graphDialog        = new GraphDialog(this);
        oscilloscopeDialog = new OscilloscopeDialog(this);

        // Wire dialogs into canvas
        canvasPanel.setGraphDialog(graphDialog);
        canvasPanel.setOscilloscopeDialog(oscilloscopeDialog);

        // Menu bar
        setJMenuBar(buildMenuBar());

        // Persistent component palette (left)
        JPanel toolbar = createToolbar();

        // Layout
        add(canvasPanel, BorderLayout.CENTER);
        add(toolbar,     BorderLayout.WEST);

        pack();
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setVisible(true);

        // Simulation loop
        setupSimulationLoop();
    }

    // =========================================================================
    // Menu bar
    // =========================================================================

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(BG_APP);
        bar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));

        bar.add(buildFileMenu());
        bar.add(buildViewMenu());
        bar.add(buildHelpMenu());

        return bar;
    }

    // ---- File menu ----------------------------------------------------------

    private JMenu buildFileMenu() {
        JMenu menu = styledMenu("File");

        JMenuItem newItem   = styledItem("New Circuit",         KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        JMenuItem openItem  = styledItem("Open Circuit…",       KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        JMenuItem saveItem  = styledItem("Save",                KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        JMenuItem saveAsItem = styledItem("Save As…",           KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        JMenuItem exitItem  = styledItem("Exit",                null);

        newItem.addActionListener(e -> newCircuit());
        openItem.addActionListener(e -> openCircuit());
        saveItem.addActionListener(e -> saveCircuit(false));
        saveAsItem.addActionListener(e -> saveCircuit(true));
        exitItem.addActionListener(e -> System.exit(0));

        menu.add(newItem);
        menu.addSeparator();
        menu.add(openItem);
        menu.add(saveItem);
        menu.add(saveAsItem);
        menu.addSeparator();
        menu.add(exitItem);
        return menu;
    }

    // ---- View menu ----------------------------------------------------------

    private JMenu buildViewMenu() {
        JMenu menu = styledMenu("View");

        JMenuItem scopeItem = styledItem("Oscilloscope",
                KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        JMenuItem graphItem = styledItem("Real-Time Graph (Alt+Click component)",
                KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK));
        JMenuItem resetViewItem = styledItem("Reset View (Home)",
                KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0));

        scopeItem.addActionListener(e -> {
            oscilloscopeDialog.setVisible(!oscilloscopeDialog.isVisible());
            oscilloscopeDialog.toFront();
        });
        graphItem.addActionListener(e -> {
            graphDialog.setVisible(!graphDialog.isVisible());
            graphDialog.toFront();
        });
        resetViewItem.addActionListener(e -> canvasPanel.requestFocusInWindow());

        menu.add(scopeItem);
        menu.add(graphItem);
        menu.addSeparator();
        menu.add(resetViewItem);
        return menu;
    }

    // ---- Help menu ----------------------------------------------------------

    private JMenu buildHelpMenu() {
        JMenu menu = styledMenu("Help");

        JMenuItem shortcutsItem = styledItem("Keyboard Shortcuts", null);
        shortcutsItem.addActionListener(e -> showShortcutsDialog());

        menu.add(shortcutsItem);
        return menu;
    }

    // ---- Menu helpers -------------------------------------------------------

    private JMenu styledMenu(String text) {
        JMenu m = new JMenu(text);
        m.setForeground(TEXT);
        m.setFont(new Font("Dialog", Font.BOLD, 13));
        return m;
    }

    private JMenuItem styledItem(String text, KeyStroke ks) {
        JMenuItem item = new JMenuItem(text);
        item.setBackground(SURFACE);
        item.setForeground(TEXT);
        item.setFont(new Font("Dialog", Font.PLAIN, 13));
        if (ks != null) item.setAccelerator(ks);
        return item;
    }

    // =========================================================================
    // File operations
    // =========================================================================

    private void newCircuit() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Create a new empty circuit? Unsaved changes will be lost.",
                "New Circuit", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            canvasPanel.clearGrid();
            lastSavedFile = null;
            setTitle("Circuit Simulator — New Circuit");
        }
    }

    private void openCircuit() {
        JFileChooser fc = buildFileChooser();
        int result = fc.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = ensureExtension(fc.getSelectedFile());
        try {
            Component[][] loaded = CircuitIO.loadFromFile(file);
            canvasPanel.setGrid(loaded);
            lastSavedFile = file;
            setTitle("Circuit Simulator — " + file.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to open circuit:\n" + ex.getMessage(),
                    "Open Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveCircuit(boolean saveAs) {
        if (!saveAs && lastSavedFile != null) {
            // Quick save to same file
            doSave(lastSavedFile);
        } else {
            JFileChooser fc = buildFileChooser();
            if (lastSavedFile != null) fc.setSelectedFile(lastSavedFile);
            int result = fc.showSaveDialog(this);
            if (result != JFileChooser.APPROVE_OPTION) return;
            File file = ensureExtension(fc.getSelectedFile());
            doSave(file);
        }
    }

    private void doSave(File file) {
        try {
            CircuitIO.saveToFile(canvasPanel.getGrid(), file);
            lastSavedFile = file;
            setTitle("Circuit Simulator — " + file.getName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to save circuit:\n" + ex.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JFileChooser buildFileChooser() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Circuit Simulator Files (*.csim)", "csim"));
        fc.setAcceptAllFileFilterUsed(false);
        return fc;
    }

    private File ensureExtension(File f) {
        if (!f.getName().toLowerCase().endsWith(".csim")) {
            return new File(f.getParentFile(), f.getName() + ".csim");
        }
        return f;
    }

    // =========================================================================
    // Toolbar
    // =========================================================================

    private JPanel createToolbar() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER),
            BorderFactory.createEmptyBorder(10, 8, 10, 8)));
        panel.setPreferredSize(new Dimension(218, 0));

        JLabel paletteTitle = new JLabel("COMPONENT PALETTE");
        paletteTitle.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        paletteTitle.setForeground(CYAN);
        paletteTitle.setFont(new Font("Dialog", Font.BOLD, 11));
        panel.add(paletteTitle);
        panel.add(Box.createVerticalStrut(8));

        JLabel actionsTitle = new JLabel("ACTIONS");
        actionsTitle.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        actionsTitle.setForeground(MUTED);
        actionsTitle.setFont(new Font("Dialog", Font.BOLD, 10));
        panel.add(actionsTitle);
        panel.add(Box.createVerticalStrut(4));

        JPanel actions = new JPanel(new GridLayout(0, 2, 4, 4));
        actions.setOpaque(false);
        actions.add(toolbarButton("New", "Create a new circuit", e -> newCircuit()));
        actions.add(toolbarButton("Open", "Open a .csim circuit", e -> openCircuit()));
        actions.add(toolbarButton("Save", "Save the current circuit", e -> saveCircuit(false)));
        actions.add(toolbarButton("Undo", "Undo the last edit (Ctrl+Z)", e -> canvasPanel.undo()));
        actions.add(toolbarButton("Redo", "Redo the last edit (Ctrl+Y)", e -> canvasPanel.redo()));
        actions.add(toolbarButton("Route", "Re-route all wires (F)", e -> canvasPanel.autoRouteWires()));
        actions.add(toolbarButton("Reset View", "Reset zoom and pan (Home)", e -> {
            canvasPanel.requestFocusInWindow();
            dispatchKey(KeyEvent.VK_HOME);
        }));
        actions.add(toolbarButton("Scope", "Show or hide the oscilloscope", e -> {
            oscilloscopeDialog.setVisible(!oscilloscopeDialog.isVisible());
            oscilloscopeDialog.toFront();
        }));
        actions.add(toolbarButton("Graph", "Show or hide the real-time graph", e -> {
            graphDialog.setVisible(!graphDialog.isVisible());
            graphDialog.toFront();
        }));
        panel.add(actions);
        panel.add(Box.createVerticalStrut(10));

        JLabel toolsTitle = new JLabel("PLACE COMPONENT");
        toolsTitle.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        toolsTitle.setForeground(MUTED);
        toolsTitle.setFont(new Font("Dialog", Font.BOLD, 10));
        panel.add(toolsTitle);
        panel.add(Box.createVerticalStrut(4));

        JPanel tools = new JPanel();
        tools.setLayout(new BoxLayout(tools, BoxLayout.Y_AXIS));
        tools.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        addToolGroup(tools, group, "Build", new Object[][] {
                {"Wire", "W", CanvasPanel.Tool.WIRE},
                {"Battery", "B", CanvasPanel.Tool.BATTERY},
                {"Bulb", "L", CanvasPanel.Tool.BULB},
                {"Resistor", "R", CanvasPanel.Tool.RESISTOR},
                {"Switch", "S", CanvasPanel.Tool.SWITCH}
        });
        addToolGroup(tools, group, "Measure", new Object[][] {
                {"Ammeter", "A", CanvasPanel.Tool.AMMETER},
                {"Voltmeter", "V", CanvasPanel.Tool.VOLTMETER}
        });
        addToolGroup(tools, group, "Semiconductor", new Object[][] {
                {"Diode", "D", CanvasPanel.Tool.DIODE},
                {"LED", "E", CanvasPanel.Tool.LED},
                {"Fuse", "U", CanvasPanel.Tool.FUSE}
        });
        addToolGroup(tools, group, "Reactive", new Object[][] {
                {"Capacitor", "C", CanvasPanel.Tool.CAPACITOR},
                {"Inductor", "I", CanvasPanel.Tool.INDUCTOR},
                {"AC Motor", "M", CanvasPanel.Tool.AC_MOTOR}
        });
        addToolGroup(tools, group, "Annotate", new Object[][] {
                {"Text Label", "T", CanvasPanel.Tool.TEXT_LABEL},
                {"Eraser", "Del", CanvasPanel.Tool.ERASER}
        });
        panel.add(tools);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private void addToolGroup(JPanel parent, ButtonGroup group, String title, Object[][] tools) {
        JPanel groupPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        groupPanel.setOpaque(false);
        groupPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER), title,
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Dialog", Font.BOLD, 10), MUTED));
            groupPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            groupPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 92));

        for (int i = 0; i < tools.length; i++) {
            String label = (String) tools[i][0];
            String shortcut = (String) tools[i][1];
            CanvasPanel.Tool tool = (CanvasPanel.Tool) tools[i][2];
            JToggleButton button = createToolButton(label, shortcut, tool);
            if (tool == CanvasPanel.Tool.WIRE) button.setSelected(true);
            group.add(button);
            groupPanel.add(button);
        }
        parent.add(groupPanel);
    }

    private JToggleButton createToolButton(String label, String shortcut, CanvasPanel.Tool tool) {
        JToggleButton button = new JToggleButton(label);
        button.setFocusable(false);
        button.setFont(new Font("Dialog", Font.BOLD, 11));
        button.setBackground(FIELD);
        button.setForeground(TEXT);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setToolTipText(label + " tool (" + shortcut + ")");
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(5, 7, 5, 7)));
        button.addChangeListener(e -> {
            if (button.isSelected()) {
                button.setBackground(MINT);
                button.setForeground(BG_APP);
            } else {
                button.setBackground(FIELD);
                button.setForeground(TEXT);
            }
        });
        button.addActionListener(e -> {
            canvasPanel.setActiveTool(tool);
            canvasPanel.requestFocusInWindow();
        });
        return button;
    }

    private JButton toolbarButton(String label, String tooltip, ActionListener listener) {
        JButton button = new JButton(label);
        button.setFocusable(false);
        button.setFont(new Font("Dialog", Font.BOLD, 11));
        button.setToolTipText(tooltip);
        button.setForeground(TEXT);
        button.setBackground(FIELD);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        button.addActionListener(listener);
        return button;
    }

    private void dispatchKey(int keyCode) {
        canvasPanel.dispatchEvent(new KeyEvent(canvasPanel, KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED));
    }

    // =========================================================================
    // Simulation loop
    // =========================================================================

    private void setupSimulationLoop() {
        updateTimer = new Timer(16, e -> {
            canvasPanel.tick();
            canvasPanel.repaint();
            // Repaint open floating dialogs
            if (oscilloscopeDialog != null && oscilloscopeDialog.isVisible()) {
                oscilloscopeDialog.getScopePanel().repaint();
            }
            if (graphDialog != null && graphDialog.isVisible()) {
                graphDialog.getGraphPanel().repaint();
            }
        });
        updateTimer.start();
    }

    // =========================================================================
    // Shortcuts dialog
    // =========================================================================

    private void showShortcutsDialog() {
        String[][] shortcuts = {
            {"Scroll Wheel",      "Zoom in/out (centered on cursor)"},
            {"Middle Drag",       "Pan the canvas"},
            {"Home",              "Reset zoom and pan"},
            {"Ctrl+Z / Ctrl+Y",   "Undo / Redo"},
            {"Ctrl+S",            "Save circuit"},
            {"Ctrl+O",            "Open circuit"},
            {"Ctrl+N",            "New circuit"},
            {"Ctrl+Shift+O",      "Toggle Oscilloscope"},
            {"R",                 "Rotate hovered component"},
            {"C",                 "Cycle wire type / toggle switch"},
            {"G",                 "Open graph for hovered component"},
            {"Alt+Click",         "Open real-time graph for component"},
            {"Double-Click",      "Open property editor"},
            {"Delete / Backspace","Delete hovered component"},
            {"F",                 "Re-route all wires"},
            {"+/-",               "Zoom in/out"},
            {"Arrow keys / WASD", "Pan the canvas"},
        };

        String[] cols = {"Shortcut", "Action"};
        JTable table = new JTable(shortcuts, cols);
        table.setEnabled(false);
        table.setFont(new Font("Monospaced", Font.PLAIN, 12));
        table.setRowHeight(22);
        table.setBackground(FIELD);
        table.setForeground(TEXT);
        table.setGridColor(BORDER);
        table.getTableHeader().setBackground(SURFACE);
        table.getTableHeader().setForeground(MINT);
        table.getTableHeader().setFont(new Font("Dialog", Font.BOLD, 12));
        table.getColumnModel().getColumn(0).setPreferredWidth(180);
        table.getColumnModel().getColumn(1).setPreferredWidth(340);

        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(540, 380));
        sp.getViewport().setBackground(FIELD);

        JOptionPane.showMessageDialog(this, sp, "Keyboard Shortcuts", JOptionPane.PLAIN_MESSAGE);
    }

    // =========================================================================
    // Entry point
    // =========================================================================

    public static void main(String[] args) {
        // Use system anti-aliasing for better text rendering
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(CircuitSim::new);
    }
}