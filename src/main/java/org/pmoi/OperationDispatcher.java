package org.pmoi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.business.GeneOntologyMapper;
import org.pmoi.business.NCBIQueryClient;
import org.pmoi.business.SecretomeManager;
import org.pmoi.business.StringdbQueryClient;
import org.pmoi.models.Gene;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class OperationDispatcher {
    private static final Logger LOGGER = LogManager.getRootLogger();
    private StringdbQueryClient stringdbQueryClient;

    public OperationDispatcher() {
        stringdbQueryClient = new StringdbQueryClient();
    }

    public void run() {
        List<String> membranome = getMembranomeFromDEGenes().stream().map(Gene::getGeneName).collect(Collectors.toList());
        List<String> secretome = getSecretomeFromFile("secretome.txt").stream().map(Gene::getGeneName).collect(Collectors.toList());

        LOGGER.info("Number of secreted proteins: " + secretome.size());

        writeInteractions(secretome, membranome, "interactionNetworkS2M.tsv");

//        StringBuffer outputBuffer = new StringBuffer("#node1\tnode2\tscore\n");
//        membranome.forEach(e -> {
//            Map<String, String> interactors = stringdbQueryClient.getProteinNetwork(e);
//            List<String> interactorsNames = new ArrayList<>(interactors.keySet());
//            interactorsNames.retainAll(secretome);
//            if (!interactorsNames.isEmpty()) {
//                interactorsNames.forEach(interactor -> outputBuffer.append(e).append("\t")
//                        .append(interactor).append("\t")
//                        .append(interactors.get(interactor)).append("\n"));
//            }
//        });
//
//        try {
//            BufferedWriter bw = Files.newBufferedWriter(Paths.get("interactionNetworkM2S.tsv"));
//            bw.write(outputBuffer.toString());
//            bw.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        StringBuffer finalOutputBuffer = new StringBuffer("#node1\tnode2\tscore\n");
//        secretome.forEach(e -> {
//            Map<String, String> interactors = stringdbQueryClient.getProteinNetwork(e);
//            List<String> interactorsNames = new ArrayList<>(interactors.keySet());
//            interactorsNames.retainAll(membranome);
//            if (!interactorsNames.isEmpty()) {
//                interactorsNames.forEach(interactor -> finalOutputBuffer.append(e).append("\t")
//                        .append(interactor).append("\t")
//                        .append(interactors.get(interactor)).append("\n"));
//            }
//        });
//
//        try {
//            BufferedWriter bw = Files.newBufferedWriter(Paths.get("interactionNetworkS2M.tsv"));
//            bw.write(finalOutputBuffer.toString());
//            bw.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    private void writeInteractions(List<String> set1, List<String> set2, String outputFileName) {
        StringBuffer outputBuffer = new StringBuffer("#node1\tnode2\tscore\n");
        ExecutorService executorService = Executors.newFixedThreadPool(3);

        set1.forEach( e -> executorService.submit(() -> {
            Map<String, String> interactors = stringdbQueryClient.getProteinNetwork(e);
            List<String> interactorsNames = new ArrayList<>(interactors.keySet());
            interactorsNames.retainAll(set2);
            if (!interactorsNames.isEmpty()) {
                interactorsNames.forEach(interactor -> outputBuffer.append(String.format("%s\t%s\t%s\n", e, interactor, interactors.get(interactor))));
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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Gene> getMembranomeFromDEGenes() {
        NCBIQueryClient ncbiQueryClient = new NCBIQueryClient();
        GeneOntologyMapper goMapper = new GeneOntologyMapper();
        try {
            goMapper.load("gene2go");
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Gene> inputGenes = readDEGeneFile("DE_genes.txt");

        ExecutorService executor = Executors.newFixedThreadPool(4);
        AtomicInteger index = new AtomicInteger(0);

        assert inputGenes != null;
        inputGenes.forEach(g -> executor.submit(() -> ncbiQueryClient.geneNameToEntrezID(inputGenes.get(index.getAndIncrement()))));

        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // if a gene has no EntrezID it will also get removed here
        return inputGenes.parallelStream()
                .filter(g -> g.getGeneEntrezID() != null && !g.getGeneEntrezID().isEmpty())
                .filter(e -> goMapper.checkGO(e.getGeneEntrezID()))
                .collect(Collectors.toList());
    }

    private List<Gene> getSecretomeFromFile(String filePath) {
        List<Gene> inputGenes = readSecretomeGeneFile(filePath);
        NCBIQueryClient ncbiQueryClient = new NCBIQueryClient();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        assert inputGenes != null;
        inputGenes.forEach(g -> executor.submit(() -> ncbiQueryClient.entrezIDToGeneName(g)));

        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // keep only proteins that match an entry in the DB
        SecretomeManager secretomeDB = SecretomeManager.getInstance();
        return inputGenes.stream().filter(e -> secretomeDB.isSecreted(e.getGeneName())).collect(Collectors.toList());
    }

    private List<Gene> readDEGeneFile(String filePath) {
        try {
            return Files.lines(Path.of(filePath))
                    .filter(Predicate.not(String::isBlank))
                    .distinct()
                    .map(Gene::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<Gene> readSecretomeGeneFile(String filePath) {
        try {
            return Files.lines(Path.of(filePath))
                    .filter(Predicate.not(String::isBlank))
                    .distinct()
                    .map(e -> new Gene(Integer.parseInt(e)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
