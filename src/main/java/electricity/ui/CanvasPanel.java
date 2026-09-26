package electricity.ui;

import electricity.command.EditCommand;
import electricity.io.CircuitIO;
import electricity.model.*;
import electricity.model.Component;
import electricity.solver.CircuitSolver;
import electricity.solver.GridDimensions;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import javax.swing.*;

public class CanvasPanel extends JPanel {
    public enum Tool {
        WIRE, BATTERY, BULB, RESISTOR, SWITCH, AMMETER, VOLTMETER,
        DIODE, LED, FUSE, TEXT_LABEL, CAPACITOR, INDUCTOR, AC_MOTOR,
        ERASER
    }

    private static final int TILE_SIZE = 64;
    // 2D grid storing placed components
    private Component[][] grid = new Component[GridDimensions.HEIGHT][GridDimensions.WIDTH];

    private final InteractionState interactionState = new InteractionState();
    private final Renderer renderer;

    private Tool activeTool = Tool.WIRE;

    // Direction memory for each placement tool type
    private Map<Tool, Direction> toolDirections = new HashMap<>();

    // Undo/Redo stacks
    private Stack<EditCommand> undoStack = new Stack<>();
    private Stack<EditCommand> redoStack = new Stack<>();

    // Callbacks to CircuitSim for dialogs
    private Runnable openGraphForHoveredComp;
    private GraphDialog graphDialog;
    private OscilloscopeDialog oscilloscopeDialog;

    // Static frame counter for animation
    private static int frameCount = 0;

    public CanvasPanel() {
        // Responsive viewport
        setPreferredSize(new Dimension(1100, 700));
        setBackground(new Color(16, 21, 27));
        setBorder(BorderFactory.createLineBorder(new Color(42, 57, 67), 1));

        // Initialize default directions for all placement tools
        for (Tool t : Tool.values()) {
            toolDirections.put(t, Direction.NORTH);
        }

        new InputController(this, interactionState);
        renderer = new Renderer(interactionState);
        setFocusable(true);
    }

    // ---- Accessors / setters used by CircuitSim ----------------------------

    public void setActiveTool(Tool tool) { this.activeTool = tool; }

    public Component[][] getGrid() { return grid; }

    public void setGrid(Component[][] newGrid) {
        this.grid = newGrid;
        undoStack.clear();
        redoStack.clear();
        CircuitSolver.autoRouteAllWires(grid);
        repaint();
    }

    public void clearGrid() {
        grid = new Component[GridDimensions.HEIGHT][GridDimensions.WIDTH];
        undoStack.clear();
        redoStack.clear();
        repaint();
    }

    public void setGraphDialog(GraphDialog dlg)  { this.graphDialog = dlg; }
    public void setOscilloscopeDialog(OscilloscopeDialog dlg) { this.oscilloscopeDialog = dlg; }

    // -------------------------------------------------------------------------
    // Command handling
    // -------------------------------------------------------------------------

    void executeCommand(EditCommand cmd) {
        cmd.execute();
        undoStack.push(cmd);
        redoStack.clear();
        CircuitSolver.autoRouteAllWires(grid);
        repaint();
    }

    void undo() {
        if (!undoStack.isEmpty()) {
            EditCommand cmd = undoStack.pop();
            cmd.undo();
            redoStack.push(cmd);
            CircuitSolver.autoRouteAllWires(grid);
            repaint();
        }
    }

    void redo() {
        if (!redoStack.isEmpty()) {
            EditCommand cmd = redoStack.pop();
            cmd.redo();
            undoStack.push(cmd);
            CircuitSolver.autoRouteAllWires(grid);
            repaint();
        }
    }

    // -------------------------------------------------------------------------
    // Coordinate mapping
    // -------------------------------------------------------------------------

    boolean isInsideGrid(int col, int row) {
        return col >= 0 && col < GridDimensions.WIDTH && row >= 0 && row < GridDimensions.HEIGHT;
    }

    boolean isEraserOrWire() {
        return activeTool == Tool.ERASER || activeTool == Tool.WIRE;
    }

