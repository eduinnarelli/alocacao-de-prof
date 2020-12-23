package problems.pap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;

import problems.Evaluator;
import solutions.Solution;

/**
 * Class representing the Professor Allocation Problem, with methods to evaluate
 * the neighborhood moves and check feasibility.
 * 
 * @author brichau, einnarelli, rmeirelles
 */
public class PAP implements Evaluator<int[]> {

  /**
   * dimension of the domain
   */
  public final Integer size;

  /**
   * penalty given to infeasible solutions
   */
  public final int pen = 5000;

  /**
   * number of professors
   */
  public int P;

  /**
   * number of disciplines
   */
  public int D;

  /**
   * number of times
   */
  public int T;

  /**
   * number of rooms available
   */
  public int S;

  /**
   * number of times an professor can work at the week
   */
  public int H;

  /**
   * number of times required per discipline
   */
  public int[] h;

  /**
   * avaliation of professor p at discipline d
   */
  public int[][] a;

  /**
   * possibility of professor p to work at time t
   */
  public int[][] r;

  /**
   * number of times t a discipline d is allocated at in solution
   */
  public int[] w;

  /**
   * professor p allocated to discipline d in solution
   */
  public int[][] x;

  /**
   * discipline d given at time t in solution
   */
  public int[][] y;

  /**
   * professor p working at time t in solution
   */
  public int[][] z;

  /**
   * number of professors p allocated to discipline d in solution
   */
  public int[] npd;

  /**
   * number of disciplines d allocated at time t in solution
   */
  public int[] ndt;

  /**
   * number of times t a professor p works in solution
   */
  public int[] ntp;

  /**
   * The constructor for the PAP class.
   * 
   * @param filename Name of the file containing the input for setting the PAP.
   * @throws IOException Necessary for I/O operations.
   */
  public PAP(String filename) throws IOException {
    size = readInput(filename);
  }

  /*
   * (non-Javadoc)
   * 
   * @see problems.Evaluator#getDomainSize()
   */
  @Override
  public Integer getDomainSize() {
    return size;
  }

  /*
   * (non-Javadoc)
   * 
   * @see problems.Evaluator#evaluate()
   */
  @Override
  public Double evaluate(Solution<int[]> sol) {
    setVariables(sol);
    return sol.cost = evaluatePAP(sol);
  }

  /**
   * Evaluate PAP objective function, penalizing it if there are non allocated
   * disciplines or if the solution is infeasible.
   * 
   * @param sol PAP solution to be evaluated.
   */
  public Double evaluatePAP(Solution<int[]> sol) {

    Double _cost = 0.0;

    for (int p = 0; p < P; p++) {
      for (int d = 0; d < D; d++) {

        // increment cost
        _cost += a[p][d] * x[p][d];

      }
    }

    for (int d = 0; d < D; d++) {

      // penalize non allocated disciplines
      if (w[d] == 0)
        _cost -= 100;

      // penalize infeasible solutions
      else if (w[d] != h[d])
        _cost -= pen;

    }

    return _cost;

  }

  /*
   * (non-Javadoc)
   * 
   * @see problems.Evaluator#evaluateInsertionCost(java.lang.Object,
   * solutions.Solution)
   */
  @Override
  public Double evaluateInsertionCost(int[] elem, Solution<int[]> sol) {

    setVariables(sol);
    return evaluateInsertionPAP(elem);

  }

  /**
   * Evaluate the insertion of an element [p, d, t] in the current solution.
   * 
   * @param elem The element to enter the solution.
   */
  public Double evaluateInsertionPAP(int[] elem) {

    Double insCost = 0.0;

    // get professor and discipline
    int p = elem[0], d = elem[1];

    // element already in solution
    if (x[p][d] == 1) {
      return 0.0;
    }

    // increment cost; as in a feasible solution a discipline is allocated to at
    // most one professor, the insertion will always remove the cost penalty
    // associated to the non allocation of a discipline
    insCost += a[p][d] + 100;

    // remove feasibility penalty if insertion make solution feasible
    if (w[d] == h[d] - 1) {
      insCost += pen;
    }

    return insCost;

  }

  /*
   * (non-Javadoc)
   * 
   * @see problems.Evaluator#evaluateRemovalCost(java.lang.Object,
   * solutions.Solution)
   */
  @Override
  public Double evaluateRemovalCost(int[] elem, Solution<int[]> sol) {

    setVariables(sol);
    return evaluateRemovalPAP(elem);

  }

  /**
   * Evaluate the removal of an element [p, d, t] in the current solution.
   * 
   * @param elem The element to leave the solution.
   */
  public Double evaluateRemovalPAP(int[] elem) {

    Double remCost = 0.0;

    // get professor and discipline
    int p = elem[0], d = elem[1];

    // element not in solution
    if (x[p][d] == 0) {
      return 0.0;
    }

    // decrement cost; as in a feasible solution a discipline is allocated to at
    // most one professor, the removal will always penalize the cost
    remCost -= a[p][d] + 100;

    // penalize infeasibility if the removal makes the solution infeasible
    if (w[d] == h[d]) {
      remCost -= pen;
    }

    return remCost;

  }

  /*
   * (non-Javadoc)
   * 
   * @see problems.Evaluator#evaluateExchangeCost(java.lang.Object,
   * java.lang.Object, solutions.Solution)
   */
  @Override
  public Double evaluateExchangeCost(int[] elemIn, int[] elemOut, Solution<int[]> sol) {

    setVariables(sol);
    return evaluateExchangePAP(elemIn, elemOut);

  }

