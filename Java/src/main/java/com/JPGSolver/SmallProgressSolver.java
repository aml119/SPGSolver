package com.JPGSolver;

import com.google.common.base.Stopwatch;
// import com.google.common.primitives.Ints;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.Queue;

/*
TODO
    -test whole thing - least equal is fucked
    -FUCKING COMMENT YOU MORON
    -remove unused functions
    -halve the size of measures

QUESTIONS
    -
*/


public class SmallProgressSolver implements Solver {

    private int highestPriority;
    private boolean debug = true;

    public void test()
    {
        int[] mi0 = {0,2,0,3,0,1};
        int[] mi1 = {0,3,0,4,0,0};
        int[] mi2 = {0,2,0,2,0,1};
        int[] mi3 = {0,1,0,4,0,1};
        int[] mi4 = {0,3,0,0,0,0};

        int[] mx = {0,3,0,4,0,1};

        Measure[] m = new Measure[5];

        m[0] = new Measure(5);
        m[0].setMeasure(mi0);
        //m[0].setTop(true);
        m[1] = new Measure(5);
        m[1].setMeasure(mi1);
        m[2] = new Measure(5);
        m[2].setMeasure(mi2);
        m[3] = new Measure(5);
        m[3].setMeasure(mi3);
        m[4] = new Measure(5);
        m[4].setMeasure(mi4);


        Node v = new Node();
        v.setIndex(0);
        v.setPlayer(0);
        v.setPriority(3);

        Node w1 = new Node();
        w1.setIndex(1);
        w1.setPlayer(1);
        w1.setPriority(2);
        Node w2 = new Node();
        w2.setIndex(2);
        w2.setPlayer(1);
        w2.setPriority(4);
        Node w3 = new Node();
        w3.setIndex(3);
        w3.setPlayer(0);
        w3.setPriority(1);
        Node w4 = new Node();
        w4.setIndex(4);
        w4.setPlayer(0);
        w4.setPriority(2);

        System.out.print("v: ");
        m[0].print();
        System.out.print("w1: ");
        m[1].print();
        System.out.print("w2: ");
        m[2].print();
        System.out.print("w3: ");
        m[3].print();
        System.out.print("w4: ");
        m[4].print();

        Measure mt = new Measure(5);
        Measure.setMax(mx);
        System.out.println("TESTING LEAST_ABOVE");
        while(!mt.isTop()) {
            mt.print();
            mt = Measure.leastAbove(mt, 5);
        }
        mt.print();
        System.out.println("FINISHED LEAST_ABOVE");

        // System.out.print("l(v, w1): ");
        // Measure res = singleLift(m, v, w1);
        // res.print();
        //
        // System.out.print("l(v, w2): ");
        // res = singleLift(m, v, w2);
        // res.print();
        //
        // System.out.print("l(w2, w3): ");
        // res = singleLift(m, w2, w3);
        // res.print();
        //
        // System.out.print("l(w2, w4): ");
        // res = singleLift(m, w2, w4);
        // res.print();


    }

