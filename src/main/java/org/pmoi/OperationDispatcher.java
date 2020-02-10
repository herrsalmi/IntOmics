package org.pmoi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.business.*;
import org.pmoi.models.*;
import org.pmoi.ui.RandomWalkLineChart;
import org.pmoi.utils.GSEA;
import org.pmoi.utils.io.CsvFormater;
import org.pmoi.utils.io.OutputFormater;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class OperationDispatcher {
    private static final Logger LOGGER = LogManager.getRootLogger();
    private StringdbQueryClient stringdbQueryClient;
    private OutputFormater formater;

    public OperationDispatcher() {
        stringdbQueryClient = new StringdbQueryClient();
    }

    public void run(String prefix, ProteomeType proteomeType, SecretomeMappingMode mappingMode, OutputFormater formater) {
        this.formater = formater;
        String extension = formater instanceof CsvFormater ? "csv" : "txt";
        String output = String.format("%s_%s_%s_%s_fc%1.1f.%s",
                prefix, proteomeType.label, mappingMode.label, ApplicationParameters.getInstance().getStringDBScore(),
                ApplicationParameters.getInstance().getGeneFoldChange(), extension);
        MembranomeManager membranomeManager = MembranomeManager.getInstance();
        SecretomeManager secretomeManager = SecretomeManager.getInstance();
        secretomeManager.setMappingMode(mappingMode);
        List<Protein> allSecretome;
        LOGGER.info("Loading secretome");
        allSecretome = switch (proteomeType) {
            case LABEL_FREE -> secretomeManager.getSecretomeFromLabelFreeFile("input/Secretome_label_free.csv");
            case LCMS -> secretomeManager.getSecretomeFromLCMSFile("input/Secretome.csv");
            default -> throw new IllegalStateException("Unexpected value: " + proteomeType);
        };
        assert allSecretome != null;
        List<Protein> secretome = allSecretome.stream()
                .filter(e -> e.isMoreExpressedInDepletedSamples(ApplicationParameters.getInstance().getProteinFoldChange()))
                .collect(Collectors.toList());

        LOGGER.info("Loading 9h transcriptome");
        List<Gene> membranome = membranomeManager.getMembranomeFromDEGenes("input/Gene_DE_9h.csv");

        List<Gene> transcriptome = null;
        if (ApplicationParameters.getInstance().use48H()) {
            LOGGER.info("Loading 48h transcriptome");
            transcriptome = membranomeManager.getDEGenesExceptMembranome("input/Gene_DE_48h.csv");
        }

        LOGGER.info("Number of secreted proteins: " + allSecretome.size());
        LOGGER.info("Number of secreted proteins more expressed in depleted samples: " + secretome.size());

        writeInteractions(secretome, membranome, transcriptome, output);

    }

    private void writeInteractions(List<Protein> secretome, List<Gene> membranome, List<Gene> transcriptome, String outputFileName) {
        List<ResultRecord> resultSet = Collections.synchronizedList(new ArrayList<>());
        ForkJoinPool customThreadPool = new ForkJoinPool(3);
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        PathwayClient pathwayClient = PathwayClient.getInstance();

        var filteredTranscriptome = transcriptome.stream().filter(e -> pathwayClient.isInAnyPathway(e.getName())).collect(Collectors.toList());
        //
//        RandomWalk randomWalk = new RandomWalk();
//        randomWalk.walk(pathwayClient.getGenesFromPathway("PI3K-Akt Signaling Pathway"), transcriptome);
//        RandomWalkLineChart.main(randomWalk.getRunningSum());

        LOGGER.info("Running GSEA");
        GSEA gseaT = new GSEA();
        var pvalue = gseaT.run(pathwayClient.getGenesFromPathway("Collagen degradation"),
                filteredTranscriptome);
        RandomWalkLineChart.main(gseaT.getPhit(), gseaT.getPmiss());
        System.exit(0);

        secretome.forEach(e -> executorService.submit(() -> {
            // Adding StringDB interactors
            //Map<String, String> interactors = stringdbQueryClient.getProteinNetwork(e.getName());
            Map<String, String> interactors = new HashMap<>();
            // Add gene from the pathway to the map
//            pathwayClient.getIntercatorsFromPathway(e.getName()).stream()
//                    .filter(gene -> !gene.equals(e.getName()))
//                    .forEach(gene -> interactors.putIfAbsent(gene, "NA"));
            pathwayClient.getPathways(e.getName()).stream()
                    .flatMap(p -> p.getGenes().stream())
                    .distinct()
                    .filter(gene -> !gene.getName().equals(e.getName()))
                    .forEach(gene -> interactors.putIfAbsent(gene.getName(), "NA"));
            List<String> interactorsNames = new ArrayList<>(interactors.keySet());
            interactorsNames.retainAll(membranome.stream().map(Feature::getName).collect(Collectors.toList()));
            if (!interactorsNames.isEmpty()) {
                interactorsNames.forEach(interactor -> {
                    Gene gene = membranome.stream().filter(g -> g.getName().equals(interactor))
                            .findFirst().orElse(null);
                    // make a deep copy of the gene otherwise you will get unexpected results with pathways
                    assert gene != null;
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

        if (ApplicationParameters.getInstance().use48H()) {
            LOGGER.info("Looking for pathway interactions ...");

            var resultMap = resultSet.stream().collect(Collectors.groupingBy(ResultRecord::getProtein));

            resultMap.forEach((key, value) -> {
                // get a list of pathways where the protein is involved
//                var pathways = pathwayClient.KEGGSearch(key.getEntrezID());
//                pathways.stream().map(e -> e.split(" {2}")).forEach(e -> key.addPathway(new Pathway(e[0], e[1])));
//                key.getPathways().forEach(e -> e.setGenes(pathwayClient.getKEGGPathwayGenes(e.getPathwayID())));
                var pathways = pathwayClient.getPathways(key.getName());
                pathways.forEach(key::addPathway);
                value.forEach(resultRecord -> resultRecord.getProtein().getPathways().forEach(p -> {
                    if (p.getGenes().contains(resultRecord.getGene()))
                        resultRecord.getGene().setInteractors(p.getName(), p.getGenes().stream().distinct().filter(transcriptome::contains).collect(Collectors.toList()));
                }));
            });
        }

        LOGGER.info("Writing results ...");

        NCBIQueryClient ncbiQueryClient = new NCBIQueryClient();
        //List<ResultsFX> fxList = new ArrayList<>();
        if (ApplicationParameters.getInstance().use48H()) {

            // Calculate pathway pvalue using hypergeometric test
//            resultSet.parallelStream()
//                    .collect(Collectors.groupingBy(ResultRecord::getProtein))
//                    .forEach((k, v) -> {
//                        v.stream().forEach(e -> {
//                            e.getGene().getGeneSets()
//                                    .forEach(en -> {
//                                        HypergeometricDistribution distribution = new HypergeometricDistribution((int)pathwayClient.getNumberOfGenes(),
//                                                pathwayClient.getNumberOfGenesByPathway(en.getName()), transcriptome.size());
//                                        en.setPvalue(distribution.probability(en.getGenes().size()));
//                                    });
//                        });
//                    });
            // Calculate pathway pvalue using GSEA
            ExecutorService service = Executors.newFixedThreadPool(8);
            resultSet.parallelStream()
                    .collect(Collectors.groupingBy(ResultRecord::getProtein))
                    .forEach((k, v) -> v.forEach(e -> e.getGene().getGeneSets()
                            .forEach(en -> {
                                service.submit(() -> {
                                    GSEA gsea = new GSEA();
                                    en.setPvalue(gsea.run(en.getGenes(), filteredTranscriptome));
                                    en.setScore(gsea.getNormalizedScore());
                                });
                            })));
            service.shutdown();
            try {
                service.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Remove results with pathways having pvalue < 0.05
            var finalResultSet = resultSet.parallelStream()
                    .filter(e -> e.getGene().getGeneSets().get(0).getPvalue() < 0.05 )
                    .collect(Collectors.toList());

            customThreadPool.submit(() -> finalResultSet.parallelStream().collect(Collectors.groupingBy(ResultRecord::getProtein))
                    .forEach((k, v) -> {
                        k.setDescription(ncbiQueryClient.fetchDescription(k.getEntrezID()));
                        v = v.stream().sorted(Comparator.comparingDouble(o -> o.getGene().getFoldChange())).sorted(Collections.reverseOrder()).collect(Collectors.toList());
                        v.get(0).getGene().setDescription(ncbiQueryClient.fetchDescription(v.get(0).getGene().getEntrezID()));
                        formater.append(k.getName(), k.getDescription(), String.valueOf(k.depletedMeanScore()), String.valueOf(k.rinsedMeanScore()),
                                v.get(0).getGene().getName(), v.get(0).getGene().getDescription(),
                                v.get(0).getInteractionScore(), String.valueOf(v.get(0).getGene().getFdr()), String.valueOf(v.get(0).getGene().getFoldChange()),
                                v.get(0).getGene().getGeneSets().stream()
                                        .sorted(Comparator.comparingDouble(GeneSet::getPvalue))
                                        .map(e -> e.getName() + ": {" + e.getScore() + "} "
                                                + e.getGenes().stream().sorted(Collections.reverseOrder())
                                                .map(Gene::getName).collect(Collectors.joining(",", "[", "]")))
                                        .collect(Collectors.joining("; ")));
                        v.stream().skip(1).forEach(e -> {
                            e.getGene().setDescription(ncbiQueryClient.fetchDescription(e.getGene().getEntrezID()));
                            formater.append("", "", "", "", e.getGene().getName(), e.getGene().getDescription(), e.getInteractionScore(),
                                    String.valueOf(e.getGene().getFdr()), String.valueOf(e.getGene().getFoldChange()),
                                    e.getGene().getGeneSets().stream()
                                            .sorted(Comparator.comparingDouble(GeneSet::getPvalue))
                                            .map(en -> en.getName() + ": {" + en.getScore() + "} "
                                                    + en.getGenes().stream().sorted(Collections.reverseOrder())
                                                    .map(Gene::getName).collect(Collectors.joining(",", "[", "]")))
                                            .collect(Collectors.joining("; ")));
                        });
                    }));
        } else {
            customThreadPool.submit(() -> resultSet.parallelStream().collect(Collectors.groupingBy(ResultRecord::getProtein))
                    .forEach((k, v) -> {
                        k.setDescription(ncbiQueryClient.fetchDescription(k.getEntrezID()));
                        v = v.stream().sorted(Comparator.comparingDouble(o -> o.getGene().getFoldChange())).sorted(Collections.reverseOrder()).collect(Collectors.toList());
                        v.get(0).getGene().setDescription(ncbiQueryClient.fetchDescription(v.get(0).getGene().getEntrezID()));
                        formater.append(k.getName(), k.getDescription(), String.valueOf(k.depletedMeanScore()), String.valueOf(k.rinsedMeanScore()),
                                v.get(0).getGene().getName(), v.get(0).getGene().getDescription(),
                                v.get(0).getInteractionScore(), String.valueOf(v.get(0).getGene().getFdr()), String.valueOf(v.get(0).getGene().getFoldChange()));

                        v.stream().skip(1).forEach(e -> {
                            e.getGene().setDescription(ncbiQueryClient.fetchDescription(e.getGene().getEntrezID()));
                            formater.append("", "", "", "", e.getGene().getName(), e.getGene().getDescription(), e.getInteractionScore(),
                                    String.valueOf(e.getGene().getFdr()), String.valueOf(e.getGene().getFoldChange()));
                        });
                    }));
        }
        customThreadPool.shutdown();
        try {
            customThreadPool.awaitTermination(1, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        resultSet.forEach(e -> {
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
            bw.write(formater.getText());
        } catch (IOException e) {
            e.printStackTrace();
        }
        EntrezIDMapper.getInstance().close();
        pathwayClient.close();
//        MainFX.main(fxList);
    }

}
