package org.pmoi.models;

public abstract class Feature {
    protected String name;
    protected String entrezID;

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
}
