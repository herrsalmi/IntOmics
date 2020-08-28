package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.Args;
import org.pmoi.database.GeneMapper;
import org.pmoi.model.Gene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TranscriptomeManager {
    private static TranscriptomeManager instance;
    private static final Logger LOGGER = LogManager.getRootLogger();

    private TranscriptomeManager() {
    }

    /**
     * Extracts genes for which protein product are part of the membrane
     * @param fileName file name
     * @return list of gene (membranome)
     */
    public List<Gene> getMembranomeFromGenes(String fileName) {
        SurfaceomeMapper surfaceomeMapper = SurfaceomeMapper.getInstance();
        var mapper = GeneMapper.getInstance();
        List<Gene> inputGenes = Collections.emptyList();
        try (var stream = Files.lines(Path.of(fileName))){
            inputGenes = stream
                    .filter(l -> !l.startsWith("#"))
                    .filter(Predicate.not(String::isBlank))
                    .filter(l -> !l.trim().startsWith(";"))
                    .distinct()
                    .map(l -> new Gene(l, ""))
                    .collect(Collectors.toList());
            ExecutorService executor = Executors.newFixedThreadPool(Args.getInstance().getThreads());
            inputGenes.forEach(g -> executor.submit(() -> g.setEntrezID(mapper.getId(g.getName()).orElse(""))));
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.DAYS);
        } catch (IOException e) {
            LOGGER.error(e);
        } catch (InterruptedException e) {
            LOGGER.error(e);
            Thread.currentThread().interrupt();
        }

        // if a gene has no EntrezID it will also get removed here
        return inputGenes.parallelStream()
                .filter(g -> g.getEntrezID() != null && !g.getEntrezID().isEmpty())
                .filter(g -> surfaceomeMapper.isSurfaceProtein(g.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Loads DE genes from file
     * @param fileName file name
     * @return list of DE genes
     */
    public List<Gene> getDEGenes(String fileName) {
        Predicate<Gene> condition = e -> true;
        return getDEGenesWithCondition(condition, fileName, true);
    }

    /**
     * Helper function to load genes from a file using a predicate as a filter
     * @param condition predicate for filtering genes
     * @param filename file name
     * @param useFC filter genes also based on FC
     * @return list of genes
     */
    private List<Gene> getDEGenesWithCondition(Predicate<Gene> condition, String filename, boolean useFC) {
        var mapper = GeneMapper.getInstance();
        List<Gene> inputGenes = readDEGeneFile(filename).stream().distinct().collect(Collectors.toList());
        ExecutorService executor = Executors.newFixedThreadPool(Args.getInstance().getThreads());
        inputGenes.forEach(g -> executor.submit(() -> g.setEntrezID(mapper.getId(g.getName()).orElse(""))));
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            LOGGER.error(e);
            Thread.currentThread().interrupt();
        }
        // if a gene has no EntrezID it will also get removed here
        return inputGenes.parallelStream()
                .filter(g -> g.getEntrezID() != null && !g.getEntrezID().isEmpty())
                .filter(g -> {
                    if (useFC)
                        return Math.abs(g.getFoldChange()) >= Args.getInstance().getFoldChange();
                    else
                        return true;
                })
                .filter(condition)
                .collect(Collectors.toList());
    }

    /**
     * Helper function to load genes from a file and map them to Gene objects
     * @param filePath file path
     * @return list of genes
     */
    private List<Gene> readDEGeneFile(String filePath) {
        try (var stream = Files.lines(Path.of(filePath))){
            return stream
                    .skip(1)
                    .filter(Predicate.not(String::isBlank))
                    .filter(l -> !l.trim().startsWith(";"))
                    .distinct()
                    .map(Gene::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error(e);
        }
        return Collections.emptyList();
    }

    public static synchronized TranscriptomeManager getInstance() {
        if (instance == null)
            instance = new TranscriptomeManager();
        return instance;
    }
}
