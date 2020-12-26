package problems.pap.solvers;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import metaheuristics.tabusearch.AbstractTS;
import problems.pap.PAP_Inverse;
import solutions.Solution;

/**
 * Metaheuristic TS (Tabu Search) for obtaining an optimal solution to the
 * Professor Allocation Problem. Since by default this TS considers minimization
 * problems, an inverse objective function is adopted.
 * 
 * @author brichau, einnarelli, rmeirelles
 */
public class TS_PAP extends AbstractTS<int[]> {

    /**
     * empty element to enter tabu list
     */
    private final int[] fake = new int[] {};

    /**
     * problem evaluator
     */
    PAP_Inverse pap;

    /**
     * Constructor for the TS_PAP class. An inverse PAP objective function is passed
     * as argument for the superclass constructor.
     * 
     * @param tenure          The Tabu tenure parameter.
     * @param iterations      The number of iterations which the TS will be
     *                        executed.
     * @param filename        Name of the file for which the objective function
     *                        parameters should be read.
     * @param resultsFileName The file where the results will be stored.
     * @param instName        The instance name.
     * @throws IOException necessary for I/O operations.
     */
    public TS_PAP(Integer tenure, Integer iterations, String filename, String resultsFileName, String instName)
            throws IOException {

        super(new PAP_Inverse(filename), tenure, iterations, resultsFileName, instName);

        // cast to PAP_Inverse to have access to it's attributes and methods
        pap = (PAP_Inverse) this.ObjFunction;

    }

    /*
     * (non-Javadoc)
     * 
     * @see metaheuristics.tabusearch.AbstractTS#makeCL()
     */
    @Override
    public ArrayList<int[]> makeCL() {

        ArrayList<int[]> _CL = new ArrayList<int[]>();

        // we generate every [p, d, t] array possible
        for (int p = 0; p < pap.P; p++) {
            for (int d = 0; d < pap.D; d++) {
                for (int t = 0; t < pap.T; t++) {
                    _CL.add(new int[] { p, d, t });
                }
            }
        }

        return _CL;

    }

    /*
     * (non-Javadoc)
     * 
     * @see metaheuristics.tabusearch.AbstractTS#makeRCL()
     */
    @Override
    public ArrayList<int[]> makeRCL() {
        ArrayList<int[]> _RCL = new ArrayList<int[]>();
        return _RCL;
    }

    /*
     * (non-Javadoc)
     * 
     * @see metaheuristics.tabusearch.AbstractTS#makeTL()
     */
    @Override
    public ArrayDeque<int[]> makeTL() {

        ArrayDeque<int[]> _TS = new ArrayDeque<int[]>(2 * tenure);

        for (int i = 0; i < 2 * tenure; i++) {
            _TS.add(fake);
        }

        return _TS;

    }

    /*
     * {@inheritDoc}
     * 
     * This method initializes an empty CL and iterate over every possible
     * candidate. One element is inserted in CL if it is feasible and is not in the
     * current solution.
     */
    @Override
    public void updateCL() {

        ArrayList<int[]> _CL = new ArrayList<int[]>();
        Set<List<Integer>> sol = new HashSet<List<Integer>>();

        pap.accumulate(currentSol);

        // solution to list
        for (int[] elem : currentSol)
            sol.add(Arrays.asList(new Integer[] { elem[0], elem[1], elem[2] }));

        for (int p = 0; p < pap.P; p++) {
            for (int d = 0; d < pap.D; d++) {
                for (int t = 0; t < pap.T; t++) {

                    // candidate as list to check if it is in solution
                    List<Integer> elist = Arrays.asList(new Integer[] { p, d, t });

                    // an element can be a candidate if not in solution
                    if (!sol.contains(elist)) {

                        // cast list of Integer to array of int
                        int[] e = elist.stream().mapToInt(Integer::intValue).toArray();

                        // an element is only a candidate if it is feasible to the problem
                        if (pap.isElemFeasible(e))
                            _CL.add(e);

                    }

                }
            }
        }

        // copy _CL into CL
        CL = new ArrayList<int[]>(_CL);

    }

