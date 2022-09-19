package me.legosteenjaap.reworkedrivers.mixin;

import me.legosteenjaap.reworkedrivers.RiverDirection;
import me.legosteenjaap.reworkedrivers.interfaces.ChunkRiverInterface;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;

@Mixin(ChunkAccess.class)
public class ChunkAccessMixin implements ChunkRiverInterface {

    private double riverPoint;
    private ArrayList<RiverDirection> riverUpDirections = new ArrayList<>();
    private ArrayList<RiverDirection> riverDownDirections = new ArrayList<>();

    @Override
    public void addRiverUpDirection(RiverDirection riverDirection) {
        riverUpDirections.add(riverDirection);
    }

    @Override
    public ArrayList<RiverDirection> getRiverUpDirections() {
        return riverUpDirections;
    }

    @Override
    public void addRiverDownDirection(RiverDirection riverDirection) {
        riverDownDirections.add(riverDirection);
    }

    @Override
    public ArrayList<RiverDirection> getRiverDownDirections() {
        return riverDownDirections;
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
