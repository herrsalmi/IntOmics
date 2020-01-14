package org.pmoi.business;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GeneOntologyMapper {

    private Map<String, List<String>> internalDB;

    public GeneOntologyMapper() {
        this.internalDB = new ConcurrentHashMap<>(100000);
        try {
            load("gene2go");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void load(String file) throws IOException {
        Files.lines(Path.of(file)).skip(1).forEach(l -> {
            String[] data = l.split("\t");
            internalDB.putIfAbsent(data[1], new ArrayList<>());
            internalDB.get(data[1]).add(data[2]);
        });
    }

    public boolean checkMembranomeGO(String entrezID) {
        if (!internalDB.containsKey(entrezID))
            return false;
        return internalDB.get(entrezID).contains("GO:0009986") || internalDB.get(entrezID).contains("GO:0005886");
    }

    public boolean checkSecretomeGO(String entrezID) {
        if (!internalDB.containsKey(entrezID))
            return false;
        return internalDB.get(entrezID).contains("GO:0031012") || internalDB.get(entrezID).contains("GO:0005615");
    }
}
