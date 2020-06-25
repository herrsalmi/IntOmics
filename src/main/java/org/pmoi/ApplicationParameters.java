package org.pmoi;

public class ApplicationParameters {

    private static ApplicationParameters instance;

    private final String version = "0.1b";
    private final int maxTries = 100;
    private final double geneFoldChange = 1.5;
    //TODO should be specified by the used
    private final String ncbiAPIKey = "40065544fb6667a5a723b649063fbe596e08";
    private final boolean addPathways = true;

    private ApplicationParameters() {

    }

    public String getVersion() {
        return version;
    }

    public int getMaxTries() {
        return maxTries;
    }

    public String getNcbiAPIKey() {
        return ncbiAPIKey;
    }

    public double getGeneFoldChange() {
        return geneFoldChange;
    }

    public boolean addPathways() {
        return addPathways;
    }

    public static synchronized ApplicationParameters getInstance() {
        if (instance == null) {
            instance = new ApplicationParameters();
        }
        return instance;
    }
}
