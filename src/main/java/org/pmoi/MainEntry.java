package org.pmoi;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.pmoi.database.SpeciesHelper;
import org.pmoi.util.io.HtmlFormatter;
import org.pmoi.util.io.OutputFormatter;
import org.pmoi.util.io.TSVFormatter;
import org.pmoi.util.io.TextFormatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class MainEntry {

    private static final String VERSION = "1.2.1";
    private static final String PROG_NAME = "IntOmics";

    public static final String OUT_DIR = "output/";

    public MainEntry(String[] args) {
        Args params = Args.getInstance();
        try {
            JCommander jc = JCommander.newBuilder()
                    .addObject(params)
                    .args(args)
                    .build();
            jc.setProgramName(PROG_NAME);
            if (params.isHelp()) {
                System.out.println("Program: IntOmics (secretomics and transcriptomics data integration)");
                System.out.println("Version: " + VERSION);
                jc.usage();
                System.exit(0);
            }
        } catch (ParameterException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
        OperationDispatcher operationDispatcher = new OperationDispatcher();
        try {
            Files.createDirectory(Path.of(OUT_DIR));
        } catch (IOException ignored) {
        }
        OutputFormatter formatter;
        switch (params.getFormat()) {
            case TSV:
                formatter = new TSVFormatter();
                break;
            case FWF:
                formatter = new TextFormatter();
                break;
            case HTML:
                formatter = new HtmlFormatter();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + params.getFormat());
        }
        SpeciesHelper.makeSpecies(params.getSpecies());
        try {
            operationDispatcher.setup(OUT_DIR + "S2M", formatter).run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        new MainEntry(args);
    }
}
