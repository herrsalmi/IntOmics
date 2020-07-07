package org.pmoi;

public class ApplicationParameters {

    private static ApplicationParameters instance;

    private static final double GENE_FOLD_CHANGE = 1.5;

    private ApplicationParameters() {
    }

    public double getGeneFoldChange() {
        return GENE_FOLD_CHANGE;
    }

    public static synchronized ApplicationParameters getInstance() {
        if (instance == null) {
            instance = new ApplicationParameters();
        }
        return instance;
    }
}