    /**
     * {@inheritDoc}
     * 
     * This createEmptySol instantiates an empty solution and it attributes a -100*D
     * cost, since none discipline is allocated.
     */
    @Override
    public Solution<int[]> createEmptySol() {
        Solution<int[]> sol = new Solution<int[]>();
        sol.cost = 10000.0 * pap.D;
        return sol;
    }

    /**
     * {@inheritDoc}
     * 
     * The local search operator developed for the PAP objective function is
     * composed by the neighborhood moves Insertion, Removal and 2-Exchange.
     */
    @Override
    public Solution<int[]> neighborhoodMove() {

        Double minDeltaCost;
        int[] bestCandIn = null, bestCandOut = null;
        minDeltaCost = Double.POSITIVE_INFINITY;

        // update candidate list
        updateCL();

        // Evaluate insertions of non-tabu candidates
        for (int[] candIn : CL) {
            Double deltaCost = ObjFunction.evaluateInsertionCost(candIn, currentSol);
            if (!TL.contains(candIn) || currentSol.cost + deltaCost < incumbentSol.cost) {
                if (deltaCost < minDeltaCost) {
                    minDeltaCost = deltaCost;
                    bestCandIn = candIn;
                    bestCandOut = null;
                }
            }
        }

        // Evaluate removals of non-tabu candidates
        for (int[] candOut : currentSol) {
            Double deltaCost = ObjFunction.evaluateRemovalCost(candOut, currentSol);
            if (!TL.contains(candOut) || currentSol.cost + deltaCost < incumbentSol.cost) {
                if (deltaCost < minDeltaCost) {
                    minDeltaCost = deltaCost;
                    bestCandIn = null;
                    bestCandOut = candOut;
                }
            }
        }

        // Evaluate exchanges of non-tabu candidates
        for (int[] candIn : CL) {
            for (int[] candOut : currentSol) {
                Double deltaCost = ObjFunction.evaluateExchangeCost(candIn, candOut, currentSol);
                if ((!TL.contains(candIn) && !TL.contains(candOut))
                        || currentSol.cost + deltaCost < incumbentSol.cost) {
                    if (deltaCost < minDeltaCost) {
                        minDeltaCost = deltaCost;
                        bestCandIn = candIn;
                        bestCandOut = candOut;
                    }
                }
            }
        }

        // Implement the best non-tabu move
        TL.poll();
        if (bestCandOut != null) {
            currentSol.remove(bestCandOut);
            TL.add(bestCandOut);
        } else {
            TL.add(fake);
        }
        TL.poll();
        if (bestCandIn != null) {
            currentSol.add(bestCandIn);
            TL.add(bestCandIn);
        } else {
            TL.add(fake);
        }
        ObjFunction.evaluate(currentSol);

        return null;

    }

    /*
     * Run Tabu Search for PAP.
     */
    public static void run(int tenure, int maxIt, String filename, double maxTime, String resultsFileName,
            String instName) throws IOException {

        long startTime = System.currentTimeMillis();
        TS_PAP ts = new TS_PAP(tenure, maxIt, filename, resultsFileName, instName);
        Solution<int[]> bestSol = ts.solve(maxTime);
        System.out.println("maxVal = " + bestSol);
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        System.out.println("Time = " + (double) totalTime / (double) 1000 + " seg");
        System.out.println(ts.pap.isSolFeasible(bestSol));
        System.out.println(Arrays.toString(ts.pap.ntp));
        System.out.println(Arrays.toString(ts.pap.nhdp));

    }

    /**
     * A main method used for testing the TS metaheuristic.
     */
    public static void main(String[] args) throws IOException {

        String inst[] = { "instances/P50D50S1.pap", "instances/P50D50S3.pap", "instances/P50D50S5.pap",
                "instances/P70D70S1.pap", "instances/P70D70S3.pap", "instances/P70D70S5.pap", "instances/P70D100S6.pap",
                "instances/P70D100S8.pap", "instances/P70D100S10.pap", "instances/P100D150S10.pap",
                "instances/P100D150S10.pap", "instances/P100D150S20.pap" };

        // test all instances
        for (String file : inst) {
            String name = file.substring(file.indexOf("/") + 1, file.indexOf("."));
            TS_PAP.run(20, 1000, file, 1800.0, "pap_ts_resultados.csv", name);
        }

    }

}
