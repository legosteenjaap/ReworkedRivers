package me.legosteenjaap.reworkedrivers.interfaces;

import me.legosteenjaap.reworkedrivers.RiverDirection;

import java.util.ArrayList;
import java.util.List;

public interface ChunkRiverInterface {

    void addRiverDirection(RiverDirection riverDirection);
    ArrayList<RiverDirection> getRiverDirections();
    void setRiverPoint(double riverPoint);
    double getRiverPoint();

}
