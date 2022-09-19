package me.legosteenjaap.reworkedrivers;

import net.minecraft.world.level.ChunkPos;
import org.checkerframework.checker.units.qual.C;

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

    public static ChunkPos addDirectionToChunkPos(ChunkPos chunkPos, RiverDirection riverDirection) {
        switch (riverDirection) {
            case NORTH -> {
                return new ChunkPos(chunkPos.x, chunkPos.z - 1);
            }
            case NORTHEAST -> {
                return new ChunkPos(chunkPos.x + 1, chunkPos.z - 1);
            }
            case EAST -> {
                return new ChunkPos(chunkPos.x + 1, chunkPos.z);
            }
            case SOUTHEAST -> {
                return new ChunkPos(chunkPos.x + 1, chunkPos.z + 1);
            }
            case SOUTH -> {
                return new ChunkPos(chunkPos.x, chunkPos.z + 1);
            }
            case SOUTHWEST -> {
                return new ChunkPos(chunkPos.x - 1, chunkPos.z + 1);
            }
            case WEST -> {
                return new ChunkPos(chunkPos.x - 1, chunkPos.z);
            }
            case NORTHWEST -> {
                return new ChunkPos(chunkPos.x - 1, chunkPos.z - 1);
            }
        }
        throw new AssertionError();
    }

    public RiverDirection getOpposite () {
        switch (this) {
            case NORTH -> {
                return SOUTH;
            }
            case NORTHEAST -> {
                return SOUTHWEST;
            }
            case EAST -> {
                return WEST;
            }
            case SOUTHEAST -> {
                return NORTHWEST;
            }
            case SOUTH -> {
                return NORTH;
            }
            case SOUTHWEST -> {
                return NORTHEAST;
            }
            case WEST -> {
                return EAST;
            }
            case NORTHWEST -> {
                return SOUTHEAST;
            }
        }
        throw new AssertionError();
    }

}
