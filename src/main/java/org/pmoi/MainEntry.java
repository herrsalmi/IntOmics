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

    private static final String VERSION = "0.1b";
    public static final boolean USE_GENE_NAME = true;
    private static final String PROG_NAME = "IntOmics";
    public static final int MAX_TRIES = 100;
    public static final int STRINGDB_SCORE = 900;
    public static final double FC = 1.3;
    public static final String NCBI_API_KEY = "40065544fb6667a5a723b649063fbe596e08";

    public static BiMap<String, String> internalDB;

    public MainEntry() {
        loadInternalDB();
        //System.exit(0);
        OperationDispatcher operationDispatcher = new OperationDispatcher();
        operationDispatcher.run("interactionNetworkS2M_LF_GO_900.tsv", ProteomeType.LABEL_FREE, SecretomeMappingMode.GOTERM);
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
            var geneIDs = Files.lines(Paths.get("gene2go"))
                    .skip(1)
                    .map(e -> e.split("\t")[1])
                    .collect(Collectors.toSet());
            BiMap<String, String> map = Maps.synchronizedBiMap(HashBiMap.create(200000));

            NCBIQueryClient ncbiQueryClient = new NCBIQueryClient();
            ExecutorService executorService = Executors.newFixedThreadPool(4);
            AtomicInteger counter = new AtomicInteger(1);
            geneIDs.forEach(e -> executorService.submit(() -> {
                System.out.print(String.format("\rProcessing %d out of %d", counter.getAndIncrement(), geneIDs.size()));
                try {
                    map.put(e, ncbiQueryClient.entrezIDToGeneName(e));
                } catch (IllegalArgumentException ex) {
                    System.out.println(String.format("\nERROR! unable to insert gene name for [%s], value already present", e));
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
