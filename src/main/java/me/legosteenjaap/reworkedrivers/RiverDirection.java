package me.legosteenjaap.reworkedrivers;

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
        switch (riverDirection) {
            case NORTH -> {
                return new ChunkPos(chunkPos.x, chunkPos.z - 1);
            }
            case EAST -> {
                return new ChunkPos(chunkPos.x + 1, chunkPos.z);
            }
            case SOUTH -> {
                return new ChunkPos(chunkPos.x, chunkPos.z + 1);
            }
            case WEST -> {
                return new ChunkPos(chunkPos.x - 1, chunkPos.z);
            }
        }
        throw new AssertionError();
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

}
