package electricity.model;

public enum Direction {
    NORTH(0),
    EAST(90),
    SOUTH(180),
    WEST(270);

    public final double degrees;

    Direction(double degrees) {
        this.degrees = degrees;
    }

    public Direction getOpposite() {
        switch (this) {
            case NORTH: return SOUTH;
            case EAST: return WEST;
            case SOUTH: return NORTH;
            case WEST: return EAST;
            default: return NORTH;
        }
    }

    public Direction rotateCW() {
        switch (this) {
            case NORTH: return EAST;
            case EAST: return SOUTH;
            case SOUTH: return WEST;
            case WEST: return NORTH;
            default: return NORTH;
        }
    }
}
