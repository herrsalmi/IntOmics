package org.pmoi.models;

import java.util.Objects;

public class Gene extends Feature{

    public Gene(String line) {
        String[] info = line.split(";");
        this.name = info[0];
        this.fdr = info[1];
        this.foldChange = info[2];
    }

    public Gene(int entrezID) {
        this.entrezID = String.valueOf(entrezID);
    }

    public Gene(String name, String entrezID) {
        this.name = name;
        this.entrezID = entrezID;
    }

    @Override
    public String toString() {
        return String.format("[%s : %s]", this.getName(), this.getEntrezID());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Gene gene = (Gene) o;
        return name.equals(gene.name) &&
                Objects.equals(entrezID, gene.entrezID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, entrezID);
    }
}
