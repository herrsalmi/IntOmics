package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.pmoi.MainEntry;
import org.pmoi.model.vis.VisGraph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class GraphVisualizer {

    private static final Logger LOGGER = LogManager.getRootLogger();

    private GraphVisualizer(){}

    public static void makeHTML(VisGraph graph) {
        try {
            InputStream in = GraphVisualizer.class.getResourceAsStream("/main.html");
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            List<String> lines = br.lines().collect(Collectors.toList());
            String data = "setTheData(" + graph.getNodesJson() +  "," + graph.getEdgesJson() + ")";
            lines.set(lines.indexOf("//anchor"), data);
            Files.write(Paths.get(MainEntry.OUT_DIR + "network.html"), lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.error(e);
        }

    }

}
