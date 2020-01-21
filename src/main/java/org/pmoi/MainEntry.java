package org.pmoi;


import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import org.pmoi.business.NCBIQueryClient;
import org.pmoi.models.ProteomeType;
import org.pmoi.models.SecretomeMappingMode;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MainEntry {

    public static BiMap<String, String> internalDB;

    public MainEntry() {
//        loadInternalDB();
//        System.exit(0);
        OperationDispatcher operationDispatcher = new OperationDispatcher();
        operationDispatcher.run("output/interactionNetworkS2M_LF_GO_900.tsv", ProteomeType.LABEL_FREE, SecretomeMappingMode.GOTERM);
    }

    private void loadInternalDB() {
        if (Files.notExists(Paths.get("internalDB.obj"))) {
            createInternalDB();
        } else {
            try {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File("internalDB.obj")));
                internalDB = (BiMap<String, String>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }

    private void createInternalDB() {
        try {
            var geneNames = Files.lines(Paths.get("genes.txt"))
                    .skip(1)
                    .map(e -> e.split("\\.")[0])
                    .collect(Collectors.toSet());
            BiMap<String, String> map = Maps.synchronizedBiMap(HashBiMap.create(200000));

            NCBIQueryClient ncbiQueryClient = new NCBIQueryClient();
            ExecutorService executorService = Executors.newFixedThreadPool(4);
            AtomicInteger counter = new AtomicInteger(1);
            geneNames.forEach(e -> executorService.submit(() -> {
                System.out.print(String.format("\rProcessing %d out of %d ", counter.getAndIncrement(), geneNames.size()));
                try {
                    map.put(ncbiQueryClient.geneNameToEntrezID(e), e);
                } catch (IllegalArgumentException ex) {
                    System.out.println(String.format("\nERROR! unable to insert gene name for [%s], value already present ", e));
                }

            }));
            executorService.shutdown();
            executorService.awaitTermination(10, TimeUnit.DAYS);
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File("internalDB.obj")));
            oos.writeObject(map);
            oos.close();
            internalDB = map;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        new MainEntry();
    }
}
