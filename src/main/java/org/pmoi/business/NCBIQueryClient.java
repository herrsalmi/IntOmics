package org.pmoi.business;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.pmoi.ApplicationParameters;
import org.pmoi.Args;
import org.pmoi.model.Feature;
import org.pmoi.model.Gene;
import org.pmoi.util.HttpConnector;

import javax.xml.XMLConstants;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author A Salmi
 */
public class NCBIQueryClient {

    private static final Logger LOGGER = LogManager.getRootLogger();

    public void geneNameToEntrezID(Feature feature) {
        int counter = 0;
        while (true) {
            try {
                LOGGER.debug("Processing feature: [{}]", feature.getName());
                URL url = new URL(String.format("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?" +
                                "db=gene&term=%s AND Homo sapiens[Organism]&sort=relevance&%s",
                        feature.getName(), Args.getInstance().getNcbiAPIKey()));
                SAXBuilder saxBuilder = new SAXBuilder();
                saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
                Document document = saxBuilder.build(url);
                Element ncbiId = document.getRootElement().getChild("IdList");
                Optional<Element> id = ncbiId.getChildren().stream().findFirst();
                feature.setEntrezID(id.map(Element::getText).orElse(null));
                return;

            } catch (JDOMException | IOException e) {
                LOGGER.debug(String.format("Error getting ID. %s. Feature: [%s]. Retrying (%d/%d)", e.getMessage(),
                        feature.getName(), counter + 1, ApplicationParameters.getInstance().getMaxTries()));
                if (++counter == ApplicationParameters.getInstance().getMaxTries()) {
                    LOGGER.error(String.format("Error getting ID for feature: [%s]. Aborting!", feature.getName()));
                    return;
                }
            }
        }
    }

    public void entrezIDToGeneName(Feature feature) {
        int counter = 0;
        while (true) {
            try {
                LOGGER.debug("Processing feature: [{}]", feature.getEntrezID());
                URL url = new URL(String.format("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?" +
                                "db=gene&id=%s&retmode=text&%s",
                        feature.getEntrezID(), Args.getInstance().getNcbiAPIKey()));
                HttpConnector httpConnector = new HttpConnector();
                String ncbiResultContent = httpConnector.getContent(url);
                // the first line always contains the gene name following the format: 1. NAME\n
                ncbiResultContent = ncbiResultContent.split("\n")[1];
                feature.setName(ncbiResultContent.substring(3));
                return;
            } catch (IOException e) {
                LOGGER.warn(String.format("Unknown error when getting feature name. Feature: [%s]. Retrying (%d/%d)",
                        feature.getEntrezID(), counter + 1, ApplicationParameters.getInstance().getMaxTries()));
                if (++counter == ApplicationParameters.getInstance().getMaxTries()) {
                    LOGGER.error(String.format("%nError getting name for feature: [%s]. Aborting!",
                            feature.getEntrezID()));
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

    public String geneNameToEntrezID(String name) {
        Gene gene = new Gene(name, "");
        geneNameToEntrezID(gene);
        return gene.getEntrezID();
    }

    public String fetchDescription(String id) {
        int counter = 0;
        while (true) {
            try {
                URL url = new URL(String.format("https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?" +
                                "db=gene&id=%s&retmode=text&%s", id, Args.getInstance().getNcbiAPIKey()));
                HttpConnector httpConnector = new HttpConnector();
                String ncbiResultContent = httpConnector.getContent(url);
                Pattern pattern = Pattern.compile("Name: (.*)(?= \\[)");
                Matcher matcher = pattern.matcher(ncbiResultContent);
                if (matcher.find()) {
                    return matcher.group(1);
                }
                return null;
            } catch (IOException e) {
                if (++counter == ApplicationParameters.getInstance().getMaxTries()) {
                    return null;
                }
            }
        }
    }
}
