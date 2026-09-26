package electricity.assets;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import java.io.InputStream;

public class SpriteLoader {
    public static BufferedImage load(String path) {
        // 1. Try loading as ClassLoader resource (works inside JAR and local classpath)
        try {
            String resourcePath = path.replace("\\", "/");
            InputStream is = SpriteLoader.class.getClassLoader().getResourceAsStream(resourcePath);
            if (is != null) {
                try {
                    return ImageIO.read(is);
                } finally {
                    is.close();
                }
            }
        } catch (Exception e) {
            // Ignore and fall back to local file load
        }

        // 2. Fall back to standard local file reading
        File file = new File(path);
        if (!file.exists()) {
            return null; // Silent fallback
        }
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            System.err.println("Failed to load sprite fallback: " + path);
            return null;
        }
    }

    /**
     * Draws a sprite rotated by degrees around its center coordinate.
     */
    public static void drawRotated(Graphics2D g2d, BufferedImage image, int x, int y, int size, double degrees) {
        if (image == null) return;

        // Save original graphics transform configuration
        AffineTransform originalTransform = g2d.getTransform();

        // Translate the canvas coordinate system to the center of target cell
        g2d.translate(x + size / 2.0, y + size / 2.0);

        // Perform rotation (degrees converted to radians)
        g2d.rotate(Math.toRadians(degrees));

        // Draw image relative to new center origin
        g2d.drawImage(image, -size / 2, -size / 2, size, size, null);

        // Restore original transform
        g2d.setTransform(originalTransform);
    }
}
