package metaheuristics.tabusearch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Random;

import problems.Evaluator;
import solutions.Solution;

/**
 * Abstract class for metaheuristic Tabu Search. It consider a minimization
 * problem.
 * 
 * @author ccavellucci, fusberti
 * @param <E> Generic type of the candidate to enter the solution.
 */
public abstract class AbstractTS<E> {

	/**
	 * flag that indicates whether the code should print more information on screen
	 */
	public static boolean verbose = true;

	/**
	 * a random number generator
	 */
	private Random rng;

	/**
	 * path to store the history file.
	 */
	protected String resultsFileName;

	/**
	 * instance name.
	 */
	protected String instName;

	/**
	 * the objective function being optimized
	 */
	protected Evaluator<E> ObjFunction;

	/**
	 * the incumbent solution cost
	 */
	protected Double incumbentCost;

	/**
	 * the current solution cost
	 */
	protected Double currentCost;

	/**
	 * the incumbent solution
	 */
	protected Solution<E> incumbentSol;

	/**
	 * the current solution
	 */
	protected Solution<E> currentSol;

	/**
	 * the number of iterations the TS main loop executes.
	 */
	protected Integer iterations;

	/**
	 * current iteration.
	 */
	protected Integer currIteration;

	/**
	 * the tabu tenure.
	 */
	protected Integer tenure;

	/**
	 * the Candidate List of elements to enter the solution.
	 */
	protected ArrayList<E> CL;

	/**
	 * the Restricted Candidate List of elements to enter the solution.
	 */
	protected ArrayList<E> RCL;

	/**
	 * the Tabu List of elements to enter the solution.
	 */
	protected ArrayDeque<E> TL;

	/**
	 * Creates the Candidate List, which is an ArrayList of candidate elements that
	 * can enter a solution.
	 * 
	 * @return The Candidate List.
	 */
	public abstract ArrayList<E> makeCL();

	/**
	 * Creates the Restricted Candidate List, which is an ArrayList of the best
	 * candidate elements that can enter a solution.
	 * 
	 * @return The Restricted Candidate List.
	 */
	public abstract ArrayList<E> makeRCL();

	/**
	 * Creates the Tabu List, which is an ArrayDeque of the Tabu candidate elements.
	 * The number of iterations a candidate is considered tabu is given by the Tabu
	 * Tenure {@link #tenure}
	 * 
	 * @return The Tabu List.
	 */
	public abstract ArrayDeque<E> makeTL();

	/**
	 * Updates the Candidate List according to the incumbent solution
	 * {@link #currentSol}. In other words, this method is responsible for updating
	 * the costs of the candidate solution elements.
	 */
	public abstract void updateCL();

	/**
	 * Creates a new solution which is empty, i.e., does not contain any candidate
	 * solution element.
	 * 
	 * @return An empty solution.
	 */
	public abstract Solution<E> createEmptySol();

	/**
	 * The TS local search phase is responsible for repeatedly applying a
	 * neighborhood operation while the solution is getting improved, i.e., until a
	 * local optimum is attained. When a local optimum is attained the search
	 * continues by exploring moves which can make the current solution worse.
	 * Cycling is prevented by not allowing forbidden (tabu) moves that would
	 * otherwise backtrack to a previous solution.
	 * 
	 * @return An local optimum solution.
	 */
	public abstract Solution<E> neighborhoodMove();

	/**
	 * Constructor for the AbstractTS class.
	 * 
	 * @param objFunction     The objective function being minimized.
	 * @param tenure          The Tabu tenure parameter.
	 * @param iterations      The number of iterations which the TS will be
	 *                        executed.
	 * @param resultsFileName The file where the results will be stored.
	 * @param instName        The instance name.
	 */
	public AbstractTS(Evaluator<E> objFunction, Integer tenure, Integer iterations, String resultsFileName,
			String instName) {
		this.ObjFunction = objFunction;
		this.tenure = tenure;
		this.iterations = iterations;
		this.resultsFileName = resultsFileName;
		this.instName = instName;
		this.rng = new Random(0);
	}

