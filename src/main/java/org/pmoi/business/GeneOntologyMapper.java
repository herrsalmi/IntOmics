package org.pmoi.business;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
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
        Files.lines(Path.of("gene2go")).skip(1).forEach(l -> {
            String[] data = l.split("\t");
            internalDB.putIfAbsent(data[1], new HashSet<>());
            internalDB.get(data[1]).add(data[2]);
        });
        Files.lines(Path.of("mart_export.txt")).skip(1)
                .map(l -> l.split("\t"))
                .filter(data -> data.length == 4)
                .forEach(data -> {
                    internalDB.putIfAbsent(data[3], new HashSet<>());
                    internalDB.get(data[3]).add(data[1]);
                });
    }

    public boolean checkMembranomeGO(String entrezID) {
        if (!internalDB.containsKey(entrezID))
            return false;
        return internalDB.get(entrezID).contains("GO:0009986") || internalDB.get(entrezID).contains("GO:0005886") || internalDB.get(entrezID).contains("GO:0004888");
    }

    public boolean checkSecretomeGO(String entrezID) {
        if (!internalDB.containsKey(entrezID))
            return false;
        return internalDB.get(entrezID).contains("GO:0031012") || internalDB.get(entrezID).contains("GO:0005615");
    }
}
