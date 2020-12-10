package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.Args;
import org.pmoi.model.Gene;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

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
     * when dealing with human data, the human surfaceome data is used to look for surface proteins.
     * when dealing with other species, surface proteins are defined using GO terms
     */
    private void init() {
        switch (Args.getInstance().getSpecies()) {
            case HUMAN -> {
                internalDB = new HashSet<>(3000);
                try (InputStream in = getClass().getResourceAsStream("/surfaceome.txt")) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(in));
                    var stream = br.lines();
                    stream.forEach(l -> internalDB.add(l.trim()));
                } catch (IOException e) {
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
