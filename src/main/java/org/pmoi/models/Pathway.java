package org.pmoi.models;

import java.util.ArrayList;
import java.util.List;

public class Pathway {

    private String pathwayID;
    private String name;
    private List<Gene> genes;

    public Pathway(String pathwayID, String name) {
        this.pathwayID = pathwayID;
        this.name = name;
        this.genes = new ArrayList<>();
    }

    public String getPathwayID() {
        return pathwayID;
    }

    public void setPathwayID(String pathwayID) {
        this.pathwayID = pathwayID;
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

    public void addGene(Gene gene) {
        this.genes.add(gene);
    }
}