package org.pmoi;

public class ApplicationParameters {

    private static ApplicationParameters instance;

    private static final String STRING = "0.1b";
    private static final int MAX_TRIES = 100;
    private static final double GENE_FOLD_CHANGE = 1.5;
    private static final boolean ADD_PATHWAYS = true;

    private ApplicationParameters() {
    }

    public String getVersion() {
        return STRING;
    }

    public int getMaxTries() {
        return MAX_TRIES;
    }

    public double getGeneFoldChange() {
        return GENE_FOLD_CHANGE;
    }

    public boolean addPathways() {
        return ADD_PATHWAYS;
    }

    public static synchronized ApplicationParameters getInstance() {
        if (instance == null) {
            instance = new ApplicationParameters();
        }
        return instance;
    }
}
