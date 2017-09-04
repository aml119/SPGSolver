package com.JPGSolver;

import com.google.common.base.Stopwatch;
// import com.google.common.primitives.Ints;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.array.TIntArrayList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.Queue;

/*
TODO
    -change EVERYTHING so that a measure is valued at its length, e < [e]
    -implement and test leastAbove and leastEqual
        -caseTwo
        -caseThree
        -caseFour
    -test whole algorithm
*/

public class SuccinctProgressSolver implements Solver {

    private int highestPriority;

    private void printWhole(Measure m) {
        if (m.isTop()) {
            System.out.print("<TOP>");
            return;
        }

        if (m.getMeasure().isEmpty()) {
            System.out.print("[]");
            return;
        }

        System.out.print("[");
        int tuplesLeft = m.getNumberOfUsedTuples();
        int iStart = d - 1;
        for (int i = iStart; i > 0 && tuplesLeft > 0; i -= 2) {
            if (m.getTupleIndicator().contains(i)) {
                System.out.print(i);
                printTuple(m.getMeasure().get(m.getTupleIndicator().indexOf(0, i)));
            } else {
                System.out.print("[]");
            }
            tuplesLeft--;
            System.out.print(", ");
        }
        System.out.print("]");
    }

    private void printTuple(ArrayList<Boolean> t) {
        System.out.print("[");
        for (int i = 0; i < t.size(); i++) {
            System.out.print(t.get(i).booleanValue() + ", ");
        }
        System.out.print("]");
    }

