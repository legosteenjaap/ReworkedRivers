package me.legosteenjaap.reworkedrivers.interfaces;

import me.legosteenjaap.reworkedrivers.RiverDirection;

import java.util.ArrayList;
import java.util.List;

public interface ChunkRiverInterface {

    void addRiverUpDirection(RiverDirection riverDirection);
    void addRiverDownDirection(RiverDirection riverDirection);
    ArrayList<RiverDirection> getRiverUpDirections();
    ArrayList<RiverDirection> getRiverDownDirections();
    void setRiverPoint(double riverPoint);
    double getRiverPoint();

}
