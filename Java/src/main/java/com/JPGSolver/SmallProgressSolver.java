package com.JPGSolver;

import com.google.common.base.Stopwatch;
// import com.google.common.primitives.Ints;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.Queue;

public class SmallProgressSolver implements Solver {

    private int highestPriority;

    /*
        the small progress measures algorithm works on a slightly different
        definition of parity games, wherein the two players are competing
        for the smallest priority rather than the largest. Therefore the
        graph needs preprocessing so that priorities are changed, so that the
        largest priorities become the smallest and vice versa, maintaining the
        parity of each priority.
        This is done by effectively mirroring the priorities around an even
        number greater than the highestPriority (or equal to), then reducing the
        priorities st. the smallest is 0 or 1.
    */
    public Graph preprocessGraph(Graph other) {
        // choose lowest even number >= highest priority
        int mirror = highestPriority + (highestPriority % 2);
        // clone the graph
        Graph g = new Graph(other);
        // change priority of each node
        for (int i = 0; i < g.length(); i++) {
            g.info[i].setPriority(mirror - g.info[i].getPriority());
        }
        App.log_debug("Original Graph:");
        App.log_debug(other.toString());
        App.log_debug("Preprocessed Graph:");
        App.log_debug(g.toString());
        return g;
    }

    /*
        function called to solve the graph, returns the solution.
        Solution is 2 arrays, one for the winning region of player 0 (int[0][])
        and one for the winning region of player 1 (int[1][])
    */
    public int[][] win(Graph g) {
        App.log_verbose("Solving with SmallProgressSolver");

        // find the highest priority, maxPriority function is weird so needs
        // a bitset
        // the bitset will be used to determine what is in the q quickly
        BitSet nodesPresent = new BitSet(g.length());
        highestPriority = g.maxPriority(nodesPresent);


        g = preprocessGraph(g);
        // calculate and set the maximum progress measure
        initMax(g, highestPriority);
        App.log_debug(maxToString());

        //initialise progress measures for all nodes
        Measure[] progressMeasures = new Measure[g.length()];
        for (int i = 0; i < g.length(); i++) {
            progressMeasures[i] = new Measure(highestPriority);
        }

        // create a queue of nodes to lift
        // q is initially full so set all bits of bitset to true
        Queue<Node> q = new LinkedList<Node>();
        for (int i = 0; i < g.length(); i++) {
            q.add(g.info[i]);
            nodesPresent.set(i);
        }

        int numDequeues = 0;

        // attempt to lift each node on the queue
        Measure liftedMeasure;
        Node next;
        while(!q.isEmpty()) {
            numDequeues++;
            // get the next node to try to lift
            next = q.poll();
            nodesPresent.clear(next.getIndex());

            liftedMeasure = lift(progressMeasures, next, g);

            App.log_debug("");
            App.log_verbose("   dequeue node " + next.getIndex());
            App.log_debug("m: " + progressMeasures[next.getIndex()].toString());
            App.log_debug("lifted measure: ");
            App.log_debug(liftedMeasure.toString());
            App.log_debug("");

            // if a lift is successful, add all predecessors of that node to the queue.
            if (!liftedMeasure.equals(progressMeasures[next.getIndex()], highestPriority)) {
                TIntArrayList injNodeIndexes = next.getInj();
                for (int i = 0; i < injNodeIndexes.size(); i++) {
                    int toAdd = injNodeIndexes.get(i);
                    if (!nodesPresent.get(toAdd)) {
                        App.log_debug("enqueue node " + toAdd);
                        nodesPresent.set(toAdd);
                        q.add(g.info[toAdd]);
                    }
                }
                progressMeasures[next.getIndex()] = liftedMeasure;
            }
        }

        System.out.println("numDequeues: " + numDequeues);

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




    // returns a new measure for the node after lifting. may be the same as the
    // current measure.
    Measure lift(Measure[] progressMeasures, Node node, Graph g) {
        // get all nodes reached from outgoing edges and get the progressed measure
        TIntArrayList adjNodes = node.getAdj();
        Measure bestLiftedMeasure;
        Measure newMeasure;

        if (node.getPlayer() == 0) {
            // if node is even, choose the minimum progressed measure,
            // therefore start with the top measure
            bestLiftedMeasure = new Measure(highestPriority, true);
            App.log_debug("Lifting from a p0 node");
            for (int i = 0; i < adjNodes.size(); i++) {
                newMeasure = singleLift(progressMeasures, node, g.info[adjNodes.get(i)]);
                if (newMeasure.lessThan(bestLiftedMeasure, highestPriority)) {
                    bestLiftedMeasure = newMeasure;
                }
                App.log_debug("singly lifted to " + newMeasure.toString());
                App.log_debug("current best " + bestLiftedMeasure.toString());
            }
        } else {
            // if node is odd, choose the maximum progressed measure.
            // therefore start with the minimum measure
            bestLiftedMeasure = new Measure(highestPriority);
            App.log_debug("Lifting from a p1 node");
            for (int i = 0; i < adjNodes.size(); i++) {
                newMeasure = singleLift(progressMeasures, node, g.info[adjNodes.get(i)]);
                if (newMeasure.greaterThan(bestLiftedMeasure, highestPriority)) {
                    bestLiftedMeasure = newMeasure;
                }
                App.log_debug("singly lifted to " + newMeasure.toString());
                App.log_debug("current best " + bestLiftedMeasure.toString());
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
                App.log_debug("path: even, >= up to " + from.getPriority());
                return fromMeasure;
            }
            else {
                App.log_debug("path: even, < up to " + from.getPriority());
                return toMeasure.leastEqual(from.getPriority(), highestPriority);
            }
        }
        else    // then this is an odd priority
        {
            if (fromMeasure.greaterThan(toMeasure, from.getPriority())) {
                App.log_debug("path: odd, > up to " + from.getPriority());
                return fromMeasure;
            } else {
                App.log_debug("path: odd, <= up to " + from.getPriority());
                return toMeasure.leastAbove(from.getPriority());
            }
        }
    }

    static int[] max;


    public static void setMax(int[] m) {
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

    public static String maxToString() {
        String s = "[" + max[0];
        for (int i = 1; i < max.length; i++) {
            s += ", " + max[i];
        }
        s += "]";
        return s;
    }


    private class Measure {
        private boolean top;
        // the largest element that is not top
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

        public Measure(Measure other, int highestPriority) {
            measure = new int[highestPriority + 1];
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

        // returns the smallest element that is greater than the other measure
        public Measure leastAbove(int pTrunc) {
            int[] newMeasure = new int[measure.length];
            if (isTop()) return new Measure(newMeasure, true);
            System.arraycopy(measure, 0, newMeasure, 0, measure.length);
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

        // returns the smallest possible measure that is equal to the other measure
        // i.e copies the other measure, and sets all elements past pTrunc (which
        // is the element up to which we compare) to zero.
        public Measure leastEqual(int pTrunc, int highestPriority) {
            Measure newMeasure = new Measure(this, highestPriority);
            if (isTop()) {
                newMeasure.setTop(true);
                return newMeasure;
            }
            int index = pTrunc + 1;
            for (int i = pTrunc + 1; i < this.length(); i++) {
                newMeasure.setMeasure(i, 0);
            }
            return newMeasure;
        }

        public String toString() {
            String s = "[" + measure[0];
            for (int i = 1; i < measure.length; i++) {
                s += ", " + measure[i];
            }
            s += "]";
            return s;
        }

        public boolean equals(Measure other, int pTruncation) {
            if (this.top == other.isTop() && this.top) return true;
            else if (this.top != other.isTop()) return false;

            int[] otherMeasure = other.getMeasure();
            for (int i = 1; i <= pTruncation; i += 2)
            {
                if (otherMeasure[i] != measure[i]) return false;
            }
            return true;
        }

        public boolean lessThan(Measure other, int pTruncation) {
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

        public boolean greaterThan(Measure other, int pTruncation) {
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
}
