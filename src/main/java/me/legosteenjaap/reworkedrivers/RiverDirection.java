package me.legosteenjaap.reworkedrivers;

public enum RiverDirection {
    NORTH,
    NORTHEAST(true),
    EAST,
    SOUTHEAST(true),
    SOUTH,
    SOUTHWEST(true),
    WEST,
    NORTHWEST (true);

    private final boolean isDiagonal;

    private RiverDirection() {
        this.isDiagonal = false;
    }

    private RiverDirection(boolean isDiagonal) {
        this.isDiagonal = isDiagonal;
    }

    public boolean isDiagonal() {
        return this.isDiagonal;
    }

}
