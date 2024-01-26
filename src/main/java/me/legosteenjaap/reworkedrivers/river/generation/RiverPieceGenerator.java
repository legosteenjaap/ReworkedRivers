package me.legosteenjaap.reworkedrivers.river.generation;

import me.legosteenjaap.reworkedrivers.interfaces.ChunkRiverInterface;
import me.legosteenjaap.reworkedrivers.river.RiverBendType;
import me.legosteenjaap.reworkedrivers.river.RiverDirection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

import static me.legosteenjaap.reworkedrivers.ReworkedRivers.DEBUG_HEIGHT;

public class RiverPieceGenerator {

    /**
     * This method places all the blocks for a river in a specified chunk with a specified direction
     * @param worldGenRegion This object holds all the currently loaded neighbouring chunks which is needed for generation
     * @param chunkAccess Chunk where method starts generating from
     * @param direction Direction in which the river generates
     */
    public static void generateRiverPiece(WorldGenRegion worldGenRegion, ChunkAccess chunkAccess, RiverDirection direction, RiverBendType riverBendType, boolean isNoiseBased) {
        ChunkPos chunkPos = chunkAccess.getPos();
        int startHeight = getStartHeight(worldGenRegion, chunkAccess, isNoiseBased);
        int currentYlevel = startHeight;
        ChunkPos otherChunkPos = RiverDirection.addDirectionToChunkPos(chunkPos, direction);
        int otherChunkYLevel = getStartHeight(worldGenRegion, worldGenRegion.getChunk(otherChunkPos.x, otherChunkPos.z), isNoiseBased);
        int width = 0;
        if (otherChunkYLevel > currentYlevel) return;

        //The river height is decided by checking if the terrain around the river is lower than the river
        boolean isSouthOrEast = direction == RiverDirection.SOUTH || direction == RiverDirection.EAST;
        boolean isNorthOrSouth = direction == RiverDirection.NORTH || direction == RiverDirection.SOUTH;

        //To make sure that it feels like waterfalls are carved into the terrain, terrain height is always compared to the terrain one block forward. This variable is used to determine what forward is.
        int backwardsMultiplier = isSouthOrEast ? -1 : 1;
        Integer lastOffset = null;
        for (int z = 0; z <= 17; z++) {
            BlockPos blockPos;
            int offset;
            switch (riverBendType) {
                case BOTH -> offset = (int) ((Math.sin((float) z / 16 * Math.PI * 2) * 2));
                case RIGHT -> offset = (int) ((Math.sin((float) z / 16 * Math.PI) * 6));
                case LEFT -> offset = (int) ((Math.sin(((float) z / 16 + 1) * Math.PI) * 6));
                default -> offset = 0;
            }
            if (direction == RiverDirection.SOUTH || direction == RiverDirection.WEST) offset = -offset;
            if (lastOffset == null) lastOffset = offset;
            int startX = 0;
            switch (riverBendType) {
                case LEFT -> startX = isNorthOrSouth ? 7 : 8;
                case RIGHT -> startX = isNorthOrSouth ? 8 : 7;
            }
            boolean hadHeightDifference = false;
            if (isNorthOrSouth) {
                BlockPos startPos = new BlockPos(chunkPos.getBlockX(startX), currentYlevel, chunkPos.getBlockZ(isSouthOrEast ? 7 : 8));
                blockPos = isSouthOrEast ? startPos.offset(offset, 0, z + 1) : startPos.offset(offset, 0, -z - 1);
                int height = getCurrentHeight(worldGenRegion, blockPos, true, width);
                if (DEBUG_HEIGHT) {
                    height = startHeight - z * 2;
                }
                if (z == 16) {
                    height = otherChunkYLevel;
                }
                if (height != otherChunkYLevel && (height <= worldGenRegion.getSeaLevel() - 1 || height <= otherChunkYLevel)) {
                    height = otherChunkYLevel;
                }
                if (currentYlevel > height) {
                    setFlowingRiverBlock(worldGenRegion, startPos.getX() + lastOffset, blockPos.getZ() + backwardsMultiplier, currentYlevel, height, width, direction);
                    currentYlevel = height;
                    blockPos = blockPos.atY(currentYlevel);
                    hadHeightDifference = true;
                }
            } else {
                //switches x and z if direction is on the northwest axis
                BlockPos startPos = new BlockPos(chunkPos.getBlockX(isSouthOrEast ? 7 : 8), currentYlevel, chunkPos.getBlockZ(startX));
                blockPos = isSouthOrEast ? startPos.offset(z + 1, 0, offset) : startPos.offset(-z - 1, 0, offset);
                int height = getCurrentHeight(worldGenRegion, blockPos, false, width);
                if (DEBUG_HEIGHT) height = startHeight - z * 2;
                if (z == 16) {
                    height = otherChunkYLevel;
                }
                if (height != otherChunkYLevel && (height <= worldGenRegion.getSeaLevel() - 1 || height <= otherChunkYLevel)) {
                    height = otherChunkYLevel;
                }
                if (currentYlevel > height) {
                    setFlowingRiverBlock(worldGenRegion, blockPos.getX() + backwardsMultiplier, startPos.getZ() + lastOffset, currentYlevel, height, width, direction);
                    currentYlevel = height;
                    blockPos = blockPos.atY(currentYlevel);
                    hadHeightDifference = true;
                }
            }

            if (lastOffset < offset) {
                setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-offset + lastOffset - 1, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, -offset + lastOffset - 1));
                setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(1, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, 1));
                for (int i = 0; i <= offset - lastOffset; i++) {
                    setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-i, 0, backwardsMultiplier + 1) : blockPos.offset(backwardsMultiplier + 1, 0, -i));
                    setRiverBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-i, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, -i), width, direction, hadHeightDifference);
                    setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-i, 0, backwardsMultiplier - 1) : blockPos.offset(backwardsMultiplier - 1, 0, -i));
                }
            } else if (lastOffset > offset) {
                setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-1, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, -1));
                setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-offset + lastOffset + 1, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, -offset + lastOffset + 1));
                for (int i = 0; i >= offset - lastOffset; i--) {
                    setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-i, 0, backwardsMultiplier + 1) : blockPos.offset(backwardsMultiplier + 1, 0, -i));
                    setRiverBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-i, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, -i), width, direction, hadHeightDifference);
                    setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-i, 0, backwardsMultiplier - 1) : blockPos.offset(backwardsMultiplier - 1, 0, -i));
                }
            } else {
                setRiverBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(0, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, 0), width, direction, hadHeightDifference);
                setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(-1, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, -1));
                setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(1, 0, backwardsMultiplier) : blockPos.offset(backwardsMultiplier, 0, 1));
                setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(0, 0, backwardsMultiplier - 1) : blockPos.offset(backwardsMultiplier - 1, 0, 0));
                setSupportBlock(worldGenRegion, isNorthOrSouth ? blockPos.offset(0, 0, backwardsMultiplier + 1) : blockPos.offset(backwardsMultiplier + 1, 0, 0));
            }
            lastOffset = offset;
        }

    }

    /**
     * Places one water block at a position and makes sure that blocks above it are removed.
     * @param worldGenRegion This object holds all the currently loaded neighbouring chunks which is needed for generation
     * @param blockPos Position at which the block is to be placed
     */
    private static void setRiverBlock(WorldGenRegion worldGenRegion, BlockPos blockPos, int width, RiverDirection riverDirection, boolean hadHeightDifference) {
        if (!worldGenRegion.hasChunkAt(blockPos)) throw new AssertionError();
        BlockState blockState = Blocks.WATER.defaultBlockState();
        if (riverDirection.getXOffset() == 0) {
            for (int i = -width; i <= width; i++) {
                BlockPos changedBlockPos = blockPos.offset(i, 0, 0);
                worldGenRegion.setBlock(changedBlockPos, blockState, 0);
                setSupportBlock(worldGenRegion, changedBlockPos.below());
                for(int y = 1; y <= 3; y++) {
                    if (worldGenRegion.getBlockState(changedBlockPos.atY(changedBlockPos.getY() + y)).getBlock() != blockState.getBlock()) worldGenRegion.setBlock(changedBlockPos.atY(changedBlockPos.getY() + y), Blocks.AIR.defaultBlockState(), 0);
                }
            }
            if (!hadHeightDifference) {
                for (int i = 0; i <= 3; i++) {
                    BlockPos changedBlockPos = blockPos.offset(width + i, 0, 0);
                    BlockPos changedBlockPos_ = blockPos.offset(-width - i, 0, 0);
                    for (int y = (int) (1 + 0.25 * Math.pow(i, 2)); y <= 3; y++) {
                        if (worldGenRegion.getBlockState(changedBlockPos.atY(changedBlockPos.getY() + y)).getBlock() != blockState.getBlock())
                            worldGenRegion.setBlock(changedBlockPos.atY(changedBlockPos.getY() + y), Blocks.AIR.defaultBlockState(), 0);
                        if (worldGenRegion.getBlockState(changedBlockPos_.atY(changedBlockPos_.getY() + y)).getBlock() != blockState.getBlock())
                            worldGenRegion.setBlock(changedBlockPos_.atY(changedBlockPos_.getY() + y), Blocks.AIR.defaultBlockState(), 0);
                    }
                }
            }
        } else {
            for (int i = -width; i <= width; i++) {
                BlockPos changedBlockPos = blockPos.offset(0, 0, i);
                worldGenRegion.setBlock(changedBlockPos, blockState, 0);
                setSupportBlock(worldGenRegion, changedBlockPos.below());
                for(int y = 1; y <= 3; y++) {
                    if (worldGenRegion.getBlockState(changedBlockPos.atY(changedBlockPos.getY() + y)).getBlock() != blockState.getBlock()) worldGenRegion.setBlock(changedBlockPos.atY(changedBlockPos.getY() + y), Blocks.AIR.defaultBlockState(), 0);
                }
            }
            if (!hadHeightDifference) {
                for (int i = 0; i <= 3; i++) {
                    BlockPos changedBlockPos = blockPos.offset(0, 0, width + i);
                    BlockPos changedBlockPos_ = blockPos.offset(0, 0, -width - i);
                    for (int y = (int) (1 + 0.5 * Math.pow(i, 2)); y <= 3; y++) {
                        if (worldGenRegion.getBlockState(changedBlockPos.atY(changedBlockPos.getY() + y)).getBlock() != blockState.getBlock())
                            worldGenRegion.setBlock(changedBlockPos.atY(changedBlockPos.getY() + y), Blocks.AIR.defaultBlockState(), 0);
                        if (worldGenRegion.getBlockState(changedBlockPos_.atY(changedBlockPos_.getY() + y)).getBlock() != blockState.getBlock())
                            worldGenRegion.setBlock(changedBlockPos_.atY(changedBlockPos_.getY() + y), Blocks.AIR.defaultBlockState(), 0);
                    }
                }
            }
        }


    }

    /**
     * Method used to place water blocks that flow downwards when a river's height drops
     * @param worldGenRegion This object holds all the currently loaded neighbouring chunks which is needed for generation
     * @param x xPosition
     * @param z zPosition
     * @param lastYLevel The current height level from which the river flows downwards
     * @param newYLevel The new height level where the river flows towards
     */
    private static void setFlowingRiverBlock(WorldGenRegion worldGenRegion, int x, int z, int lastYLevel, int newYLevel, int width, RiverDirection riverDirection) {
        if (!worldGenRegion.hasChunkAt(new BlockPos(x, lastYLevel, z))) throw new AssertionError();
        if (lastYLevel < newYLevel) throw new AssertionError();
        if (riverDirection.getXOffset() == 0) {
            for (int i = -width; i <= width; i++) {
                worldGenRegion.setBlock(new BlockPos(x + i, lastYLevel, z), Fluids.FLOWING_WATER.getFlowing(7, false).createLegacyBlock(), 0);
                for (int y  = lastYLevel - 1; y > newYLevel; y--) {
                    worldGenRegion.setBlock(new BlockPos(x + i, y, z), Fluids.FLOWING_WATER.getFlowing(8, true).createLegacyBlock(), 0);
                }              }
        } else {
            for (int i = -width; i <= width; i++) {
                worldGenRegion.setBlock(new BlockPos(x, lastYLevel, z + i), Fluids.FLOWING_WATER.getFlowing(7, false).createLegacyBlock(), 0);
                for (int y  = lastYLevel - 1; y > newYLevel; y--) {
                    worldGenRegion.setBlock(new BlockPos(x, y, z + i), Fluids.FLOWING_WATER.getFlowing(8, true).createLegacyBlock(), 0);
                }
            }
        }

    }

    /**
     * Places a simple support block for a river used to make sure that water doesn't flow on air
     * @param worldGenRegion This object holds all the currently loaded neighbouring chunks which is needed for generation
     * @param blockPos Position at which the block is to be placed
     */
    private static void setSupportBlock(WorldGenRegion worldGenRegion, BlockPos blockPos) {
        int height = worldGenRegion.getHeight(Heightmap.Types.WORLD_SURFACE_WG, blockPos.getX(), blockPos.getZ());
        if (worldGenRegion.getBlockState(blockPos).getBlock() == Blocks.AIR && height <= blockPos.getY()) {
            for (int i = height; i <= blockPos.getY(); i++) {
                worldGenRegion.setBlock(blockPos.atY(i), Blocks.STONE.defaultBlockState(), 1);
            }
        }
    }

    /**
     * Gets the height at which a river piece in a certain chunks needs to start
     * @param worldGenRegion Used to determine sea level
     * @param chunkAccess Chunk where river piece starts
     * @return Starting height for river piece
     */
    private static int getStartHeight(WorldGenRegion worldGenRegion, ChunkAccess chunkAccess, boolean isNoiseBased) {
        if (DEBUG_HEIGHT) {
            if (((ChunkRiverInterface)chunkAccess).hasRiverDirections()) {
                return 128;
            } else {
                return 96;
            }
        }
        if (!isNoiseBased) return worldGenRegion.getHeight(Heightmap.Types.WORLD_SURFACE_WG, chunkAccess.getPos().getMiddleBlockX(), chunkAccess.getPos().getMiddleBlockZ()) - 1;
        int riverPoint = ((ChunkRiverInterface)chunkAccess).getRiverPoint() - 1;
        return Math.max(riverPoint, worldGenRegion.getSeaLevel() - 1);
    }

    /**
     * Checks the height for a certain piece of terrain. It checks an area consisting of 4 blocks (2 for water and 2 around water) and returns the lowest value.
     * @param worldGenRegion This object holds the currently loaded and usable chunks which is needed to determine height
     * @param startPos BlockPos from which the method starts scanning
     * @param isNorthOrSouth Direction in which terrain needs to be scanned
     * @return Height for terrain piece
     */
    private static int getCurrentHeight(WorldGenRegion worldGenRegion, BlockPos startPos, boolean isNorthOrSouth, int width) {
        Integer lowestY = null;
        int extraSearchRange = 2;
        if (isNorthOrSouth) {
            for (int i = -width - extraSearchRange; i <= width + extraSearchRange; i++) {
                int height = worldGenRegion.getHeight(Heightmap.Types.WORLD_SURFACE_WG, startPos.getX() + i, startPos.getZ()) - 1;
                if (lowestY == null || lowestY > height) lowestY = height;
            }
        } else {
            for (int i = -width - extraSearchRange; i <= width + extraSearchRange; i++) {
                int height = worldGenRegion.getHeight(Heightmap.Types.WORLD_SURFACE_WG, startPos.getX(), startPos.getZ() + i) - 1;
                if (lowestY == null || lowestY > height) lowestY = height;
            }
        }
        return lowestY;
    }


}
