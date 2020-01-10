package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

public class SecretomeManager {

    private static SecretomeManager instance;

    private static final Logger LOGGER = LogManager.getRootLogger();

    private Set<String> internalDB;
    private final String dbFilePath = "metazSecKB.txt";

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

    public static synchronized SecretomeManager getInstance() {
        if (instance == null)
            instance = new SecretomeManager();
        return instance;
    }


}
