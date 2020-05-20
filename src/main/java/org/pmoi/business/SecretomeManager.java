package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.models.Protein;
import org.pmoi.models.SecretomeMappingMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private SecretomeMappingMode mode;
    private Predicate<Protein> secretomeMapper;

    private SecretomeManager() {
        loadDB();
    }

    private void loadDB() {
        try {
            String dbFilePath = "metazSecKB.txt";
            this.internalDB = Files.lines(Path.of(dbFilePath)).map(e -> {
                if (e.split("\t").length != 5) {
                    LOGGER.error("Wrong number of fileds in line: " + e);
                }
                return e.split("\t")[4];
            }).collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
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

    public List<Protein> LoadSecretomeFile(String filePath) {
        isMappingModeSet();
        try {
            var data = Files.lines(Path.of(filePath))
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
                e.printStackTrace();
            }
            return data.stream()
                    .filter(e -> e.getName() != null)
                    .filter(secretomeMapper)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
