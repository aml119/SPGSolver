package com.JPGSolver;

import gnu.trove.list.array.TIntArrayList;

import java.io.*;
import java.util.BitSet;
import java.util.Optional;
import java.util.stream.Stream;

public class Node {
    private int index = -1;
    private int player = -1;
    private int priority = -1;
    private final TIntArrayList adj = new TIntArrayList();
    private final TIntArrayList inj = new TIntArrayList();


    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getPlayer() {
        return player;
    }

    public void setPlayer(int player) {
        this.player = player;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public void addAdj(int destination) {
        adj.add(destination);
    }

    public void addInj(int origin) {
        synchronized (this.inj) {
            inj.add(origin);
        }
    }

    public TIntArrayList getAdj() {
        return adj;
    }

    public TIntArrayList getInj() {
        return inj;
    }

    // public void setAdj(TIntArrayList adj) {
    //     this.adj = adj;
    // }
    //
    // public void setInj(TIntArrayList inj) {
    //     this.inj = inj;
    // }

    public String toString(){
        if (player == 0){
            return "(" + String.format("%02d", priority) + ")";
        } else {
            return "[" + String.format("%02d", priority) + "]";
        }
    }
}
