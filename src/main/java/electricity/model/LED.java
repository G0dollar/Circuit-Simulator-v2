package electricity.model;

import electricity.assets.SpriteLoader;
import java.awt.*;
import java.awt.image.BufferedImage;

public class LED extends Diode {
    private static BufferedImage spriteActive;
    private static BufferedImage spriteInactive;

    public LED(int col, int row) {
        super(col, row);
        if (spriteActive == null) {
            spriteActive = SpriteLoader.load("Sprites/Components/LED(ON).png");
            if (spriteActive == null) {
                spriteActive = SpriteLoader.load("Sprites/Components/LED.png");
            }
        }
        if (spriteInactive == null) {
            spriteInactive = SpriteLoader.load("Sprites/Components/LED(OFF).png");
            if (spriteInactive == null) {
                spriteInactive = spriteActive;
            }
        }
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

            // Circle body (glows red when active)
            g2d.setColor(isActive() && isForward() ? new Color(255, 50, 50, 60) : new Color(45, 45, 55));
            g2d.fillOval(-18, -18, 36, 36);
            
            // LED outer ring color
            g2d.setColor(isActive() && isForward() ? new Color(255, 60, 60) : new Color(85, 85, 105));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawOval(-18, -18, 36, 36);

            // Diode triangle
            int[] px = {-8, -8, 6};
            int[] py = {-8, 8, 0};
            g2d.setColor(isActive() && isForward() ? new Color(255, 80, 80) : new Color(150, 150, 160));
            g2d.fillPolygon(px, py, 3);
            
            // Cathode line bar
            g2d.setStroke(new BasicStroke(2.5f));
            g2d.drawLine(6, -8, 6, 8);

            // Draw two small outward pointing arrows (light emission)
            g2d.setColor(isActive() && isForward() ? new Color(255, 120, 120) : new Color(110, 110, 125));
            g2d.setStroke(new BasicStroke(1.5f));
            
            // Arrow 1
            g2d.drawLine(-4, -12, -10, -18);
            g2d.drawLine(-10, -18, -9, -15);
            g2d.drawLine(-10, -18, -7, -17);
            
            // Arrow 2
            g2d.drawLine(4, -12, -2, -18);
            g2d.drawLine(-2, -18, -1, -15);
            g2d.drawLine(-2, -18, 1, -17);

            g2d.setTransform(oldTx);
        }
    }
}
