package org.pmoi.business;

import com.google.common.collect.BiMap;
import org.pmoi.models.Feature;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URISyntaxException;
import java.util.Objects;

public class EntrezIDMapper {

    private static volatile EntrezIDMapper instance;

    private BiMap<String, String> internalDB;
    private NCBIQueryClient ncbiQueryClient;

    private EntrezIDMapper() {
        loadInternalDB();
    }

    private void loadInternalDB() {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(Objects.requireNonNull(getClass()
                    .getClassLoader().getResource("internalDB.obj")).toURI())));
            this.internalDB = (BiMap<String, String>) ois.readObject();
        } catch (IOException | ClassNotFoundException | URISyntaxException e) {
            e.printStackTrace();
        }
        this.ncbiQueryClient = new NCBIQueryClient();
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
