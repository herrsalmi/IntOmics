package org.pmoi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.business.MembranomeManager;
import org.pmoi.business.NCBIQueryClient;
import org.pmoi.business.SecretomeManager;
import org.pmoi.business.StringdbQueryClient;
import org.pmoi.models.*;
import org.pmoi.ui.MainFX;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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
        List<Protein> secretome = allSecretome.stream()
                .filter(e -> e.isMoreExpressedInDepletedSamples(MainEntry.FC))
                .collect(Collectors.toList());
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
        var resultSet = Collections.synchronizedList(new ArrayList<ResultRecord>());
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        set1.forEach( e -> executorService.submit(() -> {
            Map<String, String> interactors = stringdbQueryClient.getProteinNetwork(e.getName());
            List<String> interactorsNames = new ArrayList<>(interactors.keySet());
            interactorsNames.retainAll(set2.stream().map(Feature::getName).collect(Collectors.toList()));
            if (!interactorsNames.isEmpty()) {
                interactorsNames.forEach(interactor -> {
                    Gene gene = set2.stream().filter(g -> g.getName().equals(interactor))
                            .findFirst().orElse(null);
                    resultSet.add(new ResultRecord(e, gene, interactors.get(interactor)));
                });
            }
        }));

        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        LOGGER.info("Writing results ...");

        NCBIQueryClient ncbiQueryClient = new NCBIQueryClient();
        StringBuffer outputBuffer = new StringBuffer(String.format("%-10s %-6s %-45s %-10s %-6s %-45s %-10s %-10s %-10s %-10s %-10s\n",
                "#protein", "ID", "name", "gene", "ID", "name", "interaction_score", "score D", "score R", "gene_fdr", "gene_fc"));
        List<ResultsFX> fxList = new ArrayList<>();
        resultSet.forEach(e -> {
            e.getProtein().setDescription(ncbiQueryClient.fetchDescription(e.getProtein().getEntrezID()));
            e.getGene().setDescription(ncbiQueryClient.fetchDescription(e.getGene().getEntrezID()));
            outputBuffer.append(String.format("%-10s %-6s %-45s %-10s %-6s %-45s %-10s %-10s %-10s %-10s %-10s\n",
                    e.getProtein().getName(), e.getProtein().getEntrezID(), e.getProtein().getDescription(),
                    e.getGene().getName(), e.getGene().getEntrezID(), e.getGene().getDescription(),
                    e.getInteractionScore(), e.getProtein().depletedMeanScore(), e.getProtein().rinsedMeanScore(),
                    e.getGene().getFdr(), e.getGene().getFoldChange()));
            fxList.add(new ResultsFX(e.getProtein().getName(), e.getProtein().getEntrezID(), e.getProtein().getDescription(),
                    e.getGene().getName(), e.getGene().getEntrezID(), e.getGene().getDescription(),
                    e.getInteractionScore(), String.valueOf(e.getProtein().depletedMeanScore()), String.valueOf(e.getProtein().rinsedMeanScore()),
                    e.getGene().getFdr(), e.getGene().getFoldChange(), ""));
        });

        try {
            BufferedWriter bw = Files.newBufferedWriter(Paths.get(outputFileName));
            bw.write(outputBuffer.toString());
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        MainFX.main(fxList);
    }

}
