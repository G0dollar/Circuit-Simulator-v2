package electricity.ui;

import java.awt.Cursor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.SwingUtilities;

import electricity.model.Component;
import electricity.solver.GridDimensions;

public class InputController {
    private static final int TILE_SIZE = 64;
    private final CanvasPanel canvas;
    private final InteractionState state;

    public InputController(CanvasPanel canvas, InteractionState state) {
        this.canvas = canvas;
        this.state = state;
        setupMouseListeners();
        setupKeyListeners();
    }

    private void setupMouseListeners() {
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                canvas.requestFocusInWindow();

                if (SwingUtilities.isMiddleMouseButton(e)) {
                    state.isPanning = true;
                    state.lastDragX = e.getX();
                    state.lastDragY = e.getY();
                    canvas.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    return;
                }

                int col = state.getGridCol(e.getX(), TILE_SIZE);
                int row = state.getGridRow(e.getY(), TILE_SIZE);

                if (canvas.isInsideGrid(col, row)) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        if (e.getClickCount() == 2) {
                            Component comp = canvas.getGrid()[row][col];
                            if (comp != null) {
                                if (e.isAltDown()) canvas.openGraphFor(comp);
                                else canvas.openPropertyEditor(comp);
                            }
                        } else if (e.getClickCount() == 1) {
                            if (e.isAltDown()) {
                                Component comp = canvas.getGrid()[row][col];
                                if (comp != null) canvas.openGraphFor(comp);
                                return;
                            }
                            if (canvas.isEraserOrWire()) {
                                state.dragStartCol = col;
                                state.dragStartRow = row;
                                state.dragEndCol = col;
                                state.dragEndRow = row;
                                state.isDragging = true;
                            } else {
                                canvas.placeComponent(col, row);
                            }
                        }
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        Component comp = canvas.getGrid()[row][col];
                        if (comp != null) canvas.rotateAndSave(comp);
                    }
                    canvas.repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (state.isPanning) {
                    state.panX += e.getX() - state.lastDragX;
                    state.panY += e.getY() - state.lastDragY;
                    state.lastDragX = e.getX();
                    state.lastDragY = e.getY();
                    canvas.repaint();
                    return;
                }

                if (state.isDragging && canvas.isEraserOrWire()) {
                    int col = Math.max(0, Math.min(GridDimensions.WIDTH - 1, state.getGridCol(e.getX(), TILE_SIZE)));
                    int row = Math.max(0, Math.min(GridDimensions.HEIGHT - 1, state.getGridRow(e.getY(), TILE_SIZE)));
                    state.dragEndCol = col;
                    state.dragEndRow = row;
                    state.hoverCol = col;
                    state.hoverRow = row;
                    canvas.repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (state.isPanning) {
                    state.isPanning = false;
                    canvas.setCursor(Cursor.getDefaultCursor());
                    return;
                }

                if (state.isDragging) {
                    canvas.finishDrag();
                    canvas.repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int oldCol = state.hoverCol;
                int oldRow = state.hoverRow;
                int col = state.getGridCol(e.getX(), TILE_SIZE);
                int row = state.getGridRow(e.getY(), TILE_SIZE);
                if (canvas.isInsideGrid(col, row)) {
                    state.hoverCol = col;
                    state.hoverRow = row;
                } else {
                    state.hoverCol = -1;
                    state.hoverRow = -1;
                }
                if (state.hoverCol != oldCol || state.hoverRow != oldRow) canvas.repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double oldZoom = state.zoom;
                double newZoom = e.getWheelRotation() < 0
                        ? Math.min(4.0, oldZoom * 1.1)
                        : Math.max(0.25, oldZoom / 1.1);
                state.zoomAround(newZoom, e.getX(), e.getY());
                updateHover(e.getX(), e.getY());
                canvas.repaint();
            }

            private void updateHover(int screenX, int screenY) {
                int oldCol = state.hoverCol;
                int oldRow = state.hoverRow;
                int col = state.getGridCol(screenX, TILE_SIZE);
                int row = state.getGridRow(screenY, TILE_SIZE);
                if (canvas.isInsideGrid(col, row)) {
                    state.hoverCol = col;
                    state.hoverRow = row;
                } else {
                    state.hoverCol = -1;
                    state.hoverRow = -1;
                }
                if (state.hoverCol != oldCol || state.hoverRow != oldRow) canvas.repaint();
            }
        };

        canvas.addMouseListener(mouseHandler);
        canvas.addMouseMotionListener(mouseHandler);
        canvas.addMouseWheelListener(mouseHandler);
    }

    private void setupKeyListeners() {
        canvas.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int keyCode = e.getKeyCode();

                if (e.isControlDown()) {
                    if (keyCode == KeyEvent.VK_Z) { canvas.undo(); return; }
                    if (keyCode == KeyEvent.VK_Y) { canvas.redo(); return; }
                }

                if (keyCode == KeyEvent.VK_EQUALS || keyCode == KeyEvent.VK_ADD) {
                    state.zoomAround(Math.min(4.0, state.zoom + 0.1),
                            canvas.getWidth() / 2, canvas.getHeight() / 2);
                    canvas.repaint();
                    return;
                } else if (keyCode == KeyEvent.VK_MINUS || keyCode == KeyEvent.VK_SUBTRACT) {
                    state.zoomAround(Math.max(0.25, state.zoom - 0.1),
                            canvas.getWidth() / 2, canvas.getHeight() / 2);
                    canvas.repaint();
                    return;
                }

                if (keyCode == KeyEvent.VK_HOME) {
                    state.zoom = 1.0;
                    state.panX = 0;
                    state.panY = 0;
                    canvas.repaint();
                    return;
                }

                int panStep = (int) (40 / state.zoom);
                if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_W) { state.panY += panStep; canvas.repaint(); return; }
                if (keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_S) { state.panY -= panStep; canvas.repaint(); return; }
                if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_A) { state.panX += panStep; canvas.repaint(); return; }
                if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_D) { state.panX -= panStep; canvas.repaint(); return; }

                if (canvas.isInsideGrid(state.hoverCol, state.hoverRow)) {
                    Component comp = canvas.getGridComponent(state.hoverCol, state.hoverRow);

                    if (keyCode == KeyEvent.VK_R) {
                        if (comp != null) canvas.rotateAndSave(comp);
                        else canvas.rotateActiveToolDirection();
                    } else if (keyCode == KeyEvent.VK_C) {
                        canvas.cycleHoveredComponent(comp);
                    } else if (keyCode == KeyEvent.VK_DELETE || keyCode == KeyEvent.VK_BACK_SPACE) {
                        if (comp != null) canvas.deleteHoveredComponent();
                    } else if (keyCode == KeyEvent.VK_G) {
                        if (comp != null) canvas.openGraphFor(comp);
                    } else if (keyCode == KeyEvent.VK_F) {
                        canvas.autoRouteWires();
                    }
                }
            }
        });
    }
}
