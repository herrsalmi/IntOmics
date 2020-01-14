package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.pmoi.MainEntry;
import org.pmoi.handler.HttpConnector;
import org.pmoi.models.Feature;
import org.pmoi.models.Gene;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

/**
 * @author A Salmi
 */
public class NCBIQueryClient {
    private static final Logger LOGGER = LogManager.getRootLogger();

    public String geneNameToEntrezID(Feature feature) {
        int counter = 0;
        while (true) {
            try {
                LOGGER.info(String.format("Processing feature: [%s]", feature.getName()));
                URL url = new URL(String.format("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gene&term=%s AND Homo sapiens[Organism]&sort=relevance&api_key=%s", feature.getName(), MainEntry.NCBI_API_KEY));
                SAXBuilder saxBuilder = new SAXBuilder();
                Document document = saxBuilder.build(url);
                Element ncbiId = document.getRootElement().getChild("IdList");
                Optional<Element> id = ncbiId.getChildren().stream().findFirst();
                feature.setEntrezID(id.map(Element::getText).orElse(null));
                return feature.getEntrezID();

            } catch (JDOMException | IOException e) {
                LOGGER.warn(String.format("Unknown error when getting ID. Feature: [%s]. Retrying (%d/%d)", feature.getName(), counter + 1, MainEntry.MAX_TRIES));
                if (++counter == MainEntry.MAX_TRIES) {
                    LOGGER.error(String.format("Error getting ID for feature: [%s]. Aborting!", feature.getName()));
                    return null;
                }
            }
        }
    }

    public void entrezIDToGeneName(Feature feature) {
        int counter = 0;
        while (true) {
            try {
                LOGGER.info(String.format("Processing feature: [%s]", feature.getEntrezID()));
                URL url = new URL(String.format("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=gene&id=%s&retmode=text&api_key=%s", feature.getEntrezID(), MainEntry.NCBI_API_KEY));
                HttpConnector httpConnector = new HttpConnector();
                String ncbiResultContent = httpConnector.getContent(url);
                // the first line always contains the gene name following the format: 1. NAME\n
                //TODO replace this using regex
                ncbiResultContent = ncbiResultContent.split("\n")[1];
                feature.setName(ncbiResultContent.substring(3));
                return;
            } catch (IOException e) {
                LOGGER.warn(String.format("Unknown error when getting feature name. Feature: [%s]. Retrying (%d/%d)", feature.getEntrezID(), counter + 1, MainEntry.MAX_TRIES));
                if (++counter == MainEntry.MAX_TRIES) {
                    LOGGER.error(String.format("Error getting name for feature: [%s]. Aborting!", feature.getEntrezID()));
                    return;
                }
            }
        }
    }

    public String entrezIDToGeneName(String id) {
        Gene gene = new Gene("", id);
        entrezIDToGeneName(gene);
        return gene.getName();
    }
}
