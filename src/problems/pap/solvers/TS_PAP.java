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

        System.out.println(_CL.size());

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
        sol.cost = -100.0 * pap.D;
        return sol;
    }

    @Override
    public Solution<int[]> neighborhoodMove() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Boolean constructiveStopCriteria() {
        Boolean isSolFeasible = pap.isSolFeasible(currentSol);
        System.out.println(isSolFeasible);
        return super.constructiveStopCriteria() && isSolFeasible;
    }

    /**
     * A main method used for testing the TS metaheuristic.
     */
    public static void main(String[] args) throws IOException {

        TS_PAP tabusearch = new TS_PAP(20, 10000, "instances/P50D50S5.pap");
        PAP_Inverse pap = (PAP_Inverse) tabusearch.ObjFunction;

        int[] test = new int[] { 1, 2, 3 };
        int[] test2 = new int[] { 1, 2, 3 };

        tabusearch.constructiveHeuristic();

        System.out.println(pap.isSolFeasible(tabusearch.currentSol));
        System.out.println(tabusearch.currentSol.toString());

    }

}
