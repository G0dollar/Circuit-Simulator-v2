package electricity.model;

import electricity.assets.SpriteLoader;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Switch extends Component {
    private boolean isOpen = true; // Switches start open by default
    private static BufferedImage spriteOn;
    private static BufferedImage spriteOff;

    public Switch(int col, int row) {
        super(col, row);
        if (spriteOn == null) {
            spriteOn = SpriteLoader.load("Sprites/Components/Lever(ON).png");
        }
        if (spriteOff == null) {
            spriteOff = SpriteLoader.load("Sprites/Components/Lever(OFF).png");
        }
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        this.isOpen = open;
    }

    public void toggle() {
        this.isOpen = !this.isOpen;
    }

    /**
     * A switch connects on opposite sides, but electricity can only pass through
     * if the switch is closed (!isOpen).
     */
    @Override
    public boolean connectsTo(Direction globalDir) {
        Direction localDir = globalToLocal(globalDir);
        return (localDir == Direction.EAST || localDir == Direction.WEST);
    }

    @Override
    public void draw(Graphics2D g2d, int tileSize) {
        int x = col * tileSize;
        int y = row * tileSize;
        
        // When switch is open (isOpen = true), it is "OFF"
        // When switch is closed (isOpen = false), it is "ON"
        BufferedImage img = isOpen ? spriteOff : spriteOn;
        if (img == null) {
            img = (spriteOff != null) ? spriteOff : spriteOn;
        }

        SpriteLoader.drawRotated(g2d, img, x, y, tileSize, direction.degrees);
    }
}
