package org.pmoi.model;

import java.io.Serializable;

public abstract class Feature implements Serializable {
    protected String name;
    protected String ncbiID;
    protected double pvalue;
    protected double foldChange;
    protected String description;

    public Feature(String name, String ncbiID, double pvalue, double foldChange) {
        this.name = name;
        this.ncbiID = ncbiID;
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

    public String getNcbiID() {
        return ncbiID;
    }

    public void setNcbiID(String ncbiID) {
        this.ncbiID = ncbiID;
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
