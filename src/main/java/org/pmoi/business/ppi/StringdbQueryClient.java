package org.pmoi.business.ppi;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.pmoi.Args;
import org.pmoi.database.SpeciesManager;
import org.pmoi.util.HttpConnector;

import javax.xml.XMLConstants;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * project FunctionalAnalysis
 * Created by ayyoub on 3/12/18.
 */
public class StringdbQueryClient implements InteractionQueryClient{

    private static final Logger LOGGER = LogManager.getRootLogger();
    private static int taxonomyId;

    public StringdbQueryClient() {
        if (taxonomyId == 0) {
            taxonomyId = SpeciesManager.get().getTaxonomyId();
        }
    }

    /**
     * Returns a map representing the PPI network. Keys are gene names, values are interaction score
     * @param symbol gene name
     * @return map of [gene name : score]
     */
    @Override
    public Map<String, String> getProteinNetwork(String symbol) {
        LOGGER.debug("Searching StringDB for gene {}", symbol);
        String url = String.format("https://string-db.org/api/xml/interaction_partners?species=%d&required_score=%s&identifiers=%s",
                taxonomyId, Args.getInstance().getStringDBScore(), symbol);
        Map<String, String> map = new HashMap<>();
        // see if there is an entry in StringDB for the gene
        URLConnection connection;
        try {
            connection = new URL(url).openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == 500) {
                LOGGER.warn("No entry in StringDB for GENE:{}", symbol);
                return map;
            }
        } catch (IOException ignored) {
            // no need to handle the exception
        }

        int count = 0;

        while (true) {
            try {
                SAXBuilder saxBuilder = new SAXBuilder();
                saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
                Document document = saxBuilder.build(url);
                List<Element> el = document.getRootElement().getChildren();
                if (el != null && !el.isEmpty()) {
                    el.forEach(l -> map.put(l.getChildText("preferredName_B"), l.getChildText("score")));
                }
                return map;
            } catch (JDOMException | IOException e) {
                LOGGER.warn(String.format("Network I/O error while connecting to StringDB. GENE:%s. Retrying ... (%d/%d)",
                        symbol, ++count, HttpConnector.MAX_TRIES));
                if (count == HttpConnector.MAX_TRIES) {
                    LOGGER.error(String.format("StringDB data not retrieved for GENE:%s", symbol));
                    return map;
                }
            }
        }
    }
}
