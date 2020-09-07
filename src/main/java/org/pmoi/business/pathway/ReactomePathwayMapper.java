package org.pmoi.business.pathway;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.database.GeneMapper;
import org.pmoi.model.Gene;
import org.pmoi.model.Pathway;
import org.pmoi.util.HttpConnector;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ReactomePathwayMapper implements PathwayMapper{

    private static final Logger LOGGER = LogManager.getRootLogger();

    private final HttpConnector connector;

    public ReactomePathwayMapper() {
        connector = new HttpConnector();
    }

    @Override
    public List<Pathway> getPathways(String gene) {
        return search(gene).stream()
                .map(e -> new Pathway(e, getEntityName(e).orElseGet(() -> ""),
                        getInteractors(e).stream().map(g -> new Gene(e, "")).collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    public void listAll() throws IOException, InterruptedException {
        int pages = (int) Math.ceil(pathwayCount() / 25.);
        List<String> pathwaysID = Collections.synchronizedList(new ArrayList<>());

        Function<Integer, Stream<JsonElement>> mapper = i -> {
            try {
                URL url = new URL(String.format("https://reactome.org/ContentService/data/schema/Pathway?species=9606&page=%d&offset=25", i));
                String content = connector.getContent(url);
                var objectArray = JsonParser.parseString(content).getAsJsonArray();
                return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(objectArray.iterator(), Spliterator.ORDERED),
                        false);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        };

        IntStream.rangeClosed(1, pages).parallel().boxed().flatMap(mapper)
                .map(JsonElement::getAsJsonObject)
                .map(e -> e.get("stId").getAsString())
                .forEach(pathwaysID::add);

        LOGGER.debug("Number of pathway IDs loaded: {}", pathwaysID.size());

//        var genes = pathwaysID.parallelStream()
//                .map(this::getInteractors)
//                .flatMap(Collection::stream)
//                .distinct()
//                .collect(Collectors.toList());

        //////
        AtomicInteger counter = new AtomicInteger(1);
        Set<String> genes = Collections.synchronizedSet(new HashSet<>());
        ExecutorService executor = Executors.newFixedThreadPool(8);
        System.out.println("Starting ...");
        pathwaysID.forEach(e -> executor.submit(() -> {
            genes.addAll(getInteractors(e));
            System.out.printf("\rGetting pathway %d out of 2423", counter.getAndIncrement());
        }));
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.DAYS);
        System.out.println("");
        /////
        LOGGER.debug("Number of distinct genes: {}", genes.size());
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("reactome_genes.obj"));
        oos.writeObject(genes);
        oos.close();
    }

    private int pathwayCount() {
        try {
            URL url = new URL("https://reactome.org/ContentService/data/schema/Pathway/count?species=9606");
            String result = connector.getContent(url);
            return Integer.parseInt(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
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
