package org.pmoi;

import com.google.common.collect.Comparators;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.business.*;
import org.pmoi.models.*;
import org.pmoi.ui.MainFX;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
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

    public void run(String output, ProteomeType proteomeType, SecretomeMappingMode mappingMode) {
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
        LOGGER.info("Loading 9h transcriptome");
        List<Gene> membranome = membranomeManager.getMembranomeFromDEGenes("Gene_DE_9h.csv");

        LOGGER.info("Loading 48h transcriptome");
        List<Gene> transcriptome = membranomeManager.getDEGenesExceptMembranome("Gene_DE_48h.csv");

        LOGGER.info("Number of secreted proteins: " + allSecretome.size());
        LOGGER.info("Number of secreted proteins more expressed in depleted samples: " + secretome.size());

        writeInteractions(secretome, membranome, transcriptome, output);

    }

    private void writeInteractions(List<Protein> secretome, List<Gene> membranome, List<Gene> transcriptome, String outputFileName) {
        List<ResultRecord> resultSet = Collections.synchronizedList(new ArrayList<ResultRecord>());
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        secretome.forEach(e -> executorService.submit(() -> {
            Map<String, String> interactors = stringdbQueryClient.getProteinNetwork(e.getName());
            List<String> interactorsNames = new ArrayList<>(interactors.keySet());
            interactorsNames.retainAll(membranome.stream().map(Feature::getName).collect(Collectors.toList()));
            if (!interactorsNames.isEmpty()) {
                interactorsNames.forEach(interactor -> {
                    Gene gene = membranome.stream().filter(g -> g.getName().equals(interactor))
                            .findFirst().orElse(null);
                    // make a deep copy of the gene otherwise you will get unexpected results with pathways
                    resultSet.add(new ResultRecord(e, (Gene) gene.clone(), interactors.get(interactor)));
                });
            }
        }));

        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        LOGGER.info("Looking for pathway interactions ...");

        // TODO This code will highly likely throw an error
        var resultMap = resultSet.stream().collect(Collectors.groupingBy(ResultRecord::getProtein));
        PathwayClient pathwayClient = new PathwayClient();
        resultMap.forEach((key, value) -> {
            var pathways = pathwayClient.KEGGSearch(key.getEntrezID());
            pathways.stream().map(e -> e.split(" {2}")).forEach(e -> key.addPathway(new Pathway(e[0], e[1])));
            key.getPathways().forEach(e -> e.setGenes(pathwayClient.getPathwayGenes(e.getPathwayID())));
            value.forEach(resultRecord -> {
                resultRecord.getProtein().getPathways().forEach(p -> {
                    if (p.getGenes().contains(resultRecord.getGene()))
                        resultRecord.getGene().setInteractors(p.getName(), p.getGenes().stream().distinct().filter(transcriptome::contains).collect(Collectors.toList()));
                });
            });
        });

        LOGGER.info("Writing results ...");

        NCBIQueryClient ncbiQueryClient = new NCBIQueryClient();
        StringBuffer outputBuffer = new StringBuffer(String.format("%-10s %-6s %-50s %-10s %-10s %-10s %-6s %-50s %-10s %-10s %-10s\n",
                "#protein", "ID", "name", "score D", "score R", "gene", "ID", "name", "I score", "gene_fdr", "gene_fc"));
        List<ResultsFX> fxList = new ArrayList<>();
        resultSet.stream().collect(Collectors.groupingBy(ResultRecord::getProtein))
                .forEach((k, v) -> {
                    k.setDescription(ncbiQueryClient.fetchDescription(k.getEntrezID()));
                    v = v.stream().sorted(Comparator.comparingDouble(o -> o.getGene().getFoldChange())).sorted(Collections.reverseOrder()).collect(Collectors.toList());
                    v.get(0).getGene().setDescription(ncbiQueryClient.fetchDescription(v.get(0).getGene().getEntrezID()));
                    outputBuffer.append(String.format("%-10s %-6s %-50s %-10s %-10s %-10s %-6s %-50s %-10s %-10s %-10s %s\n",
                            k.getName(), k.getEntrezID(), k.getDescription(), k.depletedMeanScore(), k.rinsedMeanScore(),
                            v.get(0).getGene().getName(), v.get(0).getGene().getEntrezID(), v.get(0).getGene().getDescription(),
                            v.get(0).getInteractionScore(), v.get(0).getGene().getFdr(), v.get(0).getGene().getFoldChange(),
                            v.get(0).getGene().getInteractors().entrySet().stream()
                                    .map(e -> e.getKey() + ": " + e.getValue().stream().map(Gene::getName).collect(Collectors.joining(",", "[", "]")))
                                    .collect(Collectors.joining("; "))));
                    v.stream().skip(1).forEach(e -> {
                        e.getGene().setDescription(ncbiQueryClient.fetchDescription(e.getGene().getEntrezID()));
                        outputBuffer.append(String.format("%-10s %-6s %-50s %-10s %-10s %-10s %-6s %-50s %-10s %-10s %-10s %s\n",
                                "", "", "", "", "", e.getGene().getName(), e.getGene().getEntrezID(),
                                e.getGene().getDescription(), e.getInteractionScore(),
                                e.getGene().getFdr(), e.getGene().getFoldChange(),
                                e.getGene().getInteractors().entrySet().stream()
                                        .map(en -> en.getKey() + ": " + en.getValue().stream().map(Gene::getName).collect(Collectors.joining(",", "[", "]")))
                                        .collect(Collectors.joining("; "))));
                    });
                });
//        resultSet.forEach(e -> {
//            e.getProtein().setDescription(ncbiQueryClient.fetchDescription(e.getProtein().getEntrezID()));
//            e.getGene().setDescription(ncbiQueryClient.fetchDescription(e.getGene().getEntrezID()));
//            outputBuffer.append(String.format("%-10s %-6s %-50s %-10s %-10s %-10s %-6s %-50s %-10s %-10s %-10s\n",
//                    e.getProtein().getName(), e.getProtein().getEntrezID(), e.getProtein().getDescription(),
//                    e.getGene().getName(), e.getGene().getEntrezID(), e.getGene().getDescription(),
//                    e.getInteractionScore(), e.getProtein().depletedMeanScore(), e.getProtein().rinsedMeanScore(),
//                    e.getGene().getFdr(), e.getGene().getFoldChange()));
//            fxList.add(new ResultsFX(e.getProtein().getName(), e.getProtein().getEntrezID(), e.getProtein().getDescription(),
//                    e.getGene().getName(), e.getGene().getEntrezID(), e.getGene().getDescription(),
//                    e.getInteractionScore(), String.valueOf(e.getProtein().depletedMeanScore()), String.valueOf(e.getProtein().rinsedMeanScore()),
//                    e.getGene().getFdr(), e.getGene().getFoldChange(), ""));
//        });

        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(outputFileName))) {
            bw.write(outputBuffer.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

//        MainFX.main(fxList);
    }

}
