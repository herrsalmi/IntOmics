package org.pmoi.business.ppi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.Args;
import org.pmoi.database.GeneMapper;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CachedInteractionQueryClient implements InteractionQueryClient{

    private static final Logger LOGGER = LogManager.getRootLogger();

    private static CachedInteractionQueryClient instance;

    private Map<String, Map<String, Double>> internalDB;

    private CachedInteractionQueryClient() {
        LOGGER.debug("Loading PPI database");
        try {
            load();
        } catch (IOException e) {
            LOGGER.error(e);
        }
        LOGGER.debug("PPI database loaded");
    }

    private void load() throws IOException{
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(
                getClass().getClassLoader().getResource("ppi.obj").toURI())))){
            this.internalDB = (Map<String, Map<String, Double>>) ois.readObject();
        } catch (ClassNotFoundException | URISyntaxException e) {
            LOGGER.error(e);
        }
    }

    @Override
    public Map<String, String> getProteinNetwork(String symbol) {
        if (!internalDB.containsKey(symbol))
            return GeneMapper.getInstance().getAliases(symbol)
                    .stream()
                    .map(this::getNetworkHelper)
                    .filter(e -> !e.isEmpty())
                    .findAny()
                    .orElse(Collections.emptyMap());

        return getNetworkHelper(symbol);
    }

    public Map<String, String> getNetworkHelper(String symbol) {
        return internalDB.getOrDefault(symbol, Collections.emptyMap())
                .entrySet().stream()
                .filter(e -> e.getValue() >= Args.getInstance().getStringDBScore() / 1000.)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
    }

    public void createCache() {
        try (Stream<String> stream = Files.lines(Path.of("PPI.txt"))) {
            internalDB = stream.map(l -> l.split(" "))
                    .collect(Collectors.toMap(l -> l[0], l -> new HashMap<>(Map.of(l[1], Double.parseDouble(l[2]) / 1000)), (m1, m2) -> {
                        m1.putAll(m2);
                        return m1;
                    }));
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("ppi.obj"));
            oos.writeObject(internalDB);
            oos.close();
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
            System.exit(1);
        }
    }

    public static synchronized CachedInteractionQueryClient getInstance() {
        if (instance == null)
            instance = new CachedInteractionQueryClient();
        return instance;
    }
}
