package org.pmoi.model;

public abstract class Feature {
    protected String name;
    protected String entrezID;
    protected double pvalue;
    protected double foldChange;
    protected String description;

    public Feature(String name, String entrezID, double pvalue, double foldChange) {
        this.name = name;
        this.entrezID = entrezID;
        this.pvalue = pvalue;
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

    public double getPvalue() {
        return pvalue;
    }

    public void setPvalue(double pvalue) {
        this.pvalue = pvalue;
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
