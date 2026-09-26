package electricity.ui;

import electricity.model.*;
import electricity.model.Component;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

/**
 * Dark-themed oscilloscope panel that displays real-time voltage waveforms
 * for all ACMotor sources in the circuit. Maintains per-channel rolling buffers.
 */
public class OscilloscopePanel extends JPanel {

    // Buffer length in samples (each sample = one simulation tick)
    private static final int BUFFER_SIZE = 512;

    // Maximum number of simultaneously displayed channels
    private static final int MAX_CHANNELS = 4;

    // Oscilloscope channel colors (glowing style)
    private static final Color[] CHANNEL_COLORS = {
        Color.decode("#35C6D8"),
        Color.decode("#63D6A2"),
        Color.decode("#E86A5B"),
        Color.decode("#6DA8E8"),
    };

    // Per-channel rolling buffers
    private final double[][] voltageBuffer = new double[MAX_CHANNELS][BUFFER_SIZE];
    private final double[][] currentBuffer = new double[MAX_CHANNELS][BUFFER_SIZE];
    private int writePos = 0;

    // Channel metadata
    private final String[] channelLabels = new String[MAX_CHANNELS];
    private int activeChannels = 0;

    // Display state
    private boolean frozen = false;
    private double voltageScale = 10.0;  // ±voltageScale V shown
    private double timeScale  = 1.0;     // samples per pixel (future)

    public OscilloscopePanel() {
        setBackground(Color.decode("#101820"));
        setPreferredSize(new Dimension(700, 360));
        for (int ch = 0; ch < MAX_CHANNELS; ch++) {
            channelLabels[ch] = "CH" + (ch + 1);
        }
    }

    // -------------------------------------------------------------------------
    // Data ingestion
    // -------------------------------------------------------------------------

    /**
     * Called each simulation tick. Pulls voltage & current from all ACMotors
     * in the grid and stores them in rolling buffers.
     */
    public void pushData(Component[][] grid) {
        if (frozen) return;

        int ch = 0;
        int height = grid.length;
        int width  = grid[0].length;

        for (int r = 0; r < height && ch < MAX_CHANNELS; r++) {
            for (int c = 0; c < width && ch < MAX_CHANNELS; c++) {
                Component comp = grid[r][c];
                if (comp instanceof ACMotor) {
                    ACMotor ac = (ACMotor) comp;
                    voltageBuffer[ch][writePos] = ac.getCurrentVoltage();
                    currentBuffer[ch][writePos] = ac.getCurrent();
                    channelLabels[ch] = String.format("AC %.1fV@%.1fHz (%d,%d)",
                            ac.getPeakVoltage(), ac.getFrequency(), c, r);
                    ch++;
                }
            }
        }

        // Also sample batteries and other voltage sources
        for (int r = 0; r < height && ch < MAX_CHANNELS; r++) {
            for (int c = 0; c < width && ch < MAX_CHANNELS; c++) {
                Component comp = grid[r][c];
                if (comp instanceof Battery && comp.isActive()) {
                    Battery b = (Battery) comp;
                    voltageBuffer[ch][writePos] = b.getVoltageDrop();
                    currentBuffer[ch][writePos] = b.getCurrent();
                    channelLabels[ch] = String.format("Bat %.1fV (%d,%d)", b.getVoltage(), c, r);
                    ch++;
                }
            }
        }

        activeChannels = ch;
        writePos = (writePos + 1) % BUFFER_SIZE;
    }

    // -------------------------------------------------------------------------
    // Controls
    // -------------------------------------------------------------------------

    public boolean isFrozen() { return frozen; }
    public void setFrozen(boolean frozen) { this.frozen = frozen; }

    public double getVoltageScale() { return voltageScale; }
    public void setVoltageScale(double s) { this.voltageScale = Math.max(0.1, s); }

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

        int leftMargin  = 55;
        int rightMargin = 15;
        int topMargin   = 15;
        int botMargin   = 35;
        int plotW = w - leftMargin - rightMargin;
        int plotH = h - topMargin - botMargin;

        // --- Background & scanlines ---
        g2d.setColor(Color.decode("#101820"));
        g2d.fillRect(0, 0, w, h);

        // Subtle scanline effect
        g2d.setColor(new Color(255, 255, 255, 5));
        for (int y = topMargin; y < topMargin + plotH; y += 4) {
            g2d.drawLine(leftMargin, y, leftMargin + plotW, y);
        }

        // --- Plot area border (phosphor glow) ---
        g2d.setColor(Color.decode("#172029"));
        g2d.fillRect(leftMargin, topMargin, plotW, plotH);
        g2d.setColor(new Color(99, 214, 162, 80));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRect(leftMargin - 1, topMargin - 1, plotW + 2, plotH + 2);

        // --- Grid lines ---
        drawGrid(g2d, leftMargin, topMargin, plotW, plotH);

        // --- Axis labels ---
        drawAxisLabels(g2d, leftMargin, topMargin, plotW, plotH);

