package org.pmoi.model;

public enum PathwayMode {
    KEGG ("KEGG"),
    WIKIPATHWAYS ("WikiPathways");

    private final String name;

    PathwayMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
