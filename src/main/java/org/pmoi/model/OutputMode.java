package org.pmoi.model;

public enum OutputMode {
    TSV("tsv"),
    FWF("fwf"),
    HTML("html");

    private String mode;

    OutputMode(String mode) {
        this.mode = mode;
    }

    public String getMode() {
        return mode;
    }
}
