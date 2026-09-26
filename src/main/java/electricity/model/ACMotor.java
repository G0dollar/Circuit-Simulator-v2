package electricity.model;

import electricity.assets.SpriteLoader;
import electricity.ui.CanvasPanel;
import java.awt.*;
import java.awt.image.BufferedImage;

public class ACMotor extends Component {
    private static BufferedImage[] activeFrames;
    private static BufferedImage spriteInactive;
    private double peakVoltage = 9.0; // Peak amplitude in Volts
    private double frequency = 1.0;   // Frequency in Hertz
    private double currentVoltage = 0.0; // Current instant voltage solved

    public ACMotor(int col, int row) {
        super(col, row);
        if (activeFrames == null) {
            activeFrames = new BufferedImage[12];
            boolean loadedAll = true;
            for (int i = 0; i < 12; i++) {
                activeFrames[i] = SpriteLoader.load("Sprites/Components/AC Motor/AC Motor" + (i + 1) + ".png");
                if (activeFrames[i] == null) {
                    loadedAll = false;
                }
            }
            if (!loadedAll) {
                // Try falling back to generic ACSource(ON) or ACSource
                BufferedImage singleActive = SpriteLoader.load("Sprites/Components/ACSource(ON).png");
                if (singleActive == null) {
                    singleActive = SpriteLoader.load("Sprites/Components/ACSource.png");
                }
                if (singleActive != null) {
                    for (int i = 0; i < 12; i++) {
                        activeFrames[i] = singleActive;
                    }
                } else {
                    activeFrames = null;
                }
            }
        }
        if (spriteInactive == null) {
            spriteInactive = SpriteLoader.load("Sprites/Components/AC Motor(OFF).png");
            if (spriteInactive == null) {
                spriteInactive = SpriteLoader.load("Sprites/Components/ACSource(OFF).png");
            }
        }
    }

    public double getPeakVoltage() {
        return peakVoltage;
    }

    public void setPeakVoltage(double peakVoltage) {
        this.peakVoltage = peakVoltage;
    }

    public double getFrequency() {
        return frequency;
    }

    public void setFrequency(double frequency) {
        this.frequency = frequency;
    }

    public double getCurrentVoltage() {
        return currentVoltage;
    }

    public void setCurrentVoltage(double currentVoltage) {
        this.currentVoltage = currentVoltage;
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

        BufferedImage img = null;
        if (isActive) {
            if (activeFrames != null) {
                int frameIdx = (CanvasPanel.getFrameCount() / 3) % 12; // Cycle every 3 ticks
                img = activeFrames[frameIdx];
            }
        } else {
            img = spriteInactive;
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

            // Circle body
            g2d.setColor(new Color(45, 45, 55));
            g2d.fillOval(-18, -18, 36, 36);
            g2d.setColor(isActive ? new Color(255, 190, 60) : new Color(80, 80, 95));
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawOval(-18, -18, 36, 36);

            // Draw wave tilde symbol inside circle (~)
            g2d.setStroke(new BasicStroke(2f));
            g2d.setColor(new Color(220, 220, 240));
            // Draw sine wave using small segments
            int prevX = -10;
            int prevY = 0;
            for (int dx = -9; dx <= 10; dx++) {
                double rad = (dx / 10.0) * Math.PI * 1.5;
                int dy = (int) (Math.sin(rad) * 6);
                g2d.drawLine(prevX, prevY, dx, dy);
                prevX = dx;
                prevY = dy;
            }

            g2d.setTransform(oldTx);
        }
    }
}
