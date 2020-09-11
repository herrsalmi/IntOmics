package org.pmoi;

import com.beust.jcommander.Parameter;
import org.pmoi.model.OutputMode;
import org.pmoi.model.PathwayMode;
import org.pmoi.validator.*;

public class Args {

    private static Args instance;

    private Args() {
    }

    @Parameter(names = {"-p", "--proteins"}, description = "File containing secreted proteins", required = true,
            validateWith = ProteinValidator.class, order = 0)
    private String secretome;

    @Parameter(names = {"-g", "--genes"}, description = "File containing differentially expressed genes", required = true,
            validateWith = GeneValidator.class, order = 1)
    private String transcriptome;

    @Parameter(names = {"-a", "--allgenes"}, description = "File containing all expressed genes", required = true,
            validateWith = GeneValidator.class, order = 2)
    private String allGenes;

    @Parameter(names = {"-f", "--format"}, description = "Output format: TSV or Fixed Width Format",
            validateWith = FormatValidator.class, order = 3)
    private OutputMode format = OutputMode.TSV;

    @Parameter(names = {"-db", "--pathway"}, description = "Pathway database for GSEA",
            validateWith = PathwayValidator.class, order = 4)
    private PathwayMode pathwayDB = PathwayMode.WIKIPATHWAYS;

    @Parameter(names = {"-s", "--minscore"}, description = "Minimum StringDB interaction score",
            validateWith = InteractionScoreValidator.class, order = 5)
    private int stringDBScore = 900;

    @Parameter(names = {"-fc"}, description = "Fold change cutoff",
            validateWith = FoldChangeValidator.class, order = 6)
    private double foldChange = 1.5;

    @Parameter(names = {"-pv", "--pvalue"}, description = "P-value cutoff", validateWith = PvalueValidator.class
            , order = 7)
    private double pvalue = 0.05;

    @Parameter(names = {"-d"}, description = "Custom separator for CSV file", order = 8)
    private String separator = ";";

    @Parameter(names = {"-t", "--threads"}, description = "Number of threads to use",
            validateWith = ThreadsValidator.class, order = 9)
    private int threads = 4;

    @Parameter(names = {"--no-cached-sets"}, description = "Pull an up to date list of pathways", order = 10)
    private boolean useOnlineDB = false;

    @Parameter(names = {"--no-cached-ppi"}, description = "Force use StringDB's online service", order = 11)
    private boolean useOnlinePPI = false;

    @Parameter(names = {"-h", "--help"}, description = "Print help screen", help = true, order = 12)
    private boolean help;

    public String getSecretome() {
        return secretome;
    }

    public String getTranscriptome() {
        return transcriptome;
    }

    public OutputMode getFormat() {
        return format;
    }

    public int getStringDBScore() {
        return stringDBScore;
    }

    public String getAllGenes() {
        return allGenes;
    }

    public String getSeparator() {
        return separator;
    }

    public PathwayMode getPathwayDB() {
        return pathwayDB;
    }

    public int getThreads() {
        return threads;
    }

    public double getFoldChange() {
        return foldChange;
    }

    public double getPvalue() {
        return pvalue;
    }

    public boolean isHelp() {
        return help;
    }

    public boolean useOnlineDB() {
        return useOnlineDB;
    }

    public boolean useOnlinePPI() {
        return useOnlinePPI;
    }

    public static synchronized Args getInstance() {
        if (instance == null)
            instance = new Args();
        return instance;
    }
}
