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
    private GeneMapper mapper;

    private SecretomeManager() {
        mapper = GeneMapper.getInstance();
        GeneOntologyMapper goMapper = new GeneOntologyMapper();
        secretomeMapper = switch(Args.getInstance().getSpecies()) {
            case HUMAN -> protein -> goMapper.checkSecretomeGO(protein.getNcbiID());
            case MOUSE, RAT, COW -> protein -> goMapper.checkMembranomeGO(protein.getNcbiID());
        };
    }

    public boolean isSecreted(String gene) {
        return internalDB.contains(gene.toUpperCase());
    }

    /**
     * Load protein names/IDs from a file
     * @param filePath file path
     * @return list of proteins
     */
    public List<Protein> loadSecretomeFile(String filePath) {
        try (var stream = Files.lines(Path.of(filePath))){
            var data = stream
                    .filter(e -> !e.startsWith("#"))
                    .filter(Predicate.not(String::isBlank))
                    .distinct()
                    .map(Protein::new)
                    .collect(Collectors.toList());

            // convert id <=> name in order to have them both
            ExecutorService executor = Executors.newFixedThreadPool(Args.getInstance().getThreads());
            if (data.get(0).getNcbiID() != null) {
                data.forEach(e -> executor.submit(() -> e.setName(mapper.getSymbol(e.getNcbiID()).orElse(""))));
            } else {
                data.forEach(e -> executor.submit(() -> e.setNcbiID(mapper.getId(e.getName()).orElse(""))));
            }
            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.DAYS);

            return data.stream()
                    .filter(e -> e.getName() != null)
                    .filter(e -> {
                        if (e.getNcbiID().isEmpty()) {
                            LOGGER.warn("Protein {} not recognized!", e.getName());
                            return false;
                        } else {
                            return true;
                        }
                    })
                    .filter(secretomeMapper)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.error(e);
        } catch (InterruptedException e) {
            LOGGER.error(e);
            Thread.currentThread().interrupt();
        }
        return Collections.emptyList();
    }

    public static synchronized SecretomeManager getInstance() {
        if (instance == null)
            instance = new SecretomeManager();
        return instance;
    }


}
