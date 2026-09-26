package electricity.model;

import electricity.assets.SpriteLoader;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Ammeter extends Component {
    private static BufferedImage spriteActive;
    private static BufferedImage spriteInactive;

    public Ammeter(int col, int row) {
        super(col, row);
        if (spriteActive == null) {
            spriteActive = SpriteLoader.load("Sprites/Components/Ammeter(ON).png");
            if (spriteActive == null) {
                spriteActive = SpriteLoader.load("Sprites/Components/Ammeter.png");
            }
        }
        if (spriteInactive == null) {
            spriteInactive = SpriteLoader.load("Sprites/Components/Ammeter(OFF).png");
            if (spriteInactive == null) {
                spriteInactive = spriteActive;
            }
        }
    }

    @Override
    public boolean connectsTo(Direction globalDir) {
        Direction localDir = globalToLocal(globalDir);
        // Connects East-West locally
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
            // Draw the custom sprite rotated
            SpriteLoader.drawRotated(g2d, img, x, y, tileSize, direction.degrees);
        } else {
            // Draw fallback vector graphics
            java.awt.geom.AffineTransform oldTx = g2d.getTransform();
            g2d.translate(x + tileSize / 2, y + tileSize / 2);
            g2d.rotate(Math.toRadians(direction.degrees));

            // Draw local terminals
            g2d.setColor(new Color(130, 130, 145));
            g2d.setStroke(new BasicStroke(4f));
            g2d.drawLine(-tileSize / 2, 0, -18, 0);
            g2d.drawLine(18, 0, tileSize / 2, 0);

            // Draw body circle
            g2d.setColor(new Color(45, 45, 55));
            g2d.fillOval(-20, -20, 40, 40);
            g2d.setColor(new Color(75, 75, 95));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawOval(-20, -20, 40, 40);

            g2d.setTransform(oldTx);

            // Draw letter 'A' (unrotated for readability)
            g2d.setFont(new Font("Dialog", Font.BOLD, 14));
            g2d.setColor(new Color(220, 220, 240));
            FontMetrics fm = g2d.getFontMetrics();
            String label = "A";
            int lx = x + (tileSize - fm.stringWidth(label)) / 2;
            int ly = y + (tileSize + fm.getAscent() - fm.getDescent()) / 2 - 8;
            g2d.drawString(label, lx, ly);
        }

        // Draw the digital readout (unrotated)
        g2d.setFont(new Font("Monospaced", Font.BOLD, 11));
        FontMetrics fm = g2d.getFontMetrics();
        String valStr = String.format("%.2fA", current);

        int tx = x + (tileSize - fm.stringWidth(valStr)) / 2;
        int ty = y + (tileSize + fm.getAscent() - fm.getDescent()) / 2 + (img == null ? 10 : 0);

        // Draw dark digital background pill
        int padX = 4;
        int padY = 2;
        int w = fm.stringWidth(valStr) + padX * 2;
        int h = fm.getHeight() + padY * 2;
        int rx = x + (tileSize - w) / 2;
        int ry = ty - fm.getAscent() - padY;

        g2d.setColor(new Color(15, 15, 20, 210));
        g2d.fillRoundRect(rx, ry, w, h, 6, 6);
        g2d.setColor(new Color(60, 60, 75));
        g2d.setStroke(new BasicStroke(1f));
        g2d.drawRoundRect(rx, ry, w, h, 6, 6);

        // Digital glow green color
        g2d.setColor(isActive ? new Color(80, 220, 140) : new Color(130, 130, 140));
        g2d.drawString(valStr, tx, ty);
    }
}
