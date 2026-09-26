package electricity.solver;

import electricity.model.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;

public class CircuitSolver {
    private static final double SINGULAR_PIVOT_THRESHOLD = 1e-10;

    private static final int NUM_H_BORDERS = (GridDimensions.HEIGHT + 1) * GridDimensions.WIDTH;
    private static final int NUM_V_BORDERS = GridDimensions.HEIGHT * (GridDimensions.WIDTH + 1);
    private static final int TOTAL_BORDERS = NUM_H_BORDERS + NUM_V_BORDERS;

    private static double lastTotalCurrent = -1.0;

    /**
     * Helper class representing an electrical branch (resistor, battery, bulb, switch).
     */
    private static class Branch {
        int b1; // Border ID 1
        int b2; // Border ID 2
        double resistance;
        double voltage; // positive at b1, negative at b2
        Component comp;

        public Branch(int b1, int b2, double resistance, double voltage, Component comp) {
            this.b1 = b1;
            this.b2 = b2;
            this.resistance = resistance;
            this.voltage = voltage;
            this.comp = comp;
        }
    }

    /**
     * Disjoint Set Union (Union-Find) to group connected wire terminals.
     */
    private static class DisjointSet {
        int[] parent;

        public DisjointSet(int size) {
            parent = new int[size];
            for (int i = 0; i < size; i++) {
                parent[i] = i;
            }
        }

        public int find(int i) {
            if (parent[i] == i) {
                return i;
            }
            return parent[i] = find(parent[i]); // Path compression
        }

        public void union(int i, int j) {
            int rootI = find(i);
            int rootJ = find(j);
            if (rootI != rootJ) {
                parent[rootI] = rootJ;
            }
        }
    }