	/**
	 * The TS constructive heuristic, which is responsible for building a feasible
	 * solution by selecting in a greedy fashion, candidate elements to enter the
	 * solution.
	 * 
	 * @return A feasible solution to the problem being minimized.
	 */
	public Solution<E> constructiveHeuristic() {

		CL = makeCL();
		RCL = makeRCL();
		currentSol = createEmptySol();
		currentCost = Double.POSITIVE_INFINITY;

		/* Main loop, which repeats until the stopping criteria is reached. */
		while (!constructiveStopCriteria()) {

			Double maxCost = Double.NEGATIVE_INFINITY, minCost = Double.POSITIVE_INFINITY;
			currentCost = currentSol.cost;
			updateCL();

			// always stop when CL is empty
			if (CL.size() == 0)
				break;

			/*
			 * Explore all candidate elements to enter the solution, saving the highest and
			 * lowest cost variation achieved by the candidates.
			 */
			for (E c : CL) {
				Double deltaCost = ObjFunction.evaluateInsertionCost(c, currentSol);
				if (deltaCost < minCost)
					minCost = deltaCost;
				if (deltaCost > maxCost)
					maxCost = deltaCost;
			}

			/*
			 * Among all candidates, insert into the RCL those with the highest performance.
			 */
			for (E c : CL) {
				Double deltaCost = ObjFunction.evaluateInsertionCost(c, currentSol);
				if (deltaCost <= minCost) {
					RCL.add(c);
				}
			}

			/* Choose a candidate randomly from the RCL */
			int rndIndex = rng.nextInt(RCL.size());
			E inCand = RCL.get(rndIndex);
			CL.remove(inCand);
			currentSol.add(inCand);
			ObjFunction.evaluate(currentSol);
			RCL.clear();

		}

		return currentSol;
	}

	/**
	 * The TS mainframe. It consists of a constructive heuristic followed by a loop,
	 * in which each iteration a neighborhood move is performed on the current
	 * solution. The best solution is returned as result.
	 * 
	 * @param maxTime Time limit.
	 * @return The best feasible solution obtained throughout all iterations.
	 */
	public Solution<E> solve(double maxTime) {

		long startTime = System.currentTimeMillis(), endTime;
		double totalTime;

		// constructive phase
		incumbentSol = createEmptySol();
		constructiveHeuristic();
		TL = makeTL();

		for (currIteration = 0; currIteration < iterations; currIteration++) {

			// local search
			neighborhoodMove();

			if (incumbentSol.cost > currentSol.cost) {
				// found a better solution
				incumbentSol = new Solution<E>(currentSol);
				if (verbose)
					printSolutionMeasure((System.currentTimeMillis() - startTime) / (double) 1000);
			}

			endTime = System.currentTimeMillis();
			totalTime = (endTime - startTime) / (double) 1000;

			// if it exceeded the time limit, break the loop
			if (totalTime > maxTime)
				break;

		}

		return incumbentSol;
	}

	/**
	 * A standard stopping criteria for the constructive heuristic is to repeat
	 * until the incumbent solution improves by inserting a new candidate element.
	 * 
	 * @return true if the criteria is met.
	 */
	public Boolean constructiveStopCriteria() {
		return (currentCost > currentSol.cost) ? false : true;
	}

	/**
	 * Prints the measures of the new incumbent solution
	 * 
	 * @param totalTime Time ellapsed.
	 */
	private void printSolutionMeasure(double totalTime) {

		// print result in stdout
		if (verbose)
			System.out.println("(Iter. " + currIteration + ", Time " + totalTime + ") BestSol = " + incumbentSol);

		// write result in file if a file name was given
		if (resultsFileName != null) {
			try {
				printSolutionMeasuresToFile(totalTime);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}

	}

	/**
	 * Saves the measures of the new incumbent solution to file
	 * 
	 * @param totalTime Time ellapsed.
	 * @throws IOException Necessary for I/O operations.
	 */
	private void printSolutionMeasuresToFile(double totalTime) throws IOException {

		FileWriter file;
		File fp = new File(resultsFileName);

		// create or open file
		if (!fp.exists()) {
			file = new FileWriter(resultsFileName);
			String header = "instance;solutionCost;iterations;time;solutionSize\n";
			file.write(header);
		} else {
			file = new FileWriter(resultsFileName, true);
		}

		// write current result to file
		String result = String.format("%s;%s;%s;%s;%s\n", instName, -1.00 * incumbentSol.cost,
				currIteration, totalTime, incumbentSol.size());
		file.write(result);
		file.close();

	}

}
