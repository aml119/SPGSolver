package com.JPGSolver;

import com.beust.jcommander.JCommander;
import com.google.common.base.Stopwatch;
import com.google.common.primitives.Ints;
import com.opencsv.CSVWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
    SOLVER INFO:
        -AsyncSolver3           works
        -SmallProgressSolver    works
        -RecursiveSolver        works

        -AsyncSolver    works, but slower than AsyncSolver3

        -IterativeSolver            broken
        -ImprovedRecursiveSolver    broken
*/

public class App {

    private static int verbosity;

    public static void runTests(AsyncSolver3 solver, int cores, int min, int max, int step, int tries, String path, String generator) {

        List<String[]> dataAttr = new ArrayList<>();
        List<String[]> dataTot = new ArrayList<>();

        Runtime.getRuntime().addShutdownHook(
            new Thread("app-shutdown-hook") {
                @Override
                public void run() {
                    System.out.println("External Termination");
                    saveResults(path,dataAttr, dataTot);
                }
            }
        );

        int colunms = 1 + 1 + cores;
        String[] row =  new String[colunms];
        row [0] = "Attractor Time";
        row [1] = "Seq";
        for (int i = 2; i < cores + 2; i++) {
            row[i] = Integer.toString(i-1);
        }
        dataAttr.add(row);
        dataTot.add(new String[cores]);
        row = Arrays.copyOf(row, colunms);
        row [0] = "Total Time";
        dataTot.add(row);

        try {
            int cur = min;
            RecursiveSolver seq = new RecursiveSolver();
            for (cur = min; cur <= max; cur += step) {
                for (int t = 1; t <= tries; t++) {
                    String[] rowTot = new String[colunms];
                    String[] rowAttr = new String[colunms];
                    rowTot[0] = Integer.toString(cur) + "-" + Integer.toString(t);
                    rowAttr[0] = rowTot[0];

                    File f = new File(path + rowTot[0]);
                    if (!f.exists()) {
                        //home/pgsolver/bin/randomgame 20000 20000 1 20000 >> 20000-2
                        try {
                            f.createNewFile();
                            String sCur = Integer.toString(cur);
                            Process p = new ProcessBuilder(generator, sCur, sCur, "1",sCur).redirectOutput(f).start();
                            System.out.println("Generating Graph ................ " + f);
                            p.waitFor();
                        } catch (Exception e) {
                            throw new RuntimeException("randomgame Exception");
                        }
                    }

                    Graph G = Graph.initFromFile(path + rowTot[0]);

                    System.out.println("Testing ......................... " + f);
                    //System.out.print("Seq");
                    int[][] res = seq.win(G);
                    rowTot[1] = swSecondify(seq.swTot.toString());
                    rowAttr[1] = swSecondify(seq.swAttr.toString());
                    for (int i = 1; i <= cores; i++) {
                        int y = i + 1;
                        //System.out.print(" " + i);
                        solver.setCores(i);
                        if (!checkSolution(res, solver.win(G))) throw new RuntimeException("Incorrect Result!");
                        rowTot[y] = swSecondify(solver.swTot.toString());
                        rowAttr[y] = swSecondify(solver.swAttr.toString());
                    }
                    //System.out.print(" Done\n");
                    dataAttr.add(rowAttr);
                    dataTot.add(rowTot);
                    G = null;
                    System.gc();
                }
            }
        } catch(OutOfMemoryError e) {
            System.out.println("OOM!");
        } finally {
            saveResults(path,dataAttr, dataTot);
        }
        System.out.println("Done");
    }

    public static void saveResults(String path, List<String[]> dataAttr, List<String[]> dataTot) {
        String csv = path + "results.csv";
        int ind = 1;
        while (new File(csv).exists()) {
            csv = path + "results" + ind + ".csv";
            ind++;
        }
        System.out.println("Writing Results to " + csv);
        CSVWriter writer;
        try {
            writer = new CSVWriter(new FileWriter(csv));
            writer.writeAll(dataAttr);
            writer.writeAll(dataTot);
            writer.close();
        } catch(IOException e) {
            throw new RuntimeException(" I/O error occurs");
        }
    }

    public static void printHelp() {
        System.out.println();
        System.out.println("Usage: java -jar JPGSolver.jar [-options] [filepaths]");
        System.out.println("Where options are:");
        System.out.println("   --justHeat/-jh              Only outputs the timings, not the solutions.");
        System.out.println("   --verify/-vf                Verify the solution given. Can be used with -jh.");
        System.out.println("   --verbosity/-vb             Verbosity Level, must be followed with a 1 (for verbose) or 2 (for debug)");
        System.out.println("   --smallpm/-spm              Solve the supplied games with the small progress measures algorithm");
        System.out.println("   --succinctpm/-suc           Solve the game with the succinct progress measures algorithm");
        System.out.println("   --zielonka/-z               Solve the supplied games with the zielonka recursive algorithm.");
        System.out.println("   --parallelZielonka/-pz [n]  where n is the number of threads to run.");
        System.out.println("   --tests/-t [a b c d e f g]  Run a set of tests with the following arguments:");
        System.out.println("       a: number of threads to run, one solver for each number from 1 to n will be run");
        System.out.println("       b: smallest game to generate/ solve (in number of nodes)");
        System.out.println("       c: largest game to generate/ solve");
        System.out.println("       d: difference between the number of nodes between each game");
        System.out.println("       e: number of games generated for each increment of nodes");
        System.out.println("       f: path to a directory to hold the games generated");
        System.out.println("       g: path to the randomgame generator from PGSolver");
        System.out.println();
        System.out.println("And any number of file paths to parity games generated by PGsolver.");
        System.out.println();
    }

