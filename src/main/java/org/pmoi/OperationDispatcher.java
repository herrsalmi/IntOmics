package org.pmoi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.business.GeneOntologyMapper;
import org.pmoi.business.NCBIQueryClient;
import org.pmoi.business.SecretomeManager;
import org.pmoi.business.StringdbQueryClient;
import org.pmoi.handler.Parser;
import org.pmoi.models.Feature;
import org.pmoi.models.Gene;
import org.pmoi.models.Mode;
import org.pmoi.models.Protein;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    public void run(Mode mode) {

        List<Protein> allSecretome;
        switch (mode) {
            case LABEL_FREE:
                allSecretome = getSecretomeFromLabelFreeFile("Secretome_label_free.csv");
                break;
            case LCMS:
                allSecretome = getSecretomeFromLCMSFile("Secretome.csv");
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + mode);
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
        List<Gene> membranome = getMembranomeFromDEGenes("Gene_DE.csv");

        LOGGER.info("Number of secreted proteins: " + allSecretome.size());
        LOGGER.info("Number of secreted proteins more expressed in depleted samples: " + secretome.size());
        writeInteractions(secretome, membranome, "interactionNetworkS2M_LF_GO.tsv");

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

    private List<Gene> getMembranomeFromDEGenes(String fileName) {
        NCBIQueryClient ncbiQueryClient = new NCBIQueryClient();
        GeneOntologyMapper goMapper = new GeneOntologyMapper();
        try {
            goMapper.load("gene2go");
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Gene> inputGenes = Objects.requireNonNull(readDEGeneFile(fileName)).stream().distinct().collect(Collectors.toList());
        ExecutorService executor = Executors.newFixedThreadPool(4);
        inputGenes.forEach(g -> executor.submit(() -> ncbiQueryClient.geneNameToEntrezID(g)));
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // if a gene has no EntrezID it will also get removed here
        return inputGenes.parallelStream()
                .filter(g -> g.getEntrezID() != null && !g.getEntrezID().isEmpty())
                .filter(e -> goMapper.checkMembrannomeGO(e.getEntrezID()))
                .collect(Collectors.toList());
    }

    private List<Protein> getSecretomeFromLCMSFile(String filePath) {
        List<Protein> inputProtein = Objects.requireNonNull(LoadSecretomeFile(filePath))
                .stream()
                .filter(e -> e.getEntrezID() != null)
                .collect(Collectors.toList());
        NCBIQueryClient ncbiQueryClient = new NCBIQueryClient();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        inputProtein.forEach(g -> executor.submit(() -> ncbiQueryClient.entrezIDToGeneName(g)));

        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // keep only proteins that match an entry in the DB
        SecretomeManager secretomeDB = SecretomeManager.getInstance();
        return inputProtein.stream()
                .filter(e -> e.getName() != null)
                .filter(e -> secretomeDB.isSecreted(e.getName()))
                .collect(Collectors.toList());
    }

    private List<Protein> getSecretomeFromLabelFreeFile(String filePath) {
        try {
            SecretomeManager secretomeDB = SecretomeManager.getInstance();
            var inputProteins =  Files.lines(Path.of(filePath))
                    .skip(1)
                    .filter(Predicate.not(String::isBlank))
                    .distinct()
                    .map(e -> e.split(";"))
                    .filter(e -> Parser.tryParseDouble(e[3]))
                    .filter(e -> Parser.tryParseDouble(e[4]))
                    .filter(e -> Double.parseDouble(e[3]) < 0.05 && Double.parseDouble(e[4]) > 1.3)
                    .map(e -> new Protein(e[6], Double.parseDouble(e[7]), Double.parseDouble(e[8])))
                    //.filter(e -> secretomeDB.isSecreted(e.getName()))
                    .collect(Collectors.toList());

            ExecutorService executor = Executors.newFixedThreadPool(4);
            NCBIQueryClient ncbiQueryClient = new NCBIQueryClient();
            inputProteins.forEach(g -> executor.submit(() -> ncbiQueryClient.geneNameToEntrezID(g)));
            executor.shutdown();
            try {
                executor.awaitTermination(10, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            GeneOntologyMapper goMapper = new GeneOntologyMapper();
            return inputProteins.stream()
                    .filter(e -> goMapper.checkSecretomeGO(e.getEntrezID()))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<Gene> readDEGeneFile(String filePath) {
        try {
            return Files.lines(Path.of(filePath))
                    .skip(1)
                    .filter(Predicate.not(String::isBlank))
                    .distinct()
                    .map(Gene::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private List<Protein> LoadSecretomeFile(String filePath) {
        try {
            return Files.lines(Path.of(filePath))
                    .skip(1)
                    .filter(Predicate.not(String::isBlank))
                    .distinct()
                    .map(Protein::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