    void finishDrag() {
        int minCol = Math.min(interactionState.dragStartCol, interactionState.dragEndCol);
        int maxCol = Math.max(interactionState.dragStartCol, interactionState.dragEndCol);
        int minRow = Math.min(interactionState.dragStartRow, interactionState.dragEndRow);
        int maxRow = Math.max(interactionState.dragStartRow, interactionState.dragEndRow);

        if (activeTool == Tool.ERASER) {
            EditCommand.CompositeCommand compCmd = new EditCommand.CompositeCommand();
            for (int r = minRow; r <= maxRow; r++) {
                for (int c = minCol; c <= maxCol; c++) {
                    if (grid[r][c] != null) compCmd.add(new EditCommand.PlaceComponentCommand(grid, c, r, null));
                }
            }
            if (!compCmd.isEmpty()) executeCommand(compCmd);
        } else if (activeTool == Tool.WIRE) {
            EditCommand.CompositeCommand compCmd = new EditCommand.CompositeCommand();
            if (interactionState.dragStartCol == interactionState.dragEndCol || interactionState.dragStartRow == interactionState.dragEndRow) {
                if (interactionState.dragStartCol == interactionState.dragEndCol) {
                    for (int r = minRow; r <= maxRow; r++) {
                        EditCommand sub = createWirePlacementCommand(interactionState.dragStartCol, r);
                        if (sub != null) compCmd.add(sub);
                    }
                } else {
                    for (int c = minCol; c <= maxCol; c++) {
                        EditCommand sub = createWirePlacementCommand(c, interactionState.dragStartRow);
                        if (sub != null) compCmd.add(sub);
                    }
                }
            } else {
                for (int c = minCol; c <= maxCol; c++) {
                    EditCommand s1 = createWirePlacementCommand(c, minRow);
                    if (s1 != null) compCmd.add(s1);
                    EditCommand s2 = createWirePlacementCommand(c, maxRow);
                    if (s2 != null) compCmd.add(s2);
                }
                for (int r = minRow; r <= maxRow; r++) {
                    EditCommand s1 = createWirePlacementCommand(minCol, r);
                    if (s1 != null) compCmd.add(s1);
                    EditCommand s2 = createWirePlacementCommand(maxCol, r);
                    if (s2 != null) compCmd.add(s2);
                }
            }
            if (!compCmd.isEmpty()) executeCommand(compCmd);
        }

        interactionState.isDragging = false;
        interactionState.dragStartCol = -1;
        interactionState.dragStartRow = -1;
    }

    Component getGridComponent(int col, int row) { return grid[row][col]; }

    void rotateActiveToolDirection() {
        toolDirections.put(activeTool, toolDirections.get(activeTool).rotateCW());
        repaint();
    }

    void cycleHoveredComponent(Component comp) {
        if (comp instanceof Wire) {
            Wire wire = (Wire) comp;
            Wire.WireType nextType;
            switch (wire.getType()) {
                case STRAIGHT: nextType = Wire.WireType.CORNER; break;
                case CORNER: nextType = Wire.WireType.T_JUNCTION; break;
                case T_JUNCTION: nextType = Wire.WireType.CROSS; break;
                case CROSS: nextType = Wire.WireType.DOTTED; break;
                case DOTTED:
                default: nextType = Wire.WireType.STRAIGHT; break;
            }
            Wire newWire = new Wire(interactionState.hoverCol, interactionState.hoverRow, nextType);
            newWire.setDirection(wire.getDirection());
            newWire.setActive(wire.isActive());
            executeCommand(new EditCommand.PlaceComponentCommand(grid, interactionState.hoverCol, interactionState.hoverRow, newWire));
        } else if (comp instanceof Switch) {
            Switch oldSw = (Switch) comp;
            Switch newSw = new Switch(interactionState.hoverCol, interactionState.hoverRow);
            newSw.setDirection(oldSw.getDirection());
            newSw.setActive(oldSw.isActive());
            newSw.setOpen(!oldSw.isOpen());
            executeCommand(new EditCommand.PlaceComponentCommand(grid, interactionState.hoverCol, interactionState.hoverRow, newSw));
        }
    }

    void deleteHoveredComponent() {
        executeCommand(new EditCommand.PlaceComponentCommand(grid, interactionState.hoverCol, interactionState.hoverRow, null));
    }

    // -------------------------------------------------------------------------
    // Graph integration
    // -------------------------------------------------------------------------

    void openGraphFor(Component comp) {
        if (graphDialog == null) return;
        String label = buildComponentLabel(comp);
        graphDialog.setComponent(comp, label);
        graphDialog.getGraphPanel().tick(); // start collecting immediately
        if (!graphDialog.isVisible()) graphDialog.setVisible(true);
        graphDialog.toFront();
    }

