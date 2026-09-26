package electricity.ui;

import electricity.model.Component;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.Arrays;

/**
 * Real-time line-graph panel that plots voltage AND current for a single
 * monitored component over a rolling time window.
 */
public class GraphPanel extends JPanel {

    private static final int BUFFER_SIZE = 300;

    // Rolling data buffers
    private final double[] voltageData = new double[BUFFER_SIZE];
    private final double[] currentData = new double[BUFFER_SIZE];
    private int writePos = 0;
    private int sampleCount = 0;

    // Auto-scale state
    private double maxVoltage = 1.0;
    private double maxCurrent = 0.01;

    // Component being monitored (may be null)
    private Component monitored = null;
    private String componentLabel = "None";

    // Color palette
    private static final Color COLOR_VOLTAGE = Color.decode("#35C6D8");
    private static final Color COLOR_CURRENT = Color.decode("#E86A5B");
    private static final Color BG = Color.decode("#101820");
    private static final Color PLOT = Color.decode("#172029");
    private static final Color GRID = Color.decode("#344955");
    private static final Color TEXT = Color.decode("#E8F0F2");
    private static final Color MUTED = Color.decode("#9BAEB5");

    public GraphPanel() {
        setBackground(BG);
        setPreferredSize(new Dimension(520, 300));
    }

    // -------------------------------------------------------------------------
    // Data
    // -------------------------------------------------------------------------

    public void setMonitoredComponent(Component comp, String label) {
        this.monitored   = comp;
        this.componentLabel = label;
        // Reset buffers on component change
        Arrays.fill(voltageData, 0.0);
        Arrays.fill(currentData, 0.0);
        writePos     = 0;
        sampleCount  = 0;
        maxVoltage   = 1.0;
        maxCurrent   = 0.01;
    }

    /** Push one sample from the currently monitored component. Call each tick. */
    public void tick() {
        if (monitored == null) return;

        double v = monitored.getVoltageDrop();
        double i = monitored.getCurrent();

        voltageData[writePos] = v;
        currentData[writePos] = i;
        writePos = (writePos + 1) % BUFFER_SIZE;
        sampleCount = Math.min(sampleCount + 1, BUFFER_SIZE);

        // Smooth auto-scale (drift toward actual max)
        maxVoltage = Math.max(maxVoltage * 0.995, Math.abs(v) * 1.1 + 0.01);
        maxCurrent = Math.max(maxCurrent * 0.995, Math.abs(i) * 1.1 + 0.001);
    }

    public Component getMonitoredComponent() { return monitored; }

    // -------------------------------------------------------------------------
    // Painting
    // -------------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        int leftMargin  = 60;
        int rightMargin = 60;
        int topMargin   = 30;
        int botMargin   = 40;
        int pw = w - leftMargin - rightMargin;
        int ph = h - topMargin  - botMargin;

        // Background
        g2d.setColor(BG);
        g2d.fillRect(0, 0, w, h);

        // Title bar
        drawTitleBar(g2d, w, topMargin);

        // Plot area
        g2d.setColor(PLOT);
        g2d.fillRect(leftMargin, topMargin, pw, ph);

        // Grid
        drawGrid(g2d, leftMargin, topMargin, pw, ph);

        // Axes
        drawAxes(g2d, leftMargin, topMargin, pw, ph);

        // Waveforms
        if (sampleCount > 1) {
            drawSeries(g2d, voltageData, maxVoltage,  COLOR_VOLTAGE, leftMargin, topMargin, pw, ph, true);
            drawSeries(g2d, currentData, maxCurrent,  COLOR_CURRENT, leftMargin, topMargin, pw, ph, false);
        } else {
            // No data yet
            g2d.setFont(new Font("Dialog", Font.PLAIN, 12));
            g2d.setColor(MUTED);
            String msg = (monitored == null) ? "No component selected" : "Waiting for signal…";
            int mw = g2d.getFontMetrics().stringWidth(msg);
            g2d.drawString(msg, leftMargin + (pw - mw) / 2, topMargin + ph / 2);
        }

        // Legend
        drawLegend(g2d, leftMargin, topMargin, pw, ph);

