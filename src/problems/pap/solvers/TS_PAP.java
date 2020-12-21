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

public class TS_PAP extends AbstractTS<int[]> {

    private final int[] fake = new int[] {};

    PAP_Inverse pap;

    public TS_PAP(Integer tenure, Integer iterations, String filename) throws IOException {

        super(new PAP_Inverse(filename), tenure, iterations);

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

    @Override
    public void updateCL() {

        Set<List<Integer>> _CL = new HashSet<List<Integer>>();
        Set<List<Integer>> sol = new HashSet<List<Integer>>();

        // solution to list
        for (int[] elem : currentSol)
            sol.add(Arrays.asList(new Integer[] { elem[0], elem[1], elem[2] }));

        // initialize _CL with all elements not in solution
        for (int p = 0; p < pap.P; p++) {
            for (int d = 0; d < pap.D; d++) {
                for (int t = 0; t < pap.T; t++) {

                    // candidate as list
                    List<Integer> e = Arrays.asList(new Integer[] {p, d, t});

                    // add to _CL if not in solution
                    if (!sol.contains(e)) {
                        _CL.add(e);
                    }

                }
            }
        }

        // TODO - is element feasible?

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
        sol.cost = -100.0 * pap.D;
        return sol;
    }

    @Override
    public Solution<int[]> neighborhoodMove() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * A main method used for testing the TS metaheuristic.
     */
    public static void main(String[] args) throws IOException {

        TS_PAP tabusearch = new TS_PAP(20, 10000, "instances/P50D50S1.pap");
        PAP_Inverse pap = (PAP_Inverse) tabusearch.ObjFunction;
        
        int[] test = new int[] { 1, 2, 3 };
        int[] test2 = new int[] { 1, 2, 3 };

        tabusearch.constructiveHeuristic();

        System.out.println(tabusearch.currentSol.toString());
        System.out.println(pap.isSolFeasible(tabusearch.currentSol));

    }

}
