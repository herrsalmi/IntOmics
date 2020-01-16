package org.pmoi.models;

public abstract class Feature {
    protected String name;
    protected String entrezID;
    protected String fdr;
    protected String foldChange;
    protected String description;

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

    public String getFdr() {
        return fdr;
    }

    public void setFdr(String fdr) {
        this.fdr = fdr;
    }

    public String getFoldChange() {
        return foldChange;
    }

    public void setFoldChange(String foldChange) {
        this.foldChange = foldChange;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
