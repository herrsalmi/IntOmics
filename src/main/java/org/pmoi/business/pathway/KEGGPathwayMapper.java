package org.pmoi.business.pathway;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.Args;
import org.pmoi.model.Gene;
import org.pmoi.model.Pathway;
import org.pmoi.util.HttpConnector;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KEGGPathwayMapper implements PathwayMapper{

    private static final Logger LOGGER = LogManager.getRootLogger();

    private Map<String, Set<String>> pathwayDB;
    private final String internal_db_name = "pathwayDB_KEGG.obj";
    private int initialSize = 0;

    KEGGPathwayMapper() {
        LOGGER.debug("Loadling KEGGG pathways DB");
        try {
            init();
        } catch (IOException | URISyntaxException e) {
            LOGGER.error(e);
            initKEGGPathways();
        }
    }

    @Override
    public List<Pathway> getPathways(String gene) {
        return pathwayDB.entrySet().parallelStream().filter(e -> e.getValue().contains(gene))
                .map(e -> new Pathway(e.getKey(), e.getValue().stream().map(g -> new Gene(g, ""))
                        .collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    /**
     * Checks if a gene is present in a pathway
     * @param gene gene symbol
     * @return true if the gene is in a pathway
     */
    @Override
    public boolean isInAnyPathway(String gene) {
        return pathwayDB.values().parallelStream().anyMatch(e -> e.contains(gene));
    }

    private void init() throws IOException, URISyntaxException, NullPointerException {
        LOGGER.debug("Reading file {}", internal_db_name);
        // if the file is in resource folder this shouldn't fail. If it does fail the caller method will take care of it
        var file = new FileInputStream(new File(getClass().getClassLoader()
                .getResource(internal_db_name).toURI()));
        try (ObjectInputStream ois = new ObjectInputStream(file)) {
            pathwayDB = (Map<String, Set<String>>) ois.readObject();
            initialSize = ois.readInt();
            LOGGER.debug("Object loaded into memory. Number of pathways: {}. Initial size = {}", pathwayDB.size(), initialSize);
        } catch (ClassNotFoundException e) {
            LOGGER.error(e);
        } catch (EOFException e) {
            // do nothing
        }
        if (Args.getInstance().useOnlineDB()) {
            initKEGGPathways();
        }
    }

    /**
     * Loads all KEGG pathways into internal DB
     */
    private void initKEGGPathways() {
        LOGGER.debug("Fetching KEGG pathways");
        var pathways = listKEGG();
        // in case the user used -i option, check if there are new pathways (only comparing the number)
        if (initialSize == pathways.size()) {
            LOGGER.warn("No new pathways found. Using cached database" );
            return;
        }
        pathwayDB.clear();
        this.initialSize = pathways.size();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        pathways.forEach((k, v) -> executor.submit(() -> {
            URL url = null;
            try {
                url = new URL("http://rest.kegg.jp/get/" + k + "/");
            } catch (MalformedURLException e) {
                LOGGER.error(e);
            }
            pathwayDB.put(v, new HashSet<>(getListResults(url, "[0-9]+ {2}(.+)(?=;)")));
        }));
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LOGGER.debug("Internal DB initialized with {} pathways", pathwayDB.size());
    }

    /**
     * Lists all pathways in KEGG
     *
     * @return Map containing [pathways ID, name]
     */
    private Map<String, String> listKEGG() {
        try {
            HttpConnector httpConnector = new HttpConnector();
            String result = httpConnector.getContent(new URL("http://rest.kegg.jp/list/pathway/hsa"));
            Pattern pattern = Pattern.compile("path:(hsa\\d+)\\t(.+) -");
            Matcher matcher = pattern.matcher(result);
            Map<String, String> resultMap = new HashMap<>();
            while (matcher.find()) {
                resultMap.put(matcher.group(1).trim(), matcher.group(2).trim());
            }
            return resultMap;
        } catch (IOException e) {
            LOGGER.error("Error getting KEGG pathways. Aborting!");
            System.exit(1);
        }
        return null;
    }
    /**
     * KEGG helper function that returns a list of results matching a regex
     * @param url request
     * @param expression regex matching the desired result
     * @return list of matching results
     */
    private List<String> getListResults(URL url, String expression) {
        int counter = 0;
        while (true) {
            try {
                HttpConnector httpConnector = new HttpConnector();
                String result = httpConnector.getContent(url);
                Pattern pattern = Pattern.compile(expression);
                Matcher matcher = pattern.matcher(result);
                List<String> resultList = new ArrayList<>();
                while (matcher.find()) {
                    resultList.add(matcher.group(1).trim());
                }
                return resultList;
            } catch (IOException e) {
                if (++counter == HttpConnector.MAX_TRIES) {
                    LOGGER.error(String.format("Error getting KEGG results. URL: [%s]. Aborting!", url));
                    return Collections.emptyList();
                }
            }
        }
    }

}
