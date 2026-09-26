import electricity.model.*;
import electricity.solver.CircuitSolver;
import electricity.solver.GridDimensions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestSolver {
    // Ideal batteries use CircuitSolver's 1e-4 ohm numerical floor, so allow its small error.
    private static final double EPSILON = 1e-3;

    @Test
    void testSingleResistor() {
        Component[][] grid = newGrid();
        Battery battery = battery(2, 2, 12.0, 0.0);
        Resistor resistor = resistor(3, 2, 6.0);
        grid[2][2] = battery;
        grid[2][3] = resistor;
        closeLoop(grid, 2, 2, 3);

        CircuitSolver.solve(grid, 0.0);

        // R_total = 6 ohms, so I = V / R = 12 / 6 = 2 A.
        assertNear("single resistor current", 2.0, resistor.getCurrent());
        assertNear("single resistor voltage", 12.0, resistor.getVoltageDrop());
        assertNear("single resistor battery current", 2.0, battery.getCurrent());
    }

    @Test
    void testSeriesResistors() {
        Component[][] grid = newGrid();
        Battery battery = battery(2, 2, 12.0, 0.0);
        Resistor first = resistor(3, 2, 2.0);
        Resistor second = resistor(4, 2, 4.0);
        grid[2][2] = battery;
        grid[2][3] = first;
        grid[2][4] = second;
        closeLoop(grid, 2, 2, 4);

        CircuitSolver.solve(grid, 0.0);

        // R_total = R1 + R2 = 6 ohms, so I = 12 / 6 = 2 A.
        // V1 = I * R1 = 4 V and V2 = I * R2 = 8 V.
        assertNear("series total current", 2.0, battery.getCurrent());
        assertNear("series first resistor current", 2.0, first.getCurrent());
        assertNear("series second resistor current", 2.0, second.getCurrent());
        assertNear("series first resistor voltage", 4.0, first.getVoltageDrop());
        assertNear("series second resistor voltage", 8.0, second.getVoltageDrop());
    }

    @Test
    void testParallelResistors() {
        Component[][] grid = newGrid();
        Battery battery = battery(3, 2, 12.0, 0.0);
        Resistor upper = resistor(3, 1, 6.0);
        Resistor lower = resistor(3, 3, 3.0);
        grid[2][3] = battery;
        grid[1][3] = upper;
        grid[3][3] = lower;
        parallelRails(grid, 3, 1, 3);

        CircuitSolver.solve(grid, 0.0);

        // Both branches have 12 V: I1 = 12 / 6 = 2 A and I2 = 12 / 3 = 4 A.
        // Therefore the battery current is I_total = I1 + I2 = 6 A.
        assertNear("parallel upper current", 2.0, upper.getCurrent());
        assertNear("parallel lower current", 4.0, lower.getCurrent());
        assertNear("parallel upper voltage", 12.0, upper.getVoltageDrop());
        assertNear("parallel lower voltage", 12.0, lower.getVoltageDrop());
        assertNear("parallel total current", 6.0, battery.getCurrent());
    }

    @Test
    void testBatteryInternalResistance() {
        Component[][] grid = newGrid();
        Battery battery = battery(2, 2, 12.0, 2.0);
        Resistor resistor = resistor(3, 2, 4.0);
        grid[2][2] = battery;
        grid[2][3] = resistor;
        closeLoop(grid, 2, 2, 3);

        CircuitSolver.solve(grid, 0.0);

        // R_total = R_internal + R_load = 2 + 4 = 6 ohms, so I = 12 / 6 = 2 A.
        // Terminal voltage = rated voltage - I * R_internal = 12 - 2 * 2 = 8 V.
        assertNear("internal resistance current", 2.0, battery.getCurrent());
        assertNear("load voltage with internal resistance", 8.0, resistor.getVoltageDrop());
        assertNear("battery terminal voltage", 8.0, battery.getVoltageDrop());
    }

    @Test
    void testOpenSwitch() {
        Component[][] grid = newGrid();
        Battery battery = battery(2, 2, 12.0, 0.0);
        Switch openSwitch = new Switch(3, 2);
        openSwitch.setOpen(true);
        Resistor resistor = resistor(4, 2, 6.0);
        grid[2][2] = battery;
        grid[2][3] = openSwitch;
        grid[2][4] = resistor;
        closeLoop(grid, 2, 2, 4);

        CircuitSolver.solve(grid, 0.0);

        // The open switch is modeled as 1e9 ohms, so I = 12 / (6 + 1e9), approximately 0 A.
        assertNearZero("open switch battery current", battery.getCurrent());
        assertNearZero("open switch current", openSwitch.getCurrent());
        assertNearZero("open switch resistor current", resistor.getCurrent());
    }

    @Test
    void testDiodeForwardAndReverseBias() {
        Component[][] forwardGrid = newGrid();
        Battery forwardBattery = battery(2, 2, 10.0, 0.0);
        Resistor forwardResistor = resistor(3, 2, 100.0);
        Diode forwardDiode = new Diode(4, 2);
        forwardDiode.setForward(true);
        forwardGrid[2][2] = forwardBattery;
        forwardGrid[2][3] = forwardResistor;
        forwardGrid[2][4] = forwardDiode;
        closeLoop(forwardGrid, 2, 2, 4);

        CircuitSolver.solve(forwardGrid, 0.0);

        // Forward diode resistance is 0.1 ohm: I = 10 / (100 + 0.1) = 0.0999001 A.
        assertNear("forward diode current", 10.0 / 100.1, forwardDiode.getCurrent());
        assertTrue(forwardDiode.getCurrent() > 0.09, "forward diode conducts");

        Component[][] reverseGrid = newGrid();
        Battery reverseBattery = battery(2, 2, 10.0, 0.0);
        Resistor reverseResistor = resistor(3, 2, 100.0);
        Diode reverseDiode = new Diode(4, 2);
        // Rotate the diode so its anode faces the battery's negative side.
        reverseDiode.setDirection(Direction.SOUTH);
        reverseDiode.setForward(false);
        reverseGrid[2][2] = reverseBattery;
        reverseGrid[2][3] = reverseResistor;
        reverseGrid[2][4] = reverseDiode;
        closeLoop(reverseGrid, 2, 2, 4);

        CircuitSolver.solve(reverseGrid, 0.0);

        // Reverse diode resistance is 1e9 ohms: I = 10 / (100 + 1e9), approximately 0 A.
        assertNearZero("reverse diode current", reverseDiode.getCurrent());
        assertNearZero("reverse diode resistor current", reverseResistor.getCurrent());
    }

    @Test
    void testIndependentCircuits() {
        Component[][] grid = newGrid();
        Battery firstBattery = battery(2, 2, 12.0, 0.0);
        Resistor firstResistor = resistor(3, 2, 6.0);
        Battery secondBattery = battery(12, 2, 9.0, 0.0);
        Resistor secondResistor = resistor(13, 2, 3.0);
        grid[2][2] = firstBattery;
        grid[2][3] = firstResistor;
        grid[2][12] = secondBattery;
        grid[2][13] = secondResistor;
        closeLoop(grid, 2, 2, 3);
        closeLoop(grid, 2, 12, 13);

        CircuitSolver.solve(grid, 0.0);

        // Circuit 1: I1 = 12 / 6 = 2 A. Circuit 2: I2 = 9 / 3 = 3 A.
        // Since there is no shared wire path, neither circuit changes the other current.
        assertNear("independent first current", 2.0, firstResistor.getCurrent());
        assertNear("independent second current", 3.0, secondResistor.getCurrent());
        assertNear("independent first battery current", 2.0, firstBattery.getCurrent());
        assertNear("independent second battery current", 3.0, secondBattery.getCurrent());
    }

    @Test
    void testCapacitorWithoutBattery() {
        Component[][] grid = newGrid();
        Capacitor capacitor = new Capacitor(2, 2);
        capacitor.setCapacitance(0.001);
        capacitor.setChargeVoltage(10.0);
        Resistor resistor = resistor(3, 2, 33.2);
        grid[2][2] = capacitor;
        grid[2][3] = resistor;
        closeLoop(grid, 2, 2, 3);

        CircuitSolver.solve(grid, 0.0);

        // R_cap = dt / C = 0.0166 / 0.001 = 16.6 ohms.
        // With no battery, the charged capacitor drives I = 10 / (16.6 + 33.2) = 0.200803 A.
        assertNear("capacitor-only resistor current", 10.0 / 49.8, resistor.getCurrent());
        assertTrue(Double.isFinite(capacitor.getCurrent()), "capacitor-only circuit remains finite");
    }

    @Test
    void testFloatingNearSingularCircuit() {
        Component[][] grid = newGrid();
        Resistor first = resistor(2, 2, 10.0);
        Resistor second = resistor(10, 10, 20.0);
        grid[2][2] = first;
        grid[10][10] = second;

        CircuitSolver.solve(grid, 0.0);

        // With no source, each isolated resistor has zero voltage and zero current.
        // The second disconnected branch leaves a floating matrix block; the solver
        // skips its near-zero pivot, returns zero, and flags only that branch as an error.
        assertNearZero("floating first resistor current", first.getCurrent());
        assertNearZero("floating second resistor current", second.getCurrent());
        assertNearZero("floating first resistor voltage", first.getVoltageDrop());
        assertNearZero("floating second resistor voltage", second.getVoltageDrop());
        assertTrue(!first.hasSolverError(), "grounded branch is not flagged");
        assertTrue(second.hasSolverError(), "floating branch is flagged");
    }

    @Test
    void testDiodeConvergenceFromWrongState() {
        Component[][] grid = newGrid();
        Battery battery = battery(2, 2, 10.0, 0.0);
        Resistor resistor = resistor(3, 2, 100.0);
        Diode diode = new Diode(4, 2);
        diode.setForward(false);
        grid[2][2] = battery;
        grid[2][3] = resistor;
        grid[2][4] = diode;
        closeLoop(grid, 2, 2, 4);

        CircuitSolver.solve(grid, 0.0);

        // The battery makes the diode's anode positive, so the first reverse-state iteration
        // changes to forward bias; the next iteration solves I = 10 / (100 + 0.1) = 0.0999001 A.
        assertTrue(diode.isForward(), "diode converges to forward state");
        assertNear("converged diode current", 10.0 / 100.1, diode.getCurrent());
    }

    @Test
    void testLargeGroundedCircuitHasNoFalseWarnings() {
        Component[][] grid = newGrid();
        Battery battery = battery(19, 25, 20.0, 1.0);
        Resistor[] resistors = new Resistor[10];
        grid[25][19] = battery;
        for (int col = 20; col < 30; col++) {
            resistors[col - 20] = resistor(col, 25, 10.0);
            grid[25][col] = resistors[col - 20];
        }
        closeLoop(grid, 25, 19, 29);

        CircuitSolver.solve(grid, 0.0);

        // R_total = R_internal + 10 * R = 1 + 10 * 10 = 101 ohms, so I = 20 / 101 A.
        assertNear("large grounded circuit current", 20.0 / 101.0, battery.getCurrent());
        assertTrue(!battery.hasSolverError(), "large battery is not falsely flagged");
        for (Resistor resistor : resistors) {
            assertTrue(!resistor.hasSolverError(), "large grounded resistor is not falsely flagged");
        }
    }

    private static Component[][] newGrid() {
        return new Component[GridDimensions.HEIGHT][GridDimensions.WIDTH];
    }

    private static Battery battery(int col, int row, double voltage, double internalResistance) {
        Battery battery = new Battery(col, row);
        battery.setDirection(Direction.NORTH);
        battery.setVoltage(voltage);
        battery.setInternalResistance(internalResistance);
        return battery;
    }

    private static Resistor resistor(int col, int row, double resistance) {
        Resistor resistor = new Resistor(col, row);
        resistor.setDirection(Direction.NORTH);
        resistor.setResistance(resistance);
        return resistor;
    }

    private static void addCrossWire(Component[][] grid, int col, int row) {
        grid[row][col] = new Wire(col, row, Wire.WireType.CROSS);
    }

    private static void closeLoop(Component[][] grid, int row, int firstCol, int lastCol) {
        int topRow = row - 1;
        int bottomRow = row + 1;
        int leftRail = firstCol - 1;
        int rightRail = lastCol + 1;

        for (int currentRow = topRow; currentRow <= bottomRow; currentRow++) {
            addCrossWire(grid, leftRail, currentRow);
            addCrossWire(grid, rightRail, currentRow);
        }
        for (int col = leftRail; col <= rightRail; col++) {
            addCrossWire(grid, col, topRow);
            addCrossWire(grid, col, bottomRow);
        }
    }

    private static void parallelRails(Component[][] grid, int centerCol, int topRow, int bottomRow) {
        int leftRail = centerCol - 1;
        int rightRail = centerCol + 1;
        for (int row = topRow; row <= bottomRow; row++) {
            addCrossWire(grid, leftRail, row);
            addCrossWire(grid, rightRail, row);
        }
    }

    private static void assertNear(String name, double expected, double actual) {
        assertEquals(expected, actual, EPSILON, name);
    }

    private static void assertNearZero(String name, double actual) {
        assertNear(name, 0.0, actual);
    }
}
