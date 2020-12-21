package problems.pap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.util.Set;
import java.util.HashSet;

import problems.Evaluator;
import solutions.Solution;

public class PAP implements Evaluator<int[]> {

  public final Integer size;

  public int P;

  public int D;

  public int T;

  public int S;

  public int H;

  public int[] h;

  public int[][] a;

  public int[][] r;

  public int[] w;

  public int[][] x;

  public int[][] y;

  public int[][] z;

  /**
   * number of professors p allocated to discipline d
   */
  public int[] npd;

  /**
   * number of periods t a discipline d is allocated at
   */
  public int[] ntd;

  /**
   * number of disciplines d allocated at time t
   */
  public int[] ndt;

  /**
   * number of times t a professor p works
   */
  public int[] ntp;

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

  @Override
  public Double evaluate(Solution<int[]> sol) {

    Double _cost = 0.0;
    Set<Integer> alloc = new HashSet<Integer>();

    for (int[] elem : sol) {

      // get professor and discipline
      int p = elem[0], d = elem[1], t = elem[2];

      // increment cost
      _cost += a[p][d];

      // add d to the allocated set
      alloc.add(d);

    }

    // penalize non allocated disciplines
    _cost -= 100 * (D - alloc.size());

    return sol.cost = _cost;

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

  public Double evaluateInsertionPAP(int[] elem) {

    Double insCost = 0.0;

    // get professor and discipline
    int p = elem[0], d = elem[1], t = elem[2];

    // element already in solution
    if (x[p][d] == 1) {
      return 0.0;
    }

    // increment cost
    insCost += a[p][d];

    // if the discipline is not allocated to a professor already, the insertion will
    // remove the cost penalty associated to it
    if (w[d] == 0) {
      insCost += 100;
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

  public Double evaluateRemovalPAP(int[] elem) {

    Double remCost = 0.0;

    // get professor and discipline
    int p = elem[0], d = elem[1];

    // element not in solution
    if (x[p][d] == 0) {
      return 0.0;
    }

    // decrement cost
    remCost -= a[p][d];

    // if the discipline is allocated already and there aren't other professors
    // giving it, removing the element would penalize the solution
    if (w[d] == 1) {
      remCost -= 100;
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

      // if the discipline dIn is not allocated already, the insertion will remove the
      // cost penalty
      if (w[dIn] == 0) {
        exCost += 100;
      }

      // if the discipline dOut is allocated already and there aren't other professors
      // giving it, removing the element would penalize the solution
      if (w[dOut] == 1) {
        exCost -= 100;
      }

    }

    return 0.0;

  }

  public void accumulate(Solution<int[]> sol) {

    setVariables(sol);

    // instantiate accumulators
    npd = new int[D];
    ntd = new int[D];
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
        if (y[d][t] == 1) {
          ntd[d]++;
          ndt[t]++;
        }
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

  public Boolean isElemFeasible(int[] elem) {

    // get professor p, discipline d and time t
    int p = elem[0], d = elem[1], t = elem[2];

    // if there is other professor giving d, elem is infeasible
    if (npd[d] == 1 && x[p][d] == 0)
      return false;

    // if d is already allocated at h[d] periods, elem is infeasible
    if (ntd[d] == h[d] && y[d][t] == 0)
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

  public Boolean isSolFeasible(Solution<int[]> sol) {

    accumulate(sol);

    for (int d = 0; d < D; d++) {
      for (int t = 0; t < T; t++) {

        // at most S disciplines can be allocated at time t
        if (ndt[t] > S)
          return false;

      }

      // each discipline d requires h[d] times in a week
      if (ntd[d] != h[d])
        return false;

      for (int p = 0; p < P; p++) {

        // a discipline must be allocated to at most 1 professor
        if (npd[d] > 1)
          return false;

      }
    }

    for (int p = 0; p < P; p++) {
      for (int t = 0; t < T; t++) {

        // a professor p only can work at time t if r[p][t] == 1
        if (z[p][t] > r[p][t])
          return false;

        // a professor p can work in at most H times
        if (ntp[p] > H)
          return false;

      }
    }

    return true;

  }

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
