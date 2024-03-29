package org.pmoi.util;

import org.pmoi.model.PathwayResponse;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.LinkedList;
import java.util.List;

public class PathwayResponceHandler extends DefaultHandler {
    private String elementValue;
    private LinkedList<PathwayResponse> pathwayResponses;

    @Override
    public void characters(char[] ch, int start, int length) {
        elementValue += new String(ch, start, length);
    }

    @Override
    public void startDocument() throws SAXException {
        pathwayResponses = new LinkedList<>();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        elementValue = "";
        if (qName.equals("ns1:result") || qName.equals("ns1:pathways"))
            pathwayResponses.add(new PathwayResponse());
    }

    @Override
    public void endElement(String uri, String localName, String qName) {
        if ("ns2:id".equals(qName)) {
            pathwayResponses.getLast().setId(elementValue);
        } else if ("ns2:name".equals(qName)) {
            pathwayResponses.getLast().setName(elementValue.replace("&amp;", "&"));
        }
    }

    public List<PathwayResponse> getPathwayResponses() {
        return pathwayResponses;
    }
}
