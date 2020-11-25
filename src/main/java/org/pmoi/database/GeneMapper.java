package org.pmoi.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.Args;
import org.pmoi.util.GZIPFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility class used to convert between gene symbol and gene ID
 */
public class GeneMapper {

    private static final Logger LOGGER = LogManager.getRootLogger();

    private static GeneMapper instance;

    private List<GeneInfo> internalDB;

    private GeneMapper() {
        LOGGER.debug("initializing gene mapper DB");
        init();
        LOGGER.debug("initializing gene mapper DB done. size: {}", internalDB.size());
    }

    public static synchronized GeneMapper getInstance() {
        if (instance == null) {
            instance = new GeneMapper();
        }
        return instance;
    }

    private void init() {
        if (Args.getInstance().getSpecies().equals(SupportedSpecies.HUMAN)) {
            try (var stream = Files.lines(Path.of(getClass().getClassLoader().getResource("Homo_sapiens.gene_info").toURI()))) {
                // the Gene class used here is the private inner class
                internalDB = stream.dropWhile(l -> l.startsWith("#")).map(GeneInfo::new).collect(Collectors.toList());
            } catch (URISyntaxException | IOException e) {
                LOGGER.error("Failed to initialize gene_info_db");
                System.exit(1);
            }
        } else {
            Species species = SpeciesManager.get();
            try {
                LOGGER.info("Downloading gene info file for {}", species.getName());
                ReadableByteChannel readableByteChannel = Channels.newChannel(species.getUrl().openStream());
                Path tmpFile = Files.createTempFile(null, null);
                tmpFile.toFile().deleteOnExit();
                FileOutputStream fileOutputStream  = new FileOutputStream(tmpFile.toFile());
                FileChannel fileChannel = fileOutputStream.getChannel();
                fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

                try (var stream = GZIPFile.lines(tmpFile)){
                    internalDB = stream.dropWhile(l -> l.startsWith("#")).map(GeneInfo::new).collect(Collectors.toList());
                }
            } catch (IOException e) {
                LOGGER.error("Unable to get gene info file from server");
                System.exit(1);
            }
        }

    }

    /**
     * Convert from gene ID to gene symbol
     * @param id gene ID
     * @return gene symbol
     */
    public Optional<String> getSymbol(String id) {
        return internalDB.parallelStream().filter(e -> e.id.equals(id)).findAny().map(e -> e.symbol);
    }

    /**
     * Find aliases for a given gene symbol
     * @param symbol gene symbol
     * @return list of aliases
     */
    public List<String> getAliases(String symbol) {
        var result = internalDB.parallelStream().filter(e -> e.symbol.equals(symbol)).findAny();
        return result.map(e -> e.synonyms).orElse(Collections.emptyList());
    }

    /**
     * Find a gene symbol for the supplied alias
     * @param alias gene alias
     * @return an optional containing gene symbol
     */
    public Optional<String> getSymbolFromAlias(String alias) {
        var result = internalDB.parallelStream().filter(e -> e.symbol.equalsIgnoreCase(alias)).findAny();
        if (result.isPresent())
            return result.map(gene -> gene.symbol);
        return internalDB.parallelStream()
                .filter(e -> e.synonyms.contains(alias.toUpperCase())).findAny().map(e -> e.symbol);
    }

    /**
     * Find the gene symbol matching the display name for the supplied alias
     * @param alias gene alias
     * @param displayName gene display name (from Reactome DB)
     * @return an optional containing gene symbol
     */
    public Optional<String> getSymbolFromAlias(String alias, String displayName) {
        var result = internalDB.parallelStream().filter(e -> e.symbol.equalsIgnoreCase(alias)).findAny();
        if (result.isPresent())
            return result.map(gene -> gene.symbol);
        return internalDB.parallelStream()
                .filter(e -> e.synonyms.contains(alias.toUpperCase()) && displayName.contains(e.symbol))
                .findAny().map(e -> e.symbol);
    }

    /**
     * Convert from gene symbol (or alias) to gene ID
     * @param symbol gene symbol
     * @return gene ID
     */
    public Optional<String> getId(String symbol) {
        var result = internalDB.parallelStream().filter(e -> e.symbol.equalsIgnoreCase(symbol)).findAny().map(e -> e.id);
        if (result.isPresent())
            return result;
        return internalDB.parallelStream().filter(e -> e.synonyms.contains(symbol.toUpperCase())).findAny().map(e -> e.id);
    }

    /**
     * Get gene description (i.e long name)
     * @param id gene ID
     * @return gene description
     */
    public Optional<String> getDescription(String id) {
        return internalDB.parallelStream().filter(e -> e.id.equals(id)).findAny().map(e -> e.description);
    }

    private static class GeneInfo {
        private final String id;
        private final String symbol;
        private final String description;
        private final List<String> synonyms;

        public GeneInfo(String line) {
            int pos = line.indexOf('\t') + 1;
            int end = line.indexOf('\t', pos);
            this.id = line.substring(pos, end);
            pos = end + 1;
            end = line.indexOf('\t', pos);
            this.symbol = line.substring(pos, end);
            pos = end + 1;
            end = line.indexOf('\t', pos);
            pos = end + 1;
            end = line.indexOf('\t', pos);
            this.synonyms = line.substring(pos, end).equals("-") ? Collections.emptyList() : Arrays.asList(line.substring(pos, end).split("\\|"));
            for (int i = 0; i < 4; i++) {
                pos = end + 1;
                end = line.indexOf('\t', pos);
            }
            this.description = line.substring(pos, end);
        }
    }
}
