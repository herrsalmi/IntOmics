package org.pmoi.model;

public enum PathwayMode {
    KEGG ("KEGG"),
    WIKIPATHWAYS ("WIKIPATHWAYS"),
    REACTOME("REACTOME");

    private final String name;

    PathwayMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
