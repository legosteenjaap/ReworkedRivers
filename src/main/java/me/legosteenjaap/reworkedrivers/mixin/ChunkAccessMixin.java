package me.legosteenjaap.reworkedrivers.mixin;

import me.legosteenjaap.reworkedrivers.interfaces.ChunkRiverInterface;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChunkAccess.class)
public class ChunkAccessMixin implements ChunkRiverInterface {

    private double riverPoint;

    @Override
    public void setRiverPoint(double riverPoint) {
        this.riverPoint = riverPoint;
    }

    @Override
    public double getRiverPoint() {
        return this.riverPoint;
    }
}
