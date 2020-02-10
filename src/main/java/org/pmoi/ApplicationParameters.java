package org.pmoi;

public class ApplicationParameters {

    private static volatile ApplicationParameters instance;

    private final String version = "0.1b";
    private final String progName = "IntOmics";
    private final int maxTries = 100;
    private final int stringDBScore = 900;
    private final double proteinFoldChange = 1.3;
    private final double geneFoldChange = 1.5;
    private final boolean use48h = true;
    private final String ncbiAPIKey = "40065544fb6667a5a723b649063fbe596e08";

    private ApplicationParameters() {

    }

    public String getVersion() {
        return version;
    }

    public String getProgName() {
        return progName;
    }

    public int getMaxTries() {
        return maxTries;
    }

    public int getStringDBScore() {
        return stringDBScore;
    }

    public double getProteinFoldChange() {
        return proteinFoldChange;
    }

    public String getNcbiAPIKey() {
        return ncbiAPIKey;
    }

    public double getGeneFoldChange() {
        return geneFoldChange;
    }

    public boolean use48H() {
        return use48h;
    }

    public static ApplicationParameters getInstance() {
        if (instance == null) {
            synchronized (ApplicationParameters.class) {
                if (instance == null) {
                    instance = new ApplicationParameters();
                }
            }
        }
        return instance;
    }
}
