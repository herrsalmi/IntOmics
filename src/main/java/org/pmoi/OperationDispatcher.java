package org.pmoi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.business.*;
import org.pmoi.handler.Parser;
import org.pmoi.models.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class OperationDispatcher {
    private static final Logger LOGGER = LogManager.getRootLogger();
    private StringdbQueryClient stringdbQueryClient;

    public OperationDispatcher() {
        stringdbQueryClient = new StringdbQueryClient();
    }

    public void run(String output,ProteomeType proteomeType, SecretomeMappingMode mappingMode) {
        MembranomeManager membranomeManager = MembranomeManager.getInstance();
        SecretomeManager secretomeManager = SecretomeManager.getInstance();
        secretomeManager.setMappingMode(mappingMode);
        List<Protein> allSecretome;
        LOGGER.info("Loading secretome");
        switch (proteomeType) {
            case LABEL_FREE:
                allSecretome = secretomeManager.getSecretomeFromLabelFreeFile("Secretome_label_free.csv");
                break;
            case LCMS:
                allSecretome = secretomeManager.getSecretomeFromLCMSFile("Secretome.csv");
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + proteomeType);
        }
        assert allSecretome != null;
        List<Protein> secretome = allSecretome.stream().filter(Protein::isMoreExpressedInDepletedSamples).collect(Collectors.toList());
        //
//        System.out.println(allSecretome.size());
//        allSecretome.forEach(System.out::println);
//        System.out.println(secretome.size());
//        secretome.forEach(System.out::println);
//        System.exit(0);
        //
        LOGGER.info("Loading transcriptome");
        List<Gene> membranome = membranomeManager.getMembranomeFromDEGenes("Gene_DE.csv");

        LOGGER.info("Number of secreted proteins: " + allSecretome.size());
        LOGGER.info("Number of secreted proteins more expressed in depleted samples: " + secretome.size());

        writeInteractions(secretome, membranome, output);

    }

    private void writeInteractions(List<Protein> set1, List<Gene> set2, String outputFileName) {
        StringBuffer outputBuffer = new StringBuffer("#node1\tnode2\tinteraction_score\tgene_fdr\tgene_fc\n");
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        set1.forEach( e -> executorService.submit(() -> {
            Map<String, String> interactors = stringdbQueryClient.getProteinNetwork(e.getName());
            List<String> interactorsNames = new ArrayList<>(interactors.keySet());
            interactorsNames.retainAll(set2.stream().map(Feature::getName).collect(Collectors.toList()));
            if (!interactorsNames.isEmpty()) {
                interactorsNames.forEach(interactor -> {
                    String fdr = set2.stream().filter(g -> g.getName().equals(interactor)).map(Gene::getFdr).collect(Collectors.joining());
                    String foldChange = set2.stream().filter(g -> g.getName().equals(interactor)).map(Gene::getFoldChange).collect(Collectors.joining());
                    outputBuffer.append(String.format("%s\t%s\t%s\t%s\t%s\n", e.getName(), interactor, interactors.get(interactor), fdr, foldChange));
                });
            }
        }));

        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            BufferedWriter bw = Files.newBufferedWriter(Paths.get(outputFileName));
            bw.write(outputBuffer.toString());
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
