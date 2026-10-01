package electricity.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

import electricity.model.ACMotor;
import electricity.model.Ammeter;
import electricity.model.Battery;
import electricity.model.Bulb;
import electricity.model.Capacitor;
import electricity.model.Component;
import electricity.model.Diode;
import electricity.model.Fuse;
import electricity.model.Inductor;
import electricity.model.LED;
import electricity.model.Resistor;
import electricity.model.Voltmeter;
import electricity.model.Wire;
import electricity.solver.GridDimensions;

public class Renderer {
    private static final int TILE_SIZE = 64;
    private static final Color CANVAS_TOP = Color.decode("#101820");
    private static final Color CANVAS_BOTTOM = Color.decode("#0D1217");
    private static final Color GRID_LINE = Color.decode("#253640");
    private static final Color GRID_MAJOR = Color.decode("#344955");
    private static final Color TEXT_PRIMARY = Color.decode("#E8F0F2");
    private static final Color TEXT_MUTED = Color.decode("#9BAEB5");
    private static final Color ACCENT_MINT = Color.decode("#63D6A2");
    private static final Color ACCENT_CYAN = Color.decode("#35C6D8");
    private static final Color ACCENT_CORAL = Color.decode("#E86A5B");
    private static final Color WARNING_AMBER = Color.decode("#F2B84B");
    private static final Color WARNING_SURFACE = Color.decode("#3A2D18");
    private final InteractionState state;

    public Renderer(InteractionState state) {
        this.state = state;
    }

    public void render(Graphics graphics, Component[][] grid, CanvasPanel.Tool activeTool,
                       int panelWidth, int panelHeight) {
        Graphics2D g2d = (Graphics2D) graphics;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        drawCanvasBackground(g2d, panelWidth, panelHeight);

        AffineTransform oldTx = g2d.getTransform();
        g2d.translate(state.panX, state.panY);
        g2d.scale(state.zoom, state.zoom);

        drawGrid(g2d);

        for (int r = 0; r < GridDimensions.HEIGHT; r++) {
            for (int c = 0; c < GridDimensions.WIDTH; c++) {
                Component comp = grid[r][c];
                if (comp != null) comp.draw(g2d, TILE_SIZE);
            }
        }

        drawSolverWarningMarks(g2d, grid);

        for (int r = 0; r < GridDimensions.HEIGHT; r++) {
            for (int c = 0; c < GridDimensions.WIDTH; c++) {
                Component comp = grid[r][c];
                if (comp != null) {
                    String label = getComponentLabel(comp);
                    if (label != null) drawHoverLabel(g2d, label, c * TILE_SIZE, r * TILE_SIZE);
                }
            }
        }

        drawDragOverlays(g2d, activeTool);
        drawHoverHighlight(g2d, activeTool);

        g2d.setTransform(oldTx);
        drawCurrentLegend(g2d, grid, panelWidth, panelHeight);
        drawZoomIndicator(g2d, state.zoom);
        drawWarningBanner(g2d, grid, panelWidth);
    }

    private void drawCanvasBackground(Graphics2D g2d, int width, int height) {
        g2d.setPaint(new java.awt.GradientPaint(0, 0, CANVAS_TOP, 0, height, CANVAS_BOTTOM));
        g2d.fillRect(0, 0, width, height);
        g2d.setColor(new Color(99, 214, 162, 10));
        for (int y = 52; y < height; y += 52) {
            g2d.drawLine(0, y, width, y);
        }
    }

    private void drawSolverWarningMarks(Graphics2D g2d, Component[][] grid) {
        for (int r = 0; r < GridDimensions.HEIGHT; r++) {
            for (int c = 0; c < GridDimensions.WIDTH; c++) {
                Component comp = grid[r][c];
                if (comp == null || !comp.hasSolverError()) continue;
                int x = c * TILE_SIZE;
                int y = r * TILE_SIZE;
                g2d.setColor(new Color(WARNING_AMBER.getRed(), WARNING_AMBER.getGreen(), WARNING_AMBER.getBlue(), 220));
                g2d.setStroke(new BasicStroke(3f));
                g2d.drawRect(x + 3, y + 3, TILE_SIZE - 6, TILE_SIZE - 6);
                g2d.setColor(WARNING_AMBER);
                g2d.fillOval(x + TILE_SIZE - 18, y + 4, 14, 14);
                g2d.setColor(new Color(35, 25, 10));
                g2d.setFont(new Font("Dialog", Font.BOLD, 11));
                g2d.drawString("!", x + TILE_SIZE - 14, y + 15);
            }
        }
    }

