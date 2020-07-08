package org.pmoi.model;

import com.google.common.math.DoubleMath;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Gene extends Feature implements Comparable<Gene>, Serializable {

    private List<GeneSet> geneSets;

    public Gene(Gene gene) {
        this.name = gene.name;
        this.entrezID = gene.entrezID;
        this.fdr = gene.fdr;
        this.foldChange = gene.foldChange;
        this.geneSets = new ArrayList<>();
    }

    public Gene(String line) {
        String[] info = line.split(";");
        this.name = info[0];
        this.fdr = Double.parseDouble(info[1].replace(",", "."));
        this.foldChange = Double.parseDouble(info[2].replace(",", "."));
        geneSets = new ArrayList<>();
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
        this.geneSets = new ArrayList<>();
    }

    public List<GeneSet> getGeneSets() {
        return geneSets;
    }

    public void setInteractors(String name, List<Gene> interactors) {
        this.geneSets.add(new GeneSet(name, interactors));
    }

    public double significanceScore() {
        return Math.signum(foldChange) * DoubleMath.log2(Math.abs(foldChange)) * (-Math.log10(fdr));
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
    public int compareTo(Gene o) {
        return Double.compare(this.getFoldChange(), o.getFoldChange());
    }
}