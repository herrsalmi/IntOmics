package org.pmoi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.business.*;
import org.pmoi.database.GeneMapper;
import org.pmoi.model.*;
import org.pmoi.model.vis.VisEdge;
import org.pmoi.model.vis.VisGraph;
import org.pmoi.model.vis.VisNode;
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
    private final InteractionQueryClient ppiQueryClient;
    private OutputFormatter formatter;
    private PathwayClient pathwayClient;

    public OperationDispatcher() {
        ppiQueryClient = Args.getInstance().getStringDBScore() > 700 ?
                CachedInteractionQueryClient.getInstance() : new StringdbQueryClient();
    }

    public Runner setup(String prefix, OutputFormatter formatter) {
        this.formatter = formatter;
        String extension = formatter instanceof TSVFormatter ? "tsv" : "txt";
        String output = String.format("%s_%s_fc%1.1f.%s", prefix, Args.getInstance().getStringDBScore(),
                Args.getInstance().getFoldChange(), extension);
        TranscriptomeManager transcriptomeManager = TranscriptomeManager.getInstance();
        SecretomeManager secretomeManager = SecretomeManager.getInstance();
        pathwayClient = PathwayClient.getInstance();
        CSVValidator validator = new CSVValidator();
        if (!validator.isConform(Args.getInstance().getTranscriptome()))
            System.exit(1);
        LOGGER.info("Loading secretome");
        var secretome = secretomeManager.loadSecretomeFile(Args.getInstance().getSecretome());

        LOGGER.info("Loading membranome");
        List<Gene> membranome = transcriptomeManager.getMembranomeFromGenes(Args.getInstance().getAllGenes());

        LOGGER.info("Loading transcriptome");
        List<Gene> transcriptome = transcriptomeManager.getDEGenes(Args.getInstance().getTranscriptome());

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
            filteredTranscriptome = transcriptome.parallelStream().filter(e -> pathwayClient.isInAnyPathway(e.getName())).collect(Collectors.toList());
            LOGGER.info("Getting PPI network ...");
            secretome.forEach(e -> executorService.submit(() -> {
                // Adding StringDB interactors
                // the map contains <interactor, score>
                Map<String, String> interactors = ppiQueryClient.getProteinNetwork(e.getName());
                // Add gene from the pathway to the map
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
            executorService.awaitTermination(1, TimeUnit.HOURS);
            return this;
        }

        private Runner getPathways() {
            LOGGER.info("Looking for pathway interactions ...");
            var resultMap = resultSet.stream().collect(Collectors.groupingBy(ResultRecord::getProtein));
            resultMap.forEach((key, value) -> {
                // get a list of pathways where the protein is involved
                if (Args.getInstance().getPathwayDB().equals(PathwayMode.KEGG)) {
                    var pathways = pathwayClient.KEGGSearch(key.getEntrezID());
                    pathways.stream().map(e -> e.split(" {2}")).forEach(e -> key.addPathway(new Pathway(e[0], e[1])));
                    key.getPathways().forEach(e -> e.setGenes(pathwayClient.getKEGGPathwayGenes(e.getPathwayID())));

                } else if (Args.getInstance().getPathwayDB().equals(PathwayMode.WIKIPATHWAYS)) {
                    var pathways = pathwayClient.getPathways(key.getName());
                    if (pathways == null || pathways.isEmpty())
                        return;
                    pathways.forEach(key::addPathway);

                }
                value.forEach(resultRecord -> {
                    LOGGER.debug("Processing [P: {} # G: {}]", key.getName(), resultRecord.getGene().getName());
                    resultRecord.getProtein().getPathways().forEach(p -> {
                        if (p.getGenes().contains(resultRecord.getGene()))
                            resultRecord.getGene().setInteractors(p.getName(), p.getGenes().stream().distinct().filter(transcriptome::contains).collect(Collectors.toList()));
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
            //resultSet.parallelStream().forEach(e -> e.getGene().getGeneSets().removeIf(geneSet -> geneSet.getPvalue() >= 0.05));
            return this;
        }

        private void writeResults() {
            LOGGER.info("Writing results ...");
            resultSet.stream().collect(Collectors.groupingBy(ResultRecord::getProtein))
                    .forEach((k, v) -> {
                        k.setDescription(mapper.getDescription(k.getEntrezID()).orElse("-"));
                        v.sort(Comparator.comparingDouble((ResultRecord o) -> o.getGene().getFoldChange()).reversed());
                        v.forEach(e -> e.getGene().setDescription(mapper.getDescription(e.getGene().getEntrezID()).orElse("-")));
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
