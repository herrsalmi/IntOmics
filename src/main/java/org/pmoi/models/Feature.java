package org.pmoi.models;

public abstract class Feature {
    protected String name;
    protected String entrezID;
    protected double fdr;
    protected double foldChange;
    protected String description;

    public Feature(String name, String entrezID, double fdr, double foldChange) {
        this.name = name;
        this.entrezID = entrezID;
        this.fdr = fdr;
        this.foldChange = foldChange;
    }

    public Feature() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEntrezID() {
        return entrezID;
    }

    public void setEntrezID(String entrezID) {
        this.entrezID = entrezID;
    }

    public double getFdr() {
        return fdr;
    }

    public void setFdr(double fdr) {
        this.fdr = fdr;
    }

    public double getFoldChange() {
        return foldChange;
    }

    public void setFoldChange(double foldChange) {
        this.foldChange = foldChange;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
