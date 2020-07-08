package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GeneOntologyMapper {

    private static final Logger LOGGER = LogManager.getRootLogger();
    private Map<String, Set<String>> internalDB;

    public GeneOntologyMapper() {
        LOGGER.debug("Loading GO database");
        this.internalDB = new ConcurrentHashMap<>(100000);
        try {
            load();
        } catch (IOException e) {
            LOGGER.error(e);
        }
        LOGGER.debug("GO database loaded");
    }

    private void load() throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(
                Objects.requireNonNull(getClass().getClassLoader().getResource("GODB.obj")).toURI())))){
            this.internalDB = (Map<String, Set<String>>) ois.readObject();
        } catch (ClassNotFoundException | URISyntaxException e) {
            LOGGER.error(e);
        }

    }

    public boolean checkMembranomeGO(String entrezID) {
        if (!internalDB.containsKey(entrezID))
            return false;
        return (internalDB.get(entrezID).contains("GO:0009986") || internalDB.get(entrezID).contains("GO:0005886"));
    }

    /**
     * Check for annotations: "extracellular matrix" (GO:0031012) or "extracellular space" (GO:0005615)
     * @param entrezID gene ID
     * @return true if the protein product is actively secreted
     */
    public boolean checkSecretomeGO(String entrezID) {
        if (!internalDB.containsKey(entrezID))
            return false;
        return internalDB.get(entrezID).contains("GO:0031012") || internalDB.get(entrezID).contains("GO:0005615");
    }
}
