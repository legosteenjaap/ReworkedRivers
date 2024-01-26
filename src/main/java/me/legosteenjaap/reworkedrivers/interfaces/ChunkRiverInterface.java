package me.legosteenjaap.reworkedrivers.interfaces;

import me.legosteenjaap.reworkedrivers.river.RiverBendType;
import me.legosteenjaap.reworkedrivers.river.RiverDirection;

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
