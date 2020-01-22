package org.pmoi;


import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import org.pmoi.models.ProteomeType;
import org.pmoi.models.SecretomeMappingMode;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

public class MainEntry {

    public static BiMap<String, String> internalDB;

    public MainEntry() {
//        loadInternalDB();
//        System.exit(0);
        OperationDispatcher operationDispatcher = new OperationDispatcher();
        operationDispatcher.run("output/S2M", ProteomeType.LABEL_FREE, SecretomeMappingMode.GOTERM);
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
            BiMap<String, String> map = Maps.synchronizedBiMap(HashBiMap.create(30000));
            AtomicInteger counter = new AtomicInteger(0);
            Files.lines(Paths.get("genes.txt"))
                    .skip(1)
                    .map(e -> e.split("\t"))
                    .filter(e -> e[0].equals("9606"))
                    .forEach(e -> {
                        System.out.println(String.format("%d - {%s : %s}",counter.incrementAndGet(), e[2], e[5]));
                        map.put(e[2], e[5]);
                    });


//            NCBIQueryClient ncbiQueryClient = new NCBIQueryClient();
//            ExecutorService executorService = Executors.newFixedThreadPool(4);
//            AtomicInteger counter = new AtomicInteger(1);
//            geneNames.forEach(e -> executorService.submit(() -> {
//                System.out.print(String.format("\rProcessing %d out of %d ", counter.getAndIncrement(), geneNames.size()));
//                try {
//                    map.put(ncbiQueryClient.geneNameToEntrezID(e), e);
//                } catch (IllegalArgumentException ex) {
//                    System.out.println(String.format("\nERROR! unable to insert gene name for [%s], value already present ", e));
//                }
//
//            }));
//            executorService.shutdown();
//            executorService.awaitTermination(10, TimeUnit.DAYS);
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File("internalDB.obj")));
            oos.writeObject(map);
            oos.close();
            internalDB = map;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        new MainEntry();
    }
}
