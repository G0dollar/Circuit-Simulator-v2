package electricity.model;

import electricity.assets.SpriteLoader;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Fuse extends Component {
    private static BufferedImage spriteActive;
    private static BufferedImage spriteInactive;
    private double limit = 5.0; // Current limit in Amperes
    private boolean isBlown = false;

    public Fuse(int col, int row) {
        super(col, row);
        if (spriteActive == null) {
            spriteActive = SpriteLoader.load("Sprites/Components/Fuse(ON).png");
            if (spriteActive == null) {
                spriteActive = SpriteLoader.load("Sprites/Components/Fuse.png");
            }
        }
        if (spriteInactive == null) {
            spriteInactive = SpriteLoader.load("Sprites/Components/Fuse(OFF).png");
            if (spriteInactive == null) {
                spriteInactive = spriteActive;
            }
        }
    }

    public double getLimit() {
        return limit;
    }

    public void setLimit(double limit) {
        this.limit = limit;
    }

    public boolean isBlown() {
        return isBlown;
    }

    public void setBlown(boolean blown) {
        this.isBlown = blown;
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

        BufferedImage img = isBlown ? spriteInactive : spriteActive;
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
            g2d.drawLine(-tileSize / 2, 0, -16, 0);
            g2d.drawLine(16, 0, tileSize / 2, 0);

            // Body glass tube
            g2d.setColor(new Color(50, 50, 60, 180));
            g2d.fillRoundRect(-16, -8, 32, 16, 4, 4);
            g2d.setColor(new Color(100, 100, 120));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRoundRect(-16, -8, 32, 16, 4, 4);

            // Metal end caps
            g2d.setColor(new Color(170, 170, 185));
            g2d.fillRect(-16, -8, 5, 16);
            g2d.fillRect(11, -8, 5, 16);

            // Wavy fuse wire
            g2d.setStroke(new BasicStroke(1.5f));
            if (isBlown) {
                g2d.setColor(new Color(255, 100, 100)); // Red hot blown ends
                g2d.drawLine(-11, 0, -4, 0);
                g2d.drawLine(4, 0, 11, 0);
                
                // Draw small spark/blast marks
                g2d.setColor(new Color(255, 160, 50));
                g2d.drawLine(-2, -3, 2, 3);
                g2d.drawLine(-2, 3, 2, -3);
            } else {
                g2d.setColor(isActive ? new Color(255, 230, 100) : new Color(130, 130, 145));
                // Draw wavy wire
                g2d.drawLine(-11, 0, -6, -2);
                g2d.drawLine(-6, -2, -2, 2);
                g2d.drawLine(-2, 2, 2, -2);
                g2d.drawLine(2, -2, 6, 2);
                g2d.drawLine(6, 2, 11, 0);
            }

            g2d.setTransform(oldTx);
        }
    }
}
