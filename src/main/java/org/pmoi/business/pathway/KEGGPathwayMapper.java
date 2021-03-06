package org.pmoi.business.pathway;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.Args;
import org.pmoi.database.SpeciesHelper;
import org.pmoi.database.SupportedSpecies;
import org.pmoi.model.Feature;
import org.pmoi.model.Gene;
import org.pmoi.model.Pathway;
import org.pmoi.util.HttpConnector;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class KEGGPathwayMapper implements PathwayMapper{

    private static final Logger LOGGER = LogManager.getRootLogger();

    private List<Pathway> pathwayDB;
    private static final String DB_KEGG_OBJ = "pathwayDB_KEGG." + Args.getInstance().getSpecies() + ".obj";
    public static final String DB_PATH = "sets/";
    private int initialSize = 0;

    KEGGPathwayMapper() {
        LOGGER.debug("Loading KEGGG pathways DB");
        try {
            if (Args.getInstance().useOnlineDB() && Args.getInstance().ignoreCheck()) {
                pathwayDB = new ArrayList<>(400);
                initKEGGPathways();
            } else {
                init();
            }
        } catch (IOException | URISyntaxException e) {
            LOGGER.debug("Unable to load file {}. Initializing from online service ...", DB_KEGG_OBJ);
            initKEGGPathways();
        }
    }

    @Override
    public List<Pathway> getPathways(String gene) {
        return pathwayDB.parallelStream().filter(e -> e.getGenes().stream().map(Feature::getName).anyMatch(gene::equalsIgnoreCase))
                .collect(Collectors.toList());
    }

    /**
     * Checks if a gene is present in a pathway
     * @param gene gene symbol
     * @return true if the gene is in a pathway
     */
    @Override
    public boolean isInAnyPathway(String gene) {
        return pathwayDB.parallelStream().anyMatch(e -> e.getGenes().stream().map(Feature::getName)
                .anyMatch(gene::equalsIgnoreCase));
    }

    private void init() throws IOException, URISyntaxException {
        LOGGER.debug("Reading file {}", DB_KEGG_OBJ);
        InputStream file;
        // check if there is an updated version in sets folder
        if (Files.exists(Path.of(DB_PATH + DB_KEGG_OBJ), LinkOption.NOFOLLOW_LINKS)) {
            LOGGER.debug("Newer version of {} found in sets folder", DB_KEGG_OBJ);
            file = new FileInputStream(new File(DB_PATH + DB_KEGG_OBJ));
        } else {
            // if the file is in resource folder this shouldn't fail. If it does fail the caller method will take care of it
            var url = getClass().getClassLoader().getResource(DB_KEGG_OBJ);
            if (url == null)
                throw new IOException("File not found");
            file = getClass().getResourceAsStream("/" + DB_KEGG_OBJ);
        }

        try (ObjectInputStream ois = new ObjectInputStream(file)) {
            pathwayDB = (List<Pathway>) ois.readObject();
            initialSize = ois.readInt();
            // check if it's the right species
            SupportedSpecies species = (SupportedSpecies) ois.readObject();
            if (!Args.getInstance().getSpecies().equals(species))
                throw new IOException("Wrong species found in file");
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
        LOGGER.info("Getting KEGG pathways ...");
        var pathways = listKEGG();
        // in case the user used -i option, check if there are new pathways (only comparing the number)
        if (initialSize == pathways.size()) {
            LOGGER.warn("No new pathways found. Using cached database" );
            return;
        }
        if (pathwayDB != null) {
            pathwayDB.clear();
        } else {
            pathwayDB = new ArrayList<>(400);
        }
        this.initialSize = pathways.size();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        pathways.forEach((k, v) -> executor.submit(() -> {
            URL url = null;
            try {
                url = new URL("http://rest.kegg.jp/get/" + k + "/");
            } catch (MalformedURLException e) {
                LOGGER.error(e);
            }
            pathwayDB.add(new Pathway(k, v, getListResults(url)));
        }));
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LOGGER.debug("Internal DB initialized with {} pathways", pathwayDB.size());
        LOGGER.debug("Writing updated version to sets folder");
        if (Files.isDirectory(Path.of(DB_PATH))) {
            LOGGER.warn("Directory '{}' already exists! Writing file ...", DB_PATH);
        } else {
            try {
                Files.createDirectory(Path.of(DB_PATH));
            } catch (IOException e) {
                LOGGER.error("Can't create directory '{}'!", DB_PATH);
                return;
            }
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(DB_PATH + DB_KEGG_OBJ)))) {
            oos.writeObject(pathwayDB);
            oos.writeInt(initialSize);
            oos.writeObject(Args.getInstance().getSpecies());
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    /**
     * Lists all pathways in KEGG
     *
     * @return Map containing [pathways ID, name]
     */
    private Map<String, String> listKEGG() {
        try {
            String keggOrgId = SpeciesHelper.get().getKeggOrgId();
            HttpConnector httpConnector = new HttpConnector();
            String result = httpConnector.getContent(new URL("http://rest.kegg.jp/list/pathway/" + keggOrgId));
            Pattern pattern = Pattern.compile(String.format("path:(%s\\d+)\\t(.+) -", keggOrgId));
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
     * @return list of matching results
     */
    private List<Gene> getListResults(URL url) {
        int counter = 0;
        while (true) {
            try {
                HttpConnector httpConnector = new HttpConnector();
                String result = httpConnector.getContent(url);
                Pattern pattern = Pattern.compile("[0-9]+ {2}(.+)(?=;)");
                Matcher matcher = pattern.matcher(result);
                List<Gene> resultList = new ArrayList<>();
                while (matcher.find()) {
                    resultList.add(new Gene(matcher.group(1).trim(), ""));
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
