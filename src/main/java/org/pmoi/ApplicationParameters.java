package org.pmoi;

public class ApplicationParameters {

    private static ApplicationParameters instance;

    private static final String STRING = "0.1b";
    private static final int MAX_TRIES = 100;
    private static final double GENE_FOLD_CHANGE = 1.5;
    //TODO should be specified by the used
    private static final String NCBI_API_KEY = "40065544fb6667a5a723b649063fbe596e08";
    private static final boolean ADD_PATHWAYS = true;

    private ApplicationParameters() {

    }

    public String getVersion() {
        return STRING;
    }

    public int getMaxTries() {
        return MAX_TRIES;
    }

    public String getNcbiAPIKey() {
        return NCBI_API_KEY;
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
