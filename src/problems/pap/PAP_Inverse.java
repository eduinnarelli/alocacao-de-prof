package problems.pap;

import java.io.IOException;

import solutions.Solution;

public class PAP_Inverse extends PAP {

    public PAP_Inverse(String filename) throws IOException {
        super(filename);
    }

    @Override
    public Double evaluate(Solution<int[]> sol) {
        return -super.evaluate(sol);
    }

    @Override
    public Double evaluateInsertionPAP(int[] elem) {
        return -super.evaluateInsertionPAP(elem);
    }

    @Override
    public Double evaluateRemovalPAP(int[] elem) {
        return -super.evaluateRemovalPAP(elem);
    }

    @Override
    public Double evaluateExchangePAP(int[] elemIn, int[] elemOut) {
        return -super.evaluateExchangePAP(elemIn, elemOut);
    }

}