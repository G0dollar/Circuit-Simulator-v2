package electricity.model;

import electricity.assets.SpriteLoader;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Diode extends Component {
    private static BufferedImage spriteActive;
    private static BufferedImage spriteInactive;
    private boolean isForward = true; // State resolved by iterative solver

    public Diode(int col, int row) {
        super(col, row);
        if (spriteActive == null) {
            spriteActive = SpriteLoader.load("Sprites/Components/Diode(ON).png");
            if (spriteActive == null) {
                spriteActive = SpriteLoader.load("Sprites/Components/Diode.png");
            }
        }
        if (spriteInactive == null) {
            spriteInactive = SpriteLoader.load("Sprites/Components/Diode(OFF).png");
            if (spriteInactive == null) {
                spriteInactive = spriteActive;
            }
        }
    }

    public boolean isForward() {
        return isForward;
    }

    public void setForward(boolean forward) {
        this.isForward = forward;
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
            g2d.drawLine(-tileSize / 2, 0, -14, 0);
            g2d.drawLine(14, 0, tileSize / 2, 0);

            // Circle body
            g2d.setColor(new Color(45, 45, 55));
            g2d.fillOval(-18, -18, 36, 36);
            g2d.setColor(new Color(75, 75, 95));
            g2d.drawOval(-18, -18, 36, 36);

            // Schematic symbol: Triangle pointing East (anode to cathode)
            // Anode (West) is at x=-8. Cathode (East) is at x=8.
            int[] px = {-8, -8, 6};
            int[] py = {-8, 8, 0};
            g2d.setColor(isForward && isActive ? new Color(80, 220, 140) : new Color(160, 160, 175));
            g2d.fillPolygon(px, py, 3);
            
            // Cathode line bar
            g2d.setStroke(new BasicStroke(2.5f));
            g2d.drawLine(6, -8, 6, 8);

            g2d.setTransform(oldTx);
        }
    }
}