        // --- Waveforms ---
        if (activeChannels == 0) {
            // No signal – show placeholder message
            g2d.setFont(new Font("Monospaced", Font.PLAIN, 13));
            g2d.setColor(new Color(99, 214, 162, 150));
            String msg = "NO SIGNAL  —  Add an AC Source to the circuit";
            int mw = g2d.getFontMetrics().stringWidth(msg);
            g2d.drawString(msg, leftMargin + (plotW - mw) / 2, topMargin + plotH / 2);
        } else {
            for (int ch = 0; ch < activeChannels && ch < MAX_CHANNELS; ch++) {
                drawWaveform(g2d, ch, leftMargin, topMargin, plotW, plotH);
            }
        }

        // --- Legend ---
        drawLegend(g2d, leftMargin, topMargin + plotH + 6, plotW);

        // --- Frozen indicator ---
        if (frozen) {
            g2d.setFont(new Font("Monospaced", Font.BOLD, 11));
            g2d.setColor(Color.decode("#F2B84B"));
            g2d.drawString("❚❚ FROZEN", leftMargin + plotW - 72, topMargin + 18);
        }

        g2d.dispose();
    }

    private void drawGrid(Graphics2D g2d, int lx, int ty, int pw, int ph) {
        int hDivs = 8;  // horizontal divisions
        int vDivs = 6;  // vertical divisions

        g2d.setStroke(new BasicStroke(0.5f));

        // Major grid lines
        g2d.setColor(new Color(52, 73, 85, 170));
        for (int i = 0; i <= vDivs; i++) {
            int y = ty + i * ph / vDivs;
            g2d.drawLine(lx, y, lx + pw, y);
        }
        for (int i = 0; i <= hDivs; i++) {
            int x = lx + i * pw / hDivs;
            g2d.drawLine(x, ty, x, ty + ph);
        }

        // Center axis (zero-volt line) – slightly brighter
        g2d.setColor(new Color(99, 214, 162, 130));
        g2d.setStroke(new BasicStroke(1f));
        g2d.drawLine(lx, ty + ph / 2, lx + pw, ty + ph / 2);
    }

    private void drawAxisLabels(Graphics2D g2d, int lx, int ty, int pw, int ph) {
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g2d.setColor(Color.decode("#9BAEB5"));
        FontMetrics fm = g2d.getFontMetrics();

        int vDivs = 6;
        for (int i = 0; i <= vDivs; i++) {
            double v = voltageScale - (2.0 * voltageScale * i / vDivs);
            String label = String.format("%.1f", v);
            int y = ty + i * ph / vDivs;
            g2d.drawString(label, lx - fm.stringWidth(label) - 4, y + fm.getAscent() / 2);
        }

        // Y-axis title
        Graphics2D rotG = (Graphics2D) g2d.create();
        rotG.setFont(new Font("Monospaced", Font.BOLD, 10));
        rotG.setColor(Color.decode("#63D6A2"));
        rotG.rotate(-Math.PI / 2, lx - 40, ty + ph / 2);
        rotG.drawString("Voltage (V)", lx - 40, ty + ph / 2);
        rotG.dispose();
    }

    private void drawWaveform(Graphics2D g2d, int ch, int lx, int ty, int pw, int ph) {
        Color base   = CHANNEL_COLORS[ch % CHANNEL_COLORS.length];
        Color glow   = new Color(base.getRed(), base.getGreen(), base.getBlue(), 35);
        Color bright = new Color(base.getRed(), base.getGreen(), base.getBlue(), 200);

        // Build the path
        GeneralPath path = new GeneralPath();
        int n = Math.min(BUFFER_SIZE, pw);
        boolean first = true;

        for (int px = 0; px < n; px++) {
            // Map pixel index to buffer index (most recent at right)
            int bufIdx = (writePos - n + px + BUFFER_SIZE) % BUFFER_SIZE;
            double v = voltageBuffer[ch][bufIdx];
            double norm = v / voltageScale; // -1 to +1
            norm = Math.max(-1.0, Math.min(1.0, norm));

            float x = lx + (float) px * pw / n;
            float y = ty + (float) ((1.0 - norm) / 2.0 * ph);

            if (first) { path.moveTo(x, y); first = false; }
            else        { path.lineTo(x, y); }
        }

        // Draw glow (thick, transparent)
        g2d.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(glow);
        g2d.draw(path);

        // Draw sharp line
        g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(bright);
        g2d.draw(path);
    }

    private void drawLegend(Graphics2D g2d, int lx, int y, int pw) {
        g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));
        int x = lx;
        for (int ch = 0; ch < activeChannels && ch < MAX_CHANNELS; ch++) {
            Color c = CHANNEL_COLORS[ch % CHANNEL_COLORS.length];
            g2d.setColor(c);
            g2d.fillRect(x, y + 3, 18, 4);
            g2d.setColor(Color.decode("#E8F0F2"));
            g2d.drawString(channelLabels[ch], x + 22, y + 12);
            x += g2d.getFontMetrics().stringWidth(channelLabels[ch]) + 36;
        }
    }
}
