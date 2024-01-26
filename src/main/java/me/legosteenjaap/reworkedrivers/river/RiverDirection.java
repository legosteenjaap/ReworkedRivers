package me.legosteenjaap.reworkedrivers.river;

import net.minecraft.world.level.ChunkPos;

public enum RiverDirection {
    NORTH,
    EAST,
    SOUTH,
    WEST;

    /**
     * Moves one chunk away from a starting chunk with a certain direction.
     * @param chunkPos Current position of chunk
     * @param riverDirection Direction
     * @return New chunk one chunk away from current position of chunk with a certain direction
     */
    public static ChunkPos addDirectionToChunkPos(ChunkPos chunkPos, RiverDirection riverDirection) {
        return new ChunkPos(chunkPos.x + riverDirection.getXOffset(), chunkPos.z + riverDirection.getZOffset());
    }

    /**
     * Returns the opposite direction of the current riverDirection
     * @return Opposite river direction
     */
    public RiverDirection getOpposite () {
        switch (this) {
            case NORTH -> {
                return SOUTH;
            }
            case EAST -> {
                return WEST;
            }
            case SOUTH -> {
                return NORTH;
            }
            case WEST -> {
                return EAST;
            }
        }
        throw new AssertionError();
    }

    public int getXOffset() {
        switch (this) {
            case EAST -> {
                return 1;
            }
            case WEST -> {
                return -1;
            }
        }
        return 0;
    }

    public int getZOffset() {
        switch (this) {
            case SOUTH -> {
                return 1;
            }
            case NORTH -> {
                return -1;
            }
        }
        return 0;
    }

}
