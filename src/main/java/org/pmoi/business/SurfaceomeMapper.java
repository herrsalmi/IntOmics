package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class SurfaceomeMapper {
    private static SurfaceomeMapper instance;

    private static final Logger LOGGER = LogManager.getRootLogger();
    private Set<String> internalDB;

    private SurfaceomeMapper() {
        init();
    }

    private void init() {
        internalDB = new HashSet<>(3000);
        try (Stream<String> stream = Files.lines(Path.of(getClass().getClassLoader().getResource("surfaceome.txt")
                .toURI()))){
            stream.forEach(l -> internalDB.add(l.trim()));
        } catch (IOException | URISyntaxException e) {
            LOGGER.error(e);
        }
    }

    public boolean isSurfaceProtein(String geneName) {
        return internalDB.contains(geneName);
    }

    public static synchronized SurfaceomeMapper getInstance() {
        if (instance == null)
            instance = new SurfaceomeMapper();
        return instance;
    }
}
