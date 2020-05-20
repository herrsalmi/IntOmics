package org.pmoi.business;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SurfaceomeMapper {
    private static SurfaceomeMapper instance;

    private Set<String> internalDB;

    private SurfaceomeMapper() {
        init();
    }

    private void init() {
        internalDB = new HashSet<>(3000);
        try {
            Files.lines(Path.of(Objects.requireNonNull(getClass().getClassLoader().getResource("surfaceome.txt"))
                    .toURI())).forEach(l -> internalDB.add(l.trim()));
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
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