    private String buildComponentLabel(Component comp) {
        return comp.getClass().getSimpleName() + " (" + comp.getCol() + "," + comp.getRow() + ")";
    }

    // -------------------------------------------------------------------------
    // Component placement
    // -------------------------------------------------------------------------

    void placeComponent(int col, int row) {
        Component newComp = null;
        switch (activeTool) {
            case WIRE:      newComp = new Wire(col, row, Wire.WireType.STRAIGHT); break;
            case BATTERY:   newComp = new Battery(col, row);   break;
            case BULB:      newComp = new Bulb(col, row);      break;
            case RESISTOR:  newComp = new Resistor(col, row);  break;
            case SWITCH:    newComp = new Switch(col, row);    break;
            case AMMETER:   newComp = new Ammeter(col, row);   break;
            case VOLTMETER: newComp = new Voltmeter(col, row); break;
            case DIODE:     newComp = new Diode(col, row);     break;
            case LED:       newComp = new LED(col, row);       break;
            case FUSE:      newComp = new Fuse(col, row);      break;
            case TEXT_LABEL: newComp = new TextLabel(col, row); break;
            case CAPACITOR: newComp = new Capacitor(col, row); break;
            case INDUCTOR:  newComp = new Inductor(col, row);  break;
            case AC_MOTOR:  newComp = new ACMotor(col, row);   break;
            case ERASER:
                if (grid[row][col] != null) {
                    executeCommand(new EditCommand.PlaceComponentCommand(grid, col, row, null));
                }
                return;
        }

        if (newComp != null) {
            newComp.setDirection(toolDirections.get(activeTool));
            executeCommand(new EditCommand.PlaceComponentCommand(grid, col, row, newComp));
        }
    }

    private EditCommand createWirePlacementCommand(int col, int row) {
        Component existing = grid[row][col];
        if (existing == null || existing instanceof Wire) {
            return new EditCommand.PlaceComponentCommand(grid, col, row,
                    new Wire(col, row, Wire.WireType.STRAIGHT));
        }
        return null;
    }

    public void autoRouteWires() {
        CircuitSolver.autoRouteAllWires(grid);
        repaint();
    }

    // -------------------------------------------------------------------------
    // Rotation
    // -------------------------------------------------------------------------

    void rotateAndSave(Component comp) {
        if (comp == null) return;
        Direction oldDir = comp.getDirection();
        Direction newDir = oldDir.rotateCW();
        executeCommand(new EditCommand.RotateComponentCommand(comp, oldDir, newDir));
        Tool toolType = getToolTypeForComponent(comp);
        if (toolType != null) toolDirections.put(toolType, comp.getDirection());
    }

    private Tool getToolTypeForComponent(Component comp) {
        if (comp instanceof Battery)   return Tool.BATTERY;
        if (comp instanceof Bulb)      return Tool.BULB;
        if (comp instanceof Resistor)  return Tool.RESISTOR;
        if (comp instanceof Switch)    return Tool.SWITCH;
        if (comp instanceof Ammeter)   return Tool.AMMETER;
        if (comp instanceof Voltmeter) return Tool.VOLTMETER;
        if (comp instanceof Wire)      return Tool.WIRE;
        if (comp instanceof LED)       return Tool.LED;
        if (comp instanceof Diode)     return Tool.DIODE;
        if (comp instanceof Fuse)      return Tool.FUSE;
        if (comp instanceof TextLabel) return Tool.TEXT_LABEL;
        if (comp instanceof Capacitor) return Tool.CAPACITOR;
        if (comp instanceof Inductor)  return Tool.INDUCTOR;
        if (comp instanceof ACMotor)   return Tool.AC_MOTOR;
        return null;
    }

    // -------------------------------------------------------------------------
    // Property editor  (replaces old JOptionPane prompts)
    // -------------------------------------------------------------------------

