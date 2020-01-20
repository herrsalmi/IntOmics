package org.pmoi.business;

import com.google.common.collect.BiMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EntrezIDMapper {

    private static EntrezIDMapper instance;

    private BiMap<String, String> internalDB;

    private EntrezIDMapper() {
        loadInternalDB();
    }

    private void loadInternalDB() {
        if (Files.notExists(Paths.get("internalDB.obj"))) {
            throw new UnsupportedOperationException();
        } else {
            try {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File("internalDB.obj")));
                this.internalDB = (BiMap<String, String>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public String idToName(String id) {
        return internalDB.getOrDefault(id, null);
    }

    public String nameToId(String name) {
        return internalDB.inverse().getOrDefault(name, null);
    }

    public synchronized static EntrezIDMapper getInstance() {
        if (instance == null) {
            instance = new EntrezIDMapper();
        }
        return instance;
    }
}
