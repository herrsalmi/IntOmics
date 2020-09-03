package org.pmoi.business;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.database.GeneMapper;
import org.pmoi.util.HttpConnector;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ReactomeClient {

    private static final Logger LOGGER = LogManager.getRootLogger();

    private final HttpConnector connector;

    public ReactomeClient() {
        connector = new HttpConnector();
    }

    /**
     * performs a Solr query on the Reactome knowledge base
     *
     * @param query Search term (gene symbol in this case)
     * @return list of IDs representing pathways
     */
    public List<String> search(String query) {
        try {
            URL url = new URL("https://reactome.org/ContentService/search/query?query=" + query +
                    "&species=Homo%20sapiens&types=Pathway&cluster=true");
            String result = connector.getContent(url);
            JsonObject jsonObject = JsonParser.parseString(result).getAsJsonObject();
            var entries = jsonObject.getAsJsonArray("results").get(0).getAsJsonObject().get("entries").getAsJsonArray();
            List<String> idList = new ArrayList<>();
            for (var entry : entries) {
                idList.add(entry.getAsJsonObject().get("id").getAsString());
            }
            return idList;
        } catch (FileNotFoundException e) {
            LOGGER.warn("Term [{}] not found", query);
            return Collections.emptyList();
        } catch (IOException e) {
            LOGGER.error(e);
        }
        return Collections.emptyList();
    }

    /**
     * query the Reactome knowledge base for the display name of a given entity
     *
     * @param id entity ID
     * @return display name
     */
    public Optional<String> getEntityName(String id) {
        try {
            URL url = new URL("https://reactome.org/ContentService/data/query/" + id + "/displayName");
            var name = connector.getContent(url);
            return Optional.of(name);
        } catch (FileNotFoundException e) {
            LOGGER.warn("Id [{}] not found", id);
            return Optional.empty();
        } catch (IOException e) {
            LOGGER.error(e);
        }
        return Optional.empty();
    }

    public List<String> getParticipants(String id) {
        try {
            URL url = new URL("https://reactome.org/ContentService/data/participants/" + id + "/participatingPhysicalEntities");
            String result = connector.getContent(url);
            var objectArray = JsonParser.parseString(result).getAsJsonArray();
            var stream = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(objectArray.iterator(), Spliterator.ORDERED),
                    false);
            GeneMapper geneMapper = GeneMapper.getInstance();
            return stream.map(JsonElement::getAsJsonObject).filter(e -> e.get("className").getAsString().equals("Protein"))
                    .map(e -> {
                        String name = e.get("displayName").getAsString().split(" ")[0];
                        if (geneMapper.getSymbolFromAlias(name).isPresent()){
                            return name;
                        } else {
                            for (var n : e.get("name").getAsJsonArray()){
                                var gene = geneMapper.getSymbolFromAlias(n.getAsString(), name);
                                if (gene.isPresent()) {
                                    return gene.get();
                                }
                            }
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        } catch (FileNotFoundException e) {
            LOGGER.warn("Id [{}] not found", id);
            return Collections.emptyList();
        } catch (IOException e) {
            LOGGER.error(e);
        }

        return Collections.emptyList();
    }
}
