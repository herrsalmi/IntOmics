package org.pmoi.models;

public enum ProteomeType {
    LABEL_FREE("LF"),
    LCMS("LCMS");

    public final String label;

    ProteomeType(String label) {
        this.label = label;
    }
}
