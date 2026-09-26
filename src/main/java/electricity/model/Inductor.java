package electricity.model;

import electricity.assets.SpriteLoader;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Inductor extends Component {
    private static BufferedImage spriteActive;
    private static BufferedImage spriteInactive;
    private double inductance = 1.0; // Default 1.0 Henry
    private double prevCurrent = 0.0; // Stored current from previous tick

    public Inductor(int col, int row) {
        super(col, row);
        if (spriteActive == null) {
            spriteActive = SpriteLoader.load("Sprites/Components/Inductor(ON).png");
            if (spriteActive == null) {
                spriteActive = SpriteLoader.load("Sprites/Components/Inductor.png");
            }
        }
        if (spriteInactive == null) {
            spriteInactive = SpriteLoader.load("Sprites/Components/Inductor(OFF).png");
            if (spriteInactive == null) {
                spriteInactive = spriteActive;
            }
        }
    }

    public double getInductance() {
        return inductance;
    }

    public void setInductance(double inductance) {
        this.inductance = inductance;
    }

    public double getPrevCurrent() {
        return prevCurrent;
    }

    public void setPrevCurrent(double prevCurrent) {
        this.prevCurrent = prevCurrent;
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
            g2d.drawLine(-tileSize / 2, 0, -18, 0);
            g2d.drawLine(18, 0, tileSize / 2, 0);

            // Coils: draw multiple overlapping arcs to look like an inductor coil
            g2d.setColor(isActive ? new Color(255, 170, 100) : new Color(150, 140, 130));
            g2d.setStroke(new BasicStroke(2.5f));
            
            // Draw 4 loops
            g2d.drawArc(-18, -8, 10, 16, 0, 180);
            g2d.drawArc(-9, -8, 10, 16, 0, 180);
            g2d.drawArc(0, -8, 10, 16, 0, 180);
            g2d.drawArc(9, -8, 10, 16, 0, 180);

            g2d.setTransform(oldTx);
        }
    }
}
