package me.legosteenjaap.reworkedrivers.interfaces;

import me.legosteenjaap.reworkedrivers.RiverBendType;
import me.legosteenjaap.reworkedrivers.RiverDirection;
import net.minecraft.server.level.WorldGenRegion;

import java.util.ArrayList;

public interface ChunkRiverInterface {

    void setRiverBendType(RiverBendType riverBendType);
    RiverBendType getRiverBendType();
    void addRiverDirection(RiverDirection riverDirection);
    void removeRiverDirections();
    ArrayList<RiverDirection> getRiverDirections();
    boolean hasRiverDirections();
    void setSplit(boolean hasSplit);
    boolean hasSplit();
    void setRiverPoint(int riverPoint);
    int getRiverPoint();

}
