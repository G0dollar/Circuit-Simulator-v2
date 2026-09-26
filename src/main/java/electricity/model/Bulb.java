package electricity.model;

import electricity.assets.SpriteLoader;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Bulb extends Component {
    private static BufferedImage spriteOn;
    private static BufferedImage spriteOff;
    private double resistance = 10.0; // Default 10 Ohms

    public Bulb(int col, int row) {
        super(col, row);
        if (spriteOn == null) {
            spriteOn = SpriteLoader.load("Sprites/Components/Bulb(ON).png");
        }
        if (spriteOff == null) {
            spriteOff = SpriteLoader.load("Sprites/Components/Bulb(OFF).png");
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
        BufferedImage img = isActive ? spriteOn : spriteOff;
        if (img == null) {
            img = (spriteOff != null) ? spriteOff : spriteOn;
        }
        SpriteLoader.drawRotated(g2d, img, x, y, tileSize, direction.degrees);
    }
}
