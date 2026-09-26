package electricity.model;

import electricity.assets.SpriteLoader;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Capacitor extends Component {
    private static BufferedImage spriteActive;
    private static BufferedImage spriteInactive;
    private double capacitance = 1000e-6; // Default 1000 uF
    private double chargeVoltage = 0.0;   // Stored charge potential

    public Capacitor(int col, int row) {
        super(col, row);
        if (spriteActive == null) {
            spriteActive = SpriteLoader.load("Sprites/Components/Capacitor(ON).png");
            if (spriteActive == null) {
                spriteActive = SpriteLoader.load("Sprites/Components/Capacitor.png");
            }
        }
        if (spriteInactive == null) {
            spriteInactive = SpriteLoader.load("Sprites/Components/Capacitor(OFF).png");
            if (spriteInactive == null) {
                spriteInactive = spriteActive;
            }
        }
    }

    public double getCapacitance() {
        return capacitance;
    }

    public void setCapacitance(double capacitance) {
        this.capacitance = capacitance;
    }

    public double getChargeVoltage() {
        return chargeVoltage;
    }

    public void setChargeVoltage(double chargeVoltage) {
        this.chargeVoltage = chargeVoltage;
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
        if (img == null) {
            img = (spriteActive != null) ? spriteActive : spriteInactive;
        }

        if (img != null) {
            SpriteLoader.drawRotated(g2d, img, x, y, tileSize, direction.degrees);
        } else {
            // Draw vector fallback graphics
            java.awt.geom.AffineTransform oldTx = g2d.getTransform();
            g2d.translate(x + tileSize / 2, y + tileSize / 2);
            g2d.rotate(Math.toRadians(direction.degrees));

            // Terminals
            g2d.setColor(new Color(130, 130, 145));
            g2d.setStroke(new BasicStroke(3f));
            g2d.drawLine(-tileSize / 2, 0, -4, 0);
            g2d.drawLine(4, 0, tileSize / 2, 0);

            // Parallel plates
            g2d.setColor(isActive ? new Color(100, 200, 255) : new Color(160, 160, 175));
            g2d.setStroke(new BasicStroke(4f));
            g2d.drawLine(-4, -12, -4, 12);
            g2d.drawLine(4, -12, 4, 12);

            g2d.setTransform(oldTx);
        }
    }
}
