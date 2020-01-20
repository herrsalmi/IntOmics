package org.pmoi.models;

import java.util.*;

public class Gene extends Feature{

    private Map<String, List<Gene>> interactors;

    public Gene(String line) {
        String[] info = line.split(";");
        this.name = info[0];
        this.fdr = Double.parseDouble(info[1].replace(",", "."));
        this.foldChange = Double.parseDouble(info[2].replace(",", "."));
        interactors = new HashMap<>();
    }

    public Gene(int entrezID) {
        this.entrezID = String.valueOf(entrezID);
    }

    public Gene(String name, String entrezID) {
        this.name = name;
        this.entrezID = entrezID;
    }

    public Gene(String name, String entrezID, double fdr, double fc) {
        super(name, entrezID, fdr, fc);
        this.interactors = new HashMap<>();
    }

    public Map<String, List<Gene>> getInteractors() {
        return interactors;
    }

    public void setInteractors(String name, List<Gene> interactors) {
        this.interactors.put(name, interactors);
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
        return name.equals(gene.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public Object clone() {
        try {
            return (Gene) super.clone();
        } catch (CloneNotSupportedException e) {
            return new Gene(this.name, this.entrezID, this.fdr, this.foldChange);
        }
    }
}
