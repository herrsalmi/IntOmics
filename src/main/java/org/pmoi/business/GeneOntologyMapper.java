package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.database.SpeciesHelper;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.Set;

public class GeneOntologyMapper {

    private static GeneOntologyMapper instance;

    private static final Logger LOGGER = LogManager.getRootLogger();
    private Map<String, Set<String>> internalDB;

    private GeneOntologyMapper() {
        LOGGER.debug("Loading GO database");
        try {
            load();
        } catch (IOException e) {
            LOGGER.error(e);
        }
        LOGGER.debug("GO database loaded");
    }

    public static synchronized GeneOntologyMapper getInstance() {
        if (instance == null)
            instance = new GeneOntologyMapper();
        return instance;
    }

    /**
     * Loads obj file from resources folder into memory
     * @throws IOException something wrong happened, I don't care
     */
    private void load() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/GODB_" + SpeciesHelper.get().getTaxonomyId() + ".obj")){
            ObjectInputStream ois = new ObjectInputStream(in);
            this.internalDB = (Map<String, Set<String>>) ois.readObject();
        } catch (ClassNotFoundException e) {
            LOGGER.error(e);
        }

    }

    /**
     * Check for annotations: "integral component of membrane" (GO:0016021) or "cell surface" (GO:0009986)  ["plasma membrane" (GO:0005886)]
     * @param entrezID gene ID
     * @return true if the protein product is a membrane protein
     */
    public boolean checkMembranomeGO(String entrezID) {
        if (!internalDB.containsKey(entrezID))
            return false;
        return (internalDB.get(entrezID).contains("GO:0016021") || internalDB.get(entrezID).contains("GO:0009986"));
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
