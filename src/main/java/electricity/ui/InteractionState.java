package electricity.ui;

public final class InteractionState {
    int hoverCol = -1;
    int hoverRow = -1;
    int dragStartCol = -1;
    int dragStartRow = -1;
    int dragEndCol = -1;
    int dragEndRow = -1;
    boolean isDragging;
    boolean isPanning;
    int lastDragX;
    int lastDragY;
    int panX;
    int panY;
    double zoom = 1.0;

    public int getGridCol(int screenX, int tileSize) {
        return (int) Math.floor((screenX - panX) / (tileSize * zoom));
    }

    public int getGridRow(int screenY, int tileSize) {
        return (int) Math.floor((screenY - panY) / (tileSize * zoom));
    }

    public void zoomAround(double newZoom, int screenX, int screenY) {
        double scale = newZoom / zoom;
        panX = (int) Math.round(screenX - scale * (screenX - panX));
        panY = (int) Math.round(screenY - scale * (screenY - panY));
        zoom = newZoom;
    }
}
