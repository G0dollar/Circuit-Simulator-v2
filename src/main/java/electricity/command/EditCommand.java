package electricity.command;

import electricity.model.*;
import java.util.ArrayList;
import java.util.List;

public interface EditCommand {
    void execute();
    void undo();
    void redo();

    // Command to place or replace a component in the grid
    class PlaceComponentCommand implements EditCommand {
        private final Component[][] grid;
        private final int col;
        private final int row;
        private final Component oldComp;
        private final Component newComp;

        public PlaceComponentCommand(Component[][] grid, int col, int row, Component newComp) {
            this.grid = grid;
            this.col = col;
            this.row = row;
            this.oldComp = grid[row][col];
            this.newComp = newComp;
        }

        @Override
        public void execute() {
            grid[row][col] = newComp;
        }

        @Override
        public void undo() {
            grid[row][col] = oldComp;
        }

        @Override
        public void redo() {
            grid[row][col] = newComp;
        }
    }

    // Command to rotate a component CW/CCW
    class RotateComponentCommand implements EditCommand {
        private final Component comp;
        private final Direction oldDir;
        private final Direction newDir;

        public RotateComponentCommand(Component comp, Direction oldDir, Direction newDir) {
            this.comp = comp;
            this.oldDir = oldDir;
            this.newDir = newDir;
        }

        @Override
        public void execute() {
            comp.setDirection(newDir);
        }

        @Override
        public void undo() {
            comp.setDirection(oldDir);
        }

        @Override
        public void redo() {
            comp.setDirection(newDir);
        }
    }

    // Command to update component value parameters (e.g. resistance, voltage, limit, text, capacitance, inductance)
    class ConfigureComponentCommand implements EditCommand {
        private final Component comp;
        private final double oldVal1; 
        private final double oldVal2; 
        private final double newVal1;
        private final double newVal2;
        private final String oldStr;
        private final String newStr;
        private final boolean oldBool;
        private final boolean newBool;

        public ConfigureComponentCommand(Component comp, double oldVal1, double oldVal2, double newVal1, double newVal2) {
            this(comp, oldVal1, oldVal2, newVal1, newVal2, null, null, false, false);
        }

        public ConfigureComponentCommand(Component comp, String oldStr, String newStr) {
            this(comp, 0.0, 0.0, 0.0, 0.0, oldStr, newStr, false, false);
        }

        public ConfigureComponentCommand(Component comp, double oldLimit, boolean oldBlown, double newLimit, boolean newBlown) {
            this(comp, oldLimit, 0.0, newLimit, 0.0, null, null, oldBlown, newBlown);
        }

        private ConfigureComponentCommand(Component comp, double oldVal1, double oldVal2, double newVal1, double newVal2,
                                         String oldStr, String newStr, boolean oldBool, boolean newBool) {
            this.comp = comp;
            this.oldVal1 = oldVal1;
            this.oldVal2 = oldVal2;
            this.newVal1 = newVal1;
            this.newVal2 = newVal2;
            this.oldStr = oldStr;
            this.newStr = newStr;
            this.oldBool = oldBool;
            this.newBool = newBool;
        }

        @Override
        public void execute() {
            applyValues(newVal1, newVal2, newStr, newBool);
        }

        @Override
        public void undo() {
            applyValues(oldVal1, oldVal2, oldStr, oldBool);
        }

        @Override
        public void redo() {
            applyValues(newVal1, newVal2, newStr, newBool);
        }

        private void applyValues(double v1, double v2, String s, boolean b) {
            if (comp instanceof Battery) {
                Battery battery = (Battery) comp;
                battery.setVoltage(v1);
                battery.setInternalResistance(v2);
            } else if (comp instanceof Resistor) {
                ((Resistor) comp).setResistance(v1);
            } else if (comp instanceof Bulb) {
                ((Bulb) comp).setResistance(v1);
            } else if (comp instanceof Capacitor) {
                ((Capacitor) comp).setCapacitance(v1);
            } else if (comp instanceof Inductor) {
                ((Inductor) comp).setInductance(v1);
            } else if (comp instanceof ACMotor) {
                ACMotor ac = (ACMotor) comp;
                ac.setPeakVoltage(v1);
                ac.setFrequency(v2);
            } else if (comp instanceof Fuse) {
                Fuse fuse = (Fuse) comp;
                fuse.setLimit(v1);
                fuse.setBlown(b);
            } else if (comp instanceof TextLabel) {
                ((TextLabel) comp).setText(s);
            }
        }
    }

    // Composite Command to bundle multiple actions together (e.g. area erase or rectangle drag wire placement)
    class CompositeCommand implements EditCommand {
        private final List<EditCommand> commands = new ArrayList<>();

        public void add(EditCommand cmd) {
            commands.add(cmd);
        }

        public boolean isEmpty() {
            return commands.isEmpty();
        }

        @Override
        public void execute() {
            for (EditCommand cmd : commands) {
                cmd.execute();
            }
        }

        @Override
        public void undo() {
            // Undo in reverse order to correctly restore nested dependencies
            for (int i = commands.size() - 1; i >= 0; i--) {
                commands.get(i).undo();
            }
        }

        @Override
        public void redo() {
            for (EditCommand cmd : commands) {
                cmd.redo();
            }
        }
    }
}
