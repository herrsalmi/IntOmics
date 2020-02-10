package org.pmoi.utils;

import java.util.Arrays;

public class BenjaminiHochbergFDR {
    /**
     * the raw p-values that were given as input for the constructor.
     */
    private double[] pvalues;

    /**
     * the adjusted p-values ordened in ascending order.
     */
    private double[] adjustedPvalues;

    /**
     * the number of tests.
     */
    private int m;

    /**
     * Constructor.
     *
     * @param p P-Values.
     */
    public BenjaminiHochbergFDR(double[] p) {
        this.pvalues = p;
        this.m = pvalues.length;
        this.adjustedPvalues = new double[m];
    }

    /**
     * method that calculates the Benjamini and Hochberg correction of
     * the false discovery rate.
     */
    public void calculate() {

        // order the pvalues.
        Arrays.sort(pvalues);

        // iterate through all p-values:  largest to smallest
        for (int i = m - 1; i >= 0; i--) {
            if (i == m - 1) {
                adjustedPvalues[i] = pvalues[i];
            } else {
                double unadjustedPvalue = pvalues[i];
                int divideByM = i + 1;
                double left = adjustedPvalues[i + 1];
                double right = (m / (double) divideByM) * unadjustedPvalue;
                adjustedPvalues[i] = Math.min(left, right);
            }
        }
    }

    /**
     * getter for the ordened p-values.
     *
     * @return String[] with the ordened p-values.
     */
    public double[] getOrdenedPvalues() {
        return pvalues;
    }

    /**
     * getter for the adjusted p-values.
     *
     * @return String[] with the adjusted p-values.
     */
    public double[] getAdjustedPvalues() {
        return adjustedPvalues;
    }
}
