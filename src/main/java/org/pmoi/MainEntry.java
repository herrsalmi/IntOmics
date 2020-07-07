package org.pmoi;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import org.pmoi.util.io.TSVFormatter;
import org.pmoi.util.io.TextFormatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class MainEntry {

    public MainEntry(String[] args) {
        args = new String[]{"-p", "test/secretome_withID_test.csv",
                            "-g", "test/Gene_DE_9h.csv",
                            "-a", "test/all_genes.csv",
                            "-db", "KEGG",
                            "-f", "FWF",
                            "-t", "8"};
        Args params = Args.getInstance();
        try {
            JCommander jc = JCommander.newBuilder()
                    .addObject(params)
                    .build();
            jc.parse(args);
            if (params.isHelp()) {
                jc.usage();
                System.exit(0);
            }
        } catch (ParameterException e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
        OperationDispatcher operationDispatcher = new OperationDispatcher();
        try {
            Files.createDirectory(Path.of("output"));
        } catch (IOException ignored) {
        }
        var formatter = switch (params.getFormat()) {
            case "TSV" -> new TSVFormatter();
            case "FWF" -> new TextFormatter();
            default -> throw new IllegalStateException("invalid format: " + params.getFormat());
        };
        try {
            operationDispatcher.run("output/S2M", formatter);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        new MainEntry(args);
    }
}
