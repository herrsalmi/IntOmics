package org.pmoi.business;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
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
        if (Files.exists(Paths.get("GODB.obj"))) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File("GODB.obj")))){
                this.internalDB = (Map<String, Set<String>>) ois.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {
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
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File("GODB.obj")))){
                oos.writeObject(internalDB);
            }
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
