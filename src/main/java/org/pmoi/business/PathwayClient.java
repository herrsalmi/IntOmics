package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.pmoi.Args;
import org.pmoi.model.Feature;
import org.pmoi.model.Gene;
import org.pmoi.model.Pathway;
import org.pmoi.model.PathwayResponse;
import org.pmoi.util.HttpConnector;
import org.pmoi.util.PathwayResponceHandler;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PathwayClient {
    private static PathwayClient instance;
    private static String INTERNAL_DB_NAME;
    private static final Logger LOGGER = LogManager.getRootLogger();

    private Map<String, Set<String>> pathwayDB;
    private int initialSize = 0;

    private PathwayClient() {
        LOGGER.debug("Loadling pathways DB");
        pathwayDB = new ConcurrentHashMap<>();
        try {
            initDB();
        } catch (IOException | URISyntaxException | NullPointerException e) {
            LOGGER.error(e);
            switch (Args.getInstance().getPathwayDB()) {
                case KEGG -> initKEGGPathways();
                case WIKIPATHWAYS -> initWikiPathways();
            }
        }
        LOGGER.debug("Pathways DB loaded");
    }

    public synchronized static PathwayClient getInstance() {
        if (instance == null) {
            INTERNAL_DB_NAME = switch (Args.getInstance().getPathwayDB()) {
                case KEGG -> "pathwayDB_KEGG.obj";
                case WIKIPATHWAYS -> "pathwayDB_WP.obj";
            };
            instance = new PathwayClient();
        }
        return instance;
    }

    private void initDB() throws IOException, URISyntaxException, NullPointerException {
        //TODO remove this block for production
        if (false) {
            switch (Args.getInstance().getPathwayDB()) {
                case KEGG -> initKEGGPathways();
                case WIKIPATHWAYS -> initWikiPathways();
            }
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(INTERNAL_DB_NAME)))) {
                oos.writeObject(pathwayDB);
                oos.writeInt(initialSize);
            }
            return;
        }
        LOGGER.debug("Reading file {}", INTERNAL_DB_NAME);
        var file = new FileInputStream(new File(getClass().getClassLoader()
                .getResource(INTERNAL_DB_NAME).toURI()));
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
            switch (Args.getInstance().getPathwayDB()) {
                case KEGG -> initKEGGPathways();
                case WIKIPATHWAYS -> initWikiPathways();
            }
        }

    }

    /**
     * Load pathways from WikiPathways into internal DB
     */
    public void initWikiPathways() {
        LOGGER.debug("Fetching WikiPathways entries");
        int counter = 0;
        try {
            String url = "http://webservice.wikipathways.org/listPathways?organism=Homo%20sapiens";
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            saxParser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            PathwayResponceHandler pathwayResponceHandler = new PathwayResponceHandler();
            saxParser.parse(url, pathwayResponceHandler);
            List<PathwayResponse> result = pathwayResponceHandler.getPathwayResponses();
            if (result.isEmpty()) {
                LOGGER.error("Unable to get WikiPathways results");
                System.exit(1);
            }
            // in case the user used -i option, check if there are new pathways (only comparing the number)
            if (this.initialSize == result.size()) {
                LOGGER.warn("No new pathways found. Using cached database" );
                return;
            }
            pathwayDB.clear();
            AtomicInteger count = new AtomicInteger(1);
//            ExecutorService executor = Executors.newFixedThreadPool(1);
//            var tasks = result.stream().map(e -> executor.submit(() -> {
//                System.out.print("\rGetting pathway " + count.getAndIncrement());
//                try {
//                    var urlP = new URL(String.format("http://webservice.wikipathways.org/getPathwayAs?" +
//                            "fileType=gpml&pwId=%s&revision=0", e.getId()));
//                    var saxBuilder = new SAXBuilder();
//                    var document = saxBuilder.build(urlP);
//                    var genes = gpmlBase64Decoder(document.getRootElement().getChildren().stream().findFirst()
//                            .get().getText());
//                    return new Pathway(e.getName(), genes);
//                } catch (JDOMException | IOException ex) {
//                    LOGGER.error(ex);
//                }
//                return null;
//            })).collect(Collectors.toList());
//            executor.shutdown();
//
//            var pathways = tasks.stream().map(e -> {
//                try {
//                    return e.get();
//                } catch (InterruptedException | ExecutionException ex) {
//                    ex.printStackTrace();
//                }
//                return null;
//            }).collect(Collectors.toList());
            this.initialSize = result.size();
            var pathways = result.parallelStream()
                    .map(e -> {
                        try {
                            var urlP = new URL(String.format("http://webservice.wikipathways.org/getPathwayAs?" +
                                    "fileType=gpml&pwId=%s&revision=0", e.getId()));
                            var saxBuilder = new SAXBuilder();
                            var document = saxBuilder.build(urlP);
                            var genes = gpmlBase64Decoder(document.getRootElement().getChildren().stream().findFirst()
                                    .get().getText());
                            return new Pathway(e.getName(), genes);
                        } catch (JDOMException | IOException ex) {
                            LOGGER.error(ex);
                        }
                        return null;
                    })
                    .collect(Collectors.toList());
            // Check if the pathway isn't already present under another form or a subname
            pathways.forEach(p -> {
                if (!pathwayDB.containsKey(p.getName().replace('/', '-')) &&
                        pathwayDB.keySet().stream().filter(e -> p.getName().contains(e)).findAny().isEmpty())
                    pathwayDB.put(p.getName(), p.getGenes().stream().map(Feature::getName).collect(Collectors.toSet()));
                else if (pathwayDB.containsKey(p.getName().replace('/', '-')))
                    p.setName(p.getName().replace('/', '-'));
                else
                    p.setName(pathwayDB.keySet().stream().filter(e -> p.getName().contains(e)).findAny().orElse(p.getName()));
            });

        } catch (IOException e) {
            LOGGER.error("Error getting pathways from WikiPathways. Aborting!");
            System.exit(1);
        } catch (SAXException | ParserConfigurationException e) {
            LOGGER.error(e);
        }
        LOGGER.debug("Internal DB initialized with {} pathways", pathwayDB.size());
    }

    public List<Pathway> getPathways(String gene) {
        return pathwayDB.entrySet().parallelStream().filter(e -> e.getValue().contains(gene))
                .map(e -> new Pathway(e.getKey(), e.getValue().stream().map(g -> new Gene(g, ""))
                        .collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    private List<Gene> gpmlBase64Decoder(String message) {
        try {
            return gpmlToGeneNames(new String(Base64.getDecoder().decode(message))).stream()
                    .map(e -> new Gene(e, ""))
                    .collect(Collectors.toList());
        } catch (JDOMException | IOException e) {
            LOGGER.error(e);
        }
        return Collections.emptyList();
    }

    private Set<String> gpmlToGeneNames(String gpml) throws JDOMException, IOException {
        Pattern enzymPatter = Pattern.compile("(?:\\d*\\.){3}\\d+");
        Pattern namePattern = Pattern.compile("(^[\\w-]+)");
        String attribute = "TextLabel";
        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        Document document = saxBuilder.build(new StringReader(gpml));
        var childrenList = document.getRootElement().getChildren().stream()
                .filter(c -> c.getName().equals("DataNode")).collect(Collectors.toList());
        childrenList = childrenList.stream().filter(c -> c.getAttribute("Type") != null)
                .filter(c -> c.getAttribute("Type").getValue().equals("Protein") ||
                        c.getAttribute("Type").getValue().equals("GeneProduct"))
                .filter(c -> !c.getAttribute(attribute).getValue().equals(" "))
                .filter(c -> {
                    var matcher = enzymPatter.matcher(c.getAttribute(attribute).getValue());
                    return !matcher.find();
                })
                .collect(Collectors.toList());
        return childrenList.stream().map(c -> c.getAttribute(attribute).getValue())
                .map(c -> c.contains("_") ? c.split("_")[0] : c)
                .map(c -> {
                    Matcher m = namePattern.matcher(c);
                    return m.find() ? m.group(1) : c;
                })
                .map(c -> c.startsWith("p-") ? c.substring(c.indexOf('-') + 1) : c)
                .map(c -> c.startsWith("Y-") ? c.substring(c.indexOf('-') + 1) : c)
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    /**
     * KEGG pathway search using gene id
     *
     * @param geneId Ensembl gene ID
     * @return a list of entries in string format representing {pathway ID; pathway name}
     */
    public List<String> KEGGSearch(String geneId) {
        URL url = null;
        try {
            url = new URL(String.format("http://rest.kegg.jp/get/hsa:%s/", geneId));
        } catch (MalformedURLException e) {
            LOGGER.error(e);
        }
        return getListResults(url, " +(hsa[0-9]+ .+)(?=\\n)");
    }

    /**
     * Returns a list of genes involved in a pathway
     *
     * @param pathwayID KEGG pathway ID
     * @return list of genes
     */
    public List<Gene> getKEGGPathwayGenes(String pathwayID) {
        URL url = null;
        try {
            url = new URL(String.format("http://rest.kegg.jp/get/%s/", pathwayID));
        } catch (MalformedURLException e) {
            LOGGER.error(e);
        }
        return getListResults(url, "[0-9]+ {2}(.+)(?=;)").stream()
                .map(e -> new Gene(e, ""))
                .collect(Collectors.toList());
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

    public Set<String> getPathwaysForGene(String gene) {
        return pathwayDB.entrySet().stream().filter(e -> e.getValue().contains(gene))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public int getNumberOfGenesByPathway(String pathway) {
        return pathwayDB.getOrDefault(pathway, Collections.emptySet()).size();
    }

    public boolean isInAnyPathway(String gene) {
        return pathwayDB.values().parallelStream().anyMatch(e -> e.contains(gene));
    }

}
