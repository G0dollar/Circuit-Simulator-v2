package electricity.model;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import electricity.assets.SpriteLoader;

public class Wire extends Component {
    public enum WireType {
        STRAIGHT,
        CORNER,
        T_JUNCTION,
        CROSS,
        DOTTED
    }

    private final WireType type;
    private static Map<WireType, BufferedImage> spritesOff = new HashMap<>();
    private static Map<WireType, BufferedImage> spritesOn = new HashMap<>();

    public Wire(int col, int row, WireType type) {
        super(col, row);
        this.type = type;
        loadSprites();
    }

    private void loadSprites() {
        if (spritesOff.isEmpty()) {
            spritesOff.put(WireType.STRAIGHT, SpriteLoader.load("Sprites/Wires/Straight dust(OFF).png"));
            spritesOff.put(WireType.CORNER, SpriteLoader.load("Sprites/Wires/Diagonal dust(OFF).png"));
            spritesOff.put(WireType.T_JUNCTION, SpriteLoader.load("Sprites/Wires/T-Junction dust(OFF).png"));
            spritesOff.put(WireType.CROSS, SpriteLoader.load("Sprites/Wires/Cross dust(OFF).png"));
            spritesOff.put(WireType.DOTTED, SpriteLoader.load("Sprites/Wires/Dotted dust(OFF).png"));

            // Load ON sprites (falling back to OFF if ON doesn't exist)
            BufferedImage straightOn = SpriteLoader.load("Sprites/Wires/Straight dust.png");
            spritesOn.put(WireType.STRAIGHT, straightOn != null ? straightOn : spritesOff.get(WireType.STRAIGHT));

            BufferedImage cornerOn = SpriteLoader.load("Sprites/Wires/Diagonal dust.png");
            spritesOn.put(WireType.CORNER, cornerOn != null ? cornerOn : spritesOff.get(WireType.CORNER));

            BufferedImage tOn = SpriteLoader.load("Sprites/Wires/T-Junction dust.png");
            spritesOn.put(WireType.T_JUNCTION, tOn != null ? tOn : spritesOff.get(WireType.T_JUNCTION));

            BufferedImage crossOn = SpriteLoader.load("Sprites/Wires/Cross dust.png");
            spritesOn.put(WireType.CROSS, crossOn != null ? crossOn : spritesOff.get(WireType.CROSS));

            BufferedImage dottedOn = SpriteLoader.load("Sprites/Wires/Dotted .png");
            spritesOn.put(WireType.DOTTED, dottedOn != null ? dottedOn : spritesOff.get(WireType.DOTTED));
        }
    }

    public WireType getType() {
        return type;
    }

    /**
     * Maps global direction to wire connection points based on wire geometry and orientation.
     */
    @Override
    public boolean connectsTo(Direction globalDir) {
        Direction localDir = globalToLocal(globalDir);

        switch (type) {
            case STRAIGHT:
                // Straight connects locally West and East
                return localDir == Direction.WEST || localDir == Direction.EAST;
            case CORNER:
                // Corner (diagonal) connects locally North and West (creates a 90 degree curve)
                return localDir == Direction.NORTH || localDir == Direction.WEST;
            case T_JUNCTION:
                // T-Junction connects locally West, North, and South (stem pointing West)
                return localDir == Direction.WEST || localDir == Direction.NORTH || localDir == Direction.SOUTH;
            case CROSS:
            case DOTTED:
                // Cross and Dotted connect all four sides
                return true;
        }
        return false;
    }

    @Override
    public void draw(Graphics2D g2d, int tileSize) {
        int x = col * tileSize;
        int y = row * tileSize;

        BufferedImage img = isActive ? spritesOn.get(type) : spritesOff.get(type);
        if (img == null) {
            img = spritesOff.get(type);
        }

        // Draw the base sprite
        SpriteLoader.drawRotated(g2d, img, x, y, tileSize, direction.degrees);

        // Overlay a current-based heat-map tint on only the wire image pixels (using SRC_ATOP composition)
        if (isActive && current > 0.0001 && img != null) {
            // Create a tinted version of the sprite mask matching current level
            BufferedImage tintedImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D tg = tintedImg.createGraphics();
            tg.drawImage(img, 0, 0, null);
            tg.setComposite(java.awt.AlphaComposite.SrcAtop);
            tg.setColor(getCurrentColor(current));
            tg.fillRect(0, 0, img.getWidth(), img.getHeight());
            tg.dispose();

            // Draw the tinted wire sprite with slight transparency over the base sprite
            java.awt.Composite oldComp = g2d.getComposite();
            g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.7f));
            SpriteLoader.drawRotated(g2d, tintedImg, x, y, tileSize, direction.degrees);
            g2d.setComposite(oldComp);
        }
    }

    /**
     * Maps a current magnitude to a heat-map color:
     *   0 A       → transparent (handled by caller)
     *   ~0–0.05 A → cool cyan-blue
     *   ~0.05–0.5 → amber/yellow
     *   > 0.5 A   → red (high current)
     */
    public static Color getCurrentColor(double amps) {
        // Clamp and normalize to [0, 1] over a 0–2 A range
        double norm = Math.min(1.0, Math.abs(amps) / 2.0);

        // Two-phase gradient: blue→cyan (0–0.3), cyan→yellow→red (0.3–1.0)
        if (norm < 0.3) {
            float t = (float) (norm / 0.3);
            // Blue (0, 120, 255) → Cyan (0, 220, 200)
            int r = (int) (0   + t * 0);
            int g = (int) (120 + t * 100);
            int b = (int) (255 + t * -55);
            return new Color(r, g, b);
        } else if (norm < 0.65) {
            float t = (float) ((norm - 0.3) / 0.35);
            // Cyan (0, 220, 200) → Yellow (255, 220, 0)
            int r = (int) (0   + t * 255);
            int g = (int) (220 + t * 0);
            int b = (int) (200 + t * -200);
            return new Color(r, g, b);
        } else {
            float t = (float) ((norm - 0.65) / 0.35);
            // Yellow (255, 220, 0) → Red (255, 40, 0)
            int r = 255;
            int g = (int) (220 + t * -180);
            int b = 0;
            return new Color(r, Math.max(0, g), b);
        }
    }
}
