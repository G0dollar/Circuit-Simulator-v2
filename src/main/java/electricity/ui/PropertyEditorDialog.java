package electricity.ui;

import electricity.model.*;
import electricity.model.Component;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dark-themed property editor dialog that replaces the plain JOptionPane prompts.
 * Dynamically builds the form based on the component type passed in.
 *
 * Usage:
 *   PropertyEditorDialog dlg = new PropertyEditorDialog(owner, comp);
 *   dlg.setShowGraphCallback(comp -> { ... open graph dialog ... });
 *   dlg.setVisible(true);
 *   if (dlg.isApplied()) { ... read dlg.getNewValues() and commit ... }
 */
public class PropertyEditorDialog extends JDialog {

    // ---- Colors -------------------------------------------------------------
    private static final Color BG_DARK       = Color.decode("#101820");
    private static final Color BG_PANEL      = Color.decode("#172029");
    private static final Color BG_FIELD      = Color.decode("#0D1217");
    private static final Color ACCENT        = Color.decode("#63D6A2");
    private static final Color ACCENT_CYAN   = Color.decode("#35C6D8");
    private static final Color ACCENT_RED    = Color.decode("#E97878");
    private static final Color TEXT_PRIMARY  = Color.decode("#E8F0F2");
    private static final Color TEXT_MUTED    = Color.decode("#9BAEB5");
    private static final Color BORDER_NORMAL = Color.decode("#344955");
    private static final Color BORDER_ERROR  = Color.decode("#E97878");

    // ---- State --------------------------------------------------------------
    private boolean applied = false;

    // Field containers (key → JTextField or JCheckBox)
    private final Map<String, JTextField> textFields   = new LinkedHashMap<>();
    private final Map<String, JCheckBox>  checkBoxes   = new LinkedHashMap<>();

    // Validation rules: field key → error message (null = valid)
    private final Map<String, String> fieldErrors = new HashMap<>();

    private final Component target;
    private Consumer<Component> showGraphCallback;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public PropertyEditorDialog(JFrame owner, Component target) {
        super(owner, buildTitle(target), true);
        this.target = target;
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG_DARK);

        // Header
        root.add(buildHeader(target), BorderLayout.NORTH);

        // Form
        JPanel form = buildForm(target);
        JScrollPane scroll = new JScrollPane(form,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_PANEL);
        root.add(scroll, BorderLayout.CENTER);