    private void drawWarningBanner(Graphics2D g2d, Component[][] grid, int panelWidth) {
        if (!hasSolverWarning(grid)) return;

        int width = 246;
        int x = panelWidth - width - 14;
        g2d.setColor(WARNING_SURFACE);
        g2d.fillRoundRect(x, 12, width, 28, 8, 8);
        g2d.setColor(WARNING_AMBER);
        g2d.drawRoundRect(x, 12, width, 28, 8, 8);
        g2d.setColor(TEXT_PRIMARY);
        g2d.setFont(new Font("Dialog", Font.BOLD, 11));
        g2d.drawString("!  Floating or singular node", x + 11, 30);
    }

    private boolean hasSolverWarning(Component[][] grid) {
        for (int r = 0; r < GridDimensions.HEIGHT; r++) {
            for (int c = 0; c < GridDimensions.WIDTH; c++) {
                if (grid[r][c] != null && grid[r][c].hasSolverError()) return true;
            }
        }
        return false;
    }

    private void drawDragOverlays(Graphics2D g2d, CanvasPanel.Tool activeTool) {
        if (!state.isDragging) return;

        int minCol = Math.min(state.dragStartCol, state.dragEndCol);
        int maxCol = Math.max(state.dragStartCol, state.dragEndCol);
        int minRow = Math.min(state.dragStartRow, state.dragEndRow);
        int maxRow = Math.max(state.dragStartRow, state.dragEndRow);

        if (activeTool == CanvasPanel.Tool.ERASER) {
            int x = minCol * TILE_SIZE, y = minRow * TILE_SIZE;
            int w = (maxCol - minCol + 1) * TILE_SIZE, h = (maxRow - minRow + 1) * TILE_SIZE;
                g2d.setColor(new Color(232, 106, 91, 35));
            g2d.fillRect(x, y, w, h);
            g2d.setColor(new Color(232, 106, 91, 220));
            g2d.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.drawRect(x + 1, y + 1, w - 2, h - 2);
        } else if (activeTool == CanvasPanel.Tool.WIRE) {
            if (state.dragStartCol == state.dragEndCol || state.dragStartRow == state.dragEndRow) {
                int x, y, w, h;
                if (state.dragStartCol == state.dragEndCol) {
                    x = state.dragStartCol * TILE_SIZE; y = minRow * TILE_SIZE;
                    w = TILE_SIZE; h = (maxRow - minRow + 1) * TILE_SIZE;
                } else {
                    x = minCol * TILE_SIZE; y = state.dragStartRow * TILE_SIZE;
                    w = (maxCol - minCol + 1) * TILE_SIZE; h = TILE_SIZE;
                }
                g2d.setColor(new Color(99, 214, 162, 42));
                g2d.fillRect(x, y, w, h);
                g2d.setColor(new Color(99, 214, 162, 220));
                g2d.setStroke(new BasicStroke(2f));
                g2d.drawRect(x + 1, y + 1, w - 2, h - 2);
            } else {
                for (int c = minCol; c <= maxCol; c++) {
                    drawPreviewCell(g2d, c, minRow);
                    drawPreviewCell(g2d, c, maxRow);
                }
                for (int r = minRow; r <= maxRow; r++) {
                    drawPreviewCell(g2d, minCol, r);
                    drawPreviewCell(g2d, maxCol, r);
                }
            }
        }
    }

