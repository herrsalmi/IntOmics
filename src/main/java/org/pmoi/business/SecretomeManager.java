package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.Args;
import org.pmoi.database.GeneMapper;
import org.pmoi.model.Protein;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SecretomeManager {

    private static SecretomeManager instance;

    private static final Logger LOGGER = LogManager.getRootLogger();

    private Set<String> internalDB;
    private Predicate<Protein> secretomeMapper;

    private SecretomeManager() {
        GeneOntologyMapper goMapper = new GeneOntologyMapper();
        secretomeMapper = protein -> goMapper.checkSecretomeGO(protein.getEntrezID());
    }

    public boolean isSecreted(String gene) {
        return internalDB.contains(gene.toUpperCase());
    }

    public List<Protein> loadSecretomeFile(String filePath) {
        try (var stream = Files.lines(Path.of(filePath))){
            var data = stream
                    .filter(e -> !e.startsWith("#"))
                    .filter(Predicate.not(String::isBlank))
                    .distinct()
                    .map(Protein::new)
                    .collect(Collectors.toList());

            // convert id <=> name in order to have them both
            var mapper = GeneMapper.getInstance();
            ExecutorService executor = Executors.newFixedThreadPool(Args.getInstance().getThreads());
            if (data.get(0).getEntrezID() != null) {
                data.forEach(e -> executor.submit(() -> e.setName(mapper.getSymbol(e.getEntrezID()).orElse(""))));
            } else {
                data.forEach(e -> executor.submit(() -> e.setEntrezID(mapper.getId(e.getName()).orElse(""))));
            }
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                LOGGER.error(e);
                Thread.currentThread().interrupt();
            }
            return data.stream()
                    .filter(e -> e.getName() != null)
                    .filter(secretomeMapper)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error(e);
        }
        return Collections.emptyList();
    }

    public static synchronized SecretomeManager getInstance() {
        if (instance == null)
            instance = new SecretomeManager();
        return instance;
    }


}
