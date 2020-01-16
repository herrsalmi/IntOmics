package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.pmoi.MainEntry;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class PathwayClient {
    private static final Logger LOGGER = LogManager.getRootLogger();

    public byte[] getPathway(String protein, String gene) {
        int counter = 0;
        while (true) {
            try {
                URL url = new URL(String.format("http://webservice.wikipathways.org/findPathwaysByText?query=%s AND %s", protein, gene));
                SAXBuilder saxBuilder = new SAXBuilder();
                Document document = saxBuilder.build(url);
                Optional<Element> pathwayResult = document.getRootElement().getChildren().stream().findFirst();
                AtomicReference<String> pathwayId = new AtomicReference<>();
                if (pathwayResult.isPresent()) {
                    pathwayResult.get().getChildren().forEach(c -> {
                        if (c.getName().equals("id")) {
                            pathwayId.set(c.getText());
                        }
                    });
                } else
                    return null;
                url = new URL(String.format("http://webservice.wikipathways.org/getPathwayAs?fileType=svg&pwId=%s&revision=0", pathwayId.get()));
                document = saxBuilder.build(url);
                return DatatypeConverter.parseBase64Binary(document.getRootElement().getChildren().stream().findFirst().get().getText());
            } catch (IOException | JDOMException e) {
                if (++counter == MainEntry.MAX_TRIES) {
                    LOGGER.error(String.format("Error getting pathway for: [%s - %s]. Aborting!", protein, gene));
                    return null;
                }
            }
        }
    }
}