    void openPropertyEditor(Component comp) {
        JFrame owner = (JFrame) SwingUtilities.getWindowAncestor(this);
        PropertyEditorDialog dlg = new PropertyEditorDialog(owner, comp);

        // Wire up "Show Graph" button
        dlg.setShowGraphCallback(c -> openGraphFor(c));

        dlg.setVisible(true);

        if (!dlg.isApplied()) return;

        // Read new values and commit as an undoable command
        if (comp instanceof Battery) {
            Battery b = (Battery) comp;
            double oldV = b.getVoltage();
            double oldR = b.getInternalResistance();
            double newV = dlg.getDouble("voltage", oldV);
            double newR = dlg.getDouble("internalResistance", oldR);
            if (newV != oldV || newR != oldR) {
                executeCommand(new EditCommand.ConfigureComponentCommand(comp, oldV, oldR, newV, newR));
            }
        } else if (comp instanceof Resistor) {
            Resistor r = (Resistor) comp;
            double oldR = r.getResistance();
            double newR = dlg.getDouble("resistance", oldR);
            if (newR != oldR) {
                executeCommand(new EditCommand.ConfigureComponentCommand(comp, oldR, 0.0, newR, 0.0));
            }
        } else if (comp instanceof Bulb) {
            Bulb b = (Bulb) comp;
            double oldR = b.getResistance();
            double newR = dlg.getDouble("resistance", oldR);
            if (newR != oldR) {
                executeCommand(new EditCommand.ConfigureComponentCommand(comp, oldR, 0.0, newR, 0.0));
            }
        } else if (comp instanceof Fuse) {
            Fuse f = (Fuse) comp;
            double oldL = f.getLimit();
            boolean oldB = f.isBlown();
            double newL  = dlg.getDouble("limit", oldL);
            boolean newB = dlg.getBoolean("blown", oldB);
            if (newL != oldL || newB != oldB) {
                executeCommand(new EditCommand.ConfigureComponentCommand(comp, oldL, oldB, newL, newB));
            }
        } else if (comp instanceof TextLabel) {
            TextLabel tl = (TextLabel) comp;
            String oldT = tl.getText();
            String newT = dlg.getString("text", oldT);
            if (!newT.equals(oldT)) {
                executeCommand(new EditCommand.ConfigureComponentCommand(comp, oldT, newT));
            }
        } else if (comp instanceof Capacitor) {
            Capacitor cap = (Capacitor) comp;
            double oldC = cap.getCapacitance() * 1e6; // µF
            double newC = dlg.getDouble("capacitance", oldC); // also µF
            if (Math.abs(newC - oldC) > 1e-9) {
                executeCommand(new EditCommand.ConfigureComponentCommand(comp, cap.getCapacitance(), 0.0, newC * 1e-6, 0.0));
            }
        } else if (comp instanceof Inductor) {
            Inductor ind = (Inductor) comp;
            double oldL = ind.getInductance();
            double newL = dlg.getDouble("inductance", oldL);
            if (newL != oldL) {
                executeCommand(new EditCommand.ConfigureComponentCommand(comp, oldL, 0.0, newL, 0.0));
            }
        } else if (comp instanceof ACMotor) {
            ACMotor ac = (ACMotor) comp;
            double oldV = ac.getPeakVoltage();
            double oldF = ac.getFrequency();
            double newV = dlg.getDouble("peakVoltage", oldV);
            double newF = dlg.getDouble("frequency", oldF);
            if (newV != oldV || newF != oldF) {
                executeCommand(new EditCommand.ConfigureComponentCommand(comp, oldV, oldF, newV, newF));
            }
        } else if (comp instanceof Switch) {
            Switch sw = (Switch) comp;
            boolean newOpen = dlg.getBoolean("open", sw.isOpen());
            if (newOpen != sw.isOpen()) {
                // Create a new switch with the toggled state via PlaceComponentCommand
                Switch newSw = new Switch(sw.getCol(), sw.getRow());
                newSw.setDirection(sw.getDirection());
                newSw.setOpen(newOpen);
                executeCommand(new EditCommand.PlaceComponentCommand(grid, sw.getCol(), sw.getRow(), newSw));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Simulation tick
    // -------------------------------------------------------------------------

    public static int getFrameCount() { return frameCount; }

    public void tick() {
        frameCount++;
        CircuitSolver.solve(grid, frameCount * 0.0166);

        // Push data to open graph/oscilloscope dialogs
        if (graphDialog != null && graphDialog.isVisible()) {
            graphDialog.getGraphPanel().tick();
        }
        if (oscilloscopeDialog != null && oscilloscopeDialog.isVisible()) {
            oscilloscopeDialog.getScopePanel().pushData(grid);
        }
    }

    // -------------------------------------------------------------------------
    // Painting
    // -------------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        renderer.render(g, grid, activeTool, getWidth(), getHeight());
    }
}
