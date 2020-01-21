package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.pmoi.ApplicationParameters;
import org.pmoi.handler.HttpConnector;
import org.pmoi.models.Gene;

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
                if (++counter == ApplicationParameters.getInstance().getMaxTries()) {
                    LOGGER.error(String.format("Error getting pathway for: [%s - %s]. Aborting!", protein, gene));
                    return null;
                }
            }
        }
    }

    public List<String> KEGGSearch(String geneId) {
        URL url = null;
        try {
            url = new URL(String.format("http://rest.kegg.jp/get/hsa:%s/", geneId));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return getListResults(url, " +(hsa[0-9]+ .+)(?=\\n)");
    }

    public List<Gene> getPathwayGenes(String pathwayID) {
        URL url = null;
        try {
            url = new URL(String.format("http://rest.kegg.jp/get/%s/", pathwayID));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return Objects.requireNonNull(getListResults(url, "[0-9]+ {2}(.+)(?=;)")).stream()
                .map(e -> new Gene(e, ""))
                .collect(Collectors.toList());
    }

    private List<String> getListResults(URL url, String expression) {
        int counter = 0;
        while (true) {
            try {
                HttpConnector httpConnector = new HttpConnector();
                String result = httpConnector.getContent(url);
                Pattern pattern = Pattern.compile(expression);
                Matcher matcher = pattern.matcher(result);
                List<String> resultList = new ArrayList<>();
                while (matcher.find()) {
                    resultList.add(matcher.group(1).trim());
                }
                return resultList;
            } catch (IOException e) {
                if (++counter == ApplicationParameters.getInstance().getMaxTries()) {
                    LOGGER.error(String.format("Error getting KEGG. URL: [%s]. Aborting!", url.getPath()));
                    return null;
                }
            }
        }
    }

}
