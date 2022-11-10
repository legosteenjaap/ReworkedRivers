package me.legosteenjaap.reworkedrivers.mixin;

import me.legosteenjaap.reworkedrivers.RiverDirection;
import me.legosteenjaap.reworkedrivers.interfaces.ChunkRiverInterface;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;

@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin implements ChunkRiverInterface {

    @Shadow public abstract ChunkPos getPos();

    //Chunk height estimation for river generation
    private int riverPoint;
    private final ArrayList<RiverDirection> riverDirections = new ArrayList<>();
    private boolean hasSplit = false;

    /**
     * Adds a direction for a river-piece to the chunk.
     * @param riverDirection The direction of the river-piece.
     */
    @Override
    public void addRiverDirection(RiverDirection riverDirection) {
        this.riverDirections.add(riverDirection);
    }

    /**
     * Removes the river-piece directions from this chunk
     */
    @Override
    public void removeRiverDirections() {
        this.riverDirections.clear();
    }

    /**
     * Gets all the current directions for river-pieces
     * @return Directions for river-pieces
     */
    @Override
    public ArrayList<RiverDirection> getRiverDirections() {
        return this.riverDirections;
    }

    /**
     * Checks if this chunk has directions for river-pieces
     * @return -
     */
    @Override
    public boolean hasRiverDirections() {
        return !this.riverDirections.isEmpty();
    }

    /**
     * Saves that a split occurred in this chunk
     */
    @Override
    public void setSplit() {
        hasSplit = true;
    }

    /**
     * Checks if a river-branch split occurred in this chunk.
     * @return Checks if split occurred.
     */
    @Override
    public boolean hasSplit() {
        return hasSplit;
    }

    /**
     * Sets the river point
     * @param riverPoint Terrain height estimation for rivers
     */
    @Override
    public void setRiverPoint(int riverPoint) {
        this.riverPoint = riverPoint;
    }

    /**
     * Returns the river point
     * @return Current river point
     */
    @Override
    public int getRiverPoint() {
        return this.riverPoint;
    }
}
