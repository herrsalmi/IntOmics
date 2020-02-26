package org.pmoi.utils;

import org.pmoi.models.PathwayResponse;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.LinkedList;
import java.util.List;

public class PathwayResponceHandler extends DefaultHandler {
    private String elementValue;
    private LinkedList<PathwayResponse> pathwayResponses;

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        elementValue = new String(ch, start, length);
    }

    @Override
    public void startDocument() throws SAXException {
        pathwayResponses = new LinkedList<>();
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equals("ns1:result"))
            pathwayResponses.add(new PathwayResponse());
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (qName) {
            case "ns2:id":
                pathwayResponses.getLast().setId(elementValue);
                break;
            case "ns2:name":
                pathwayResponses.getLast().setName(elementValue);
                break;
        }
    }

    public List<PathwayResponse> getPathwayResponses() {
        return pathwayResponses;
    }
}