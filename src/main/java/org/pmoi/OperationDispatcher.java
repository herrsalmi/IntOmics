package org.pmoi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.business.*;
import org.pmoi.model.*;
import org.pmoi.util.CSVValidator;
import org.pmoi.util.GSEA;
import org.pmoi.util.io.OutputFormatter;
import org.pmoi.util.io.TSVFormatter;

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
    private final StringdbQueryClient stringdbQueryClient;
    private OutputFormatter formater;

    public OperationDispatcher() {
        stringdbQueryClient = new StringdbQueryClient();
    }

    public void run(String prefix, SecretomeMappingMode mappingMode, OutputFormatter formatter) throws InterruptedException {
        this.formater = formatter;
        String extension = formatter instanceof TSVFormatter ? "tsv" : "txt";
        String output = String.format("%s_%s_%s_fc%1.1f.%s",
                prefix, mappingMode.label, Args.getInstance().getStringDBScore(),
                ApplicationParameters.getInstance().getGeneFoldChange(), extension);
        TranscriptomeManager transcriptomeManager = TranscriptomeManager.getInstance();
        SecretomeManager secretomeManager = SecretomeManager.getInstance();
        secretomeManager.setMappingMode(mappingMode);
        CSVValidator validator = new CSVValidator();
        if (!validator.isConform(Args.getInstance().getTranscriptome()))
            System.exit(1);
        LOGGER.info("Loading secretome");
        var secretome = secretomeManager.loadSecretomeFile(Args.getInstance().getSecretome());

        LOGGER.info("Loading membranome");
        List<Gene> membranome = transcriptomeManager.getMembranomeFromDEGenes(Args.getInstance().getAllGenes());

        LOGGER.info("Loading transcriptome");
        List<Gene> transcriptome = transcriptomeManager.getDEGenes(Args.getInstance().getTranscriptome());

        LOGGER.info(String.format("Number of membranome genes: %d", membranome.size()));
        LOGGER.info(String.format("Number of secreted proteins: %d", secretome.size()));
        LOGGER.info(String.format("Number of secreted proteins more expressed in depleted samples: %d", secretome.size()));

        assert transcriptome != null;
        writeInteractions(secretome, membranome, transcriptome, output);

    }

    private void writeInteractions(List<Protein> secretome, List<Gene> membranome, List<Gene> transcriptome, String outputFileName) throws InterruptedException {
        List<ResultRecord> resultSet = Collections.synchronizedList(new ArrayList<>());
        ForkJoinPool customThreadPool = new ForkJoinPool(3);
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        PathwayClient pathwayClient = PathwayClient.getInstance();

        LOGGER.info("Filtering transcriptome");
        var filteredTranscriptome = transcriptome.parallelStream().filter(e -> pathwayClient.isInAnyPathway(e.getName())).collect(Collectors.toList());

        secretome.forEach(e -> executorService.submit(() -> {
            // Adding StringDB interactors
            Map<String, String> interactors = stringdbQueryClient.getProteinNetwork(e.getName());
            //Map<String, String> interactors = new HashMap<>();
            // Add gene from the pathway to the map
            // PS: This code was used to include pathway genes as interactors instead of using StringDB
//            pathwayClient.getIntercatorsFromPathway(e.getName()).stream()
//                    .filter(gene -> !gene.equals(e.getName()))
//                    .forEach(gene -> interactors.putIfAbsent(gene, "NA"));
//            pathwayClient.getPathways(e.getName()).stream()
//                    .flatMap(p -> p.getGenes().stream())
//                    .distinct()
//                    .filter(gene -> !gene.getName().equals(e.getName()))
//                    .forEach(gene -> interactors.putIfAbsent(gene.getName(), "NA"));
            List<String> interactorsNames = new ArrayList<>(interactors.keySet());
            interactorsNames.retainAll(membranome.stream().map(Feature::getName).collect(Collectors.toList()));
            if (!interactorsNames.isEmpty()) {
                interactorsNames.forEach(interactor -> {
                    Gene gene = membranome.stream().filter(g -> g.getName().equals(interactor))
                            .findFirst().orElse(null);
                    // make a deep copy of the gene otherwise you will get unexpected results with pathways
                    assert gene != null;
                    resultSet.add(new ResultRecord(e, new Gene(gene), interactors.get(interactor)));
                });
            }
        }));

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.DAYS);

        if (ApplicationParameters.getInstance().addPathways()) {
            LOGGER.info("Looking for pathway interactions ...");

            var resultMap = resultSet.stream().collect(Collectors.groupingBy(ResultRecord::getProtein));

            resultMap.forEach((key, value) -> {
                // get a list of pathways where the protein is involved

//                var pathways = pathwayClient.KEGGSearch(key.getEntrezID());
//                pathways.stream().map(e -> e.split(" {2}")).forEach(e -> key.addPathway(new Pathway(e[0], e[1])));
//                key.getPathways().forEach(e -> e.setGenes(pathwayClient.getKEGGPathwayGenes(e.getPathwayID())));

                var pathways = pathwayClient.getPathways(key.getName());
                if (pathways == null)
                    return;
                pathways.forEach(key::addPathway);
                value.forEach(resultRecord -> {
                    LOGGER.info(String.format("Processing [P: %s # G: %s]", key.getName(), resultRecord.getGene().getName()));
                    resultRecord.getProtein().getPathways().forEach(p -> {
                        if (p.getGenes().contains(resultRecord.getGene()))
                            resultRecord.getGene().setInteractors(p.getName(), p.getGenes().stream().distinct().filter(transcriptome::contains).collect(Collectors.toList()));
                    });
                });
            });
        }

        NCBIQueryClient ncbiQueryClient = new NCBIQueryClient();
        if (ApplicationParameters.getInstance().addPathways()) {
            LOGGER.info("Running GSEA ...");
            // Calculate pathway pvalue using GSEA
            ExecutorService service = Executors.newFixedThreadPool(8);
            resultSet.parallelStream()
                    .collect(Collectors.groupingBy(ResultRecord::getProtein))
                    .forEach((k, v) -> v.forEach(e -> e.getGene().getGeneSets()
                            .forEach(en ->
                                service.submit(() -> {
                                    GSEA gsea = new GSEA();
                                    en.setPvalue(gsea.run(en.getGenes(), filteredTranscriptome));
                                    en.setScore(gsea.getNormalizedScore());
                                })
                            )));
            service.shutdown();
            service.awaitTermination(1, TimeUnit.DAYS);

            resultSet.parallelStream().forEach(e -> e.getGene().getGeneSets().removeIf(geneSet -> geneSet.getPvalue() >= 0.05));

            LOGGER.info("Writing results ...");

            customThreadPool.submit(() -> resultSet.parallelStream().collect(Collectors.groupingBy(ResultRecord::getProtein))
                    .forEach((k, v) -> {
                        k.setDescription(ncbiQueryClient.fetchDescription(k.getEntrezID()));
                        v = v.stream().sorted(Comparator.comparingDouble(o -> o.getGene().getFoldChange())).sorted(Collections.reverseOrder()).collect(Collectors.toList());
                        v.get(0).getGene().setDescription(ncbiQueryClient.fetchDescription(v.get(0).getGene().getEntrezID()));
                        formater.append(k.getName(), k.getDescription(),
                                v.get(0).getGene().getName(), v.get(0).getGene().getDescription(),
                                v.get(0).getInteractionScore(), String.valueOf(v.get(0).getGene().getFdr()), String.valueOf(v.get(0).getGene().getFoldChange()),
                                v.get(0).getGene().getGeneSets().stream()
                                        .sorted(Comparator.comparingDouble(GeneSet::getPvalue))
                                        .map(e -> e.getName() + ": {" + e.getScore() + " | " + e.getPvalue() + "} "
                                                + e.getGenes().stream().sorted(Collections.reverseOrder())
                                                .map(Gene::getName).collect(Collectors.joining(",", "[", "]")))
                                        .collect(Collectors.joining("; ")));
                        v.stream().skip(1).forEach(e -> {
                            e.getGene().setDescription(ncbiQueryClient.fetchDescription(e.getGene().getEntrezID()));
                            formater.append("", "", "", "", e.getGene().getName(), e.getGene().getDescription(), e.getInteractionScore(),
                                    String.valueOf(e.getGene().getFdr()), String.valueOf(e.getGene().getFoldChange()),
                                    e.getGene().getGeneSets().stream()
                                            .sorted(Comparator.comparingDouble(GeneSet::getPvalue))
                                            .map(en -> en.getName() + ": {" + en.getScore()  + " | " + en.getPvalue() + "} "
                                                    + en.getGenes().stream().sorted(Collections.reverseOrder())
                                                    .map(Gene::getName).collect(Collectors.joining(",", "[", "]")))
                                            .collect(Collectors.joining("; ")));
                        });
                    }));
        } else {
            customThreadPool.submit(() -> resultSet.parallelStream().collect(Collectors.groupingBy(ResultRecord::getProtein))
                    .forEach((k, v) -> {
                        k.setDescription(ncbiQueryClient.fetchDescription(k.getEntrezID()));
                        v = v.stream()
                                .sorted(Comparator.comparingDouble(o -> o.getGene().getFoldChange()))
                                .sorted(Collections.reverseOrder())
                                .collect(Collectors.toList());
                        v.get(0).getGene().setDescription(ncbiQueryClient.fetchDescription(v.get(0).getGene().getEntrezID()));
                        formater.append(k.getName(), k.getDescription(),
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
        customThreadPool.awaitTermination(1, TimeUnit.DAYS);

        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(outputFileName))) {
            bw.write(formater.getText());
        } catch (IOException e) {
            LOGGER.error(e);
        }

    }

}