    /*
        function called to solve the graph, returns the solution.
        Solution is 2 arrays, one for the winning region of player 0 (int[0][])
        and one for the winning region of player 1 (int[1][])
    */
    public int[][] win(Graph g) {
        App.log_verbose("Solving with SuccinctProgressSolver");

        // find the highest priority, maxPriority function is weird so needs
        // a bitset
        // the bitset will be used to determine what is in the q quickly
        BitSet nodesPresent = new BitSet(g.length());
        highestPriority = g.maxPriority(nodesPresent);

        // calculate and set the maximum progress measure
        init(g.numOddNodes(), highestPriority);

        App.log_debug("highest Priority: " + highestPriority);
        App.log_debug("maxBits: " + maxBits);
        App.log_debug("d: " + d);

        //initialise progress measures for all nodes
        Measure[] progressMeasures = new Measure[g.length()];
        for (int i = 0; i < g.length(); i++) {
            progressMeasures[i] = new Measure(g.info[i].getPriority());
            nodesPresent.set(i);
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
            next = q.poll();
            nodesPresent.clear(next.getIndex());

            App.log_verbose("  dequeue node " + next.getIndex());
            App.log_debug("original measure: " + progressMeasures[next.getIndex()].toString());
            liftedMeasure = lift(progressMeasures, next, g);
            App.log_debug("lifted measure: " + liftedMeasure.toString());

            // if a lift is successful, add all predecessors of that node to the queue.
            if (!liftedMeasure.equals(progressMeasures[next.getIndex()], next.getPriority())) {
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
            bestLiftedMeasure = new Measure(true);
            App.log_debug("Lifting from a p0 node");
            for (int i = 0; i < adjNodes.size(); i++) {
                newMeasure = singleLift(progressMeasures, node, g.info[adjNodes.get(i)]);
                if (newMeasure.lessThan(bestLiftedMeasure, node.getPriority())) {
                    bestLiftedMeasure = newMeasure;
                }
                App.log_debug("singly lifted to " + newMeasure.toString());
                App.log_debug("current best " + bestLiftedMeasure.toString());
            }
        } else {
            // if node is odd, choose the maximum progressed measure.
            // therefore start with the minimum measure
            bestLiftedMeasure = new Measure(node.getPriority());
            App.log_debug("Lifting from a p1 node");
            for (int i = 0; i < adjNodes.size(); i++) {
                newMeasure = singleLift(progressMeasures, node, g.info[adjNodes.get(i)]);
                if (newMeasure.greaterThan(bestLiftedMeasure, node.getPriority())) {
                    bestLiftedMeasure = newMeasure;
                }
                App.log_debug("singly lifted to " + newMeasure.toString());
                App.log_debug("current best " + bestLiftedMeasure.toString());
            }
        }
        return bestLiftedMeasure;
    }

    // returns mu(to) if from is even, or least measure > mu(to) if from is odd.
    Measure singleLift(Measure[] progressMeasures, Node from, Node to) {
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
                // return toMeasure.leastEqual(toMeasure, from.getPriority());
                return new Measure(from.getPriority(), toMeasure);
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

    // the largest element that is not top
    private static int maxBits;
    // smallest even number, >= all priorities. As described in succinct paper.
    private static int d;

    // calculate the max number of bits and the value of d
    public static void init(int numOddNodes, int highestPriority) {
        maxBits = (int) Math.ceil(Math.log((double) numOddNodes) / Math.log(2.0f));
        d = highestPriority + (highestPriority % 2);
    }

    private class Measure {
        private boolean top;
        private int bitsUsed;
        // how many tuples this measure has that aren't empty
        private int numberOfUsedTuples;
        // the max number of tuples this measure can have due to priority
        private ArrayList<ArrayList<Boolean>> measure;
        private TIntArrayList tupleIndicator;

        public boolean isTop() {
            return top;
        }

        public boolean isBottom() {
            return (!isTop() && length() == 0);
        }

        public void setTop(boolean top) {
            this.top = top;
        }

        public Measure(int priority) {
            this.top = false;
            this.measure = new ArrayList<ArrayList<Boolean>>();
            this.tupleIndicator = new TIntArrayList();
            this.bitsUsed = 0;
            this.numberOfUsedTuples = 0;
        }

        public Measure(int priority, Measure other) {
            this.top = other.isTop();
            this.measure = copyMeasure(other, priority);
            this.tupleIndicator = copyTupleIndicator(other.getTupleIndicator(),
                                                     priority);
            // find the number of tuples used
            int otherUsedIndex = d - 1 - (2 * Math.max(0,
                other.getNumberOfUsedTuples() - 1));
            if (otherUsedIndex < priority) {
                int resultIndex = (priority % 2 == 0) ? priority + 1 : priority;
                this.numberOfUsedTuples = 1 + ((d - 1) - resultIndex) / 2;
            } else {
                this.numberOfUsedTuples = other.getNumberOfUsedTuples();
            }

            calcBitsUsed();
        }

        public Measure(boolean top) {
            this.top = top;
            this.measure = new ArrayList<ArrayList<Boolean>>();
            this.tupleIndicator = new TIntArrayList();
            this.bitsUsed = 0;
            this.numberOfUsedTuples = 0;
        }

        // sets the number of bits that are present in the measure
        private void calcBitsUsed() {
            int result = 0;
            for (int i = 0; i < length(); i++) {
                result += measure.get(i).size();
            }
            bitsUsed = result;
        }

        // returns a copy of a measure, used for cloning Measure objects
        private ArrayList<ArrayList<Boolean>> copyMeasure(Measure other,
                                                          int priority) {
            ArrayList<ArrayList<Boolean>> result = new ArrayList<ArrayList<Boolean>>();

            for (int i = d - 1; i >= priority; i -= 2) {
                if (other.getTupleIndicator().contains(i)) {
                    int j = other.getTupleIndicator().indexOf(0, i);
                    ArrayList<Boolean> temp = new ArrayList<Boolean>();
                    for (int k = 0; k < other.getMeasure().get(j).size(); k++) {
                        temp.add(other.getMeasure().get(j).get(k));
                    }
                    result.add(temp);
                }
            }
            return result;
        }

        // returns a copy of a tupleIndicator, used for cloning measure objects
        private TIntArrayList copyTupleIndicator(TIntArrayList other,
                                                 int priority) {
            TIntArrayList result = new TIntArrayList();

            for (int i = d - 1; i >= priority; i--) {
                if (other.contains(i)) {
                    result.add(other.get(other.indexOf(0, i)));
                }
            }
            return result;
        }

        // number of tuples in the measure with bits used
        public int length() {
            return measure.size();
        }

        public void setTuplesUsed(int n) {
            this.numberOfUsedTuples = n;
        }

        public void setBitsUsed(int b) {
            this.bitsUsed = b;
        }

        // returns how many bits are used before the given truncation
        private int bitsUsedUpToTrunc(int trunc) {
            int result = bitsUsed;
            for (int i = 1; i < trunc; i += 2) {
                if (tupleIndicator.contains(i)) {
                    int temp = measure.get(tupleIndicator.indexOf(0, i)).size();
                    result -= temp;
                }
            }
            return result;
        }

        public ArrayList<ArrayList<Boolean>> getMeasure() {
            return this.measure;
        }

        public void setMeasure(ArrayList<ArrayList<Boolean>> m) {
            this.measure = m;
        }

        public void setMeasure(int i, ArrayList<Boolean> val) {
            measure.set(i, val);
        }

        public TIntArrayList getTupleIndicator() {
            return this.tupleIndicator;
        }

        public void setTupleIndicator(TIntArrayList t) {
            this.tupleIndicator = t;
        }

        public void setTupleIndicator(int i, int val) {
            tupleIndicator.set(i, val);
        }

        public int getNumberOfUsedTuples() {
            return numberOfUsedTuples;
        }

        public void setNumberOfUsedTuples(int n) {
            this.numberOfUsedTuples = n;
        }

        // increment the number of tupled used
        public void incNumberOfUsedTuples() {
            this.numberOfUsedTuples++;
        }

        // decrement the number of tuples used
        public void decNumberOfUsedTuples() {
            this.numberOfUsedTuples--;
        }

        // reduce the number of used tuples by n
        public void lowerNumUsedTuples(int n) {
            this.numberOfUsedTuples -= n;
        }

        public int getBitsUsed() {
            return bitsUsed;
        }

        private int calcMaxNumTuples(int priority) {
            return (d / 2) - (priority / 2);
        }

        // the position in the measure where the lowest priority tuple can be
        // for this measure
        private int lastPossibleTuplePosition(int priority) {
            int a = d - 1 - (2 * (calcMaxNumTuples(priority) - 1));
            int b = priority + ((priority + 1) % 2);
            return Math.max(a, b);
        }

        private int lastPossibleTupleIndex(int priority) {
            return tupleIndicator.indexOf(lastPossibleTuplePosition(priority));
        }

        // position of the lowest priority tuple with bits, after truncation
        private int lastTuplePosition(int priority) {
            int i = 1;
            int lastTuplePosition = tupleIndicator.get(tupleIndicator.size() - i);
            while (lastTuplePosition < priority && i < tupleIndicator.size()) {
                i++;
                lastTuplePosition = tupleIndicator.get(tupleIndicator.size() - i);
            }
            return lastTuplePosition;
        }

        // where the last tuple appears in the measure/tupleIndicator arrays.
        // (tuple must have bits in it)
        private int lastTupleIndex(int priority) {
            return tupleIndicator.indexOf(lastTuplePosition(priority));
        }

        // position of the lowest priority tuple with bits, after truncation
        private int lastUsedTuplePosition(int priority) {
            int result = d - 1 - ((numberOfUsedTuples - 1) * 2);
            while(result < priority) { result += 2; }
            return result;
        }

        // where the last tuple appears in the measure/tupleIndicator arrays.
        // (tuple could have no bits in it)
        private int lastUsedTupleIndex(int priority) {
            return tupleIndicator.indexOf(lastUsedTuplePosition(priority));
        }

        // returns the smallest element that is greater than the other measure
        public Measure leastAbove(int priority) {
            // try { Thread.sleep(600); } catch (Exception e) {}
            // if other is top, return top
            if (top) return new Measure(true);
            // there are 5 cases as described in the paper:

            if (isCaseOne(priority)) {
                return caseOne(priority);
            }

            if (isCaseTwo(priority)) {
                return caseTwo(priority);
            }

            if (isCaseThree(priority)) {
                return caseThree(priority);
            }

            if (isCaseFour(priority)) {
                return caseFour(priority);
            }

            // the last possibility is that other was the greates element
            // below top :. return top
            return new Measure(true);
        }

        // there is an unused tuple a the end of this measure
        private boolean isCaseOne(int priority) {
            if (numberOfUsedTuples == 0) return true;
            if (lastUsedTuplePosition(priority) ==
                lastPossibleTuplePosition(priority)) return false;
            return true;
        }

        // set the next empty tuple to all zeros
        private Measure caseOne(int priority) {
            Measure res = new Measure(priority, this);

            if (res.bitsUsedUpToTrunc(priority) == maxBits) {
                res.incNumberOfUsedTuples();
                return res;
            }
            // create the tuple with all the remaining zeros
            ArrayList<Boolean> newTuple = new ArrayList<Boolean>();
            for (int i = 0; i < maxBits - res.bitsUsedUpToTrunc(priority); i++) {
                newTuple.add(new Boolean(false));
            }

            res.getMeasure().add(newTuple);
            res.getTupleIndicator().add(lastUsedTuplePosition(priority) - 2);

            res.setBitsUsed(maxBits);
            res.incNumberOfUsedTuples();
            return res;
        }

        // last available tuple to this measure is used, bits to spare
        private boolean isCaseTwo(int priority) {
            if (lastUsedTuplePosition(priority) != lastPossibleTuplePosition(priority)) {
                return false;
            }

            // now check to see if there any remaining bits
            if (bitsUsedUpToTrunc(priority) == maxBits) return false;
            return true;
        }

        // add 0(1)* to the end of the last tuple using as many bits as possible
        private Measure caseTwo(int priority) {
            Measure result = new Measure(priority, this);
            int index = result.lastPossibleTupleIndex(priority);

            // either get the tuple to change or create it if it doesn't exist
            ArrayList<Boolean> tuple;
            if (index == -1) {
                tuple = new ArrayList<Boolean>();
                result.getMeasure().add(tuple);
                result.getTupleIndicator().add(lastPossibleTuplePosition(priority));
            } else {
                tuple = result.getMeasure().get(index);
            }

            tuple.add(new Boolean(true));
            for (int i = result.getBitsUsed() + 1; i < maxBits; i++) {
                tuple.add(new Boolean(false));
            }
            result.calcBitsUsed();
            return result;
        }

        // all bits used, last nonempty tuple is in form s'0(1)*
        private boolean isCaseThree(int priority) {
            // make sure all bits are used
            if (bitsUsedUpToTrunc(priority) != maxBits) return false;

            // if there is a sinlge 0 in the tuple this is a case 3.
            ArrayList<Boolean> tuple = measure.get(tupleIndicator.indexOf(0, lastTuplePosition(priority)));
            for (int i = 0; i < tuple.size(); i++) {
                if (tuple.get(i).booleanValue() == false) {
                    return true;
                }
            }
            return false;
        }

        // remove the last 0 in the last used Measure, and all following 1s.
        private Measure caseThree(int priority) {
            Measure result = new Measure(priority, this);

            int position = lastTuplePosition(priority);
            int index = lastTupleIndex(priority);

            ArrayList<Boolean> tuple = result.getMeasure().get(index);
            int i;
            for (i = tuple.size() - 1; i >= 0; i--) {
                if (tuple.get(i).booleanValue() == false) {
                    tuple.remove(i);
                    break;
                }
                tuple.remove(i);
            }

            // diff is the number of bits removed in the tuple
            int diff = (position - lastPossibleTuplePosition(priority)) / 2;
            result.lowerNumUsedTuples(diff);

            result.calcBitsUsed();
            return result;
        }

        // all bits used, last nonempty tuple is of form (1)+
        private boolean isCaseFour(int priority) {
            // make sure all bits are used
            if (bitsUsedUpToTrunc(priority) != maxBits) return false;

            //find the last nonempty tuple
            int lastTuplePosition = lastTuplePosition(priority);

            // the last tuple cannot be at the end
            if (lastTuplePosition == d - 1) return false;

            ArrayList<Boolean> tuple = measure.get(tupleIndicator.indexOf(0, lastTuplePosition));
            for (int i = 0; i < tuple.size(); i++) {
                if (tuple.get(i).booleanValue() == false) {
                    return false;
                }
            }

            return true;
        }

        // remove the last tuple and append 1(0)* to the next lowest tuple.
        private Measure caseFour(int priority) {
            Measure result = new Measure(priority, this);
            int index = lastTupleIndex(priority);
            int position = lastTuplePosition(priority);
            int prevPosition = position + 2;
            // set the tuple at index to empty
            int l = result.getMeasure().get(index).size();
            result.getMeasure().remove(index);
            result.getTupleIndicator().removeAt(index);

            // get the previous tuple
            ArrayList<Boolean> tuple;
            int prevIndex = result.getTupleIndicator().indexOf(prevPosition);
            if (prevIndex != -1) {
                tuple = result.getMeasure().get(prevIndex);
            } else {
                tuple = new ArrayList<Boolean>();
                result.getMeasure().add(tuple);
                result.getTupleIndicator().add(prevPosition);
            }
            tuple.add(new Boolean(true));
            for (int i = 0; i < l - 1; i++) {
                tuple.add(new Boolean(false));
            }
            // result.decNumberOfUsedTuples();
            int diff = (position - lastPossibleTuplePosition(priority)) / 2;
            result.lowerNumUsedTuples(diff + 1);
            return result;
        }

        // returns the smallest possible measure that is equal to the other measure
        public Measure leastEqual(Measure other, int priority) {
            // if other is top, return top
            if (other.isTop()) return other;
            // if other is bottom, return bottom
            if (other.length() == 0) return other;

            Measure result = new Measure(priority, other);

            int lastPosition = lastUsedTuplePosition(priority);
            // iterate backwards through tupleIndicator
            for (int i = other.length() - 1; i >= 0; i--) {
                if (other.getTupleIndicator().get(i) < priority) {
                    // if the value is < priority then get rid of it
                    // and the corresponding tuple
                    other.getTupleIndicator().removeAt(i);
                    other.getMeasure().remove(i);
                }
            }
            // this is the last theoretical tuple that could survive this
            int priorityPosition = priority + ((priority + 1) % 2);
            if (priorityPosition > lastPosition) {
                int diff = (priorityPosition - lastPosition) / 2;

            }
            return new Measure(0);
        }

        public String toString() {
            String s = "";
            if (isTop()) {
                return "<TOP>";
            }

            if (measure.isEmpty()) {
                return "[]";
            }

            s += "[";
            int tuplesLeft = getNumberOfUsedTuples();
            int iStart = d - 1;
            for (int i = iStart; i > 0 && tuplesLeft > 0; i -= 2) {
                if (tupleIndicator.contains(i)) {
                    s += i;
                    s += tupleToString(tupleIndicator.indexOf(0, i));
                } else {
                    s += "[]";
                }
                tuplesLeft--;
                s += ", ";
            }
            s += "]";

            return s;
        }

        private String tupleToString(int index) {
            ArrayList<Boolean> t = measure.get(index);
            String s = "[";
            for (int i = 0; i < t.size(); i++) {
                s += t.get(i).booleanValue() + ", ";
            }
            s += "]";
            return s;
        }

        public boolean equals(Measure other, int priority) {
            // if both are the bottom element
            if (this.isBottom() && other.isBottom()) return true;
            // if both are the top element
            if (this.top && other.isTop()) return true;

            if (this.top || other.isTop()) return false;

            ArrayList<ArrayList<Boolean>> otherMeasure = other.getMeasure();
            TIntArrayList otherTupleIndicator = other.getTupleIndicator();

            // start at the beginning of the representation of the tuple shown
            // in the succinct algorithm's paper
            int i;
            // keep track of how many tuples each have left
            int thisTuplesLeft = numberOfUsedTuples;
            int otherTuplesLeft = other.getNumberOfUsedTuples();

            // i already defined, only iterate down to where they have been
            // truncated, this and other must always have tuples left, in the
            // papers representation, all indices are odd :. i-=2
            for (i = d - 1; i >= priority && thisTuplesLeft > 0
            && otherTuplesLeft > 0; i -= 2) {
                if (tupleIndicator.contains(i)) {
                    // then this has a tuple at this index
                    if (otherTupleIndicator.contains(i)) {
                        // then other also has a tuple at this index
                        // compare if the tuples are the same
                        ArrayList<Boolean> t1 = measure.get(tupleIndicator.indexOf(0, i)),
                        t2 = otherMeasure.get(otherTupleIndicator.indexOf(0, i));
                        if (!tupleEqual(t1, t2)) {
                            return false;
                        }
                    } else {
                        // there is a tuple in this at index i but not in other
                        return false;
                    }
                } else {
                    if (otherTupleIndicator.contains(i)) {
                        // then other has a tuple at this index but this doesn't
                        return false;
                    }
                }
                // one tuple from each has been used (empty tuples are counted)
                thisTuplesLeft--;
                otherTuplesLeft--;
            }

            // if this or other is a different length than the other
            // even after truncating
            if (i >= priority && otherTuplesLeft != thisTuplesLeft
                && (otherTuplesLeft == 0 || thisTuplesLeft == 0)) {
                return false;
            }
            // otherwise they are equal
            return true;
        }

        private boolean lessThan(Measure other, int priority) {
            // not true if this is top or other is bottom
            if (this.top || other.isBottom()) return false;
            // must be true if other is top or this is bottom
            if (other.isTop() || this.isBottom()) return true;

            TIntArrayList otherTupleIndicator = other.getTupleIndicator();
            ArrayList<ArrayList<Boolean>> otherMeasure = other.getMeasure();

            boolean thisShorter;
            // if there are more or equal #tuples in this than other, false
            // else there are more tuples in other but not this, true

            if (Math.min(this.numberOfUsedTuples, calcMaxNumTuples(priority)) >=
            Math.min(other.getNumberOfUsedTuples(), calcMaxNumTuples(priority))) {
                thisShorter = false;
            }
            else thisShorter = true;

            // start at the left side of the succinct algos paper representation
            int iStart = d - 1;
            // keep track of how many tuples for each are left
            int thisTuplesLeft = numberOfUsedTuples;
            int otherTuplesLeft = other.getNumberOfUsedTuples();

            // i already defined, only iterate down to where they have been
            // truncated, this and other must always have tuples left, in the
            // papers representation, all indices are odd :. i-=2

            for (int i = iStart; i >= priority && thisTuplesLeft > 0
            && otherTuplesLeft > 0; i -= 2) {
                if (tupleIndicator.contains(i)) {
                    // then this has a tuple at this index
                    if (otherTupleIndicator.contains(i)) {
                        // then other also has a tuple at this index
                        // compare if the tuples are the same
                        ArrayList<Boolean> t1 = measure.get(tupleIndicator.indexOf(0, i)),
                        t2 = otherMeasure.get(otherTupleIndicator.indexOf(0, i));
                        if (tupleLessThan(t1, t2)) {
                            return true;
                        } else if (tupleGreaterThan(t1, t2)) {
                            return false;
                        }
                    } else {
                        // there is a tuple in this at index i but not in other
                        // if this' tuple starts with a 1 then it is greater
                        // if it starts with a 0 then it is less than
                        int index = tupleIndicator.indexOf(0, i);
                        if (measure.get(index).get(0).booleanValue() == true) {
                            return false;
                        } else {
                            return true;
                        }
                    }
                } else {
                    if (otherTupleIndicator.contains(i)) {
                        // then other has a tuple at this index but this doesn't
                        // if the other tuple starts with a 1 then this is less`
                        // else it starts with a 0, :. this is greater than
                        int index = otherTupleIndicator.indexOf(0, i);
                        if (otherMeasure.get(index).get(0).booleanValue() == true) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
                thisTuplesLeft--;
                otherTuplesLeft--;
            }

            return thisShorter;
        }

        private boolean greaterThan(Measure other, int priority) {
            // not true if this is bottom or other is top
            if (this.isBottom() || other.isTop()) return false;
            // must be true if this is top or other is bottom
            if (this.isTop() || other.isBottom()) return true;
            // for each tuple, if it is greater than other then true,
            // if it is lesser than other then false, else continue

            boolean thisLonger;
            // if there are more tuples in this than other, true
            // else there are more or equal #tuples in other than this, false
            if (Math.min(this.numberOfUsedTuples, calcMaxNumTuples(priority)) >
            Math.min(other.getNumberOfUsedTuples(), calcMaxNumTuples(priority))) {
                thisLonger = true;
            }
            else thisLonger = false;

            TIntArrayList otherTupleIndicator = other.getTupleIndicator();
            ArrayList<ArrayList<Boolean>> otherMeasure = other.getMeasure();

            int iStart = d - 1;
            int thisTuplesLeft = numberOfUsedTuples;
            int otherTuplesLeft = other.getNumberOfUsedTuples();

            // i already defined, only iterate down to where they have been
            // truncated, this and other must always have tuples left, in the
            // papers representation, all indices are odd :. i-=2
            for (int i = iStart; i >= priority && thisTuplesLeft > 0
            && otherTuplesLeft > 0; i -= 2) {
                if (tupleIndicator.contains(i)) {
                    // then this has a tuple at this index
                    if (otherTupleIndicator.contains(i)) {
                        // then other has a tuple at this index
                        ArrayList<Boolean> t1 = measure.get(tupleIndicator.indexOf(0, i)),
                        t2 = otherMeasure.get(otherTupleIndicator.indexOf(0, i));
                        if (tupleGreaterThan(t1, t2)) {
                            return true;
                        } else if (tupleLessThan(t1, t2)) {
                            return false;
                        }
                    } else {
                        // there is a tuple in this at index i but not in other
                        // if this' tuple starts with a 1 then it is greater
                        // if it starts with a 0 then it is less than
                        int index = tupleIndicator.indexOf(0, i);
                        if (measure.get(index).get(0).booleanValue() == true) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                } else {
                    if (otherTupleIndicator.contains(i)) {
                        // then other has a tuple at this index but this doesn't
                        // if the other tuple starts with a 1 then this is less`
                        // else it starts with a 0, :. this is greater than
                        int index = otherTupleIndicator.indexOf(0, i);
                        if (otherMeasure.get(index).get(0).booleanValue() == true) {
                            return false;
                        } else {
                            return true;
                        }
                    }
                }
                thisTuplesLeft--;
                otherTuplesLeft--;
            }

            // if this is reached, then all tuples up to the end of the shortest
            // measure are equal: therefore if this measure has less tuples then
            // other is greater.
            return thisLonger;
        }

        private boolean tupleEqual(ArrayList<Boolean> t1,
        ArrayList<Boolean> t2) {
            if (t1.size() != t2.size()) {
                return false;
            }
            for (int i = 0; i < t1.size(); i++) {
                if (t1.get(i).booleanValue() != t2.get(i).booleanValue()) {
                    return false;
                }
            }
            return true;
        }

        // returns true if tuple1 < tuple2
        private boolean tupleLessThan(ArrayList<Boolean> tuple1,
        ArrayList<Boolean> tuple2) {
            int iLimit = Math.min(tuple1.size(), tuple2.size());
            int i;
            for (i = 0; i < iLimit; i++) {
                if (tuple1.get(i).booleanValue() != tuple2.get(i).booleanValue()) {
                    return tuple2.get(i);
                }
            }
            // now we know that one of the tuples is longer than the other, and
            // up to the lenght of the shorter one, they are identical
            if (tuple1.size() < tuple2.size()) {
                if (tuple2.get(i).booleanValue() == true) {
                    return true;
                } else {
                    return false;
                }
            } else if (tuple1.size() > tuple2.size()) {
                if (tuple1.get(i).booleanValue() == true) {
                    return false;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        }

        private boolean tupleGreaterThan(ArrayList<Boolean> tuple1,
        ArrayList<Boolean> tuple2) {
            int iLimit = Math.min(tuple1.size(), tuple2.size());
            int i;
            for (i = 0; i < iLimit; i++) {
                if (tuple1.get(i).booleanValue() != tuple2.get(i).booleanValue()) {
                    // System.out.println("unequal values at same position found");
                    return tuple1.get(i);
                }
            }
            // now we know that one of the tuples is longer than the other, and
            // up to the lenght of the shorter one, they are identical
            if (tuple1.size() < tuple2.size()) {
                // System.out.println("first arg shorter than second arg");
                if (tuple2.get(i).booleanValue() == true) {
                    return false;
                } else {
                    return true;
                }
            } else if (tuple1.size() > tuple2.size()) {
                // System.out.println("first arg longer than second arg");
                if (tuple1.get(i).booleanValue() == true) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }
}
