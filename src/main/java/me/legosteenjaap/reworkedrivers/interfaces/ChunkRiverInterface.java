package me.legosteenjaap.reworkedrivers.interfaces;

import me.legosteenjaap.reworkedrivers.RiverDirection;
import net.minecraft.server.level.WorldGenRegion;

import java.util.ArrayList;

public interface ChunkRiverInterface {

    void addRiverDirection(RiverDirection riverDirection);
    void removeRiverDirections();
    ArrayList<RiverDirection> getRiverDirections();
    boolean hasRiverDirections();
    boolean isSplit(WorldGenRegion worldGenRegion);
    void setRiverPoint(double riverPoint);
    double getRiverPoint();

}
