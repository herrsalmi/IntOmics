package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.Args;
import org.pmoi.model.Gene;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class SurfaceomeMapper {
    private static SurfaceomeMapper instance;

    private static final Logger LOGGER = LogManager.getRootLogger();
    private Set<String> internalDB;
    private Predicate<Gene> surfaceomePred;

    private SurfaceomeMapper() {
        init();
    }

    /**
     * Load surface proteins from file into local object
     */
    private void init() {
        switch (Args.getInstance().getSpecies()) {
            case HUMAN -> {
                internalDB = new HashSet<>(3000);
                try (Stream<String> stream = Files.lines(Path.of(getClass().getClassLoader().getResource("surfaceome.txt")
                        .toURI()))) {
                    stream.forEach(l -> internalDB.add(l.trim()));
                } catch (IOException | URISyntaxException e) {
                    LOGGER.error(e);
                }
                surfaceomePred = gene -> internalDB.contains(gene.getName());
            }
            case MOUSE, RAT, COW -> {
                GeneOntologyMapper goMapper = GeneOntologyMapper.getInstance();
                surfaceomePred = gene -> goMapper.checkMembranomeGO(gene.getNcbiID());
            }
        }

    }

    /**
     * Check if the gene product is a surface protein
     * @param gene gene
     * @return true if the gene product is a surface protein
     */
    public boolean isSurfaceProtein(Gene gene) {
        return surfaceomePred.test(gene);
    }

    public static synchronized SurfaceomeMapper getInstance() {
        if (instance == null)
            instance = new SurfaceomeMapper();
        return instance;
    }
}
