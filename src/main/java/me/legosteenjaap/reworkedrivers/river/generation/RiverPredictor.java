package me.legosteenjaap.reworkedrivers.river.generation;

import com.google.common.collect.Lists;
import me.legosteenjaap.reworkedrivers.interfaces.ChunkRiverInterface;
import me.legosteenjaap.reworkedrivers.river.RiverBendType;
import me.legosteenjaap.reworkedrivers.river.RiverDirection;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;

public class RiverPredictor {

    /**
     * Starts a new river branch (recursive)
     * @param worldGenRegion This object holds all the currently loaded neighbouring chunks which is needed for generation
     * @param currentChunkPos Current position of chunk
     */
    public static boolean startRiverBranch(WorldGenRegion worldGenRegion, ChunkPos currentChunkPos, RiverDirection lastRiverDirection, RiverBendType lastRiverBendType) {
        List<RiverDirection> bestPossibleNeighbors = getBestPossibleNeighbors(currentChunkPos, worldGenRegion);
        if (!worldGenRegion.hasChunk(currentChunkPos.x, currentChunkPos.z)) return false;
        ChunkRiverInterface currentRiverInterface = (ChunkRiverInterface) worldGenRegion.getChunk(currentChunkPos.x, currentChunkPos.z);
        if (!bestPossibleNeighbors.isEmpty()) {
            int possibleNeighborAmount = 0;
            for (RiverDirection riverDirection : bestPossibleNeighbors) {
                ChunkPos newChunkPos = RiverDirection.addDirectionToChunkPos(currentChunkPos, riverDirection);
                ChunkRiverInterface newRiverInterface = (ChunkRiverInterface) worldGenRegion.getChunk(newChunkPos.x, newChunkPos.z);
                if (newRiverInterface.hasRiverDirections()) continue;
                possibleNeighborAmount++;
            }
            if (possibleNeighborAmount > 1) currentRiverInterface.setSplit(true);
            int successfulBranches = 0;
            for (RiverDirection riverDirection : bestPossibleNeighbors) {
                ChunkPos newChunkPos = RiverDirection.addDirectionToChunkPos(currentChunkPos, riverDirection);
                ChunkRiverInterface newRiverInterface = (ChunkRiverInterface) worldGenRegion.getChunk(newChunkPos.x, newChunkPos.z);
                if (newRiverInterface.hasRiverDirections()) continue;
                newRiverInterface.addRiverDirection(riverDirection.getOpposite());
                newRiverInterface.setRiverBendType(lastRiverBendType.getOpposite());
                if (startRiverBranch(worldGenRegion, newChunkPos, riverDirection.getOpposite(), lastRiverBendType.getOpposite())) successfulBranches++;
            }
            if (successfulBranches == 0) {
                currentRiverInterface.setSplit(false);
                removeRiverBranch(worldGenRegion, currentRiverInterface, currentChunkPos);
                return false;
            }
        } else if (currentRiverInterface.getRiverPoint() < 80) {
            removeRiverBranch(worldGenRegion, currentRiverInterface, currentChunkPos);
            return false;
        }
        return true;
    }

    /**
     * Removes river-branch if it isn't able to reach a valid source destination. A valid source is determined by estimated height this makes sure that rivers flow from places that are high up. It stops when it encounters a chunk which has multiple river-pieces flowing into the chunk.
     * @param worldGenRegion This object holds all the currently loaded neighbouring chunks which is needed for generation
     * @param currentRiverInterface Interface which holds all the river information from a chunk
     * @param currentChunkPos Current position of chunk
     */
    private static void removeRiverBranch(WorldGenRegion worldGenRegion, ChunkRiverInterface currentRiverInterface, ChunkPos currentChunkPos) {
        while (true) {
            if (currentRiverInterface.hasSplit()) return;
            if (!currentRiverInterface.hasRiverDirections()) return;
            RiverDirection currentRiverDirection = currentRiverInterface.getRiverDirections().get(0);
            currentRiverInterface.removeRiverDirections();
            currentChunkPos = RiverDirection.addDirectionToChunkPos(currentChunkPos, currentRiverDirection);
            currentRiverInterface = (ChunkRiverInterface) worldGenRegion.getChunk(currentChunkPos.x, currentChunkPos.z);
        }
    }

    /**
     * List of directions from which the river possibly can flow from (goes upwards from drain to source). This method randomly decides if a river splits or not.
     * @param chunkPos Current position of chunk
     * @param worldGenRegion This object holds all the currently loaded neighbouring chunks which is needed for generation
     * @return List of directions from which the river possibly can flow from (goes upwards from drain to source)
     */
    private static List<RiverDirection> getBestPossibleNeighbors(ChunkPos chunkPos, WorldGenRegion worldGenRegion) {
        ChunkRiverInterface currentRiverInterface = (ChunkRiverInterface) worldGenRegion.getChunk(chunkPos.x, chunkPos.z);
        int currentRiverPoint = currentRiverInterface.getRiverPoint();
        ArrayList<RiverDirection> bestPossibleNeighbors = new ArrayList<>();
        for (RiverDirection riverDirection : RiverDirection.values()) {
            ChunkPos checkingChunkPos = RiverDirection.addDirectionToChunkPos(chunkPos, riverDirection);
            if (!worldGenRegion.hasChunk(checkingChunkPos.x, checkingChunkPos.z)) {
                break;
            }
            ChunkRiverInterface checkingRiverInterface = (ChunkRiverInterface) worldGenRegion.getChunk(checkingChunkPos.x, checkingChunkPos.z);
            int checkingRiverPoint = checkingRiverInterface.getRiverPoint();
            if (checkingRiverPoint >= currentRiverPoint && checkingRiverPoint > worldGenRegion.getSeaLevel() - 5 && !checkingRiverInterface.hasRiverDirections()) {
                bestPossibleNeighbors.add(riverDirection);
            }
        }
        if (bestPossibleNeighbors.isEmpty()) return Lists.newArrayList();

        //At the end this method randomly decides if a river splits or not.
        return worldGenRegion.getRandom().nextFloat() < 0.5 ? bestPossibleNeighbors : Lists.newArrayList(bestPossibleNeighbors.get(0));
    }

}
