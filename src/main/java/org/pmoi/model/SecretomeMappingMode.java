package org.pmoi.model;

public enum SecretomeMappingMode {
    METAZSECKB("DB"),
    GOTERM("GO");

    public final String label;

    SecretomeMappingMode(String label) {
        this.label = label;
    }
}