    public int[][] win(Graph g) {
        System.out.println("solving with SmallProgressSolver");
        BitSet removed = new BitSet(g.length());

        // System.out.println(g.toString());
        highestPriority = g.maxPriority(removed);
        // System.out.println("highest priority: " + highestPriority);
        Measure.initMax(g, highestPriority);
        Measure.printMax();

        //initialise progress measures for all nodes
        Measure[] progressMeasures = new Measure[g.length()];
        for (int i = 0; i < g.length(); i++) {
            progressMeasures[i] = new Measure(highestPriority);
        }

        // create a queue of nodes to lift
        Queue<Node> q = new LinkedList<Node>();
        for (int i = 0; i < g.length(); i++) {
            // System.out.println(" [INIT] enqueue node " + i);
            q.add(g.info[i]);
        }

        // attempt to lift each node on the queue
        Measure liftedMeasure;
        Node next = q.poll();
        do {
            if (debug) System.out.println();
            if (debug) System.out.println();
            if (debug) System.out.print("   dequeue node " + next.getIndex() + ", m: ");
            if (debug) progressMeasures[next.getIndex()].print();
            liftedMeasure = lift(progressMeasures, next, g);
            if (debug) System.out.print("lifted measure: ");
            if (debug) liftedMeasure.print();
            if (debug) System.out.println();
            // if a lift is successful, add all predecessors of that node to the queue.
            if (!liftedMeasure.equals(progressMeasures[next.getIndex()], highestPriority + 1)) {
                TIntArrayList injNodeIndexes = next.getInj();
                for (int i = 0; i < injNodeIndexes.size(); i++) {
                    if (debug) System.out.println("enqueue node " + injNodeIndexes.get(i));
                    q.add(g.info[injNodeIndexes.get(i)]);
                }
                progressMeasures[next.getIndex()] = liftedMeasure;
            }
            next = q.poll();
        } while (next != null);

        // once lifting is complete, populate solution object with...
        TIntArrayList evenWinningSet = new TIntArrayList();
        TIntArrayList oddWinningSet = new TIntArrayList();
        // winning sets for each player
        for (int i = 0; i < g.length(); i++) {
            if (progressMeasures[i].isTop()) {
                oddWinningSet.add(i);
            } else {
                evenWinningSet.add(i);
            }
        }
        // winning strategies for each player
        int[][] res = new int[2][];
        res[0] = evenWinningSet.toArray();
        res[1] = oddWinningSet.toArray();

        return res;
    }




// returns a new measure for the node after lifting. may be the same as the current measure.
Measure lift(Measure[] progressMeasures, Node node, Graph g) {
    // get all nodes reached from outgoing edges and get the progressed measure
    TIntArrayList adjNodes = node.getAdj();
    Measure bestLiftedMeasure;
    Measure newMeasure;

    if (debug) System.out.println("CURRENTLY LIFTING");


    if (node.getPlayer() == 0) {
        // if node is even, choose the minimum progressed measure,
        bestLiftedMeasure = new Measure(highestPriority, true);
        if (debug) System.out.println("Lifting from a p0 node");
        for (int i = 0; i < adjNodes.size(); i++) {
            newMeasure = singleLift(progressMeasures, node, g.info[adjNodes.get(i)]);
            if (debug) System.out.print("singly lifted to ");
            if (debug) newMeasure.print();
            if (newMeasure.lessThan(bestLiftedMeasure, highestPriority)) {
                bestLiftedMeasure = newMeasure;
            }
            if (debug) System.out.print("current best ");
            if (debug) bestLiftedMeasure.print();
        }
    } else {
        bestLiftedMeasure = new Measure(highestPriority);
        // if node is odd, choose the maximum progressed measure.
        if (debug) System.out.println("Lifting from a p1 node");
        for (int i = 0; i < adjNodes.size(); i++) {
            newMeasure = singleLift(progressMeasures, node, g.info[adjNodes.get(i)]);
            if (debug) System.out.print("singly lifted to ");
            if (debug) newMeasure.print();
            if (newMeasure.greaterThan(bestLiftedMeasure, highestPriority)) {
                bestLiftedMeasure = newMeasure;
            }
            if (debug) System.out.print("current best ");
            if (debug) bestLiftedMeasure.print();
        }
    }
    return bestLiftedMeasure;
}

    // returns mu(to) if from is even, or least measure > mu(to) if from is odd.
    Measure singleLift(Measure[] progressMeasures, Node from, Node to)
    {
        Measure toMeasure = progressMeasures[to.getIndex()];
        Measure fromMeasure = progressMeasures[from.getIndex()];
        if (from.getPriority() % 2 == 0)   // then this is an even priority
        {
            if (!fromMeasure.lessThan(toMeasure, from.getPriority())) {
                if (debug) System.out.println("path: even, >= up to " + from.getPriority());
                return fromMeasure;
            }
            else {
                if (debug) System.out.println("path: even, < up to " + from.getPriority());
                return Measure.leastEqual(toMeasure, from.getPriority());
                // return toMeasure;
            }
        }
        else    // then this is an odd priority
        {
            if (fromMeasure.greaterThan(toMeasure, from.getPriority())) {
                if (debug) System.out.println("path: odd, > up to " + from.getPriority());
                return fromMeasure;
            } else {
                if (debug) System.out.println("path: odd, <= up to " + from.getPriority());
                return Measure.leastAbove(toMeasure, from.getPriority());
            }
        }
    }
}

