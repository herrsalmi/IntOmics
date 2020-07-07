package org.pmoi.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class GeneMapper {

    private static final Logger LOGGER = LogManager.getRootLogger();

    private static GeneMapper instance;

    private List<Gene> internalDB;

    private GeneMapper() {
        LOGGER.debug("initializing gene mapper DB\r");
        init();
        LOGGER.debug("initializing gene mapper DB done. size: {}", internalDB.size());
    }

    public static GeneMapper getInstance() {
        if (instance == null) {
            instance = new GeneMapper();
        }
        return instance;
    }

    private void init() {
        try (var stream = Files.lines(Path.of(getClass().getClassLoader().getResource("Homo_sapiens.gene_info").toURI()))) {
            internalDB = stream.skip(1).map(Gene::new).collect(Collectors.toList());
        } catch (URISyntaxException | IOException e) {
            LOGGER.error("Failed to initialize gene_info_db");
            System.exit(1);
        }
    }

    public Optional<String> getSymbol(String id) {
        return internalDB.parallelStream().filter(e -> e.id.equals(id)).findAny().map(e -> e.symbol);
    }

    public Optional<String> getId(String symbol) {
        var result = internalDB.parallelStream().filter(e -> e.symbol.equalsIgnoreCase(symbol)).findAny().map(e -> e.id);
        if (result.isPresent())
            return result;
        return internalDB.parallelStream().filter(e -> e.synonyms.contains(symbol.toUpperCase())).findAny().map(e -> e.id);
    }

    public Optional<String> getDescription(String id) {
        return internalDB.parallelStream().filter(e -> e.id.equals(id)).findAny().map(e -> e.description);
    }

    private static class Gene {
        private final String id;
        private final String symbol;
        private final String description;
        private final List<String> synonyms;

        public Gene(String line) {
            int pos = line.indexOf('\t') + 1;
            int end = line.indexOf('\t', pos);
            this.id = line.substring(pos, end);
            pos = end + 1;
            end = line.indexOf('\t', pos);
            this.symbol = line.substring(pos, end);
            pos = end + 1;
            end = line.indexOf('\t', pos);
            this.synonyms = line.substring(pos, end).equals("-") ? Collections.emptyList() : Arrays.asList(line.substring(pos, end).split("\\|"));
            for (int i = 0; i < 5; i++) {
                pos = end + 1;
                end = line.indexOf('\t', pos);
            }
            this.description = line.substring(pos, end);
        }
    }
}