        // Button row
        root.add(buildButtonRow(), BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(360, 200));
        setLocationRelativeTo(owner);
    }

    // -------------------------------------------------------------------------
    // Header
    // -------------------------------------------------------------------------

    private static String buildTitle(Component comp) {
        return "Properties — " + comp.getClass().getSimpleName()
               + " (" + comp.getCol() + ", " + comp.getRow() + ")";
    }

    private JPanel buildHeader(Component comp) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.decode("#101820"));
        header.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_NORMAL),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        // Component type label
        JLabel typeLbl = new JLabel(comp.getClass().getSimpleName());
        typeLbl.setForeground(ACCENT_CYAN);
        typeLbl.setFont(new Font("Dialog", Font.BOLD, 16));

        // Position sub-label
        JLabel posLbl = new JLabel("Grid (" + comp.getCol() + ", " + comp.getRow() + ")  |  "
                + "Dir: " + comp.getDirection().name());
        posLbl.setForeground(TEXT_MUTED);
        posLbl.setFont(new Font("Dialog", Font.PLAIN, 11));

        JPanel labels = new JPanel(new BorderLayout());
        labels.setOpaque(false);
        labels.add(typeLbl, BorderLayout.CENTER);
        labels.add(posLbl,  BorderLayout.SOUTH);

        header.add(labels, BorderLayout.CENTER);
        header.add(buildReadout(comp), BorderLayout.EAST);
        return header;
    }

        private JPanel buildReadout(Component comp) {
        JPanel readout = new JPanel();
        readout.setOpaque(false);
        readout.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_NORMAL),
            BorderFactory.createEmptyBorder(5, 9, 5, 9)));
        readout.setLayout(new BoxLayout(readout, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("SIMULATION READOUT");
        heading.setForeground(ACCENT_CYAN);
        heading.setFont(new Font("Dialog", Font.BOLD, 9));
        heading.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
        readout.add(heading);

        JLabel state = new JLabel(comp.hasSolverError()
            ? "WARNING"
            : (comp.isActive() ? "ACTIVE" : "INACTIVE"));
        state.setForeground(comp.hasSolverError() ? Color.decode("#F2B84B") :
            (comp.isActive() ? ACCENT : TEXT_MUTED));
        state.setFont(new Font("Dialog", Font.BOLD, 11));
        state.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
        readout.add(state);

        JLabel values = new JLabel(String.format("I %.3f A   V %.2f V", comp.getCurrent(), comp.getVoltageDrop()));
        values.setForeground(TEXT_PRIMARY);
        values.setFont(new Font("Monospaced", Font.PLAIN, 10));
        values.setAlignmentX(java.awt.Component.RIGHT_ALIGNMENT);
        readout.add(values);
        return readout;
        }

    // -------------------------------------------------------------------------
    // Form builder (dynamic per component type)
    // -------------------------------------------------------------------------

    private JPanel buildForm(Component comp) {
        JPanel form = new JPanel();
        form.setBackground(BG_PANEL);
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 70, 80), 1),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(sectionLabel("EDITABLE PARAMETERS"));
        form.add(Box.createVerticalStrut(8));

        if (comp instanceof Battery) {
            Battery b = (Battery) comp;
            addDoubleField(form, "voltage",           "Voltage (V)",            String.valueOf(b.getVoltage()),        v -> v >= 0, "Must be ≥ 0");
            addDoubleField(form, "internalResistance","Internal Resistance (Ω)", String.valueOf(b.getInternalResistance()), v -> v >= 0, "Must be ≥ 0");

        } else if (comp instanceof Resistor) {
            Resistor r = (Resistor) comp;
            addDoubleField(form, "resistance", "Resistance (Ω)", String.valueOf(r.getResistance()), v -> v >= 0.1, "Must be ≥ 0.1 Ω");

        } else if (comp instanceof Bulb) {
            Bulb b = (Bulb) comp;
            addDoubleField(form, "resistance", "Bulb Resistance (Ω)", String.valueOf(b.getResistance()), v -> v >= 0.1, "Must be ≥ 0.1 Ω");

        } else if (comp instanceof Fuse) {
            Fuse f = (Fuse) comp;
            addDoubleField(form, "limit", "Current Limit (A)", String.valueOf(f.getLimit()), v -> v > 0, "Must be > 0 A");
            addCheckBox(form, "blown", "Blown / Tripped", f.isBlown());

        } else if (comp instanceof TextLabel) {
            TextLabel tl = (TextLabel) comp;
            addTextField(form, "text", "Label Text", tl.getText());

        } else if (comp instanceof Capacitor) {
            Capacitor cap = (Capacitor) comp;
            addDoubleField(form, "capacitance", "Capacitance (µF)",
                    String.format("%.4f", cap.getCapacitance() * 1e6),
                    v -> v > 0, "Must be > 0 µF");

        } else if (comp instanceof Inductor) {
            Inductor ind = (Inductor) comp;
            addDoubleField(form, "inductance", "Inductance (H)", String.valueOf(ind.getInductance()), v -> v > 0, "Must be > 0 H");

        } else if (comp instanceof ACMotor) {
            ACMotor ac = (ACMotor) comp;
            addDoubleField(form, "peakVoltage", "Peak Voltage (V)", String.valueOf(ac.getPeakVoltage()), v -> v >= 0, "Must be ≥ 0");
            addDoubleField(form, "frequency",   "Frequency (Hz)",   String.valueOf(ac.getFrequency()),   v -> v > 0,  "Must be > 0 Hz");

        } else if (comp instanceof Switch) {
            Switch sw = (Switch) comp;
            addCheckBox(form, "open", "Switch Open (circuit broken)", sw.isOpen());

        } else {
            // Ammeter, Voltmeter, Diode, LED, Wire — no editable parameters
            JLabel infoLbl = new JLabel("No configurable properties for " + comp.getClass().getSimpleName() + ".");
            infoLbl.setForeground(TEXT_MUTED);
            infoLbl.setFont(new Font("Dialog", Font.ITALIC, 12));
            infoLbl.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            form.add(infoLbl);
        }

        return form;
    }

    private JLabel sectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(ACCENT_CYAN);
        label.setFont(new Font("Dialog", Font.BOLD, 10));
        label.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        return label;
    }

    // -------------------------------------------------------------------------
    // Form element helpers
    // -------------------------------------------------------------------------

    private interface DoubleValidator { boolean test(double v); }

    private void addDoubleField(JPanel form, String key, String label, String initialValue,
                                DoubleValidator validator, String errorMsg) {
        form.add(makeFieldRow(key, label, initialValue, text -> {
            try {
                double val = Double.parseDouble(text.trim());
                if (!validator.test(val)) {
                    fieldErrors.put(key, errorMsg);
                } else {
                    fieldErrors.remove(key);
                }
            } catch (NumberFormatException e) {
                fieldErrors.put(key, "Not a valid number");
            }
        }));
        form.add(Box.createVerticalStrut(8));
    }

    private void addTextField(JPanel form, String key, String label, String initialValue) {
        form.add(makeFieldRow(key, label, initialValue, text -> fieldErrors.remove(key)));
        form.add(Box.createVerticalStrut(8));
    }

    private JPanel makeFieldRow(String key, String labelText, String initialValue,
                                 java.util.function.Consumer<String> validator) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 56));
        row.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(labelText);
        lbl.setForeground(TEXT_PRIMARY);
        lbl.setFont(new Font("Dialog", Font.PLAIN, 12));
        lbl.setPreferredSize(new Dimension(180, 24));

        JTextField field = new JTextField(initialValue);
        field.setBackground(BG_FIELD);
        field.setForeground(TEXT_PRIMARY);
        field.setCaretColor(ACCENT);
        field.setFont(new Font("Monospaced", Font.PLAIN, 13));
        field.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(BORDER_NORMAL, 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));

        // Error label below the field
        JLabel errLbl = new JLabel(" ");
        errLbl.setForeground(ACCENT_RED);
        errLbl.setFont(new Font("Dialog", Font.PLAIN, 10));

        // Validation on each key release
        field.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                validator.accept(field.getText());
                if (fieldErrors.containsKey(key)) {
                    field.setBorder(new CompoundBorder(
                        BorderFactory.createLineBorder(BORDER_ERROR, 1),
                        BorderFactory.createEmptyBorder(4, 8, 4, 8)
                    ));
                    errLbl.setText(fieldErrors.get(key));
                } else {
                    field.setBorder(new CompoundBorder(
                        BorderFactory.createLineBorder(ACCENT.darker(), 1),
                        BorderFactory.createEmptyBorder(4, 8, 4, 8)
                    ));
                    errLbl.setText(" ");
                }
            }
        });

        JPanel fieldBlock = new JPanel(new BorderLayout());
        fieldBlock.setOpaque(false);
        fieldBlock.add(field,  BorderLayout.CENTER);
        fieldBlock.add(errLbl, BorderLayout.SOUTH);

        row.add(lbl,        BorderLayout.WEST);
        row.add(fieldBlock, BorderLayout.CENTER);

        textFields.put(key, field);
        return row;
    }

    private void addCheckBox(JPanel form, String key, String labelText, boolean initialValue) {
        JCheckBox cb = new JCheckBox(labelText, initialValue);
        cb.setBackground(BG_PANEL);
        cb.setForeground(TEXT_PRIMARY);
        cb.setFont(new Font("Dialog", Font.PLAIN, 12));
        cb.setFocusPainted(false);
        cb.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        form.add(cb);
        form.add(Box.createVerticalStrut(8));
        checkBoxes.put(key, cb);
    }

    // -------------------------------------------------------------------------
    // Button row
    // -------------------------------------------------------------------------

    private JPanel buildButtonRow() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        bar.setBackground(Color.decode("#101820"));
        bar.setBorder(new MatteBorder(1, 0, 0, 0, BORDER_NORMAL));

        // Show Graph button (left-aligned)
        JButton graphBtn = makeButton("Show Graph", ACCENT_CYAN.darker());
        graphBtn.addActionListener(e -> {
            if (showGraphCallback != null) showGraphCallback.accept(target);
        });

        JPanel leftSide = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftSide.setOpaque(false);
        leftSide.add(graphBtn);

        JButton applyBtn  = makeButton("Apply",  ACCENT.darker());
        JButton cancelBtn = makeButton("Cancel", Color.decode("#263640"));

        applyBtn.addActionListener(e -> {
            // Force validate all fields
            validateAll();
            if (fieldErrors.isEmpty()) {
                applied = true;
                dispose();
            } else {
                // Shake the dialog slightly to indicate error
                JOptionPane.showMessageDialog(this,
                    "Please fix the highlighted fields before applying.",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> {
            applied = false;
            dispose();
        });

        // Allow Enter to apply, Escape to cancel
        getRootPane().setDefaultButton(applyBtn);
        getRootPane().registerKeyboardAction(
            ev -> { applied = false; dispose(); },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        JPanel rightSide = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightSide.setOpaque(false);
        rightSide.add(cancelBtn);
        rightSide.add(applyBtn);

        bar.setLayout(new BorderLayout());
        bar.add(leftSide,  BorderLayout.WEST);
        bar.add(rightSide, BorderLayout.EAST);
        bar.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER_NORMAL),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));

        return bar;
    }

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFont(new Font("Dialog", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(bg.brighter(), 1),
            BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(bg.brighter());
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    // -------------------------------------------------------------------------
    // Validation & value extraction
    // -------------------------------------------------------------------------

    private void validateAll() {
        // Trigger key-release validators by simulating validation
        for (Map.Entry<String, JTextField> entry : textFields.entrySet()) {
            // Re-trigger by firing a dummy key event equivalent
            entry.getValue().getKeyListeners()[0]
                 .keyReleased(new KeyEvent(entry.getValue(), KeyEvent.KEY_RELEASED, 0, 0, 0, '\0'));
        }
    }

    public boolean isApplied() { return applied; }

    // ---- Value getters (used by CanvasPanel after dialog closes) ------------

    public double getDouble(String key, double defaultVal) {
        JTextField f = textFields.get(key);
        if (f == null) return defaultVal;
        try { return Double.parseDouble(f.getText().trim()); }
        catch (NumberFormatException e) { return defaultVal; }
    }

    public String getString(String key, String defaultVal) {
        JTextField f = textFields.get(key);
        if (f == null) return defaultVal;
        return f.getText();
    }

    public boolean getBoolean(String key, boolean defaultVal) {
        JCheckBox cb = checkBoxes.get(key);
        if (cb == null) return defaultVal;
        return cb.isSelected();
    }

    // ---- Callback -----------------------------------------------------------

    public void setShowGraphCallback(Consumer<Component> callback) {
        this.showGraphCallback = callback;
    }
}
