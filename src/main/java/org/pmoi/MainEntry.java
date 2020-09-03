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
            case TSV -> new TSVFormatter();
            case FWF -> new TextFormatter();
        };
        try {
            operationDispatcher.setup("output/S2M", formatter).run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        new MainEntry(args);
    }
}
