package org.pmoi.business.pathway;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.Args;
import org.pmoi.database.GeneMapper;
import org.pmoi.database.Species;
import org.pmoi.database.SpeciesHelper;
import org.pmoi.model.Gene;
import org.pmoi.model.Pathway;
import org.pmoi.util.HttpConnector;

import java.io.*;
import java.net.URISyntaxException;
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
    private Set<String> geneSet;



    ReactomePathwayMapper() {
        connector = new HttpConnector();
        try {
            init();
        } catch (URISyntaxException | IOException e) {
            LOGGER.error(e);
        }
    }

    private void init() throws URISyntaxException, IOException {
        var file = getClass().getResourceAsStream("/reactome_genes.obj");
        try (ObjectInputStream ois = new ObjectInputStream(file)) {
            geneSet = (Set<String>) ois.readObject();
            LOGGER.debug("Reactome genes loaded into memory. Number of genes: {}", geneSet.size());
        } catch (ClassNotFoundException e) {
            LOGGER.error(e);
        } catch (EOFException e) {
            // do nothing
        }
    }

    @Override
    public List<Pathway> getPathways(String gene) {
        return search(gene).stream()
                .map(e -> new Pathway(e, getEntityName(e).orElse(""),
                        getInteractors(e).stream().map(g -> new Gene(g, "")).collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isInAnyPathway(String gene) {
        return geneSet.contains(gene);
    }

    private void listAll() throws IOException, InterruptedException {
        int pages = (int) Math.ceil(pathwayCount() / 25.);
        List<String> pathwaysID = Collections.synchronizedList(new ArrayList<>());
        Species species = SpeciesHelper.get();
        Function<Integer, Stream<JsonElement>> mapper = i -> {
            try {
                URL url = new URL(String.format("https://reactome.org/ContentService/data/schema/Pathway?species=%d&page=%d&offset=25",
                        species.getTaxonomyId(), i));
                String content = connector.getContent(url);
                var objectArray = JsonParser.parseString(content).getAsJsonArray();
                return StreamSupport.stream(
                        Spliterators.spliteratorUnknownSize(objectArray.iterator(), Spliterator.ORDERED),
                        false);
            } catch (IOException e) {
                LOGGER.error(e);
            }
            return null;
        };

        IntStream.rangeClosed(1, pages).parallel().boxed().flatMap(mapper)
                .map(JsonElement::getAsJsonObject)
                .map(e -> e.get("stId").getAsString())
                .forEach(pathwaysID::add);

        LOGGER.debug("Number of pathway IDs loaded: {}", pathwaysID.size());

        //////
        AtomicInteger counter = new AtomicInteger(1);
        Set<String> genes = Collections.synchronizedSet(new HashSet<>());
        ExecutorService executor = Executors.newFixedThreadPool(Args.getInstance().getThreads());
        LOGGER.debug("Getting genes from Reactome pathways ...");
        pathwaysID.forEach(e -> executor.submit(() -> {
            genes.addAll(getInteractors(e));
            System.out.printf("\rGetting pathway %d out of %d", counter.getAndIncrement(), pathwaysID.size());
        }));
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.DAYS);
        System.out.println();
        /////
        LOGGER.debug("Number of distinct genes: {}", genes.size());
        try(ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("reactome_genes.obj"))) {
            oos.writeObject(genes);
        }
    }

    private int pathwayCount() {
        try {
            Species species = SpeciesHelper.get();
            URL url = new URL("https://reactome.org/ContentService/data/schema/Pathway/count?species=" + species.getTaxonomyId());
            String result = connector.getContent(url);
            return Integer.parseInt(result);
        } catch (IOException e) {
            LOGGER.error(e);
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
            Species species = SpeciesHelper.get();
            // look for an entry of type pathway. if no results are found, look for the protein then
            // find any pathway containing that protein
            URL url = new URL(String.format("https://reactome.org/ContentService/search/query?query=%s" +
                    "&species=%s&types=Pathway&cluster=true", query, species.getName().replace(" ", "%20")));
            String result = connector.getContent(url);
            JsonObject jsonObject = JsonParser.parseString(result).getAsJsonObject();
            var entries = jsonObject.getAsJsonArray("results").get(0).getAsJsonObject().get("entries").getAsJsonArray();
            List<String> idList = new ArrayList<>();
            for (var entry : entries) {
                idList.add(entry.getAsJsonObject().get("id").getAsString());
            }
            return idList;
        } catch (FileNotFoundException e) {
            // no entity of type pathway. looking for proteins
            LOGGER.debug("Term [{}] not found in Reactome with type 'pathway'. Looking for a protein", query);
            return proteinSearch(query);
        } catch (IOException e) {
            LOGGER.error(e);
        }
        return Collections.emptyList();
    }

    private List<String> proteinSearch(String query) {
        try {
            // look for an entry of type pathway. if no results are found, look for the protein then
            // find any pathway containing that protein
            Species species = SpeciesHelper.get();
            URL url = new URL(String.format("https://reactome.org/ContentService/search/query?query=%s" +
                    "&species=%s&types=Protein&cluster=true", query, species.getName().replace(" ", "%20")));
            String result = connector.getContent(url);
            JsonObject jsonObject = JsonParser.parseString(result).getAsJsonObject();
            var entries = jsonObject.getAsJsonArray("results").get(0).getAsJsonObject().get("entries").getAsJsonArray();

            Optional<String> id = Optional.ofNullable(entries.get(0).getAsJsonObject().get("id").getAsString());
            if (id.isEmpty())
                return Collections.emptyList();
            url = new URL("https://reactome.org/ContentService/data/pathways/low/entity/" + id.get() +
                    "?species=" + species.getTaxonomyId());
            result = connector.getContent(url);
            var objectArray = JsonParser.parseString(result).getAsJsonArray();
            var stream = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(objectArray.iterator(), Spliterator.ORDERED),
                    false);
            return stream.map(JsonElement::getAsJsonObject).map(e -> e.get("stId").getAsString()).collect(Collectors.toList());

        } catch (FileNotFoundException e) {
            // no entity of type pathway. looking for proteins
            LOGGER.warn("Term [{}] not found in Reactome", query);

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
            case PROTEIN, TRANSCRIPT -> e -> {
                GeneMapper geneMapper = GeneMapper.getInstance();
                assert e != null;
                String name = e.get("displayName").getAsString().split(" ")[0];
                if (geneMapper.getSymbolFromAlias(name).isPresent()){
                    return name;
                } else {
                    for (var n : e.get("name").getAsJsonArray()){
                        if (n.getAsString().contains("_"))
                            return n.getAsString().substring(0, n.getAsString().indexOf("_"));
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
            List<String> names = switch (type) {
                case PROTEIN, TRANSCRIPT -> List.of(ClassType.PROTEIN.getName(), ClassType.TRANSCRIPT.getName());
                case COMPLEX -> List.of(ClassType.COMPLEX.getName());
            };

            URL url = new URL(urlTemplate.replace("#", id));
            String result = connector.getContent(url);
            var objectArray = JsonParser.parseString(result).getAsJsonArray();
            var stream = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(objectArray.iterator(), Spliterator.ORDERED),
                    false);

            return stream.map(JsonElement::getAsJsonObject).filter(e -> names.contains(e.get("className").getAsString()))
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
        COMPLEX("Complex"),
        TRANSCRIPT("Genes and Transcripts");

        private final String name;

        ClassType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