class Measure {
    private boolean top;
    private static int[] max;
    private int[] measure;

    public boolean isTop() {
        return top;
    }

    public void setTop(boolean top) {
        this.top = top;
    }

    public Measure(int highestPriority, boolean top) {
        this.top = top;
        measure = new int[highestPriority + 1];
    }

    public Measure(int highestPriority) {
        top = false;
        measure = new int[highestPriority + 1];
    }

    public Measure(int[] measure) {
        this.measure = measure;
    }

    public Measure(int[] measure, boolean top) {
        this.measure = measure;
        this.top = top;
    }

    public Measure(Measure other) {
        System.arraycopy(other.getMeasure(), 0, this.measure, 0, other.getMeasure().length);
        this.top = other.isTop();
    }

    public int length() {
        return measure.length;
    }

    public int[] getMeasure() {
        return this.measure;
    }

    public void setMeasure(int[] m) {
        this.measure = m;
    }

    public void setMeasure(int i, int val) {
        measure[i] = val;
    }

    public static Measure leastAbove(Measure other, int pTrunc) {
        int[] newMeasure = new int[other.getMeasure().length];
        if (other.isTop()) return new Measure(newMeasure, true);
        System.arraycopy(other.getMeasure(), 0, newMeasure, 0, other.getMeasure().length);
        int index = pTrunc;
        boolean done = false;
        while (!done && index >= 0) {
            if (newMeasure[index] == max[index]) {
                newMeasure[index] = 0;
                index--;
            } else {
                newMeasure[index]++;
                done = true;
            }
        }
        if (index == -1) return new Measure(newMeasure, true);
        return new Measure(newMeasure);
    }

    public static Measure leastEqual(Measure other, int pTrunc) {
        Measure newMeasure = new Measure(other);
        if (other.isTop()) {
            newMeasure.setTop(true);
            return newMeasure;
        }
        int index = pTrunc + 1;
        for (int i = pTrunc + 1; i < other.length(); i++) {
            newMeasure.setMeasure(i, 0);
        }
        return newMeasure;
    }

    public static void setMax(int[] m)
    {
        max = m;
    }

    public static void initMax(Graph g, int highestPriority) {
        max = new int[highestPriority + 1];
        for (int i = 0; i < max.length; i++) {
            max[i] = 0;
        }
        for (int i = 0; i < g.info.length; i++) {
            int index = g.info[i].getPriority();
            if (index % 2 != 0) {
                max[index]++;
            }
        }
    }

    public static void printMax() {
        System.out.print("[" + max[0]);
        for (int i = 1; i < max.length; i++) {
            System.out.print(", " + max[i]);
        }
        System.out.println("]");
    }

    public void print() {
        System.out.print("[" + measure[0]);
        for (int i = 1; i < measure.length; i++) {
            System.out.print(", " + measure[i]);
        }
        if (this.top) System.out.println("](T)");
        else System.out.println("]");
    }

    public boolean equals(Measure other, int pTruncation)
    {
        if (this.top == other.isTop() && this.top) return true;
        else if (this.top != other.isTop()) return false;

        int[] otherMeasure = other.getMeasure();
        for (int i = 1; i <= pTruncation; i += 2)
        {
            if (otherMeasure[i] != measure[i]) return false;
        }
        return true;
    }

    public boolean lessThan(Measure other, int pTruncation)
    {
        if (this.top) return false;
        if (other.isTop()) return true;

        int[] otherMeasure = other.getMeasure();
        for (int i = 1; i <= pTruncation; i += 2)
        {
            if (otherMeasure[i] > measure[i]) return true;
            if (otherMeasure[i] < measure[i]) return false;
        }
        return false;
    }

    public boolean greaterThan(Measure other, int pTruncation)
    {
        if (other.isTop()) return false;
        if (this.top) return true;

        int[] otherMeasure = other.getMeasure();
        for (int i = 1; i <= pTruncation; i += 2)
        {
            if (otherMeasure[i] < measure[i]) return true;
            if (otherMeasure[i] > measure[i]) return false;
        }
        return false;
    }
}
