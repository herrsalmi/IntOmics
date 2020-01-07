package org.pmoi.models;

import java.util.Objects;

public class Gene {

    private String geneName;
    private String geneEntrezID;

    public Gene(String geneName) {
        this.geneName = geneName;
    }

    public Gene(String geneName, String geneEntrezID) {
        this.geneName = geneName;
        this.geneEntrezID = geneEntrezID;
    }

    public String getGeneName() {
        return geneName;
    }

    public void setGeneName(String geneName) {
        this.geneName = geneName;
    }

    public String getGeneEntrezID() {
        return geneEntrezID;
    }

    public void setGeneEntrezID(String geneEntrezID) {
        this.geneEntrezID = geneEntrezID;
    }

    @Override
    public String toString() {
        return String.format("[%s : %s]", this.getGeneName(), this.getGeneEntrezID());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Gene gene = (Gene) o;
        return geneName.equals(gene.geneName) &&
                Objects.equals(geneEntrezID, gene.geneEntrezID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(geneName, geneEntrezID);
    }
}
