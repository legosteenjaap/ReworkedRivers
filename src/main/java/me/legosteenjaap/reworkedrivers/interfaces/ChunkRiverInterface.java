package me.legosteenjaap.reworkedrivers.interfaces;

import me.legosteenjaap.reworkedrivers.RiverDirection;

import java.util.ArrayList;
import java.util.List;

public interface ChunkRiverInterface {

    void addRiverUpDirection(RiverDirection riverDirection);
    ArrayList<RiverDirection> getRiverUpDirections();
    void setRiverPoint(double riverPoint);
    double getRiverPoint();

}
