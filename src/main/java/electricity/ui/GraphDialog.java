package electricity.ui;

import electricity.model.Component;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/**
 * Non-modal dialog that hosts a GraphPanel for real-time voltage/current plots.
 * Can be opened via Alt+Click on any active component, or via the "Show Graph"
 * button in the PropertyEditorDialog.
 */
public class GraphDialog extends JDialog {

    private static final Color SURFACE = Color.decode("#172029");
    private static final Color SURFACE_DARK = Color.decode("#101820");
    private static final Color BORDER = Color.decode("#344955");
    private static final Color TEXT_MUTED = Color.decode("#9BAEB5");

    private final GraphPanel graphPanel;

    public GraphDialog(JFrame owner) {
        super(owner, "Real-Time Graph", false);
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        graphPanel = new GraphPanel();

        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.setBackground(SURFACE_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        content.add(graphPanel, BorderLayout.CENTER);

        JPanel statusBar = buildStatusBar();
        content.add(statusBar, BorderLayout.SOUTH);

        setContentPane(content);
        pack();
        setMinimumSize(new Dimension(400, 240));
        setLocationRelativeTo(owner);
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
        bar.setBackground(SURFACE);
        bar.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));

        JLabel hint = new JLabel("Alt+Click a component to switch monitoring target.");
        hint.setForeground(TEXT_MUTED);
        hint.setFont(new Font("Dialog", Font.PLAIN, 11));
        bar.add(hint);

        return bar;
    }

    // ---- Public API ---------------------------------------------------------

    public GraphPanel getGraphPanel() {
        return graphPanel;
    }

    /**
     * Update the dialog title and monitored component.
     */
    public void setComponent(Component comp, String label) {
        graphPanel.setMonitoredComponent(comp, label);
        setTitle("Real-Time Graph — " + label);
    }
}
