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

    @Parameter(names = {"-f", "--format"}, description = "Output format: TSV or FWF (Fixed Width Format)",
            validateWith = FormatValidator.class)
    private OutputMode format = OutputMode.TSV;

    @Parameter(names = {"-db", "--pathway"}, description = "Pathway database: KEGG or WikiPathways",
            validateWith = PathwayValidator.class)
    private PathwayMode pathwayDB = PathwayMode.WIKIPATHWAYS;

    @Parameter(names = {"-s", "--minscore"}, description = "Minimum StringDB interaction score",
            validateWith = InteractionScoreValidator.class)
    private int stringDBScore = 900;

    @Parameter(names = {"-fc"}, description = "Fold change cutoff",
            validateWith = FoldChangeValidator.class)
    private double foldChange = 1.5;

    @Parameter(names = {"-t", "--threads"}, description = "Number of threads to use",
            validateWith = ThreadsValidator.class)
    private int threads = 4;

    @Parameter(names = {"-d"}, description = "Custom separator for CSV file")
    private char separator = ';';

    @Parameter(names = {"-h", "--help"}, description = "Print help screen", help = true)
    private boolean help;

    @Parameter(names = {"-i"}, description = "Pull an up to date list of pathways")
    private boolean useOnlineDB = false;

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

    public char getSeparator() {
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

    public boolean isHelp() {
        return help;
    }

    public boolean useOnlineDB() {
        return useOnlineDB;
    }

    public static synchronized Args getInstance() {
        if (instance == null)
            instance = new Args();
        return instance;
    }
}
