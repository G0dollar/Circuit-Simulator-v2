package electricity.model;

import electricity.assets.SpriteLoader;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Battery extends Component {
    private static BufferedImage spriteActive;
    private static BufferedImage spriteInactive;
    private double voltage = 9.0; // Default 9V battery
    private double internalResistance = 0.0; // Initial value 0 Ohms (ideal battery)

    public Battery(int col, int row) {
        super(col, row);
        if (spriteActive == null) {
            spriteActive = SpriteLoader.load("Sprites/Components/Battery.png");
        }
        if (spriteInactive == null) {
            spriteInactive = SpriteLoader.load("Sprites/Components/Battery(OFF).png");
            if (spriteInactive == null) {
                spriteInactive = spriteActive;
            }
        }
    }

    public double getVoltage() {
        return voltage;
    }

    public void setVoltage(double voltage) {
        this.voltage = voltage;
    }

    public double getInternalResistance() {
        return internalResistance;
    }

    public void setInternalResistance(double internalResistance) {
        this.internalResistance = internalResistance;
    }

    /**
     * Positive terminal is locally EAST, negative is locally WEST.
     */
    @Override
    public boolean connectsTo(Direction globalDir) {
        Direction localDir = globalToLocal(globalDir);
        return localDir == Direction.EAST || localDir == Direction.WEST;
    }

    /**
     * Gets the global direction of the positive terminal.
     */
    public Direction getPositiveTerminal() {
        // Since positive is locally EAST, rotate EAST by the component's orientation
        switch (this.direction) {
            case NORTH: return Direction.EAST;
            case EAST: return Direction.SOUTH;
            case SOUTH: return Direction.WEST;
            case WEST: return Direction.NORTH;
        }
        return Direction.EAST;
    }

    /**
     * Gets the global direction of the negative terminal.
     */
    public Direction getNegativeTerminal() {
        // Since negative is locally WEST, rotate WEST by the component's orientation
        switch (this.direction) {
            case NORTH: return Direction.WEST;
            case EAST: return Direction.NORTH;
            case SOUTH: return Direction.EAST;
            case WEST: return Direction.SOUTH;
        }
        return Direction.WEST;
    }

    @Override
    public void draw(Graphics2D g2d, int tileSize) {
        int x = col * tileSize;
        int y = row * tileSize;
        BufferedImage img = isActive ? spriteActive : spriteInactive;
        if (img == null) {
            img = (spriteActive != null) ? spriteActive : spriteInactive;
        }
        // Use the rotation utility from SpriteLoader
        SpriteLoader.drawRotated(g2d, img, x, y, tileSize, direction.degrees);
    }
}
