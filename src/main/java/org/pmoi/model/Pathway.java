package org.pmoi.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Pathway implements Serializable {

    private String pathwayID;
    private String name;
    private List<Gene> genes;

    public Pathway(String pathwayID, String name) {
        this.pathwayID = pathwayID;
        this.name = name;
        this.genes = new ArrayList<>();
    }

    public Pathway(String name, List<Gene> genes) {
        this.name = name;
        this.genes = genes;
    }

    public Pathway(String pathwayID, String name, List<Gene> genes) {
        this.pathwayID = pathwayID;
        this.name = name;
        this.genes = genes;
    }

    public String getPathwayID() {
        return pathwayID;
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

    @Override
    public String toString() {
        return "Pathway{" +
                "name='" + name + '\'' +
                ", genes=" + genes +
                '}';
    }
}