        g2d.dispose();
    }

    private void drawTitleBar(Graphics2D g2d, int w, int topMargin) {
        g2d.setFont(new Font("Dialog", Font.BOLD, 12));
        g2d.setColor(TEXT);
        String title = "Monitoring: " + componentLabel;
        g2d.drawString(title, 14, topMargin - 8);
    }

    private void drawGrid(Graphics2D g2d, int lx, int ty, int pw, int ph) {
        g2d.setStroke(new BasicStroke(0.5f));
        g2d.setColor(new Color(GRID.getRed(), GRID.getGreen(), GRID.getBlue(), 150));
        int hDivs = 6, vDivs = 5;
        for (int i = 0; i <= vDivs; i++) {
            int y = ty + i * ph / vDivs;
            g2d.drawLine(lx, y, lx + pw, y);
        }
        for (int i = 0; i <= hDivs; i++) {
            int x = lx + i * pw / hDivs;
            g2d.drawLine(x, ty, x, ty + ph);
        }
    }

    private void drawAxes(Graphics2D g2d, int lx, int ty, int pw, int ph) {
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 9));
        FontMetrics fm = g2d.getFontMetrics();

        // Left axis – Voltage
        g2d.setColor(COLOR_VOLTAGE);
        int vDivs = 5;
        for (int i = 0; i <= vDivs; i++) {
            double v = maxVoltage * (1.0 - 2.0 * i / vDivs);
            String label = String.format("%.2fV", v);
            int y = ty + i * ph / vDivs;
            g2d.drawString(label, lx - fm.stringWidth(label) - 3, y + fm.getAscent() / 2);
        }

        // Right axis – Current
        g2d.setColor(COLOR_CURRENT);
        for (int i = 0; i <= vDivs; i++) {
            double ci = maxCurrent * (1.0 - 2.0 * i / vDivs);
            String label;
            if (Math.abs(ci) < 0.001) label = String.format("%.2fmA", ci * 1000);
            else label = String.format("%.3fA", ci);
            int y = ty + i * ph / vDivs;
            g2d.drawString(label, lx + pw + 4, y + fm.getAscent() / 2);
        }

        // Bottom axis – Time label
        g2d.setColor(MUTED);
        g2d.setFont(new Font("Dialog", Font.PLAIN, 10));
        String timeLabel = "← " + BUFFER_SIZE + " ticks →";
        int tw = g2d.getFontMetrics().stringWidth(timeLabel);
        g2d.drawString(timeLabel, lx + (pw - tw) / 2, ty + ph + 20);
    }

    private void drawSeries(Graphics2D g2d, double[] data, double scale,
                            Color color, int lx, int ty, int pw, int ph,
                            boolean normalPositive) {
        // normalPositive=true: range [0, scale] maps to bottom→top
        // We use [-scale, +scale] mapping for both, placing 0 at center if needed
        // For magnitude (current, voltage drop) we use [0, scale] at full height

        Color glow = new Color(color.getRed(), color.getGreen(), color.getBlue(), 30);

        int n = Math.min(sampleCount, pw);
        GeneralPath path = new GeneralPath();
        boolean first = true;

        for (int px = 0; px < n; px++) {
            int bufIdx = (writePos - n + px + BUFFER_SIZE) % BUFFER_SIZE;
            double val = data[bufIdx];

            double norm;
            if (normalPositive) {
                // Voltage: show symmetrically (can be negative for AC)
                norm = (scale > 0) ? (val / scale) : 0;
                norm = Math.max(-1.0, Math.min(1.0, norm));
            } else {
                // Current: always positive magnitude
                norm = (scale > 0) ? (val / scale) : 0;
                norm = Math.max(0, Math.min(1.0, norm));
                norm = 1.0 - norm; // invert so high = top
                norm = norm * 2.0 - 1.0; // map [0,1] → [1,-1]
            }

            float x = lx + (float) px * pw / n;
            float y;
            if (normalPositive) {
                y = ty + (float) ((1.0 - norm) / 2.0 * ph);
            } else {
                y = ty + (float) ((norm + 1.0) / 2.0 * ph);
            }

            if (first) { path.moveTo(x, y); first = false; }
            else        { path.lineTo(x, y); }
        }

        // Glow pass
        g2d.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(glow);
        g2d.draw(path);

        // Sharp line
        g2d.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(color);
        g2d.draw(path);
    }

    private void drawLegend(Graphics2D g2d, int lx, int ty, int pw, int ph) {
        int ly = ty + ph + 26;
        g2d.setFont(new Font("Dialog", Font.PLAIN, 11));

        // Voltage
        g2d.setColor(COLOR_VOLTAGE);
        g2d.fillRect(lx, ly, 20, 4);
        g2d.setColor(TEXT);
        g2d.drawString("Voltage", lx + 24, ly + 10);

        // Current
        g2d.setColor(COLOR_CURRENT);
        g2d.fillRect(lx + 90, ly, 20, 4);
        g2d.setColor(new Color(180, 200, 230));
        g2d.drawString("Current", lx + 114, ly + 10);
    }
}
