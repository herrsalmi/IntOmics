package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.pmoi.MainEntry;
import org.pmoi.handler.HttpConnector;
import org.pmoi.models.Gene;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author A Salmi
 */
public class NCBIQueryClient {
    private static final Logger LOGGER = LogManager.getRootLogger();

    public String geneNameToEntrezID(Gene gene) {
        int counter = 0;
        int max = 10;
        while (true) {
            try {
                LOGGER.info(String.format("Processing gene: [%s]", gene.getGeneName()));
                URL url = new URL(String.format("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=gene&term=%s&sort=relevance&api_key=%s", gene.getGeneName(), MainEntry.NCBI_API_KEY));
                SAXBuilder saxBuilder = new SAXBuilder();
                Document document = saxBuilder.build(url);
                Element ncbiId = document.getRootElement().getChild("IdList");
                Optional<Element> id = ncbiId.getChildren().stream().findFirst();
                gene.setGeneEntrezID(id.map(Element::getText).orElse(null));
                return gene.getGeneEntrezID();

            } catch (JDOMException | IOException e) {
                LOGGER.warn(String.format("Unknown error when getting ID. Gene: [%s]. Retrying (%d/%d)", gene.getGeneName(), counter + 1, max));
                if (++counter == max) {
                    LOGGER.error(String.format("Error getting ID for Gene: [%s]. Aborting!", gene.getGeneName()));
                    return null;
                }
            }
        }
    }

    public void entrezIDToGeneName(Gene gene) {
        int counter = 0;
        int max = 10;
        while (true) {
            try {
                LOGGER.info(String.format("Processing gene: [%s]", gene.getGeneEntrezID()));
                URL url = new URL(String.format("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=gene&id=%s&retmode=text&api_key=%s", gene.getGeneEntrezID(), MainEntry.NCBI_API_KEY));
                HttpConnector httpConnector = new HttpConnector();
                String ncbiResultContent = httpConnector.getContent(url);
                // the first line always contains the gene name following the format: 1. NAME\n
                //TODO replace this using regex
                ncbiResultContent = ncbiResultContent.split("\n")[1];
                gene.setGeneName(ncbiResultContent.substring(3));
                return;
            } catch (IOException e) {
                LOGGER.warn(String.format("Unknown error when getting gene name. Gene: [%s]. Retrying (%d/%d)", gene.getGeneEntrezID(), counter + 1, max));
                if (++counter == max) {
                    LOGGER.error(String.format("Error getting name for Gene: [%s]. Aborting!", gene.getGeneEntrezID()));
                    return;
                }
            }
        }
    }

}
