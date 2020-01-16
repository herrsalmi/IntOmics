package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.handler.NumberParser;
import org.pmoi.models.Protein;
import org.pmoi.models.SecretomeMappingMode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
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
    private final String dbFilePath = "metazSecKB.txt";
    private SecretomeMappingMode mode;
    private Predicate<Protein> secretomeMapper;

    private SecretomeManager() {
        loadDB();
    }

    private void loadDB() {
        try {
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
            case METAZSECKB:
                SecretomeManager secretomeDB = SecretomeManager.getInstance();
                secretomeMapper = protein -> secretomeDB.isSecreted(protein.getName());
                break;
            case GOTERM:
                GeneOntologyMapper goMapper = new GeneOntologyMapper();
                secretomeMapper = protein -> goMapper.checkSecretomeGO(protein.getEntrezID());
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + mode);
        }
    }

    public List<Protein> getSecretomeFromLCMSFile(String filePath) {
        isMappingModeSet();
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
        return inputProtein.stream()
                .filter(e -> e.getName() != null)
                .filter(secretomeMapper)
                .collect(Collectors.toList());
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

    public List<Protein> getSecretomeFromLabelFreeFile(String filePath) {
        isMappingModeSet();
        try {
            SecretomeManager secretomeDB = SecretomeManager.getInstance();
            var inputProteins =  Files.lines(Path.of(filePath))
                    .skip(1)
                    .filter(Predicate.not(String::isBlank))
                    .distinct()
                    .map(e -> e.split(";"))
                    .filter(e -> NumberParser.tryParseDouble(e[3]))
                    .filter(e -> NumberParser.tryParseDouble(e[4]))
                    .filter(e -> Double.parseDouble(e[3]) < 0.05 && Double.parseDouble(e[4]) > 1.3)
                    .map(e -> new Protein(e[6], Double.parseDouble(e[7]), Double.parseDouble(e[8])))
                    .collect(Collectors.toList());
            if (mode.equals(SecretomeMappingMode.GOTERM)) {
                ExecutorService executor = Executors.newFixedThreadPool(4);
                NCBIQueryClient ncbiQueryClient = new NCBIQueryClient();
                inputProteins.forEach(g -> executor.submit(() -> ncbiQueryClient.geneNameToEntrezID(g)));
                executor.shutdown();
                try {
                    executor.awaitTermination(10, TimeUnit.DAYS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return inputProteins.stream()
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