  /**
   * Evaluate the exchange of two elements [p, d, t] in the current solution.
   * 
   * @param elemIn  The element to enter the solution.
   * @param elemOut The element to leave the solution.
   */
  public Double evaluateExchangePAP(int[] elemIn, int[] elemOut) {

    // get professors and disciplines
    int pIn = elemIn[0], dIn = elemIn[1], pOut = elemOut[0], dOut = elemOut[1];

    // same solutions
    if (dIn == dOut && pIn == pOut) {
      return 0.0;

    }

    // elemIn already in solution
    if (x[pIn][dIn] == 1) {
      return evaluateRemovalPAP(elemOut);
    }

    // elemOut not in solution
    if (x[pOut][dOut] == 0) {
      return evaluateInsertionPAP(elemIn);
    }

    // avaliation exchange impact
    Double exCost = (double) (a[pIn][dIn] - a[pOut][dOut]);

    // check if disciplines are equal
    if (dIn != dOut) {

      if (w[dIn] == h[dIn] - 1) {
        exCost += pen;
      }

      if (w[dOut] == h[dOut]) {
        exCost -= pen;
      }

    }

    return 0.0;

  }

  /**
   * Calculate some sums used to check if an element is feasible.
   * 
   * @param sol The PAP solution to which calculate the sums.
   */
  public void accumulate(Solution<int[]> sol) {

    setVariables(sol);

    // instantiate accumulators
    npd = new int[D];
    ndt = new int[T];
    ntp = new int[P];

    for (int d = 0; d < D; d++) {
      for (int p = 0; p < P; p++) {
        // check if professor p gives discipline d
        if (x[p][d] == 1)
          npd[d]++;
      }

      for (int t = 0; t < T; t++) {
        // check if discipline d is given at time t
        if (y[d][t] == 1)
          ndt[t]++;
      }
    }

    for (int p = 0; p < P; p++) {
      for (int t = 0; t < T; t++) {
        // check if professor p works at time t
        if (z[p][t] == 1)
          ntp[p]++;
      }
    }

  }

  /**
   * Checks if an element [p,d,t] can enter the solution.
   * 
   * @param elem The element to be checked.
   */
  public Boolean isElemFeasible(int[] elem) {

    // get professor p, discipline d and time t
    int p = elem[0], d = elem[1], t = elem[2];

    // if there is other professor giving d, elem is infeasible
    if (npd[d] == 1 && x[p][d] == 0)
      return false;

    // if d is already allocated at h[d] periods, elem is infeasible
    if (w[d] == h[d] && y[d][t] == 0)
      return false;

    // if there are S disciplines allocated at time t, d cannot be allocated at t
    if (ndt[t] == S && y[d][t] == 0)
      return false;

    // if professor p cannot work at time t, elem is infeasible
    if (r[p][t] == 0)
      return false;

    // if professor p already work at H times, elem is infeasible
    if (ntp[p] == H && z[p][t] == 0)
      return false;

    return true;

  }

  /**
   * Set the PLI model variables, used to evaluate the solution.
   * 
   * @param sol The PAP solution.
   */
  public void setVariables(Solution<int[]> sol) {

    // reset all
    w = new int[D];
    x = new int[P][D];
    y = new int[D][T];
    z = new int[P][T];

    for (int[] elem : sol) {

      // get professor p, discipline d and time t
      int p = elem[0], d = elem[1], t = elem[2];

      // allocate them
      w[d]++;
      x[p][d] = 1;
      y[d][t] = 1;
      z[p][t] = 1;

    }

  }

  /**
   * Responsible for setting the problem parameters by reading the necessary input
   * from an external file.
   * 
   * @param filename Name of the file containing the input.
   * @return The dimension of the domain.
   * @throws IOException Necessary for I/O operations.
   */
  protected Integer readInput(String filename) throws IOException {

    Reader fileInst = new BufferedReader(new FileReader(filename));
    StreamTokenizer stok = new StreamTokenizer(fileInst);

    // Read P
    for (int i = 0; i < 2; i++)
      stok.nextToken();
    P = (int) stok.nval;

    // Read D
    for (int i = 0; i < 2; i++)
      stok.nextToken();
    D = (int) stok.nval;

    // Read T
    for (int i = 0; i < 2; i++)
      stok.nextToken();
    T = (int) stok.nval;

    // Read S
    for (int i = 0; i < 2; i++)
      stok.nextToken();
    S = (int) stok.nval;

    // Read H
    for (int i = 0; i < 2; i++)
      stok.nextToken();
    H = (int) stok.nval;

    // Read h_d
    h = new int[D];
    stok.nextToken();
    for (int d = 0; d < D; d++) {
      stok.nextToken();
      h[d] = (int) stok.nval;
    }

    // Read a_pd
    a = new int[P][D];
    stok.nextToken();
    for (int p = 0; p < P; p++) {
      for (int d = 0; d < D; d++) {
        stok.nextToken();
        a[p][d] = (int) stok.nval;
      }
    }

    // Read r_pt
    r = new int[P][T];
    stok.nextToken();
    for (int p = 0; p < P; p++) {
      for (int t = 0; t < T; t++) {
        stok.nextToken();
        r[p][t] = (int) stok.nval;
      }
    }

    return P * D * T;

  }

}
