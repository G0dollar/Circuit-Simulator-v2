package electricity.model;

import electricity.assets.SpriteLoader;
import java.awt.*;

public class TextLabel extends Component {
    private String text = "Label";

    public TextLabel(int col, int row) {
        super(col, row);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean connectsTo(Direction globalDir) {
        return false; // Does not connect physically
    }

    @Override
    public void draw(Graphics2D g2d, int tileSize) {
        int x = col * tileSize;
        int y = row * tileSize;

        // Draw light dashed border on hover or if empty to make edit position clear,
        // otherwise just draw the text
        g2d.setFont(new Font("Dialog", Font.BOLD, 12));
        FontMetrics fm = g2d.getFontMetrics();
        int tx = x + (tileSize - fm.stringWidth(text)) / 2;
        int ty = y + (tileSize + fm.getAscent() - fm.getDescent()) / 2;

        // Soft background panel to ensure text is readable
        g2d.setColor(new Color(255, 255, 255, 15));
        g2d.fillRoundRect(x + 2, y + 2, tileSize - 4, tileSize - 4, 6, 6);
        
        g2d.setColor(new Color(240, 240, 255));
        g2d.drawString(text, tx, ty);
    }
}
