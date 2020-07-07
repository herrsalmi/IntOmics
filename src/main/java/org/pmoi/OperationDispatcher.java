package org.pmoi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.business.PathwayClient;
import org.pmoi.business.SecretomeManager;
import org.pmoi.business.StringdbQueryClient;
import org.pmoi.business.TranscriptomeManager;
import org.pmoi.database.GeneMapper;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class OperationDispatcher {
    private static final Logger LOGGER = LogManager.getRootLogger();
    private final StringdbQueryClient stringdbQueryClient;
    private OutputFormatter formatter;

    public OperationDispatcher() {
        stringdbQueryClient = new StringdbQueryClient();
    }

    public void run(String prefix, OutputFormatter formatter) throws InterruptedException {
        this.formatter = formatter;
        String extension = formatter instanceof TSVFormatter ? "tsv" : "txt";
        String output = String.format("%s_%s_fc%1.1f.%s", prefix, Args.getInstance().getStringDBScore(),
                Args.getInstance().getFoldChange(), extension);
        TranscriptomeManager transcriptomeManager = TranscriptomeManager.getInstance();
        SecretomeManager secretomeManager = SecretomeManager.getInstance();
        CSVValidator validator = new CSVValidator();
        if (!validator.isConform(Args.getInstance().getTranscriptome()))
            System.exit(1);
        LOGGER.info("Loading secretome");
        var secretome = secretomeManager.loadSecretomeFile(Args.getInstance().getSecretome());

        LOGGER.info("Loading membranome");
        List<Gene> membranome = transcriptomeManager.getMembranomeFromDEGenes(Args.getInstance().getAllGenes());

        LOGGER.info("Loading transcriptome");
        List<Gene> transcriptome = transcriptomeManager.getDEGenes(Args.getInstance().getTranscriptome());

        LOGGER.info("Number of membranome genes: {}", membranome.size());
        LOGGER.info("Number of actively secreted proteins: {}", secretome.size());

        assert transcriptome != null;
        writeInteractions(secretome, membranome, transcriptome, output);

    }

    private void writeInteractions(List<Protein> secretome, List<Gene> membranome, List<Gene> transcriptome, String outputFileName) throws InterruptedException {
        List<ResultRecord> resultSet = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executorService = Executors.newFixedThreadPool(Args.getInstance().getThreads());
        PathwayClient pathwayClient = PathwayClient.getInstance();

        LOGGER.info("Filtering transcriptome");
//        Predicate<Gene> filter = switch (Args.getInstance().getPathwayDB()) {
//            case "KEGG" -> e -> !pathwayClient.KEGGSearch(e.getEntrezID()).isEmpty();
//            case "WikiPathways" -> e -> pathwayClient.isInAnyPathway(e.getName());
//            default -> throw new UnsupportedOperationException();
//        };
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

        LOGGER.info("Looking for pathway interactions ...");

        var resultMap = resultSet.stream().collect(Collectors.groupingBy(ResultRecord::getProtein));

        resultMap.forEach((key, value) -> {
            // get a list of pathways where the protein is involved
            if (Args.getInstance().getPathwayDB().equals("KEGG")) {
                var pathways = pathwayClient.KEGGSearch(key.getEntrezID());
                // TODO the next two lines should be simplified in order to return directly a list of pathways
                pathways.stream().map(e -> e.split(" {2}")).forEach(e -> key.addPathway(new Pathway(e[0], e[1])));
                key.getPathways().forEach(e -> e.setGenes(pathwayClient.getKEGGPathwayGenes(e.getPathwayID())));

            } else if (Args.getInstance().getPathwayDB().equals("WikiPathways")) {
                var pathways = pathwayClient.getPathways(key.getName());
                if (pathways == null || pathways.isEmpty())
                    return;
                pathways.forEach(key::addPathway);

            }

            value.forEach(resultRecord -> {
                LOGGER.debug(String.format("Processing [P: %s # G: %s]", key.getName(), resultRecord.getGene().getName()));
                resultRecord.getProtein().getPathways().forEach(p -> {
                    if (p.getGenes().contains(resultRecord.getGene()))
                        resultRecord.getGene().setInteractors(p.getName(), p.getGenes().stream().distinct().filter(transcriptome::contains).collect(Collectors.toList()));
                });
            });

        });

        var mapper = GeneMapper.getInstance();

        LOGGER.info("Running GSEA ...");
        // Calculate pathway pvalue using GSEA
        ExecutorService service = Executors.newFixedThreadPool(Args.getInstance().getThreads());
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
        resultSet.stream().collect(Collectors.groupingBy(ResultRecord::getProtein))
                .forEach((k, v) -> {
                    k.setDescription(mapper.getDescription(k.getEntrezID()).orElse("-"));
                    v = v.stream().sorted(Comparator.comparingDouble(o -> o.getGene().getFoldChange())).sorted(Collections.reverseOrder()).collect(Collectors.toList());
                    v.get(0).getGene().setDescription(mapper.getDescription(v.get(0).getGene().getEntrezID()).orElse("-"));
                    formatter.append(k.getName(), k.getDescription(),
                            v.get(0).getGene().getName(), v.get(0).getGene().getDescription(),
                            v.get(0).getInteractionScore(), String.valueOf(v.get(0).getGene().getFdr()), String.valueOf(v.get(0).getGene().getFoldChange()),
                            v.get(0).getGene().getGeneSets().stream()
                                    .sorted(Comparator.comparingDouble(GeneSet::getPvalue))
                                    .map(e -> e.getName() + ": {" + e.getScore() + " | " + e.getPvalue() + "} "
                                            + e.getGenes().stream().sorted(Collections.reverseOrder())
                                            .map(Gene::getName).collect(Collectors.joining(",", "[", "]")))
                                    .collect(Collectors.joining("; ")));
                    v.stream().skip(1).forEach(e -> {
                        e.getGene().setDescription(mapper.getDescription(e.getGene().getEntrezID()).orElse("-"));
                        formatter.append("", "", e.getGene().getName(), e.getGene().getDescription(), e.getInteractionScore(),
                                String.valueOf(e.getGene().getFdr()), String.valueOf(e.getGene().getFoldChange()),
                                e.getGene().getGeneSets().stream()
                                        .sorted(Comparator.comparingDouble(GeneSet::getPvalue))
                                        .map(en -> en.getName() + ": {" + en.getScore() + " | " + en.getPvalue() + "} "
                                                + en.getGenes().stream().sorted(Collections.reverseOrder())
                                                .map(Gene::getName).collect(Collectors.joining(",", "[", "]")))
                                        .collect(Collectors.joining("; ")));
                    });
                });
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(outputFileName))) {
            bw.write(formatter.getText());
        } catch (IOException e) {
            LOGGER.error(e);
        }

    }

}
