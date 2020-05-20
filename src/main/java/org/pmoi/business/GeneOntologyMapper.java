package org.pmoi.business;

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

    private Map<String, Set<String>> internalDB;

    public GeneOntologyMapper() {
        this.internalDB = new ConcurrentHashMap<>(100000);
        try {
            load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load() throws IOException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(
                Objects.requireNonNull(GeneOntologyMapper.class.getClassLoader().getResource("GODB.obj")).toURI())))){
            this.internalDB = (Map<String, Set<String>>) ois.readObject();
        } catch (ClassNotFoundException | URISyntaxException e) {
            e.printStackTrace();
        }

    }

    public boolean checkMembranomeGO(String entrezID) {
        if (!internalDB.containsKey(entrezID))
            return false;
        return (internalDB.get(entrezID).contains("GO:0009986") || internalDB.get(entrezID).contains("GO:0005886"));
    }

    public boolean checkSecretomeGO(String entrezID) {
        if (!internalDB.containsKey(entrezID))
            return false;
        return internalDB.get(entrezID).contains("GO:0031012") || internalDB.get(entrezID).contains("GO:0005615");
    }
}
