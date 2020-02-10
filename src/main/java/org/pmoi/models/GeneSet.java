package org.pmoi.models;

import java.util.List;

public class GeneSet {
    private String name;
    private List<Gene> genes;
    private double pvalue;
    private double score;

    public GeneSet(String name, List<Gene> genes) {
        this.name = name;
        this.genes = genes;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Gene> getGenes() {
        return genes;
    }

    public void setGenes(List<Gene> genes) {
        this.genes = genes;
    }

    public double getPvalue() {
        return pvalue;
    }

    public void setPvalue(double pvalue) {
        this.pvalue = pvalue;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
