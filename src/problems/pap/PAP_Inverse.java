package problems.pap;

import java.io.IOException;

import solutions.Solution;

/**
 * Class representing the inverse of the Professor Allocation Problem objective
 * function, which is used since the TS is set by default as a minimization
 * procedure.
 * 
 * @author brichau, einnarelli, rmeirelles
 */
public class PAP_Inverse extends PAP {

    /**
     * Constructor for the PAP_Inverse class.
     * 
     * @param filename Name of the file for which the objective function parameters
     *                 should be read.
     * @throws IOException Necessary for I/O operations.
     */
    public PAP_Inverse(String filename) throws IOException {
        super(filename);
    }

    /*
     * (non-Javadoc)
     * 
     * @see problems.pap.PAP#evaluate()
     */
    @Override
    public Double evaluatePAP(Solution<int[]> sol) {
        return -super.evaluatePAP(sol);
    }

    /*
     * (non-Javadoc)
     * 
     * @see problems.pap.PAP#evaluateInsertionPAP()
     */
    @Override
    public Double evaluateInsertionPAP(int[] elem) {
        return -super.evaluateInsertionPAP(elem);
    }

    /*
     * (non-Javadoc)
     * 
     * @see problems.pap.PAP#evaluateRemovalPAP()
     */
    @Override
    public Double evaluateRemovalPAP(int[] elem) {
        return -super.evaluateRemovalPAP(elem);
    }

    /*
     * (non-Javadoc)
     * 
     * @see problems.pap.PAP#evaluateExchangePAP()
     */
    @Override
    public Double evaluateExchangePAP(int[] elemIn, int[] elemOut) {
        return -super.evaluateExchangePAP(elemIn, elemOut);
    }

}