    private void drawHoverHighlight(Graphics2D g2d, CanvasPanel.Tool activeTool) {
        if (state.isDragging || state.hoverCol < 0 || state.hoverRow < 0) return;
        int hoverCol = state.hoverCol;
        int hoverRow = state.hoverRow;
        if (activeTool == CanvasPanel.Tool.ERASER) {
            g2d.setColor(new Color(232, 106, 91, 32));
            g2d.fillRect(hoverCol * TILE_SIZE, hoverRow * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            g2d.setColor(new Color(232, 106, 91, 210));
        } else {
            g2d.setColor(new Color(99, 214, 162, 28));
            g2d.fillRect(hoverCol * TILE_SIZE, hoverRow * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            g2d.setColor(new Color(99, 214, 162, 210));
        }
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRect(hoverCol * TILE_SIZE + 1, hoverRow * TILE_SIZE + 1, TILE_SIZE - 2, TILE_SIZE - 2);
    }

    private void drawPreviewCell(Graphics2D g2d, int col, int row) {
        int x = col * TILE_SIZE, y = row * TILE_SIZE;
        g2d.setColor(new Color(99, 214, 162, 42));
        g2d.fillRect(x, y, TILE_SIZE, TILE_SIZE);
        g2d.setColor(new Color(99, 214, 162, 190));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRect(x + 1, y + 1, TILE_SIZE - 2, TILE_SIZE - 2);
    }

    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(GRID_LINE);
        for (int col = 0; col <= GridDimensions.WIDTH; col++) {
            g2d.drawLine(col * TILE_SIZE, 0, col * TILE_SIZE, GridDimensions.HEIGHT * TILE_SIZE);
        }
        for (int row = 0; row <= GridDimensions.HEIGHT; row++) {
            g2d.drawLine(0, row * TILE_SIZE, GridDimensions.WIDTH * TILE_SIZE, row * TILE_SIZE);
        }
        g2d.setColor(GRID_MAJOR);
        for (int col = 0; col <= GridDimensions.WIDTH; col += 5) {
            g2d.drawLine(col * TILE_SIZE, 0, col * TILE_SIZE, GridDimensions.HEIGHT * TILE_SIZE);
        }
        for (int row = 0; row <= GridDimensions.HEIGHT; row += 5) {
            g2d.drawLine(0, row * TILE_SIZE, GridDimensions.WIDTH * TILE_SIZE, row * TILE_SIZE);
        }
    }

    private void drawCurrentLegend(Graphics2D g2d, Component[][] grid, int panelW, int panelH) {
        boolean hasActiveWire = false;
        outer:
        for (int r = 0; r < GridDimensions.HEIGHT; r++) {
            for (int c = 0; c < GridDimensions.WIDTH; c++) {
                if (grid[r][c] instanceof Wire && grid[r][c].isActive()) {
                    hasActiveWire = true;
                    break outer;
                }
            }
        }
        if (!hasActiveWire) return;

        int lx = panelW - 180;
        int ly = panelH - 120;
        int lw = 160;
        int lh = 100;

        g2d.setColor(new Color(23, 32, 41, 238));
        g2d.fillRoundRect(lx, ly, lw, lh, 10, 10);
        g2d.setColor(new Color(62, 82, 92));
        g2d.setStroke(new BasicStroke(1f));
        g2d.drawRoundRect(lx, ly, lw, lh, 10, 10);

        g2d.setFont(new Font("Dialog", Font.BOLD, 10));
        g2d.setColor(TEXT_PRIMARY);
        g2d.drawString("Wire Current", lx + 10, ly + 16);

        int barX = lx + 10;
        int barY = ly + 25;
        int barW = lw - 20;
        int barH = 14;
        int steps = barW;
        for (int i = 0; i < steps; i++) {
            double t = (double) i / steps;
            double amps = t * 2.0;
            g2d.setColor(Wire.getCurrentColor(amps));
            g2d.fillRect(barX + i, barY, 1, barH);
        }
        g2d.setColor(new Color(80, 85, 115));
        g2d.setStroke(new BasicStroke(1f));
        g2d.drawRect(barX, barY, barW, barH);

        g2d.setFont(new Font("Monospaced", Font.PLAIN, 9));
        g2d.setColor(TEXT_MUTED);
        g2d.drawString("0 A", barX, barY + barH + 12);
        String maxLabel = "≥2 A";
        int mlw = g2d.getFontMetrics().stringWidth(maxLabel);
        g2d.drawString(maxLabel, barX + barW - mlw, barY + barH + 12);

        g2d.setFont(new Font("Dialog", Font.PLAIN, 9));
        g2d.setColor(Wire.getCurrentColor(0.1));
        g2d.fillRect(barX, barY + barH + 18, 8, 8);
        g2d.setColor(TEXT_MUTED);
        g2d.drawString("Low", barX + 12, barY + barH + 26);

        g2d.setColor(Wire.getCurrentColor(0.7));
        g2d.fillRect(barX + 40, barY + barH + 18, 8, 8);
        g2d.setColor(TEXT_MUTED);
        g2d.drawString("Med", barX + 52, barY + barH + 26);

        g2d.setColor(Wire.getCurrentColor(1.8));
        g2d.fillRect(barX + 85, barY + barH + 18, 8, 8);
        g2d.setColor(TEXT_MUTED);
        g2d.drawString("High", barX + 97, barY + barH + 26);
    }

    private void drawZoomIndicator(Graphics2D g2d, double zoom) {
        g2d.setFont(new Font("Monospaced", Font.BOLD, 11));
        String zoomStr = String.format("%.0f%%", zoom * 100);
        g2d.setColor(new Color(23, 32, 41, 238));
        g2d.fillRoundRect(12, 12, 82, 28, 8, 8);
        g2d.setColor(ACCENT_CYAN);
        g2d.drawString("ZOOM " + zoomStr, 21, 30);
    }

    private void drawHoverLabel(Graphics2D g2d, String text, int cellX, int cellY) {
        g2d.setFont(new Font("Dialog", Font.BOLD, 11));
        FontMetrics fm = g2d.getFontMetrics();
        int w = fm.stringWidth(text) + 14;
        int h = fm.getHeight() + 6;
        int x = cellX + TILE_SIZE / 2 - w / 2;
        int y = cellY - h - 6;
        if (y < 2) y = cellY + TILE_SIZE + 6;

        g2d.setColor(new Color(14, 14, 22, 225));
        g2d.fillRoundRect(x, y, w, h, 8, 8);
        g2d.setColor(new Color(70, 80, 110));
        g2d.setStroke(new BasicStroke(1f));
        g2d.drawRoundRect(x, y, w, h, 8, 8);
        g2d.setColor(new Color(240, 242, 255));
        g2d.drawString(text, x + 7, y + fm.getAscent() + 2);
    }

    private String getComponentLabel(Component comp) {
        if (comp instanceof Battery) {
            Battery b = (Battery) comp;
            if (b.isActive()) return String.format("%.1fV (r:%.2fΩ) %.2fA", b.getVoltage(), b.getInternalResistance(), b.getCurrent());
            return String.format("%.1fV (r:%.2fΩ)", b.getVoltage(), b.getInternalResistance());
        } else if (comp instanceof Resistor) {
            Resistor r = (Resistor) comp;
            if (r.isActive()) return String.format("%.0fΩ  %.2fA  %.2fV", r.getResistance(), r.getCurrent(), r.getVoltageDrop());
            return String.format("%.0fΩ", r.getResistance());
        } else if (comp instanceof Bulb) {
            Bulb b = (Bulb) comp;
            if (b.isActive()) return String.format("%.0fΩ  %.2fA  %.2fV", b.getResistance(), b.getCurrent(), b.getVoltageDrop());
            return String.format("%.0fΩ", b.getResistance());
        } else if (comp instanceof LED) {
            LED led = (LED) comp;
            return led.isActive() && led.isForward() ? String.format("LED %.2fA", led.getCurrent()) : "LED (OFF)";
        } else if (comp instanceof Diode) {
            Diode d = (Diode) comp;
            return d.isActive() && d.isForward() ? String.format("Diode %.2fA", d.getCurrent()) : "Diode (OFF)";
        } else if (comp instanceof Fuse) {
            Fuse f = (Fuse) comp;
            return f.isBlown() ? "BLOWN" : String.format("Fuse %.1fA / %.2fA", f.getLimit(), f.getCurrent());
        } else if (comp instanceof Capacitor) {
            Capacitor cap = (Capacitor) comp;
            return String.format("%.0fµF  %.1fV", cap.getCapacitance() * 1e6, cap.getChargeVoltage());
        } else if (comp instanceof Inductor) {
            Inductor ind = (Inductor) comp;
            return String.format("%.1fH  %.2fA", ind.getInductance(), ind.getCurrent());
        } else if (comp instanceof ACMotor) {
            ACMotor ac = (ACMotor) comp;
            return String.format("AC %.1fV~%.1fHz  inst:%.1fV", ac.getPeakVoltage(), ac.getFrequency(), ac.getCurrentVoltage());
        } else if (comp instanceof Ammeter) {
            Ammeter a = (Ammeter) comp;
            return a.isActive() ? String.format("I = %.3fA", a.getCurrent()) : "Ammeter";
        } else if (comp instanceof Voltmeter) {
            Voltmeter v = (Voltmeter) comp;
            return v.isActive() ? String.format("V = %.2fV", v.getVoltageDrop()) : "Voltmeter";
        } else if (comp instanceof Wire && comp.isActive()) {
            return String.format("%.3fA", comp.getCurrent());
        }
        return null;
    }
}
