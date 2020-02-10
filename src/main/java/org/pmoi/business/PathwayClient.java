package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.pmoi.ApplicationParameters;
import org.pmoi.models.Feature;
import org.pmoi.models.Gene;
import org.pmoi.models.Pathway;
import org.pmoi.models.PathwayResponse;
import org.pmoi.utils.HttpConnector;
import org.pmoi.utils.PathwayResponceHandler;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PathwayClient {
    private static PathwayClient instance;

    private static final Logger LOGGER = LogManager.getRootLogger();

    private Map<String, Set<String>> pathwayDB;
    private static AtomicBoolean changed;
    private static long numberOfGenes;

    private PathwayClient() {
        pathwayDB = new HashMap<>();
        try {
            initDB();
            updateGeneCount();
            pathwayDB = Collections.synchronizedMap(pathwayDB);
            changed = new AtomicBoolean(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PathwayClient getInstance() {
        if (instance == null) {
            synchronized (PathwayClient.class) {
                if (instance == null) {
                   instance = new PathwayClient();
                }
            }
        }
        return instance;
    }

    private void updateGeneCount() {
        numberOfGenes = pathwayDB.values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .count();
    }

    private void initDB() throws IOException {
        if (!Files.exists(Paths.get("pathwayDB.obj"))) {
            Pattern pattern = Pattern.compile("Hs_(.+)(?=_WP)");
            Files.list(Paths.get("wikipathways/")).forEach(e -> {
                try {
                    Matcher matcher = pattern.matcher(e.toString());
                    if (matcher.find())
                        pathwayDB.put(matcher.group(1).replace("_", " "),
                                gpmlToGeneNames(e));
                } catch (JDOMException | IOException ex) {
                    ex.printStackTrace();
                }
            });
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File("pathwayDB.obj")))){
                oos.writeObject(pathwayDB);
            }
        } else {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File("pathwayDB.obj")))){
                pathwayDB = (Map<String, Set<String>>) ois.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public List<Pathway> getPathways(String gene) {
        int counter = 0;
        while (true) {
            try {
                String url = String.format("http://webservice.wikipathways.org/findPathwaysByText?species=Homo sapiens&query=%s", gene);
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser = factory.newSAXParser();
                PathwayResponceHandler pathwayResponceHandler = new PathwayResponceHandler();
                saxParser.parse(url, pathwayResponceHandler);
                List<PathwayResponse> result = pathwayResponceHandler.getPathwayResponses();
                assert result != null;
                if (result.isEmpty())
                    return null;
                // compare the number of pathways to the ones on the local database
                long pathwayCount = pathwayDB.values().stream().filter(l -> l.contains(gene))
                        .count();
                //System.out.println(String.format("Gene: %s. Result size: %d; DB size:%d", gene, result.size(), pathwayCount));
                if (result.size() > pathwayCount) {
                    var pathways = result.stream()
                            .map(e -> {
                                try {
                                    var urlP = new URL(String.format("http://webservice.wikipathways.org/getPathwayAs?fileType=gpml&pwId=%s&revision=0", e.getId()));
                                    var saxBuilder = new SAXBuilder();
                                    var document = saxBuilder.build(urlP);
                                    // decode base64 gpml and get list of genes
                                    var genes = gpmlBase64Decoder(document.getRootElement().getChildren().stream().findFirst().get().getText());
                                    return new Pathway(e.getName(), genes);
                                } catch (JDOMException | IOException ex) {
                                    ex.printStackTrace();
                                }
                                return null;
                            })
                            .collect(Collectors.toList());
                    //TODO this call is probably not thread safe
                    // Check if the pathway isn't already present under another form or a subname
                    pathways.forEach(p -> {
                        if (!pathwayDB.containsKey(p.getName().replace('/', '-')) &&
                                pathwayDB.keySet().stream().filter(e -> p.getName().contains(e)).findAny().isEmpty())
                            pathwayDB.put(p.getName(), p.getGenes().stream().map(Feature::getName).collect(Collectors.toSet()));
                        else if(pathwayDB.containsKey(p.getName().replace('/', '-')))
                            p.setName(p.getName().replace('/', '-'));
                        else
                            p.setName(pathwayDB.keySet().stream().filter(e -> p.getName().contains(e)).findAny().orElse(p.getName()));
                    });
                    changed.set(true);
                    updateGeneCount();
                    return pathways;
                } else {
                    return pathwayDB.entrySet().stream().filter(e -> e.getValue().contains(gene))
                            .map(e -> new Pathway(e.getKey(), e.getValue().stream().map(g -> new Gene(g, "")).collect(Collectors.toList())))
                            .collect(Collectors.toList());
                }

            } catch (IOException e) {
                if (++counter == ApplicationParameters.getInstance().getMaxTries()) {
                    LOGGER.error(String.format("Error getting pathway for: [%s]. Aborting!", gene));
                    return null;
                }
            } catch (SAXException | ParserConfigurationException e) {
                e.printStackTrace();
            }
        }
    }

    private List<Gene> gpmlBase64Decoder(String message) {
        try {
            var file = File.createTempFile("Hs_", ".gpml", Paths.get("tmp/").toFile());
            file.deleteOnExit();
            BufferedWriter bw = Files.newBufferedWriter(file.toPath());
            bw.write(new String(Base64.getDecoder().decode(message)));
            bw.close();
            return gpmlToGeneNames(file.toPath()).stream()
                    .map(e -> new Gene(e, ""))
                    .collect(Collectors.toList());
        } catch (JDOMException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Set<String> gpmlToGeneNames(Path path) throws JDOMException, IOException {
        Pattern enzymPatter = Pattern.compile("(?:\\d*\\.){3}\\d+");
        Pattern namePattern = Pattern.compile("(^[\\w-]+)");
        SAXBuilder saxBuilder = new SAXBuilder();
        Document document = saxBuilder.build(path.toFile());
        var childrenList = document.getRootElement().getChildren().stream()
                .filter(c -> c.getName().equals("DataNode")).collect(Collectors.toList());
        childrenList = childrenList.stream().filter(c -> c.getAttribute("Type") != null)
                .filter(c -> c.getAttribute("Type").getValue().equals("Protein") ||
                        c.getAttribute("Type").getValue().equals("GeneProduct"))
                .filter(c -> !c.getAttribute("TextLabel").getValue().equals(" "))
                .filter(c -> {
                    var matcher = enzymPatter.matcher(c.getAttribute("TextLabel").getValue());
                    return !matcher.find();
                })
                .collect(Collectors.toList());
        return childrenList.stream().map(c -> c.getAttribute("TextLabel").getValue())
                .map(c -> c.contains("_") ? c.split("_")[0] : c)
                .map(c -> {
                    Matcher m = namePattern.matcher(c);
                    return m.find() ? m.group(1) : c;
                })
                .map(c -> c.startsWith("p-") ? c.substring(c.indexOf("-") + 1) : c)
                .map(c -> c.startsWith("Y-") ? c.substring(c.indexOf("-") + 1) : c)
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    public List<String> KEGGSearch(String geneId) {
        URL url = null;
        try {
            url = new URL(String.format("http://rest.kegg.jp/get/hsa:%s/", geneId));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return getListResults(url, " +(hsa[0-9]+ .+)(?=\\n)");
    }

    public List<Gene> getKEGGPathwayGenes(String pathwayID) {
        URL url = null;
        try {
            url = new URL(String.format("http://rest.kegg.jp/get/%s/", pathwayID));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return Objects.requireNonNull(getListResults(url, "[0-9]+ {2}(.+)(?=;)")).stream()
                .map(e -> new Gene(e, ""))
                .collect(Collectors.toList());
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
                if (++counter == ApplicationParameters.getInstance().getMaxTries()) {
                    LOGGER.error(String.format("Error getting KEGG. URL: [%s]. Aborting!", url.getPath()));
                    return null;
                }
            }
        }
    }

    public Set<String> getIntercatorsFromPathway(String gene) {
        return pathwayDB.values().stream().filter(l -> l.contains(gene))
                .flatMap(Collection::stream)
                .filter(e -> !e.isEmpty() && ! e.isBlank())
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    public Set<String> getPathwaysForGene(String gene) {
        return pathwayDB.entrySet().stream().filter(e -> e.getValue().contains(gene))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public List<Gene> getWikipathwayGenes(String pathway) {
        return pathwayDB.get(pathway).stream()
                .map(e -> new Gene(e, ""))
                .collect(Collectors.toList());
    }

    public long getNumberOfGenes() {
        return numberOfGenes;
    }

    public int getNumberOfGenesByPathway(String pathway) {
        return pathwayDB.get(pathway).size();
    }

    public List<Gene> getGenesFromPathway(String pathway) {
        return pathwayDB.get(pathway).stream().map(e -> new Gene(e, "")).collect(Collectors.toList());
    }

    public boolean isInAnyPathway(String gene) {
        return pathwayDB.values().stream()
                .anyMatch(e -> e.contains(gene));
    }

    public synchronized void close() {
        //System.out.println(String.format("%d - %d", initialSize, pathwayDB.size()));
        if (changed.get()) {
            try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File("pathwayDB.obj")))) {
                LOGGER.info("Updating pathways database ...");
                oos.writeObject(pathwayDB);
            } catch (IOException e) {
                LOGGER.error("Unable to save pathways internal database!");
            }
        }
    }
}
