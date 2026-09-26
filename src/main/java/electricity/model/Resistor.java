package electricity.model;

import electricity.assets.SpriteLoader;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Resistor extends Component {
    private static BufferedImage spriteActive;
    private static BufferedImage spriteInactive;
    private double resistance = 100.0; // Default 100 Ohms

    public Resistor(int col, int row) {
        super(col, row);
        if (spriteActive == null) {
            spriteActive = SpriteLoader.load("Sprites/Components/Resistor.png");
        }
        if (spriteInactive == null) {
            spriteInactive = SpriteLoader.load("Sprites/Components/Resistor(OFF).png");
        }
    }

    public double getResistance() {
        return resistance;
    }

    public void setResistance(double resistance) {
        this.resistance = resistance;
    }

    @Override
    public boolean connectsTo(Direction globalDir) {
        Direction localDir = globalToLocal(globalDir);
        return localDir == Direction.EAST || localDir == Direction.WEST;
    }

    @Override
    public void draw(Graphics2D g2d, int tileSize) {
        int x = col * tileSize;
        int y = row * tileSize;
        BufferedImage img = isActive ? spriteActive : spriteInactive;
        // Fallback in case one sprite failed to load
        if (img == null) {
            img = (spriteActive != null) ? spriteActive : spriteInactive;
        }
        SpriteLoader.drawRotated(g2d, img, x, y, tileSize, direction.degrees);
    }
}
