package com.JPGSolver;

import com.beust.jcommander.JCommander;
import com.google.common.base.Stopwatch;
import com.google.common.primitives.Ints;
import com.opencsv.CSVWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class App {

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

    public static void main( String[] args ) {
        CommandLineArgs cli = new CommandLineArgs();
        new JCommander(cli, args);

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
            //runTests(new AsyncSolver3(), nthreads, minG, maxG, stpG, tries, "/home/umberto/Grafi/", "/home/umberto/pgsolver/bin/randomgame");
            return;
        }

        Solver solver, solverCheck;

        for (String file : cli.params) {
            Graph G = Graph.initFromFile(file);
            if (cli.smallpm) {
                solver = new SmallProgressSolver();
            } else {
                solver = new AsyncSolver3();
            }
            solverCheck = new AsyncSolver3();
            Stopwatch sw2 = Stopwatch.createStarted();
            int[][] solution = solver.win(G);
            sw2.stop();
            System.out.println(file + " " /*+ solver.sw + " "*/ + sw2);
            if (cli.justHeat) {
                continue;
            }
            System.out.println("Checking solution...");
            int[][] slnCheck = solverCheck.win(G);
            Arrays.sort(solution[0]);
            Arrays.sort(solution[1]);
            printSolution(solution);
            boolean valid = checkSolution(solution, slnCheck);
            System.out.println("Validity of solution: " + valid);
            System.out.println("Real Solution: ");
            if (!valid) printSolution(slnCheck);
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
}
