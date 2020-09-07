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
import java.util.function.Function;
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

    public List<String> getInteractors(String id) {
        String entitiesUrl = "https://reactome.org/ContentService/data/participants/#/participatingPhysicalEntities";
        String subunitsUrl = "https://reactome.org/ContentService/data/complex/#/subunits?excludeStructures=true";

        List<String> results = Collections.synchronizedList(getParticipants(id, entitiesUrl, ClassType.PROTEIN));

        var complexes = getParticipants(id, entitiesUrl, ClassType.COMPLEX);
        complexes.parallelStream().forEach(e -> results.addAll(getParticipants(e, subunitsUrl, ClassType.PROTEIN)));

        return results.stream().distinct().collect(Collectors.toList());
    }

    /**
     * lookup list of participants in an entity
     * @param id entity id
     * @return list of gene names
     */
    private List<String> getParticipants(String id, String urlTemplate, ClassType type) {
        Function<JsonObject, String> mapperFunction = switch (type) {
            case PROTEIN -> e -> {
                GeneMapper geneMapper = GeneMapper.getInstance();
                assert e != null;
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
            };
            case COMPLEX -> e -> {
                assert e != null;
                return e.get("stId").getAsString();
            };
        };
        try {
            URL url = new URL(urlTemplate.replace("#", id));
            String result = connector.getContent(url);
            var objectArray = JsonParser.parseString(result).getAsJsonArray();
            var stream = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(objectArray.iterator(), Spliterator.ORDERED),
                    false);

            return stream.map(JsonElement::getAsJsonObject).filter(e -> e.get("className").getAsString().equals(type.getName()))
                    .map(mapperFunction)
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

    private enum ClassType {
        PROTEIN("Protein"),
        COMPLEX("Complex");

        private final String name;

        ClassType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
