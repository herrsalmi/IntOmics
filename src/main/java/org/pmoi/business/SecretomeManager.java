package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.model.Protein;
import org.pmoi.model.SecretomeMappingMode;

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
import java.util.stream.Stream;

public class SecretomeManager {

    private static SecretomeManager instance;

    private static final Logger LOGGER = LogManager.getRootLogger();

    private Set<String> internalDB;
    private SecretomeMappingMode mode;
    private Predicate<Protein> secretomeMapper;

    private SecretomeManager() {
        loadDB();
    }

    private void loadDB() {
        String dbFilePath = "metazSecKB.txt";
        try (Stream<String> stream = Files.lines(Path.of(dbFilePath))){
            this.internalDB = stream.map(e -> {
                if (e.split("\t").length != 5) {
                    LOGGER.error("Wrong number of fields in line: {}", e);
                }
                return e.split("\t")[4];
            }).collect(Collectors.toSet());
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    public boolean isSecreted(String gene) {
        return internalDB.contains(gene.toUpperCase());
    }

    public void setMappingMode(SecretomeMappingMode mode) {
        this.mode = mode;
        switch (mode) {
            case METAZSECKB -> {
                SecretomeManager secretomeDB = SecretomeManager.getInstance();
                secretomeMapper = protein -> secretomeDB.isSecreted(protein.getName());
            }
            case GOTERM -> {
                GeneOntologyMapper goMapper = new GeneOntologyMapper();
                secretomeMapper = protein -> goMapper.checkSecretomeGO(protein.getEntrezID());
            }
            default -> throw new IllegalStateException("Unexpected value: " + mode);
        }
    }

    public List<Protein> loadSecretomeFile(String filePath) {
        isMappingModeSet();
        try (var stream = Files.lines(Path.of(filePath))){
            var data = stream
                    .filter(e -> !e.startsWith("#"))
                    .filter(Predicate.not(String::isBlank))
                    .distinct()
                    .map(Protein::new)
                    .collect(Collectors.toList());

            // convert id <=> name in order to have them both
            EntrezIDMapper mapper = EntrezIDMapper.getInstance();
            ExecutorService executor = Executors.newFixedThreadPool(4);
            if (data.get(0).getEntrezID() != null) {
                data.forEach(e -> executor.submit(() -> mapper.idToName(e)));
                executor.shutdown();
            } else if (mode.equals(SecretomeMappingMode.GOTERM)){
                data.forEach(e -> executor.submit(() -> mapper.nameToId(e)));
                executor.shutdown();
            }
            try {
                executor.awaitTermination(10, TimeUnit.DAYS);
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

    private void isMappingModeSet() {
        if (mode == null)
            throw new IllegalStateException("Mapping mode not set!");
    }


    public static synchronized SecretomeManager getInstance() {
        if (instance == null)
            instance = new SecretomeManager();
        return instance;
    }


}
