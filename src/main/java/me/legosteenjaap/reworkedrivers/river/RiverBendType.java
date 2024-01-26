package me.legosteenjaap.reworkedrivers.river;

public enum RiverBendType {
    NONE,
    BOTH,
    LEFT,
    RIGHT;

    public RiverBendType getOpposite() {
        switch (this) {
            case NONE -> {
                return NONE;
            }
            case BOTH -> {
                return BOTH;
            }
            case LEFT -> {
                return RIGHT;
            }
            case RIGHT -> {
                return LEFT;
            }
        }
        return NONE;
    }

}