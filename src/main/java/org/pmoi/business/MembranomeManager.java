package org.pmoi.business;

import org.pmoi.ApplicationParameters;
import org.pmoi.models.Gene;

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

public class MembranomeManager {
    private static MembranomeManager instance;

    private MembranomeManager() {

    }

    public List<Gene> getDEGenesExceptMembranome(String filename) {
        GeneOntologyMapper goMapper = new GeneOntologyMapper();
        Predicate<Gene> condition = e -> !goMapper.checkMembranomeGO(e.getEntrezID());
        return getDEGenesWithCondition(condition, filename);
    }

    public List<Gene> getMembranomeFromDEGenes(String fileName) {
        GeneOntologyMapper goMapper = new GeneOntologyMapper();
        Predicate<Gene> condition = e -> goMapper.checkMembranomeGO(e.getEntrezID());
        return getDEGenesWithCondition(condition, fileName);
    }

    private List<Gene> getDEGenesWithCondition(Predicate<Gene> condition, String filename) {
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
                .filter(g -> Math.abs(g.getFoldChange()) >= ApplicationParameters.getInstance().getGeneFoldChange())
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

    public static synchronized MembranomeManager getInstance() {
        if (instance == null)
            instance = new MembranomeManager();
        return instance;
    }
}
