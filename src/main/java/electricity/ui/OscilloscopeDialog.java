package electricity.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Non-modal dialog that hosts the OscilloscopePanel.
 * Provides controls for voltage scale, time scale, and freeze toggle.
 */
public class OscilloscopeDialog extends JDialog {

    private static final Color SURFACE = Color.decode("#172029");
    private static final Color SURFACE_DARK = Color.decode("#101820");
    private static final Color BORDER = Color.decode("#344955");
    private static final Color TEXT = Color.decode("#E8F0F2");
    private static final Color MUTED = Color.decode("#9BAEB5");
    private static final Color MINT = Color.decode("#63D6A2");
    private static final Color AMBER = Color.decode("#F2B84B");

    private final OscilloscopePanel scopePanel;

    public OscilloscopeDialog(JFrame owner) {
        super(owner, "Oscilloscope — AC Signal Monitor", false); // non-modal
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        scopePanel = new OscilloscopePanel();

        // ---- Control bar ----
        JPanel controls = buildControlBar();

        // ---- Layout ----
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBackground(SURFACE_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        content.add(scopePanel, BorderLayout.CENTER);
        content.add(controls, BorderLayout.SOUTH);

        setContentPane(content);
        pack();
        setMinimumSize(new Dimension(500, 280));
        setLocationRelativeTo(owner);
    }

    private JPanel buildControlBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        bar.setBackground(SURFACE);
        bar.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));

        // Voltage scale
        bar.add(makeLabel("V-Scale:"));
        String[] vScales = { "0.5V", "1V", "2V", "5V", "10V", "20V", "50V", "100V" };
        JComboBox<String> vScaleBox = new JComboBox<>(vScales);
        vScaleBox.setSelectedIndex(4); // 10V default
        styleCombo(vScaleBox);
        vScaleBox.addActionListener(e -> {
            String s = (String) vScaleBox.getSelectedItem();
            if (s == null) return;
            double val = Double.parseDouble(s.replace("V", ""));
            scopePanel.setVoltageScale(val);
            scopePanel.repaint();
        });
        bar.add(vScaleBox);

        // Separator
        bar.add(Box.createHorizontalStrut(10));

        // Freeze toggle
        JToggleButton freezeBtn = new JToggleButton("❚❚ Freeze");
        styleToggleButton(freezeBtn);
        freezeBtn.addActionListener(e -> scopePanel.setFrozen(freezeBtn.isSelected()));
        bar.add(freezeBtn);

        // Clear button
        JButton clearBtn = new JButton("⟳ Clear");
        styleButton(clearBtn);
        clearBtn.addActionListener(e -> {
            // Nothing to clear – the buffer naturally rolls; just unfreeze
            scopePanel.setFrozen(false);
            freezeBtn.setSelected(false);
            scopePanel.repaint();
        });
        bar.add(clearBtn);

        return bar;
    }

    // ---- Accessors ----------------------------------------------------------

    public OscilloscopePanel getScopePanel() {
        return scopePanel;
    }

    // ---- Style helpers -------------------------------------------------------

    private JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(MINT);
        l.setFont(new Font("Dialog", Font.PLAIN, 12));
        return l;
    }

    private void styleCombo(JComboBox<?> combo) {
        combo.setBackground(Color.decode("#0D1217"));
        combo.setForeground(TEXT);
        combo.setFont(new Font("Monospaced", Font.PLAIN, 12));
        combo.setBorder(BorderFactory.createLineBorder(BORDER));
    }

    private void styleButton(JButton btn) {
        btn.setBackground(Color.decode("#1F2D35"));
        btn.setForeground(TEXT);
        btn.setFont(new Font("Dialog", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
    }

    private void styleToggleButton(JToggleButton btn) {
        btn.setBackground(Color.decode("#1F2D35"));
        btn.setForeground(TEXT);
        btn.setFont(new Font("Dialog", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        btn.addChangeListener(e -> {
            if (btn.isSelected()) {
                btn.setBackground(new Color(242, 184, 75, 60));
                btn.setForeground(AMBER);
            } else {
                btn.setBackground(Color.decode("#1F2D35"));
                btn.setForeground(TEXT);
            }
        });
    }
}
