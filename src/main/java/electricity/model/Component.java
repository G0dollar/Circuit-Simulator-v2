package electricity.model;

import java.awt.*;

public abstract class Component {
    protected int col;
    protected int row;
    protected Direction direction = Direction.NORTH;
    protected boolean isActive = false;
    protected boolean solverError = false;
    
    // Numeric simulation values
    protected double current = 0.0;      // In Amperes (A)
    protected double voltageDrop = 0.0;  // In Volts (V)

    public Component(int col, int row) {
        this.col = col;
        this.row = row;
    }

    public double getCurrent() {
        return current;
    }

    public void setCurrent(double current) {
        this.current = current;
    }

    public double getVoltageDrop() {
        return voltageDrop;
    }

    public void setVoltageDrop(double voltageDrop) {
        this.voltageDrop = voltageDrop;
    }

    public int getCol() {
        return col;
    }

    public int getRow() {
        return row;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void rotate() {
        this.direction = this.direction.rotateCW();
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public boolean hasSolverError() {
        return solverError;
    }

    public void setSolverError(boolean solverError) {
        this.solverError = solverError;
    }

    /**
     * Helper to map a global direction into a local direction relative to the component's orientation.
     * This is useful for subclasses to define connections relative to their visual orientation.
     */
    protected Direction globalToLocal(Direction globalDir) {
        // Find how many clockwise rotations we need to undo
        switch (this.direction) {
            case NORTH:
                return globalDir; // No rotation
            case EAST:
                // East is 90 CW, so CCW is 90 CCW (or 270 CW)
                // global NORTH -> local WEST, global EAST -> local NORTH, etc.
                switch (globalDir) {
                    case NORTH: return Direction.WEST;
                    case EAST: return Direction.NORTH;
                    case SOUTH: return Direction.EAST;
                    case WEST: return Direction.SOUTH;
                }
            case SOUTH:
                // South is 180 CW, so opposite
                return globalDir.getOpposite();
            case WEST:
                // West is 270 CW, so CCW is 90 CW
                return globalDir.rotateCW();
        }
        return globalDir;
    }

    /**
     * Checks if this component has an active terminal/connection in the specified global direction.
     * @param globalDir The direction (NORTH, EAST, SOUTH, WEST) from this tile's center.
     */
    public abstract boolean connectsTo(Direction globalDir);

    /**
     * Renders the component on the grid.
     */
    public abstract void draw(Graphics2D g2d, int tileSize);
}
