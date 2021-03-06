package org.pmoi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.business.GraphVisualizer;
import org.pmoi.business.SecretomeManager;
import org.pmoi.business.TranscriptomeManager;
import org.pmoi.business.pathway.PathwayMapper;
import org.pmoi.business.pathway.PathwayMapperFactory;
import org.pmoi.business.ppi.CachedInteractionQueryClient;
import org.pmoi.business.ppi.InteractionQueryClient;
import org.pmoi.business.ppi.StringdbQueryClient;
import org.pmoi.database.GeneMapper;
import org.pmoi.database.SupportedSpecies;
import org.pmoi.model.Gene;
import org.pmoi.model.GeneSet;
import org.pmoi.model.Protein;
import org.pmoi.model.ResultRecord;
import org.pmoi.model.vis.VisEdge;
import org.pmoi.model.vis.VisGraph;
import org.pmoi.model.vis.VisNode;
import org.pmoi.util.CSVValidator;
import org.pmoi.util.GSEA;
import org.pmoi.util.io.OutputFormatter;

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
    private InteractionQueryClient ppiQueryClient;
    private OutputFormatter formatter;
    private PathwayMapper pathwayMapper;

    public Runner setup(String prefix, OutputFormatter formatter) {
        this.formatter = formatter;
        String extension = switch (Args.getInstance().getFormat()) {
            case FWF -> "txt";
            case TSV -> "tsv";
            case HTML -> "html";
        };

        String output = String.format("%s_%s_fc%1.1f.%s", prefix, Args.getInstance().getStringDBScore(),
                Args.getInstance().getFoldChange(), extension);
        CSVValidator validator = new CSVValidator();
        if (!validator.isConform(Args.getInstance().getGenesDiffExp()))
            System.exit(1);

        LOGGER.info("Initializing ...");

        ppiQueryClient = Args.getInstance().getStringDBScore() > 700
                && Args.getInstance().getSpecies().equals(SupportedSpecies.HUMAN)
                && !Args.getInstance().useOnlinePPI() ?
                CachedInteractionQueryClient.getInstance() : new StringdbQueryClient();

        TranscriptomeManager transcriptomeManager = TranscriptomeManager.getInstance();
        SecretomeManager secretomeManager = SecretomeManager.getInstance();
        pathwayMapper = PathwayMapperFactory.getPathwayMapper(Args.getInstance().getPathwayDB());
        LOGGER.info("Loading secretome");
        var secretome = secretomeManager.loadSecretomeFile(Args.getInstance().getSecretome());

        LOGGER.info("Loading transcriptome");
        List<Gene> transcriptome = transcriptomeManager.getDEGenes(Args.getInstance().getGenesDiffExp());

        LOGGER.info("Identifying membranome");
        List<Gene> membranome = transcriptomeManager.getMembranomeFromGenes(Args.getInstance().getGenesDiffExp());

        LOGGER.info("Number of membranome genes: {}", membranome.size());
        LOGGER.info("Number of actively secreted proteins: {}", secretome.size());

        assert transcriptome != null;
        return new Runner(secretome, membranome, transcriptome, output);
    }

    class Runner {
        private final List<Protein> secretome;
        private final List<Gene> membranome;
        private final List<Gene> transcriptome;
        private final String outputFileName;

        private final List<ResultRecord> resultSet = Collections.synchronizedList(new ArrayList<>());
        private final ExecutorService executorService = Executors.newFixedThreadPool(Args.getInstance().getThreads());
        private List<Gene> filteredTranscriptome;
        private final GeneMapper mapper = GeneMapper.getInstance();


        public Runner(List<Protein> secretome, List<Gene> membranome, List<Gene> transcriptome, String outputFileName) {
            this.secretome = secretome;
            this.membranome = membranome;
            this.transcriptome = transcriptome;
            this.outputFileName = outputFileName;
        }

        private Runner setInteractions() throws InterruptedException {
            LOGGER.info("Filtering transcriptome");
            filteredTranscriptome = transcriptome.parallelStream().filter(e -> pathwayMapper.isInAnyPathway(e.getName())).collect(Collectors.toList());
            LOGGER.info("Getting PPI network ...");
            var membraneGenes = membranome.stream().map(f -> f.getName().toUpperCase()).collect(Collectors.toSet());
            secretome.forEach(e -> executorService.submit(() -> {
                // Adding StringDB interactors
                // the map contains <interactor, score>
                Map<String, String> interactors = ppiQueryClient.getProteinNetwork(e.getName());
                // Add gene from the pathway to the map
                var interactorsNames = interactors.keySet().parallelStream()
                        .filter(i -> membraneGenes.stream().anyMatch(i::equalsIgnoreCase))
                        .collect(Collectors.toList());
                if (!interactorsNames.isEmpty()) {
                    interactorsNames.forEach(interactor -> {
                        Gene gene = membranome.stream().filter(g -> g.getName().equalsIgnoreCase(interactor))
                                .findFirst().orElse(null);
                        // make a deep copy of the gene otherwise you will get unexpected results with pathways
                        assert gene != null;
                        resultSet.add(new ResultRecord(e, new Gene(gene), interactors.get(interactor)));
                    });
                }
            }));

            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.HOURS);
            return this;
        }

        private Runner getPathways() {
            LOGGER.info("Looking for pathway interactions ...");
            var resultMap = resultSet.stream().collect(Collectors.groupingBy(ResultRecord::getProtein));
            resultMap.forEach((key, value) -> {
                // get a list of pathways where the protein is involved
                var pathways = pathwayMapper.getPathways(key.getName());
                if (pathways == null || pathways.isEmpty())
                    return;
                pathways.forEach(key::addPathway);

                value.forEach(resultRecord -> {
                    LOGGER.debug("Processing [P: {} # G: {}]", key.getName(), resultRecord.getGene().getName());
                    resultRecord.getProtein().getPathways().forEach(p -> {
                        if (p.getGenes().contains(resultRecord.getGene()))
                            resultRecord.getGene().setInteractors(p.getPathwayID(), p.getName(),
                                    transcriptome.stream().filter(e -> p.getGenes().contains(e)).collect(Collectors.toList()));
                    });
                });

            });
            return this;
        }

        private Runner runGSEA() throws InterruptedException {
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
            service.awaitTermination(1, TimeUnit.HOURS);
            resultSet.parallelStream()
                    .forEach(e -> e.getGene().getGeneSets().removeIf(geneSet -> geneSet.getPvalue() > Args.getInstance().getGseaPvalue()));
            return this;
        }

        private void writeResults() {
            LOGGER.info("Writing results ...");
            resultSet.stream().collect(Collectors.groupingBy(ResultRecord::getProtein))
                    .forEach((k, v) -> {
                        k.setDescription(mapper.getDescription(k.getNcbiID()).orElse("-"));
                        v.sort(Comparator.comparingDouble((ResultRecord o) -> o.getGene().getFoldChange()).reversed());
                        v.forEach(e -> e.getGene().setDescription(mapper.getDescription(e.getGene().getNcbiID()).orElse("-")));
                        formatter.append(k.getName(), k.getDescription(),
                                v.get(0).getGene().getName(), v.get(0).getGene().getDescription(),
                                v.get(0).getInteractionScore(),
                                v.get(0).getGene().getGeneSets().stream()
                                        .sorted(Comparator.comparingDouble(GeneSet::getPvalue))
                                        .map(e -> e.getName() + ": {" + e.getScore() + " | " + e.getPvalue() + "} "
                                                + e.getGenes().stream().sorted(Collections.reverseOrder())
                                                .map(Gene::getName).collect(Collectors.joining(",", "[", "]")))
                                        .collect(Collectors.joining("; ")));
                        v.stream().skip(1).forEach(e ->
                                formatter.append("", "", e.getGene().getName(), e.getGene().getDescription(), e.getInteractionScore(),
                                e.getGene().getGeneSets().stream()
                                        .sorted(Comparator.comparingDouble(GeneSet::getPvalue))
                                        .map(en -> en.getName() + ": {" + en.getScore() + " | " + en.getPvalue() + "} "
                                                + en.getGenes().stream().sorted(Collections.reverseOrder())
                                                .map(Gene::getName).collect(Collectors.joining(",", "[", "]")))
                                        .collect(Collectors.joining("; "))));
                    });
            try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(outputFileName))) {
                bw.write(formatter.getText());
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }

        public void run() throws InterruptedException {
            this.setInteractions()
                    .getPathways()
                    .runGSEA()
                    .writeResults();
            // making html graph
            VisGraph graph = new VisGraph();
            Map<String, VisNode> map = new HashMap<>();
            List<VisEdge> edges = new ArrayList<>();
            for (var e: resultSet){
                map.putIfAbsent(e.getProtein().getName(), new VisNode(e.getProtein().getName().hashCode(),
                        e.getProtein().getName(), e.getProtein().getDescription(), 1, "#3AC290"));
                map.putIfAbsent(e.getGene().getName(), new VisNode(e.getGene().getName().hashCode(),
                        e.getGene().getName(), e.getGene().getDescription(), 2, "#44AAC2"));
                edges.add(new VisEdge(map.get(e.getProtein().getName()), map.get(e.getGene().getName()), "to", ""));
            }
            graph.addNodes(map.values());
            graph.addEdges(edges);
            GraphVisualizer.makeHTML(graph);
        }
    }

}
