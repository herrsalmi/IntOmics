package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.pmoi.ApplicationParameters;
import org.pmoi.handler.HttpConnector;
import org.pmoi.models.Gene;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PathwayClient {
    private static final Logger LOGGER = LogManager.getRootLogger();

    private static Map<String, Set<String>> pathwayDB;

    public PathwayClient() {
        if (pathwayDB == null) {
            synchronized (PathwayClient.class) {
                if (pathwayDB == null) {
                    pathwayDB = new HashMap<>();
                    try {
                        initDB();
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
            Pattern enzymPatter = Pattern.compile("(?:\\d*\\.){3}\\d+");
            Files.list(Paths.get("wikipathways/")).forEach(e -> {
                SAXBuilder saxBuilder = new SAXBuilder();
                try {
                    Document document = saxBuilder.build(e.toFile());
                    var childrenList = document.getRootElement().getChildren().stream()
                            .filter(c -> c.getName().equals("DataNode")).collect(Collectors.toList());
                    childrenList = childrenList.stream().filter(c -> c.getAttribute("Type") != null)
                            .filter(c -> c.getAttribute("Type").getValue().equals("Protein") ||
                                c.getAttribute("Type").getValue().equals("GeneProduct"))
                            .filter(c -> !c.getAttribute("TextLabel").getValue().contains(" "))
                            .filter(c -> {
                                var matcher = enzymPatter.matcher(c.getAttribute("TextLabel").getValue());
                                return !matcher.find();
                            })
                            .collect(Collectors.toList());
                    Matcher matcher = pattern.matcher(e.toString());
                    if (matcher.find())
                        pathwayDB.put(matcher.group(1).replace("_", " "),
                                childrenList.stream().map(c -> c.getAttribute("TextLabel").getValue())
                                        .map(c -> c.contains("_") ? c.split("_")[0] : c)
                                        .collect(Collectors.toSet()));
                } catch (JDOMException | IOException ex) {
                    ex.printStackTrace();
                }
            });
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File("pathwayDB.obj")));){
                oos.writeObject(pathwayDB);
            }
        } else {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File("pathwayDB.obj")));){
                pathwayDB = (Map<String, Set<String>>) ois.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public byte[] getPathway(String protein, String gene) {
        int counter = 0;
        while (true) {
            try {
                URL url = new URL(String.format("http://webservice.wikipathways.org/findPathwaysByText?query=%s AND %s", protein, gene));
                SAXBuilder saxBuilder = new SAXBuilder();
                Document document = saxBuilder.build(url);
                Optional<Element> pathwayResult = document.getRootElement().getChildren().stream().findFirst();
                AtomicReference<String> pathwayId = new AtomicReference<>();
                if (pathwayResult.isPresent()) {
                    pathwayResult.get().getChildren().forEach(c -> {
                        if (c.getName().equals("id")) {
                            pathwayId.set(c.getText());
                        }
                    });
                } else
                    return null;
                url = new URL(String.format("http://webservice.wikipathways.org/getPathwayAs?fileType=svg&pwId=%s&revision=0", pathwayId.get()));
                document = saxBuilder.build(url);
                return DatatypeConverter.parseBase64Binary(document.getRootElement().getChildren().stream().findFirst().get().getText());
            } catch (IOException | JDOMException e) {
                if (++counter == ApplicationParameters.getInstance().getMaxTries()) {
                    LOGGER.error(String.format("Error getting pathway for: [%s - %s]. Aborting!", protein, gene));
                    return null;
                }
            }
        }
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

}
