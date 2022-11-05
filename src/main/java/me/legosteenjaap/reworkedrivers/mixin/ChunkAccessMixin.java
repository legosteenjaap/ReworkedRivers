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

    private double riverPoint;
    private ArrayList<RiverDirection> riverDirections = new ArrayList<>();

    @Override
    public void addRiverDirection(RiverDirection riverDirection) {
        this.riverDirections.add(riverDirection);
    }

    @Override
    public void removeRiverDirections() {
        this.riverDirections.clear();
    }

    @Override
    public ArrayList<RiverDirection> getRiverDirections() {
        return this.riverDirections;
    }

    @Override
    public boolean hasRiverDirections() {
        return !this.riverDirections.isEmpty();
    }

    @Override
    public boolean isSplit(WorldGenRegion worldGenRegion) {
        int splits = 0;
        for (RiverDirection riverDirection : RiverDirection.values()) {
            ChunkPos chunkPos = RiverDirection.addDirectionToChunkPos(this.getPos(), riverDirection);
            ChunkRiverInterface riverInterface = (ChunkRiverInterface) worldGenRegion.getChunk(chunkPos.x, chunkPos.z);
            if(riverInterface.getRiverDirections().contains(riverDirection.getOpposite())) splits++;
        }
        return splits > 1;
    }

    @Override
    public void setRiverPoint(double riverPoint) {
        this.riverPoint = riverPoint;
    }

    @Override
    public double getRiverPoint() {
        return this.riverPoint;
    }
}
