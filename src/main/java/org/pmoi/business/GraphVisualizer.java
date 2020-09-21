package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.MainEntry;
import org.pmoi.model.vis.VisGraph;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class GraphVisualizer {

    private static final Logger LOGGER = LogManager.getRootLogger();

    private GraphVisualizer(){}

    public static void makeHTML(VisGraph graph) {
        try {
            Path path = Path.of(GraphVisualizer.class.getClassLoader().getResource("main.html").toURI());
            List<String> lines = Files.readAllLines(path);
            String data = "setTheData(" + graph.getNodesJson() +  "," + graph.getEdgesJson() + ")";
            lines.set(lines.indexOf("//anchor"), data);
            Files.write(Paths.get(MainEntry.OUT_DIR + "network.html"), lines, StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            LOGGER.error(e);
        }

    }

}
