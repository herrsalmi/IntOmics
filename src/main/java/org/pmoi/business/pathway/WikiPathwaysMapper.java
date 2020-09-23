package org.pmoi.business.pathway;

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
import org.pmoi.util.PathwayResponceHandler;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WikiPathwaysMapper implements PathwayMapper{

    private static final Logger LOGGER = LogManager.getRootLogger();

    private Map<String, Set<String>> pathwayDB;
    private static final String DB_WP_OBJ = "pathwayDB_WP.obj";
    private static final String DB_PATH = "sets/";
    private int initialSize = 0;

    WikiPathwaysMapper() {
        LOGGER.debug("Loadling WikiPathways DB");
        try {
            if (Args.getInstance().useOnlineDB() && Args.getInstance().ignoreCheck()) {
                pathwayDB = new HashMap<>(400);
                initWikiPathways();
            } else {
                init();
            }
        } catch (IOException | URISyntaxException e) {
            LOGGER.error(e);
            initWikiPathways();
        }
    }

    @Override
    public List<Pathway> getPathways(String gene) {
        return pathwayDB.entrySet().parallelStream().filter(e -> e.getValue().contains(gene))
                .map(e -> new Pathway(e.getKey(), e.getValue().stream().map(g -> new Gene(g, ""))
                        .collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isInAnyPathway(String gene) {
        return pathwayDB.values().parallelStream().anyMatch(e -> e.contains(gene));
    }

    private void init() throws IOException, URISyntaxException {
        LOGGER.debug("Reading file {}", DB_WP_OBJ);
        FileInputStream file;
        // check if there is an updated version in sets folder
        if (Files.exists(Path.of(DB_PATH + DB_WP_OBJ), LinkOption.NOFOLLOW_LINKS)) {
            LOGGER.debug("Newer version of {} found if sets folder", DB_WP_OBJ);
            file = new FileInputStream(new File(DB_PATH + DB_WP_OBJ));
        } else {
            // if the file is in resource folder this shouldn't fail. If it does fail the caller method will take care of it
            file = new FileInputStream(new File(getClass().getClassLoader()
                    .getResource(DB_WP_OBJ).toURI()));
        }
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
            initWikiPathways();
        }
    }

    /**
     * Load pathways from WikiPathways into internal DB
     */
    public void initWikiPathways() {
        LOGGER.info("Fetching WikiPathways entries ...");
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
            this.initialSize = result.size();
            var pathways = result.parallelStream()
                    .map(e -> {
                        try {
                            var urlP = new URL(String.format("http://webservice.wikipathways.org/getPathwayAs?" +
                                    "fileType=gpml&pwId=%s&revision=0", e.getId()));
                            var saxBuilder = new SAXBuilder();
                            var document = saxBuilder.build(urlP);
                            var genes = gpmlBase64Decoder(document.getRootElement().getChildren().stream()
                                    .findFirst()
                                    .orElseThrow().getText());
                            return new Pathway(e.getName(), genes);
                        } catch (JDOMException | IOException ex) {
                            LOGGER.error(ex);
                        }
                        return null;
                    })
                    .collect(Collectors.toList());
            // Check if the pathway isn't already present under another form or a sub name
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
        LOGGER.debug("Writing updated version to sets folder");
        try {
            Files.createDirectory(Path.of(DB_PATH));
        } catch (IOException e) {
            LOGGER.error("Can't create directory '{}'!", DB_PATH);
            return;
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(DB_PATH + DB_WP_OBJ)))) {
            oos.writeObject(pathwayDB);
            oos.writeInt(initialSize);
        } catch (IOException e) {
            LOGGER.error(e);
        }
    }

    /**
     * Decodes a base64 text to a gpml document and extracts a list of genes
     * @param message base64 encoded text
     * @return list of genes
     */
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

    /**
     * Extracts gene names from a gpml file
     * @param gpml gpml document
     * @return set containing gene names
     * @throws JDOMException something wrong happened
     * @throws IOException definitely something wrong happened
     */
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

}
