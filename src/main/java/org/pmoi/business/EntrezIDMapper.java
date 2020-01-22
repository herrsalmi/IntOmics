package org.pmoi.business;

import com.google.common.collect.BiMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.models.Feature;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EntrezIDMapper {

    private static EntrezIDMapper instance;

    private static final Logger LOGGER = LogManager.getRootLogger();

    private BiMap<String, String> internalDB;
    private NCBIQueryClient ncbiQueryClient;
    private int intialSize;

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
            this.ncbiQueryClient = new NCBIQueryClient();
            this.intialSize = internalDB.size();
        }
    }

    public String idToName(String id) {
        String name;
        if (internalDB.get(id) == null) {
            name = ncbiQueryClient.entrezIDToGeneName(id);
            try {
                internalDB.put(id, name);
            } catch (IllegalArgumentException ignored) {
            }
        } else {
            name = internalDB.get(id);
        }
        return name;
    }

    public String nameToId(String name) {
        String id;
        if (internalDB.inverse().get(name) == null) {
            id = ncbiQueryClient.geneNameToEntrezID(name);
            try {
                internalDB.put(id, name);
            } catch (IllegalArgumentException ignored) {
            }
        } else {
            id = internalDB.inverse().get(name);
        }
        return id;
    }

    public void idToName(Feature feature) {
        feature.setName(idToName(feature.getEntrezID()));
    }

    public void nameToId(Feature feature) {
        feature.setEntrezID(nameToId(feature.getName()));
    }

    public synchronized void close() {
        if (internalDB.size() > this.intialSize) {
            try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File("internalDB.obj")))) {
                oos.writeObject(internalDB);
            } catch (IOException e) {
                LOGGER.error("Unable to save EntrezMapper internal database!");
            }
        }
    }

    public static EntrezIDMapper getInstance() {
        if (instance == null) {
            synchronized (EntrezIDMapper.class) {
                if (instance == null)
                    instance = new EntrezIDMapper();
            }
        }
        return instance;
    }
}
