package org.pmoi;


public class MainEntry {

    private static final String VERSION = "0.1";
    public static final boolean USE_GENE_NAME = true;
    private static final String PROG_NAME = "IntOmics";
    public static final int MAX_TRIES = 100;
    public static final int STRINGDB_SCORE = 980;
    public static final String NCBI_API_KEY = "40065544fb6667a5a723b649063fbe596e08";

    public MainEntry() {
        OperationDispatcher operationDispatcher = new OperationDispatcher();
        operationDispatcher.run();
    }


    public static void main(String[] args) {
        new MainEntry();
    }
}
