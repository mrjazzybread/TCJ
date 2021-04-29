package lp;

import scpsolver.constraints.LinearBiggerThanEqualsConstraint;
import scpsolver.problems.LinearProgram;


import scpsolver.lpsolver.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NormalFormGame {
    static LinearProgram lp;


    List<String> rowActions;    // actions of player 1
    List<String> colActions;    // actions of player 2
    int nRow;                    // number of actions of player 1
    int nCol;                    // number of actions of player 2
    boolean[] pRow;                // if pRow[i]==false than action i of player 1 is not considered
    boolean[] pCol;                // if pCol[j]==false than action j of player 2 is not considered
    double[][] u1;                // utility matrix of player 1
    double[][] u2;                // utility matrix of player 2

    public NormalFormGame() {
    }

    public NormalFormGame(int[][] M1, int[][] M2, String[] labelsP1, String[] labelsP2) {
        /*
         * Constructor of a NormalFormGame with data obtained from the API
         */
        nRow = labelsP1.length;
        rowActions = new ArrayList<>();
        pRow = new boolean[nRow];
        for (int i = 0; i < nRow; i++) {
            rowActions.add(labelsP1[i].substring(labelsP1[i].lastIndexOf(':') + 1));
            pRow[i] = true;
        }
        nCol = labelsP2.length;
        colActions = new ArrayList<String>();
        pCol = new boolean[nCol];
        for (int j = 0; j < nCol; j++) {
            colActions.add(labelsP2[j].substring(labelsP2[j].lastIndexOf(':') + 1));
            pCol[j] = true;
        }
        u1 = new double[nRow][nCol];
        u2 = new double[nRow][nCol];
        for (int i = 0; i < nRow; i++) {
            for (int j = 0; j < nCol; j++) {
                u1[i][j] = M1[i][j];
                u2[i][j] = M2[i][j];
            }
        }
    }

    public void showGame() {
        /*
         * Prints the game in matrix form. The names of the actions are shortened to the first letter
         */
        System.out.print("****");
        for (int j = 0; j < nCol; j++)
            if (pCol[j])
                System.out.print("***********");
        System.out.println();
        System.out.print("  ");
        for (int j = 0; j < nCol; j++)
            if (pCol[j]) {
                if (colActions.size() > 0) {
                    System.out.print("      ");
                    System.out.print(colActions.get(j).substring(0, 1));
                    System.out.print("    ");
                } else {
                    System.out.print("\t");
                    System.out.print("Col " + j);
                }
            }
        System.out.println();
        for (int i = 0; i < nRow; i++)
            if (pRow[i]) {
                if (rowActions.size() > 0) System.out.print(rowActions.get(i).substring(0, 1) + ": ");
                else System.out.print("Row " + i + ": ");
                for (int j = 0; j < nCol; j++)
                    if (pCol[j]) {
                        String fs = String.format("| %3.0f,%3.0f", u1[i][j], u2[i][j]);
                        System.out.print(fs + "  ");
                    }
                System.out.println("|");
            }
        System.out.print("****");
        for (int j = 0; j < nCol; j++)
            if (pCol[j])
                System.out.print("***********");
        System.out.println();
    }

    void remakeGame() {

    }

    void remakeGame(double min, boolean isPlayer2) {
        double[][] u = u1;
        if (isPlayer2)
            u = u2;
        double finalMin = min;
        for (int i = 0; i < u.length; i++) {
            double[] row = u[i];
            u[i] = Arrays.stream(row).map((n) -> n + finalMin).toArray();
        }
    }

    void solveGame() {
        double min = 0;
        for (double[] l : u1)
            min = Math.min(min, Arrays.stream(l).min().getAsDouble());
        if (min < 0)
            remakeGame(min, false);
        min = 0;
        for (double[] l : u2)
            min = Math.min(min, Arrays.stream(l).min().getAsDouble());
        if (min < 0)
            remakeGame(min, true);

        checkDominance();
    }

    private void removeRow(int i){
        double[][] newU1 = new double[u1.length - 1][u1[0].length];
        double[][] newU2 = new double[u1.length - 1][u1[0].length];
        for (int j = 0; j < newU1.length; j = j + 1)
            if (j < i) {
                newU1[j] = u1[j];
                newU2[j] = u2[j];
            } else {
                newU1[j] = u1[j + 1];
                newU2[j] = u2[j + 1];
            }
        u1 = newU1;
        u2 = newU2;
    }

    private void checkDominance() {
        for (int i = 0; i < u1.length && u1.length > 1; i++) {
            double e = solveLPP1(i, u1);
            if (e < 1) {
                removeRow(i);
                i = 0;
            }
        }
    }

    double solveLPP1(int ix, double[][] u) {
        double[] c = Arrays.stream(new double[u.length - 1]).map((n) -> 1.0).toArray();
        double[] b = u[ix];
        double[][] A = new double[u[0].length][u.length - 1];
        for (int i = 0; i < A.length; i = i + 1)
            for (int j = 0; j < A[i].length; j = j + 1)
                if (j < ix)
                    A[i][j] = u[j][i];
                else
                    A[i][j] = u[j + 1][i];
        double[] lb = new double[u.length - 1];
        lp = new LinearProgram(c);
        lp.setMinProblem(true);
        for (int i = 0; i < b.length; i++)
            lp.addConstraint(new LinearBiggerThanEqualsConstraint(A[i], b[i], "c" + i));
        lp.setLowerbound(lb);
        LinearProgramSolver solver = SolverFactory.newDefault();
        return lp.evaluate(solver.solve(lp));
    }

    double solveLPP2(int ix, double[][] u) {
        double[] c = Arrays.stream(new double[u.length - 1]).map((n) -> 1.0).toArray();
        double[] b = new double[u.length];
        IntStream.range(0, u.length).forEach(i -> b[i] = u[i][ix]);
        double[][] A = new double[u.length][u[0].length - 1];
        for (int i = 0; i < A.length; i = i + 1)
            for (int j = 0; j < A[i].length; j = j + 1)
                if (j < ix)
                    A[i][j] = u[j][i];
                else
                    A[i][j] = u[j + 1][i];
        double[] lb = new double[u.length - 1];
        lp = new LinearProgram(c);
        lp.setMinProblem(true);
        for (int i = 0; i < b.length; i++)
            lp.addConstraint(new LinearBiggerThanEqualsConstraint(A[i], b[i], "c" + i));
        lp.setLowerbound(lb);
        LinearProgramSolver solver = SolverFactory.newDefault();
        return lp.evaluate(solver.solve(lp));
    }

}