    public static void main( String[] args ) {
        CommandLineArgs cli = new CommandLineArgs();
        new JCommander(cli, args);

        if (cli.help) {
            printHelp();
            return;
        }

        verbosity = cli.verbosity.intValue();

        // choose the solver to use
        Solver solver;

        if (cli.zielonka) {
            solver = new RecursiveSolver();
        } else if (!cli.parallelZielonkaCores.equals(-1)) {
            solver = new AsyncSolver3(cli.parallelZielonkaCores.intValue());
        } else if (cli.smallpm) {
            solver = new SmallProgressSolver();
        } else if (cli.succinctpm) {
            solver = new SuccinctProgressSolver();
        } else {
            solver = new AsyncSolver();
        }

        if (cli.tests) {
            if (cli.params.size() < 7) throw new RuntimeException("Missing Parameters");
            List<String> par = cli.params;
            int nthreads = Integer.parseInt(par.get(0));
            int minG = Integer.parseInt(par.get(1));
            int maxG = Integer.parseInt(par.get(2));
            int stpG = Integer.parseInt(par.get(3));
            int tries = Integer.parseInt(par.get(4));
            String path = par.get(5);
            String gen = par.get(6);
            if (!new File(path).isDirectory()) throw new RuntimeException("Need a working directory");
            if (!new File(gen).canExecute()) throw new RuntimeException("Need a game generator");
            runTests(new AsyncSolver3(), nthreads, minG, maxG, stpG, tries, path, gen);
            return;
        }


        for (String file : cli.params) {
            Graph G = Graph.initFromFile(file);
            Solver solverCheck = new AsyncSolver3();
            Stopwatch sw2 = Stopwatch.createStarted();
            int[][] solution = solver.win(G);
            sw2.stop();
            System.out.println(file + " " + sw2);
            if (!cli.justHeat) {
                Arrays.sort(solution[0]);
                Arrays.sort(solution[1]);
                printSolution(solution);
            }
            if (cli.verify) {
                System.out.println("Checking solution...");
                int[][] slnCheck = solverCheck.win(G);
                boolean valid = checkSolution(solution, slnCheck);
                System.out.println("Validity of solution: " + valid);
                if (!valid) System.out.println("Real Solution: ");
                if (!valid) printSolution(slnCheck);
            }
        }
    }

    public static void cleanMain( String[] args ) {
        CommandLineArgs cli = new CommandLineArgs();
        new JCommander(cli, args);

        Stopwatch sw1 = Stopwatch.createStarted();
        Graph G = Graph.initFromFile(cli.params.get(0));
        sw1.stop();
        System.out.println("Parsed in " + sw1);
        Solver solver = cli.parallel ? new AsyncSolver() : cli.iterative ?new IterativeSolver() : new RecursiveSolver();

        Stopwatch sw2 = Stopwatch.createStarted();
        int[][] solution = solver.win(G);
        sw2.stop();
        System.out.println("Solved in " + sw2);

        Solver solver2 = new RecursiveSolver();
        int[][] solution2 = solver2.win(G);
        System.out.print(checkSolution(solution, solution2));

        if (cli.justHeat) {
            return;
        }

        Arrays.sort(solution[0]);
        Arrays.sort(solution[1]);

        printSolution(solution);
    }

    public static String swSecondify(String s) {
        String[] strings = s.split(" ");
        if (strings[1].compareTo("ms") == 0) {
            return Double.toString(Double.parseDouble(strings[0]) / 1000);
        } else if (strings[1].compareTo("s") == 0) {
            return Double.toString(Double.parseDouble(strings[0]));
        }
        return s;
    }


    public static void printSolution(int[][] solution) {
        System.out.print("\nSolution for player 0:\n{");
        int index = 0;
        for (int x : solution[0]) {
            if (index == solution[0].length-1) {
                System.out.printf(x + "}");
            } else {
                System.out.printf(x + ", ");
            }
            index += 1;
        }

        System.out.print("\nSolution for player 1:\n{");
        index = 0;
        for (int x : solution[1]) {
            if (index == solution[1].length-1) {
                System.out.printf(x + "}");
            } else {
                System.out.printf(x + ", ");
            }
            index += 1;
        }
        System.out.println();
    }

    public static boolean checkSolution(int[][] s1, int[][] s2) {
        for (int x : s1[0]) {
            if (!Ints.contains(s2[0], x)) return false;
        }
        for (int x : s1[1]) {
            if (!Ints.contains(s2[1], x)) return false;
        }
        return true;
    }

    public static void log_verbose(String s) {
        if (verbosity > 0) {
            System.out.println(s);
        }
    }

    public static void log_debug(String s) {
        if (verbosity > 1) {
            System.out.println(s);
        }
    }
}
