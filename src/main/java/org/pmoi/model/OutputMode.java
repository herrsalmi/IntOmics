package org.pmoi.model;

public enum OutputMode {
    TSV("TSV"),
    FWF("FWF");

    private String mode;

    OutputMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }
}