    /**
     * Resolves the electrical state of all components in the grid using Modified Nodal Analysis.
     */
    public static void solve(Component[][] grid, double elapsedSeconds) {
        int height = grid.length;
        int width = grid[0].length;

        // Step 1: Reset active status and values of all components
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                if (grid[r][c] != null) {
                    grid[r][c].setActive(false);
                    grid[r][c].setSolverError(false);
                    grid[r][c].setCurrent(0.0);
                    grid[r][c].setVoltageDrop(0.0);
                }
            }
        }

        // Update AC Motor voltages
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                Component comp = grid[r][c];
                if (comp instanceof ACMotor) {
                    ACMotor ac = (ACMotor) comp;
                    double t = elapsedSeconds;
                    double vInst = ac.getPeakVoltage() * Math.sin(2 * Math.PI * ac.getFrequency() * t);
                    ac.setCurrentVoltage(vInst);
                }
            }
        }

        // Step 2: Flood-fill conductivity boundaries (ideal wires and closed switches)
        DisjointSet dsu = new DisjointSet(TOTAL_BORDERS);
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                Component comp = grid[r][c];
                if (comp instanceof Wire) {
                    Wire wire = (Wire) comp;
                    List<Direction> connectedDirs = new ArrayList<>();
                    for (Direction dir : Direction.values()) {
                        if (wire.connectsTo(dir)) {
                            connectedDirs.add(dir);
                        }
                    }
                    // Union all connections of this wire cell
                    for (int i = 1; i < connectedDirs.size(); i++) {
                        dsu.union(getBorderId(r, c, connectedDirs.get(0)), 
                                  getBorderId(r, c, connectedDirs.get(i)));
                    }
                } else if (comp instanceof Switch) {
                    Switch sw = (Switch) comp;
                    if (!sw.isOpen()) {
                        List<Direction> connectedDirs = new ArrayList<>();
                        for (Direction dir : Direction.values()) {
                            if (sw.connectsTo(dir)) {
                                connectedDirs.add(dir);
                            }
                        }
                        for (int i = 1; i < connectedDirs.size(); i++) {
                            dsu.union(getBorderId(r, c, connectedDirs.get(0)), 
                                      getBorderId(r, c, connectedDirs.get(i)));
                        }
                    }
                }
            }
        }

        // Iterative Bias-State Loop for Diode/LED convergence
        boolean stateChanged = true;
        int maxIterations = 10;
        int iter = 0;

        double[] nodeVoltages = null;
        Map<Integer, Integer> rootToNodeIdx = null;
        List<Branch> branches = null;
        boolean singularSystem = false;

        while (stateChanged && iter < maxIterations) {
            stateChanged = false;
            iter++;

            // Step 3: Identify active component branches
            branches = new ArrayList<>();
            for (int r = 0; r < height; r++) {
                for (int c = 0; c < width; c++) {
                    Component comp = grid[r][c];
                    if (comp == null) continue;

                    if (comp instanceof Resistor) {
                        Resistor res = (Resistor) comp;
                        Direction d1 = getGlobalDirection(comp, Direction.WEST);
                        Direction d2 = getGlobalDirection(comp, Direction.EAST);
                        branches.add(new Branch(getBorderId(r, c, d1), getBorderId(r, c, d2), res.getResistance(), 0.0, comp));
                    } else if (comp instanceof Bulb) {
                        Bulb bulb = (Bulb) comp;
                        Direction d1 = getGlobalDirection(comp, Direction.WEST);
                        Direction d2 = getGlobalDirection(comp, Direction.EAST);
                        branches.add(new Branch(getBorderId(r, c, d1), getBorderId(r, c, d2), bulb.getResistance(), 0.0, comp));
                    } else if (comp instanceof Battery) {
                        Battery bat = (Battery) comp;
                        Direction posDir = bat.getPositiveTerminal();
                        Direction negDir = bat.getNegativeTerminal();
                        double rInt = bat.getInternalResistance();
                        if (rInt < 1e-4) {
                            rInt = 1e-4;
                        }
                        branches.add(new Branch(getBorderId(r, c, posDir), getBorderId(r, c, negDir), rInt, bat.getVoltage(), comp));
                    } else if (comp instanceof Switch) {
                        Switch sw = (Switch) comp;
                        if (sw.isOpen()) {
                            Direction d1 = getGlobalDirection(comp, Direction.WEST);
                            Direction d2 = getGlobalDirection(comp, Direction.EAST);
                            branches.add(new Branch(getBorderId(r, c, d1), getBorderId(r, c, d2), 1e9, 0.0, comp));
                        }
                    } else if (comp instanceof Ammeter) {
                        Direction d1 = getGlobalDirection(comp, Direction.WEST);
                        Direction d2 = getGlobalDirection(comp, Direction.EAST);
                        branches.add(new Branch(getBorderId(r, c, d1), getBorderId(r, c, d2), 1e-4, 0.0, comp));
                    } else if (comp instanceof Voltmeter) {
                        Direction d1 = getGlobalDirection(comp, Direction.WEST);
                        Direction d2 = getGlobalDirection(comp, Direction.EAST);
                        branches.add(new Branch(getBorderId(r, c, d1), getBorderId(r, c, d2), 1e7, 0.0, comp));
                    } else if (comp instanceof Diode) {
                        Diode diode = (Diode) comp;
                        Direction d1 = getGlobalDirection(comp, Direction.WEST); // Anode
                        Direction d2 = getGlobalDirection(comp, Direction.EAST); // Cathode
                        double rBias = diode.isForward() ? 0.1 : 1e9;
                        branches.add(new Branch(getBorderId(r, c, d1), getBorderId(r, c, d2), rBias, 0.0, comp));
                    } else if (comp instanceof Fuse) {
                        Fuse fuse = (Fuse) comp;
                        Direction d1 = getGlobalDirection(comp, Direction.WEST);
                        Direction d2 = getGlobalDirection(comp, Direction.EAST);
                        double rFuse = fuse.isBlown() ? 1e9 : 1e-4;
                        branches.add(new Branch(getBorderId(r, c, d1), getBorderId(r, c, d2), rFuse, 0.0, comp));
                    } else if (comp instanceof Capacitor) {
                        Capacitor cap = (Capacitor) comp;
                        Direction d1 = getGlobalDirection(comp, Direction.WEST);
                        Direction d2 = getGlobalDirection(comp, Direction.EAST);
                        double rCap = 0.0166 / cap.getCapacitance(); // Req = dt / C
                        branches.add(new Branch(getBorderId(r, c, d1), getBorderId(r, c, d2), rCap, cap.getChargeVoltage(), comp));
                    } else if (comp instanceof Inductor) {
                        Inductor ind = (Inductor) comp;
                        Direction d1 = getGlobalDirection(comp, Direction.WEST);
                        Direction d2 = getGlobalDirection(comp, Direction.EAST);
                        double rInd = ind.getInductance() / 0.0166; // Req = L / dt
                        double vInd = -rInd * ind.getPrevCurrent();
                        branches.add(new Branch(getBorderId(r, c, d1), getBorderId(r, c, d2), rInd, vInd, comp));
                    } else if (comp instanceof ACMotor) {
                        ACMotor ac = (ACMotor) comp;
                        Direction d1 = getGlobalDirection(comp, Direction.WEST);
                        Direction d2 = getGlobalDirection(comp, Direction.EAST);
                        branches.add(new Branch(getBorderId(r, c, d1), getBorderId(r, c, d2), 1e-4, ac.getCurrentVoltage(), comp));
                    }
                }
            }

            if (branches.isEmpty()) return;

            // Step 4: Map border roots to unique active nodes indices (0 to N-1)
            Set<Integer> activeRoots = new HashSet<>();
            for (Branch b : branches) {
                activeRoots.add(dsu.find(b.b1));
                activeRoots.add(dsu.find(b.b2));
            }

            rootToNodeIdx = new HashMap<>();
            int nodeCount = 0;

            // Select Ground (Node 0)
            int gndRoot = -1;
            for (Branch b : branches) {
                if (b.comp instanceof Battery) {
                    gndRoot = dsu.find(b.b2);
                    break;
                }
            }
            if (gndRoot == -1) {
                for (Branch b : branches) {
                    if (b.comp instanceof ACMotor) {
                        gndRoot = dsu.find(b.b2);
                        break;
                    }
                }
            }

            if (gndRoot != -1 && activeRoots.contains(gndRoot)) {
                rootToNodeIdx.put(gndRoot, 0);
                nodeCount = 1;
            } else {
                int firstRoot = activeRoots.iterator().next();
                rootToNodeIdx.put(firstRoot, 0);
                nodeCount = 1;
            }

            for (int root : activeRoots) {
                if (!rootToNodeIdx.containsKey(root)) {
                    rootToNodeIdx.put(root, nodeCount++);
                }
            }

            // Step 5: Construct the Nodal Matrix equations
            int matrixSize = nodeCount - 1; // Node 0 is Ground
            nodeVoltages = new double[nodeCount];
            nodeVoltages[0] = 0.0; // Ground is 0V

            if (matrixSize > 0) {
                double[][] A = new double[matrixSize][matrixSize];
                double[] z = new double[matrixSize];

                for (Branch b : branches) {
                    int nodeI = rootToNodeIdx.get(dsu.find(b.b1));
                    int nodeJ = rootToNodeIdx.get(dsu.find(b.b2));
                    double G = 1.0 / b.resistance;
                    double Vs = b.voltage;

                    int idxI = nodeI - 1;
                    int idxJ = nodeJ - 1;

                    if (nodeI > 0) {
                        A[idxI][idxI] += G;
                        z[idxI] += G * Vs;
                    }
                    if (nodeJ > 0) {
                        A[idxJ][idxJ] += G;
                        z[idxJ] -= G * Vs;
                    }
                    if (nodeI > 0 && nodeJ > 0) {
                        A[idxI][idxJ] -= G;
                        A[idxJ][idxI] -= G;
                    }
                }

                // Step 6: Solve linear system (Gaussian Elimination)
                LinearSolveResult result = solveLinearSystem(A, z);
                double[] x = result.values;
                singularSystem |= result.singular;
                for (int i = 1; i < nodeCount; i++) {
                    nodeVoltages[i] = x[i - 1];
                }
            }

            // Check Diode / LED state changes
            for (Branch b : branches) {
                if (b.comp instanceof Diode) {
                    Diode diode = (Diode) b.comp;
                    double v1 = nodeVoltages[rootToNodeIdx.get(dsu.find(b.b1))];
                    double v2 = nodeVoltages[rootToNodeIdx.get(dsu.find(b.b2))];
                    double vDrop = v1 - v2;

                    if (diode.isForward() && vDrop < -0.01) {
                        diode.setForward(false);
                        stateChanged = true;
                    } else if (!diode.isForward() && vDrop > 0.01) {
                        diode.setForward(true);
                        stateChanged = true;
                    }
                }
            }
        }

        if (singularSystem) {
            markFloatingBranches(branches, dsu, rootToNodeIdx);
        }

        // Step 7: Update active states, currents, and voltage drops on components
        for (Branch b : branches) {
            double v1 = nodeVoltages[rootToNodeIdx.get(dsu.find(b.b1))];
            double v2 = nodeVoltages[rootToNodeIdx.get(dsu.find(b.b2))];
            double vDrop = v1 - v2;

            double current;
            if (b.comp instanceof Battery) {
                current = Math.abs(vDrop - b.voltage) / b.resistance;
            } else if (b.comp instanceof ACMotor) {
                current = Math.abs(vDrop - b.voltage) / b.resistance;
            } else if (b.comp instanceof Capacitor) {
                current = (vDrop - b.voltage) / b.resistance;
            } else if (b.comp instanceof Inductor) {
                current = (vDrop - b.voltage) / b.resistance;
            } else {
                current = Math.abs(vDrop) / b.resistance;
            }

            double absCurrent = Math.abs(current);
            b.comp.setCurrent(absCurrent);
            b.comp.setVoltageDrop(Math.abs(vDrop));

            if (absCurrent > 0.001) {
                b.comp.setActive(true);
            }

            // Update transient component states
            if (b.comp instanceof Capacitor) {
                Capacitor cap = (Capacitor) b.comp;
                cap.setChargeVoltage(vDrop);
            } else if (b.comp instanceof Inductor) {
                Inductor ind = (Inductor) b.comp;
                ind.setPrevCurrent(current);
            } else if (b.comp instanceof Fuse) {
                Fuse fuse = (Fuse) b.comp;
                if (!fuse.isBlown() && absCurrent > fuse.getLimit()) {
                    fuse.setBlown(true);
                    System.out.printf("[CircuitSolver] Fuse Blown at (col: %d, row: %d) - Current %.3f A exceeded limit %.2f A\n", 
                                      fuse.getCol(), fuse.getRow(), absCurrent, fuse.getLimit());
                }
            }
        }

        // Step 8: Propagate current values to Wires and closed Switches using BFS
        propagateCurrentsToConductors(grid);

        // Debug logger: print state change in console when current changes
        double totalBatteryCurrent = 0.0;
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                Component comp = grid[r][c];
                if (comp instanceof Battery && comp.isActive()) {
                    totalBatteryCurrent += comp.getCurrent();
                }
            }
        }
        
        if (Math.abs(totalBatteryCurrent - lastTotalCurrent) > 0.001) {
            System.out.printf("[CircuitSolver] State Change: Total Current = %.3f A\n", totalBatteryCurrent);
            lastTotalCurrent = totalBatteryCurrent;
        }
    }

    /**
     * Breadth-First Search propagation to light up wires/switches and sync their electron speed.
     */
    private static void propagateCurrentsToConductors(Component[][] grid) {
        int height = grid.length;
        int width = grid[0].length;

        Queue<Component> queue = new LinkedList<>();
        Set<Component> visited = new HashSet<>();

        // Add all active branch components (batteries, resistors, bulbs) to start the BFS
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                Component comp = grid[r][c];
                if (comp != null && comp.isActive() && !(comp instanceof Wire) && !(comp instanceof Switch)) {
                    queue.add(comp);
                    visited.add(comp);
                }
            }
        }

        while (!queue.isEmpty()) {
            Component curr = queue.poll();

            for (Direction dir : Direction.values()) {
                int nextCol = curr.getCol() + getColOffset(dir);
                int nextRow = curr.getRow() + getRowOffset(dir);

                if (nextRow >= 0 && nextRow < height && nextCol >= 0 && nextCol < width) {
                    Component neighbor = grid[nextRow][nextCol];
                    if (neighbor != null && !visited.contains(neighbor)) {
                        // Check if they are physically connected
                        if (canConnect(curr, dir, neighbor)) {
                            // Only traverse and assign current to conductors (wires and closed switches)
                            if (neighbor instanceof Wire || (neighbor instanceof Switch && !((Switch) neighbor).isOpen())) {
                                neighbor.setActive(true);
                                neighbor.setCurrent(curr.getCurrent());
                                visited.add(neighbor);
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Gaussian Elimination solver with partial pivoting.
     */
    private static class LinearSolveResult {
        final double[] values;
        final boolean singular;

        LinearSolveResult(double[] values, boolean singular) {
            this.values = values;
            this.singular = singular;
        }
    }

    private static LinearSolveResult solveLinearSystem(double[][] A, double[] b) {
        int n = b.length;
        boolean singular = false;
        for (int p = 0; p < n; p++) {
            int max = p;
            for (int i = p + 1; i < n; i++) {
                if (Math.abs(A[i][p]) > Math.abs(A[max][p])) {
                    max = i;
                }
            }
            double[] temp = A[p]; A[p] = A[max]; A[max] = temp;
            double t = b[p]; b[p] = b[max]; b[max] = t;

            if (Math.abs(A[p][p]) <= SINGULAR_PIVOT_THRESHOLD) {
                singular = true;
                continue;
            }

            for (int i = p + 1; i < n; i++) {
                double alpha = A[i][p] / A[p][p];
                b[i] -= alpha * b[p];
                for (int j = p; j < n; j++) {
                    A[i][j] -= alpha * A[p][j];
                }
            }
        }

        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double sum = 0.0;
            for (int j = i + 1; j < n; j++) {
                sum += A[i][j] * x[j];
            }
            if (Math.abs(A[i][i]) > SINGULAR_PIVOT_THRESHOLD) {
                x[i] = (b[i] - sum) / A[i][i];
            } else {
                x[i] = 0.0;
            }
        }
        return new LinearSolveResult(x, singular);
    }

    private static void markFloatingBranches(List<Branch> branches, DisjointSet dsu,
                                             Map<Integer, Integer> rootToNodeIdx) {
        if (branches == null || branches.isEmpty() || rootToNodeIdx == null) return;

        Map<Integer, Set<Integer>> neighbors = new HashMap<>();
        for (Branch branch : branches) {
            int rootA = dsu.find(branch.b1);
            int rootB = dsu.find(branch.b2);
            neighbors.computeIfAbsent(rootA, key -> new HashSet<>()).add(rootB);
            neighbors.computeIfAbsent(rootB, key -> new HashSet<>()).add(rootA);
        }

        int groundRoot = -1;
        for (Map.Entry<Integer, Integer> entry : rootToNodeIdx.entrySet()) {
            if (entry.getValue() == 0) {
                groundRoot = entry.getKey();
                break;
            }
        }
        if (groundRoot == -1) return;

        Set<Integer> groundedRoots = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        groundedRoots.add(groundRoot);
        queue.add(groundRoot);
        while (!queue.isEmpty()) {
            int root = queue.remove();
            for (int neighbor : neighbors.getOrDefault(root, new HashSet<>())) {
                if (groundedRoots.add(neighbor)) queue.add(neighbor);
            }
        }

        for (Branch branch : branches) {
            int rootA = dsu.find(branch.b1);
            int rootB = dsu.find(branch.b2);
            if (!groundedRoots.contains(rootA) || !groundedRoots.contains(rootB)) {
                branch.comp.setSolverError(true);
            }
        }
    }

    private static int getBorderId(int r, int c, Direction dir) {
        switch (dir) {
            case NORTH: return r * GridDimensions.WIDTH + c;
            case SOUTH: return (r + 1) * GridDimensions.WIDTH + c;
            case WEST:  return NUM_H_BORDERS + r * (GridDimensions.WIDTH + 1) + c;
            case EAST:  return NUM_H_BORDERS + r * (GridDimensions.WIDTH + 1) + (c + 1);
        }
        return -1;
    }

    private static Direction getGlobalDirection(Component comp, Direction localDir) {
        Direction dir = comp.getDirection();
        int rotations = 0;
        switch (dir) {
            case NORTH: rotations = 0; break;
            case EAST:  rotations = 1; break;
            case SOUTH: rotations = 2; break;
            case WEST:  rotations = 3; break;
        }
        Direction current = localDir;
        for (int i = 0; i < rotations; i++) {
            current = current.rotateCW();
        }
        return current;
    }

    private static boolean canConnect(Component from, Direction dir, Component to) {
        if (!from.connectsTo(dir)) return false;
        if (!to.connectsTo(dir.getOpposite())) return false;

        if (from instanceof Switch && ((Switch) from).isOpen()) return false;
        if (to instanceof Switch && ((Switch) to).isOpen()) return false;

        return true;
    }

    private static int getColOffset(Direction dir) {
        switch (dir) {
            case EAST: return 1;
            case WEST: return -1;
            default: return 0;
        }
    }

    private static int getRowOffset(Direction dir) {
        switch (dir) {
            case SOUTH: return 1;
            case NORTH: return -1;
            default: return 0;
        }
    }

    /**
     * Auto-routes all wires on the grid by inspecting neighbors.
     */
    public static void autoRouteAllWires(Component[][] grid) {
        int height = grid.length;
        int width = grid[0].length;
        for (int r = 0; r < height; r++) {
            for (int c = 0; c < width; c++) {
                if (grid[r][c] instanceof Wire) {
                    autoRouteWireAt(grid, r, c);
                }
            }
        }
    }

    private static void autoRouteWireAt(Component[][] grid, int r, int c) {
        int height = grid.length;
        int width = grid[0].length;
        
        boolean north = false;
        boolean east = false;
        boolean south = false;
        boolean west = false;
        
        if (r > 0) {
            Component n = grid[r-1][c];
            if (n instanceof Wire) north = true;
            else if (n != null && n.connectsTo(Direction.SOUTH)) north = true;
        }
        if (r < height - 1) {
            Component s = grid[r+1][c];
            if (s instanceof Wire) south = true;
            else if (s != null && s.connectsTo(Direction.NORTH)) south = true;
        }
        if (c > 0) {
            Component w = grid[r][c-1];
            if (w instanceof Wire) west = true;
            else if (w != null && w.connectsTo(Direction.EAST)) west = true;
        }
        if (c < width - 1) {
            Component e = grid[r][c+1];
            if (e instanceof Wire) east = true;
            else if (e != null && e.connectsTo(Direction.WEST)) east = true;
        }
        
        int count = (north ? 1 : 0) + (east ? 1 : 0) + (south ? 1 : 0) + (west ? 1 : 0);
        
        Wire.WireType type = Wire.WireType.STRAIGHT;
        Direction dir = Direction.NORTH;
        
        if (count == 0) {
            type = Wire.WireType.DOTTED;
            dir = Direction.NORTH;
        } else if (count == 1) {
            if (north || south) {
                type = Wire.WireType.STRAIGHT;
                dir = Direction.EAST;
            } else {
                type = Wire.WireType.STRAIGHT;
                dir = Direction.NORTH;
            }
        } else if (count == 2) {
            if (west && east) {
                type = Wire.WireType.STRAIGHT;
                dir = Direction.NORTH;
            } else if (north && south) {
                type = Wire.WireType.STRAIGHT;
                dir = Direction.EAST;
            } else if (north && west) {
                type = Wire.WireType.CORNER;
                dir = Direction.NORTH;
            } else if (north && east) {
                type = Wire.WireType.CORNER;
                dir = Direction.EAST;
            } else if (south && east) {
                type = Wire.WireType.CORNER;
                dir = Direction.SOUTH;
            } else if (south && west) {
                type = Wire.WireType.CORNER;
                dir = Direction.WEST;
            }
        } else if (count == 3) {
            if (north && south && west) {
                type = Wire.WireType.T_JUNCTION;
                dir = Direction.NORTH;
            } else if (north && west && east) {
                type = Wire.WireType.T_JUNCTION;
                dir = Direction.EAST;
            } else if (north && south && east) {
                type = Wire.WireType.T_JUNCTION;
                dir = Direction.SOUTH;
            } else if (west && east && south) {
                type = Wire.WireType.T_JUNCTION;
                dir = Direction.WEST;
            }
        } else if (count == 4) {
            type = Wire.WireType.CROSS;
            dir = Direction.NORTH;
        }
        
        Wire newWire = new Wire(c, r, type);
        newWire.setDirection(dir);
        grid[r][c] = newWire;
    }
}

