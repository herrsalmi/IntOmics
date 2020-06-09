package org.pmoi.business;

import org.pmoi.ApplicationParameters;
import org.pmoi.model.Gene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TranscriptomeManager {
    private static TranscriptomeManager instance;

    private TranscriptomeManager() {

    }

    public List<Gene> getDEGenesExceptMembranome(String filename) {
        GeneOntologyMapper goMapper = new GeneOntologyMapper();
        Predicate<Gene> condition = e -> !goMapper.checkMembranomeGO(e.getEntrezID());
        return getDEGenesWithCondition(condition, filename, true);
    }

    public List<Gene> getMembranomeFromDEGenes(String fileName) {
        GeneOntologyMapper goMapper = new GeneOntologyMapper();
        SurfaceomeMapper surfaceomeMapper = SurfaceomeMapper.getInstance();
        //Predicate<Gene> condition = e -> goMapper.checkMembranomeGO(e.getEntrezID());
        Predicate<Gene> condition = e -> surfaceomeMapper.isSurfaceProtein(e.getName());
        return getDEGenesWithCondition(condition, fileName, false);
    }

    public List<Gene> getDEGenes(String fileName) {
        Predicate<Gene> condition = e -> true;
        return getDEGenesWithCondition(condition, fileName, true);
    }

    private List<Gene> getDEGenesWithCondition(Predicate<Gene> condition, String filename, boolean useFC) {
        EntrezIDMapper mapper = EntrezIDMapper.getInstance();
        List<Gene> inputGenes = Objects.requireNonNull(readDEGeneFile(filename)).stream().distinct().collect(Collectors.toList());
        ExecutorService executor = Executors.newFixedThreadPool(4);
        inputGenes.forEach(g -> executor.submit(() -> mapper.nameToId(g)));
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // if a gene has no EntrezID it will also get removed here
        return inputGenes.parallelStream()
                .filter(g -> g.getEntrezID() != null && !g.getEntrezID().isEmpty())
                .filter(g -> {
                    if (useFC)
                        return Math.abs(g.getFoldChange()) >= ApplicationParameters.getInstance().getGeneFoldChange();
                    else
                        return true;
                })
                .filter(condition)
                .collect(Collectors.toList());
    }

    private List<Gene> readDEGeneFile(String filePath) {
        try {
            return Files.lines(Path.of(filePath))
                    .skip(1)
                    .filter(Predicate.not(String::isBlank))
                    .filter(l -> !l.trim().startsWith(";"))
                    .distinct()
                    .map(Gene::new)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static synchronized TranscriptomeManager getInstance() {
        if (instance == null)
            instance = new TranscriptomeManager();
        return instance;
    }
}
