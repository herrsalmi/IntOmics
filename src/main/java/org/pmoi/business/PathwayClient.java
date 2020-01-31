package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.pmoi.ApplicationParameters;
import org.pmoi.handler.HttpConnector;
import org.pmoi.handler.PathwayResponceHandler;
import org.pmoi.models.Gene;
import org.pmoi.models.Pathway;
import org.pmoi.models.PathwayResponse;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PathwayClient {
    private static final Logger LOGGER = LogManager.getRootLogger();

    private static Map<String, Set<String>> pathwayDB;
    private static long numberOfGenes;

    public PathwayClient() {
        if (pathwayDB == null) {
            synchronized (PathwayClient.class) {
                if (pathwayDB == null) {
                    pathwayDB = new HashMap<>();
                    try {
                        initDB();
                        numberOfGenes = pathwayDB.values().stream()
                                .flatMap(Collection::stream)
                                .distinct()
                                .count();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
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

                return result.stream()
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
            var file = Files.createTempFile(Paths.get("tmp/"), "Hs_", ".gpml");
            BufferedWriter bw = Files.newBufferedWriter(file);
            bw.write(new String(Base64.getDecoder().decode(message)));
            bw.close();
            //gpmlToGeneNames(Paths.get("tmp/WP2799_101385.gpml")).forEach(System.out::println);
            gpmlToGeneNames(file).forEach(System.out::println);
            System.exit(0);
            return gpmlToGeneNames(file).stream()
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
}
